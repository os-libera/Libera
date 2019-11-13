/*
 *
 *  ******************************************************************************
 *   Copyright 2019 Korea University & Open Networking Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *   ******************************************************************************
 *   Developed by Libera team, Operating Systems Lab of Korea University
 *   ******************************************************************************
 *
 */
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.core.LiberaController;
import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.address.PhysicalIPAddress;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;

import java.nio.ByteBuffer;
import java.util.*;

import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkField;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.elements.OVXmodes.OVXmodeHandler;
import net.onrc.openvirtex.exceptions.*;
import net.onrc.openvirtex.packet.*;
import net.onrc.openvirtex.services.vm.Migration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;

public class OVXPacketIn extends OVXMessage implements Virtualizable {

    private final Logger log = LogManager.getLogger(OVXPacketIn.class.getName());
    private PhysicalPort port = null;
    private OVXPort ovxPort = null;
    private Integer tenantId = null;


    public OVXPacketIn(final OVXPacketIn pktIn) {
        super(pktIn.getOFMessage().createBuilder().build());
    }

    public OVXPacketIn(OFMessage msg) {
        super(msg);
    }

    public OVXPacketIn(final byte[] data, final short portNumber, OFVersion ofVersion) {
        super(null);

        OFFactory factory = OFFactories.getFactory(ofVersion);

        if (ofVersion == OFVersion.OF_10) {
            this.setOFMessage(factory.buildPacketIn()
                    .setInPort(OFPort.of(portNumber))
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setReason(OFPacketInReason.NO_MATCH)
                    .setData(data)
                    .build()
            );
        } else {
            Match match = factory.buildMatch()
                    .setExact(MatchField.IN_PORT, OFPort.of(portNumber))
                    .build();

            this.setOFMessage(factory.buildPacketIn()
                    .setMatch(match)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setReason(OFPacketInReason.NO_MATCH)
                    .setData(data)
                    .build()
            );
        }
    }

    public OFPacketIn getPacketIn() {
        return (OFPacketIn) this.getOFMessage();
    }

    public void setInport(short inport) {
        if (this.getOFMessage().getVersion() == OFVersion.OF_10) {
            this.setOFMessage(this.getPacketIn().createBuilder()
                    .setInPort(OFPort.of(inport))
                    .build()
            );
        } else {
            Match temp = this.getPacketIn().getMatch();
            temp = OVXMessageUtil.updateMatch(temp, temp.createBuilder()
                    .setExact(MatchField.IN_PORT, OFPort.of(inport))
                    .build());

            this.setOFMessage(
                    this.getPacketIn().createBuilder().setMatch(temp).build()
            );
        }
    }

    @Override
    public void virtualize(PhysicalSwitch sw) {
        //this.log.info("virtualize");
        //this.log.info(HexString.toHexString(this.getPacketIn().getData()));

        //OF_1.3? ?? PACKET_IN?? ?? ???? Ethernet Trailer? ??? ??? ? ??? OVX?
        // ONOS? ???? ONOS? ???? ?? ???? ???? UDP? length? ???? PACKET_OUT?? ?????.
        // ??? checksum? ??? ?? ??? Destination?? Receive? ?? ??? ??? ???.
        // ??? ????? Data?? Trailer? ???? ??? ????
        Ethernet eth2 = new Ethernet();
        eth2.deserialize(this.getPacketIn().getData(), 0, this.getPacketIn().getData().length);

        if (eth2.getPayload() instanceof IPv4) {
            IPv4 ip = (IPv4) eth2.getPayload();
            if (ip.isTruncated()) {
                ByteBuffer bb = ByteBuffer.wrap(this.getPacketIn().getData());
                byte[] temp = new byte[bb.limit() - ip.getTrimLength()];
                bb.get(temp, 0, temp.length);
                this.setOFMessage(this.getPacketIn().createBuilder().setData(temp).build());
            }
        }

        OVXSwitch vSwitch = OVXMessageUtil.untranslateXid(this, sw);
        /*
         * Fetching port from the physical switch
         */

        short inport;// = ((OFPacketIn) this.getOFMessage()).getInPort().getShortPortNumber();
        if (this.getOFMessage().getVersion() == OFVersion.OF_10) {
            inport = this.getPacketIn().getInPort().getShortPortNumber();
        } else {
            if (this.getPacketIn().getMatch().get(MatchField.IN_PORT) != null)
                inport = this.getPacketIn().getMatch().get(MatchField.IN_PORT).getShortPortNumber();
            else
                inport = 0;
        }
        //this.log.info("inport = " + inport);

        port = sw.getPort(inport);

        Mappable map = sw.getMap();
        Match match;

        match = OVXMessageUtil.loadFromPacket(
                this.getPacketIn().getData(),
                inport,
                sw.getOfVersion());


        if (OVXmodeHandler.getOVXmode() == 1) {

            if (match == null)
                return;

//        this.log.info("Match " + match.toString());

            if (match.get(MatchField.ETH_TYPE) == EthType.IPv4 && match.get(MatchField.ETH_DST) == MacAddress.of("ff:ff:ff:ff:ff:ff")) {
//            log.info("IPv4 Packets " + this.getPacketIn().toString());
                //log.info("IPv4 Packets " + HexString.toHexString(this.getPacketIn().getData()));
                //log.info(match.toString());
                return;
            }
            this.log.info("virtualize");
        }

        this.log.debug("Match " + match.toString());

        if (this.port.isEdge()) {

            this.tenantId = this.fetchTenantId(match, map, true);
            if (this.tenantId == null) {
              //  this.log.info(
                 //       "PacketIn {} does not belong to any virtual network; "
                //                + "dropping and installing a temporary drop rule. Tenantid={}",
                 //       this, tenantId);
                this.installDropRule(sw, match);
                return;
            }
            /*
             * Checks on vSwitch and the virtual port done in senndPkt.
             */
            vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);
            this.ovxPort = this.port.getOVXPort(this.tenantId, 0);
            this.sendPkt(vSwitch, match, sw);
            this.learnHostIP(match, map);
            if (OVXmodeHandler.getOVXmode() == 1) {
                this.learnAddresses(match, vSwitch);
            } else {
                this.learnAddresses(match);
            }
            this.log.debug("Edge PacketIn {} sent to virtual network {}", this.getOFMessage().toString(),
                    this.tenantId);
            return;
        }


        if (OVXmodeHandler.getOVXmode() == 1) {
            if (!this.port.isEdge()) {
                if (match.get(MatchField.ETH_TYPE) == EthType.IPv4 && match.get(MatchField.ETH_DST) != MacAddress.of("ff:ff:ff:ff:ff:ff")) {
                    log.info("this port is not edge");
                    log.info(match.toString());
                }
            }
        }

        if (OVXmodeHandler.getOVXmode() == 0) {
            log.info("for core packets in OVX default mode");
            log.info("match info for core switches in OVX default mode = {}", match.toString());
            if (match.get(MatchField.ETH_TYPE) == EthType.IPv4
                    || match.get(MatchField.ETH_TYPE) == EthType.ARP) {

                PhysicalIPAddress srcIP = new PhysicalIPAddress(
                        match.get(MatchField.IPV4_SRC).getInt()
                );

                PhysicalIPAddress dstIP = new PhysicalIPAddress(
                        match.get(MatchField.IPV4_DST).getInt()
                );

                Ethernet eth = new Ethernet();
                eth.deserialize(this.getPacketIn().getData(), 0, this.getPacketIn().getData().length);

                OVXLinkUtils lUtils = new OVXLinkUtils(eth.getSourceMAC(),
                        eth.getDestinationMAC());

                if (lUtils.isValid()) {
                    OVXPort srcPort = port.getOVXPort(lUtils.getTenantId(),
                            lUtils.getLinkId());

                    if (srcPort == null) {
                        this.log.debug(
                                "Virtual Src Port Unknown: {}, port {} with this match {}; dropping packet",
                                sw.getName(), match.get(MatchField.IN_PORT).getShortPortNumber(), match);
                        return;
                    }

                    this.setInport(srcPort.getPortNumber());

                    OVXLink link;
                    try {
                        OVXPort dstPort = map.getVirtualNetwork(
                                lUtils.getTenantId()).getNeighborPort(srcPort);
                        link = map.getVirtualSwitch(sw, lUtils.getTenantId())
                                .getMap().getVirtualNetwork(lUtils.getTenantId())
                                .getLink(dstPort, srcPort);
                    } catch (SwitchMappingException | NetworkMappingException e) {
                        return; // same as (link == null)
                    }
                    this.ovxPort = this.port.getOVXPort(lUtils.getTenantId(),
                            link.getLinkId());
                    OVXLinkField linkField = LiberaController.getInstance()
                            .getOvxLinkField();
                    // TODO: Need to check that the values in linkId and flowId
                    // don't exceed their space
                    if (linkField == OVXLinkField.MAC_ADDRESS) {
                        try {
                            LinkedList<MacAddress> macList = sw.getMap()
                                    .getVirtualNetwork(this.ovxPort.getTenantId())
                                    .getFlowManager()
                                    .getFlowValues(lUtils.getFlowId());
                            eth.setSourceMACAddress(macList.get(0).getBytes())
                                    .setDestinationMACAddress(macList.get(1).getBytes());
                            match = OVXMessageUtil.updateMatch(match, match.createBuilder()
                                    .setExact(MatchField.ETH_SRC, eth.getSourceMAC())
                                    .setExact(MatchField.ETH_DST, eth.getDestinationMAC())
                                    .build()
                            );

                        } catch (NetworkMappingException e) {
                            log.warn(e);
                        }
                    } else if (linkField == OVXLinkField.VLAN) {
                        // TODO
                        log.warn("VLAN virtual links not yet implemented.");
                        return;
                    }
                }

                if (match.get(MatchField.ETH_TYPE) == EthType.ARP) {
                    // ARP packet
                    final ARP arp = (ARP) eth.getPayload();
                    this.tenantId = this.fetchTenantId(match, map, true);
                    try {
                        if (map.hasVirtualIP(srcIP)) {
                            arp.setSenderProtocolAddress(map.getVirtualIP(srcIP)
                                    .getIp());
                        }
                        if (map.hasVirtualIP(dstIP)) {
                            arp.setTargetProtocolAddress(map.getVirtualIP(dstIP)
                                    .getIp());
                        }
                    } catch (AddressMappingException e) {
                        log.warn("Inconsistency in OVXMap? : {}", e);
                    }
                } else if (match.get(MatchField.ETH_TYPE) == EthType.IPv4) {
                    try {
                        final IPv4 ip = (IPv4) eth.getPayload();
                        ip.setDestinationAddress(map.getVirtualIP(dstIP).getIp());
                        ip.setSourceAddress(map.getVirtualIP(srcIP).getIp());
                        // TODO: Incorporate below into fetchTenantId
                        if (this.tenantId == null) {
                            this.tenantId = dstIP.getTenantId();
                        }
                    } catch (AddressMappingException e) {
                        log.warn("Could not rewrite IP fields : {}", e);
                    }
                } else {
                    this.log.info("{} handling not yet implemented; dropping",
                            match.get(MatchField.ETH_TYPE).toString());
                    this.installDropRule(sw, match);
                    return;
                }

                this.setOFMessage(this.getPacketIn().createBuilder()
                        .setData(eth.serialize())
                        .build()
                );
                vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);
                this.sendPkt(vSwitch, match, sw);
                this.log.info("IPv4 PacketIn {} sent to virtual network {}", this,
                        this.tenantId);
                return;
            }

            this.tenantId = this.fetchTenantId(match, map, true);
            if (this.tenantId == null) {
                this.log.debug(
                        "PacketIn {} does not belong to any virtual network; "
                                + "dropping and installing a temporary drop rule",
                        this);
                this.installDropRule(sw, match);
                return;
            }
            vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);
            this.sendPkt(vSwitch, match, sw);
            this.log.info("Layer2 PacketIn {} sent to virtual network {}", this.getOFMessage(),
                    this.tenantId);
        }

        if (OVXmodeHandler.getOVXmode() == 2) {
            //Aggflow mode: /*
         /* Below handles packets traveling in the core.
                    *
         *
         * The idea here si to rewrite the packets such that the controller is
                    * able to recognize them.
                    *
         * For IPv4 packets and ARP packets this means rewriting the IP fields
                    * and possibly the mac address fields if these packets are at the
                    * egress point of a virtual link.
                    */
            if (match.get(MatchField.ETH_TYPE) == EthType.IPv4
                    || match.get(MatchField.ETH_TYPE) == EthType.ARP) {

                Ethernet eth = new Ethernet();
                eth.deserialize(this.getPacketIn().getData(), 0, this.getPacketIn().getData().length);
            /* ksyang */
                tenantId = (int) eth.getSourceMAC().getLong();
                try {
                    sw.getMap().getVirtualNetwork(tenantId);
                } catch (NetworkMappingException e) {
                    log.warn("PacketIn {} does not belong to any virtual network; "
                                    + "dropping and installing a temporary drop rule. Tenant here = {}",
                            this, this.tenantId);
                    this.installDropRule(sw, match);
                    return;
                }
                vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);
                int flowId = this.fetchFlowId(match, tenantId, map);
                // rewrite the OFMatch with the values of the link
                if (tenantId != 0 && flowId != 0) {
                    OVXLinkField linkField = LiberaController.getInstance()
                            .getOvxLinkField();

                    // TODO: Need to check that the values in linkId and flowId
                    // don't exceed their space
                    if (linkField == OVXLinkField.MAC_ADDRESS) {
                        try {
                            LinkedList<MacAddress> macList = sw.getMap()
                                    .getVirtualNetwork(this.tenantId)
                                    .getFlowManager()
                                    .getFlowValues(flowId);
                            eth.setSourceMACAddress(macList.get(0).getBytes())
                                    .setDestinationMACAddress(macList.get(1).getBytes());
                            match = OVXMessageUtil.updateMatch(match, match.createBuilder()
                                    .setExact(MatchField.ETH_SRC, eth.getSourceMAC())
                                    .setExact(MatchField.ETH_DST, eth.getDestinationMAC())
                                    .build()
                            );
                            log.info("In AggFlow, rewritten match with values of link = {}", match.toString());
                        } catch (NetworkMappingException e) {
                            log.warn(e);
                        }
                    } else if (linkField == OVXLinkField.VLAN) {
                        // TODO
                        log.warn("VLAN virtual links not yet implemented.");
                        return;
                    }
                }

                this.setOFMessage(this.getPacketIn().createBuilder()
                        .setData(eth.serialize())
                        .build()
                );

                vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);

                this.sendPkt(vSwitch, match, sw);
                this.log.debug("IPv4 PacketIn {} sent to virtual network {}", this,
                        this.tenantId);
                return;
            }

            this.tenantId = this.fetchTenantId(match, map, true);
            if (this.tenantId == null) {
               // log.info("tenantid={}", tenantId);
               // this.log.debug(
                       // "PacketIn {} does not belong to any virtual network; "
                       //         + "dropping and installing a temporary drop rule",
                       // this);
                this.installDropRule(sw, match);
                return;
            }

            vSwitch = this.fetchOVXSwitch(sw, vSwitch, map);
            this.sendPkt(vSwitch, match, sw);
            this.log.info("Layer2 PacketIn {} sent to virtual network {}", this.getOFMessage(),
                    this.tenantId);
        }

    }



    private void learnHostIP(Match match, Mappable map) {
        if(match.get(MatchField.IPV4_SRC) != null) {
            try {
                OVXNetwork vnet = map.getVirtualNetwork(this.tenantId);
                Host host = vnet.getHost(ovxPort);

                if (host != null) {
                    host.setIPAddress(match.get(MatchField.IPV4_SRC).getInt());

                    //for proxy arp
                    OVXMap.getInstance().addMacHost(host.getMac(), host);
                } else {
                    log.warn("Host not found on virtual port {}", ovxPort);
                    //log.info(this.getPacketIn());
                }
            } catch (NetworkMappingException e) {
                log.info("Failed to lookup virtual network {}", this.tenantId);
            } catch (NullPointerException npe) {
                log.info("No host attached at {} port {}", this.ovxPort
                        .getParentSwitch().getSwitchName(), this.ovxPort
                        .getPhysicalPortNumber());
            }
        }
    }

    private void sendPkt(final OVXSwitch vSwitch, final Match match,
                         final PhysicalSwitch sw) {
        this.log.info("In sendPkt ...");
        if (vSwitch == null || !vSwitch.isActive()) {
            this.log.info(
                    "Controller for virtual network {} has not yet connected "
                            + "or is down", this.tenantId);
            this.installDropRule(sw, match);
            return;
        }

        this.setOFMessage(this.getPacketIn().createBuilder()
                .setBufferId(OFBufferId.of(vSwitch.addToBufferMap(this)))
                .build()
        );

        short inport;
        if(this.getOFMessage().getVersion() == OFVersion.OF_10) {
            inport = this.getPacketIn().getInPort().getShortPortNumber();
        }else{
            if(this.getPacketIn().getMatch().get(MatchField.IN_PORT) != null) {
                inport = this.getPacketIn().getMatch().get(MatchField.IN_PORT).getShortPortNumber();
            }else
                inport = 0;
        }

        if (this.port != null && this.ovxPort != null
                && this.ovxPort.isActive()) {

            //this.log.info("before-----------------------------------------------------");
            //this.log.info(HexString.toHexString(this.getPacketIn().getData()));

            this.setInport(this.ovxPort.getPortNumber());

            if((this.getPacketIn().getData() != null)
                    && (vSwitch.getMissSendLen() != OVXSetConfig.MSL_FULL)) {

                this.setOFMessage(this.getPacketIn().createBuilder()
                        .setData(Arrays.copyOf(
                                this.getPacketIn().getData(),
                                U16.f(vSwitch.getMissSendLen())))
                        .build()
                );
            }

            //this.log.info("after------------------------------------------------------");
            //this.log.info(HexString.toHexString(this.getPacketIn().getData()));
            if(OVXmodeHandler.getOVXmode() == 1) {
                if (Migration.getInstance().byPassingPackets(sw, vSwitch, match, this.getPacketIn()) == false) {
                    this.log.info("sending to controller " + match.toString());
                    vSwitch.sendMsg(this, sw);
                }
            } else {
                this.log.info("sending to controller " + match.toString());
                vSwitch.sendMsg(this, sw);
            }
            //vSwitch.sendMsg(this, sw);
        }else if (this.port == null) {
            log.info("The port {} doesn't belong to the physical switch {}", inport, sw.getName());
        }else if (this.ovxPort == null || !this.ovxPort.isActive()) {
            if(OVXmodeHandler.getOVXmode()==1) {
                Migration.getInstance().byPassingPackets(sw, vSwitch, match, this.getPacketIn());
                Migration.getInstance().doMigration(sw, vSwitch, match, inport);
            }
            else {log.debug(
                    "Virtual port associated to physical port {} in physical switch {} for "
                            + "virtual network {} is not defined or inactive {}",
                    inport, sw.getName(), this.tenantId, match.toString());
            }

        }else{
            log.info("etcs " + match.toString());
        }
    }

    private void learnAddresses(final Match match, OVXSwitch vSwitch) {
        if(match.get(MatchField.ETH_TYPE) == EthType.IPv4
                || match.get(MatchField.ETH_TYPE) == EthType.ARP) {
            if(match.get(MatchField.IPV4_SRC) != null) {
                IPMapper.getPhysicalIp(this.tenantId, match.get(MatchField.IPV4_SRC).getInt(), vSwitch);
            }else if(match.get(MatchField.ARP_SPA) != null) {
                IPMapper.getPhysicalIp(this.tenantId, match.get(MatchField.ARP_SPA).getInt(), vSwitch);
            }
        }
    }

    private void learnAddresses(final Match match) {
        if(match.get(MatchField.ETH_TYPE) == EthType.IPv4
                || match.get(MatchField.ETH_TYPE) == EthType.ARP) {
            if(match.get(MatchField.IPV4_SRC) != null) {
                IPMapper.getPhysicalIp(this.tenantId, match.get(MatchField.IPV4_SRC).getInt());
            }
            if(match.get(MatchField.IPV4_DST) != null) {
                IPMapper.getPhysicalIp(this.tenantId, match.get(MatchField.IPV4_DST).getInt());
            }
        }
    }

    private void installDropRule(final PhysicalSwitch sw, final Match match) {
        final OVXFlowMod fm = new OVXFlowMod(
                this.factory.buildFlowModify()
                        .setMatch(match)
                        .setBufferId(this.getPacketIn().getBufferId())
                        .setHardTimeout(1)
                        .build()
        );

        sw.sendMsg(fm, sw);
    }

    private Integer fetchTenantId(final Match match, final Mappable map,
                                  final boolean useMAC) {
        MacAddress mac = match.get(MatchField.ETH_SRC);
        if (useMAC && map.hasMAC(mac)) {
            try {
                return map.getMAC(mac);
            } catch (AddressMappingException e) {
                log.info("Tried to return non-mapped MAC address : {}", e);
            }
        }
        return null;
    }

    private OVXSwitch fetchOVXSwitch(PhysicalSwitch psw, OVXSwitch vswitch, Mappable map) {
        if (vswitch == null) {
            try {
                log.info("psw and tenant id here: {} {}", psw, this.tenantId);
                vswitch = map.getVirtualSwitch(psw, this.tenantId);
            } catch (SwitchMappingException e) {
                log.info("Cannot fetch non-mapped OVXSwitch: {}", e);
            }
        }
        return vswitch;
    }

    /**
     * This method brings the flowId through IPAddress of source Host and destination Host belonged to the tenant
     *
     * @param match
     * @param tenantId2
     * @param map
     * @return the flowId
     */
    //private int fetchFlowId(final Match match, final int tenantId2,  PhysicalIPAddress srcIP, PhysicalIPAddress dstIP,
      //                      Mappable map){

    private int fetchFlowId(final Match match, final int tenantId2,
                Mappable map){
        Host srcHost, dstHost;

        try {
            srcHost = map.getVirtualNetwork(tenantId).getHost(new PhysicalIPAddress(match.get(MatchField.IPV4_SRC).getInt()));
            dstHost = map.getVirtualNetwork(tenantId).getHost(new PhysicalIPAddress(match.get(MatchField.IPV4_DST).getInt()));
            //log.info("src and dst hosts here= {}, {}", srcHost, dstHost);
            int flowId = map.getVirtualNetwork(tenantId).getFlowManager().getFlowId(srcHost.getMac().getBytes(), dstHost.getMac().getBytes());
            return flowId;
            //OVXNetwork vnet = map.getVirtualNetwork(tenantId2);
            //srcHost = vnet.getHost(srcIP);
            //dstHost = vnet.getHost(dstIP);
            //int flowId = vnet.getFlowManager().getFlowId(srcHost.getMac().getBytes(), dstHost.getMac().getBytes());
            //return flowId;
        } catch (NetworkMappingException e) {
            log.debug("Failed fetchFlowId, tenantId is undefined : {}", tenantId2);
            return 0;
        } catch (NullPointerException e){
            log.debug("Failed fetchFlowId, tenantId : {}, srcHostIp : {}, dstHostIp : {}", tenantId2, match.get(MatchField.IPV4_SRC), match.get(MatchField.IPV4_DST));
            return 0;
        } catch (DroppedMessageException e) {
            log.info(e);
            return 0;
        } catch (IndexOutOfBoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}
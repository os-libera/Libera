/*******************************************************************************
 * Libera HyperVisor development based OpenVirteX for SDN 2.0
 *
 *   for Virtual Machine Migration
 *
 * This is updated by Libera Project team in Korea University
 *
 * Author: Seong-Mun Kim (bebecry@gmail.com)
 * Date: 2017-06-07
 ******************************************************************************/
package net.onrc.openvirtex.services.vm;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.packet.ARP;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.routing.ShortestPath;
import net.onrc.openvirtex.services.forwarding.mpls.MplsForwarding;
import net.onrc.openvirtex.services.path.Node;
import net.onrc.openvirtex.services.path.PathUtil;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPath;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPathBuilder;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPath;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPathBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.*;

public class Migration {
    private static Logger log = LogManager.getLogger(Migration.class.getName());
    private static Migration instance;

    private static List<Integer> mPathID;       //migrated Flow

    public Migration() {
        mPathID = new LinkedList<>();
    }

    public boolean addMigratedPathID(Integer pathID) {
        if(mPathID.contains(pathID)) {
            return false;
        }else{
            mPathID.add(pathID);
            return true;
        }
    }

    public synchronized static Migration getInstance() {
        if (Migration.instance == null) {
            log.info("Starting Migration Manager");

            Migration.instance = new Migration();
        }
        return Migration.instance;
    }

    public boolean byPassingPackets(PhysicalSwitch physicalSwitch, OVXSwitch ovxSwitch, Match match, OFPacketIn ofPacketIn) {
        Integer pathID = null;
        log.info("byPassingPackets {}", match.toString());

        pathID = MplsForwarding.getInstance().getPathIDFromMatch(match, physicalSwitch, ovxSwitch.getTenantId());
        VirtualPath vPath = VirtualPathBuilder.getInstance().getVirtualPath(pathID);

        if(vPath == null) {
            return false;
        }

        if(vPath.isMigrated() == false) {
            return false;
        }

        if(match.get(MatchField.ETH_TYPE).equals(EthType.ARP)) {
            log.info("byPassingPackets PacketIn with ARP {}", match.toString());
            MacAddress srcMac = match.get(MatchField.ETH_SRC);
            MacAddress dstMac = match.get(MatchField.ETH_DST);

            if(srcMac != null) {
                ARP arp = new ARP();
                arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
                arp.setProtocolType(Ethernet.TYPE_IPV4);
                arp.setOpCode(ARP.OP_REPLY);

                arp.setHardwareAddressLength((byte)6);
                arp.setProtocolAddressLength((byte)4);

                MacAddress mac = OVXMap.getInstance().getDstMacFromSrcMac(srcMac);

                arp.setSenderHardwareAddress(mac.getBytes());
                arp.setSenderProtocolAddress(match.get(MatchField.ARP_TPA).getBytes());  //arp_spa=10.0.0.1, arp_tpa=10.0.0.2

                arp.setTargetHardwareAddress(srcMac.getBytes());
                arp.setTargetProtocolAddress(match.get(MatchField.ARP_SPA).getBytes());

                Ethernet ethernet = new Ethernet();
                ethernet.setSourceMACAddress(mac.getBytes());
                ethernet.setDestinationMACAddress(srcMac.getBytes());
                ethernet.setEtherType(Ethernet.TYPE_ARP);
                ethernet.setPayload(arp);
                ethernet.setPad(false);

                OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);

                OFActionOutput ofActionOutput = factory.actions().buildOutput()
                        .setPort(match.get(MatchField.IN_PORT))
                        .build();

                OFPacketOut ofPacketOut = factory.buildPacketOut()
                        .setData(ethernet.serialize())
                        .setActions(Collections.singletonList((OFAction)ofActionOutput))
                        .setInPort(OFPort.CONTROLLER)
                        .setBufferId(OFBufferId.NO_BUFFER)
                        .build();

                physicalSwitch.sendMsg(new OVXMessage(ofPacketOut), null);

                return true;
            }
        }else if(match.get(MatchField.ETH_TYPE).equals(EthType.IPv4)){
            //log.info("PacketIn with IPv4");
            log.info("byPassingPackets PacketIn with IPv4 {}", match.toString());

            Host dstHost = OVXMap.getInstance().getHost(match.get(MatchField.ETH_DST));
            log.info("{}", dstHost.toString());
            if(dstHost != null) {
                log.info("DstHost is not null [" +  ofPacketIn.getData().length + "]");
                OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);
                OFActionOutput ofActionOutput = factory.actions().buildOutput()
                        .setPort(OFPort.of(dstHost.getPort().getPhysicalPort().getPortNumber()))
                        .build();

                OFPacketOut ofPacketOut = factory.buildPacketOut()
                        .setData(ofPacketIn.getData())
                        .setActions(Collections.singletonList((OFAction) ofActionOutput))
                        .setInPort(OFPort.CONTROLLER)
                        .setBufferId(OFBufferId.NO_BUFFER)
                        .build();

                dstHost.getPort().getPhysicalPort().getParentSwitch().sendMsg(new OVXMessage(ofPacketOut), null);
                return true;
            }
        }

        return false;
    }

    /*public synchronized void doMigration2(PhysicalSwitch physicalSwitch, OVXSwitch ovxSwitch, Match match, short pInport, OFPacketIn ofPacketIn) {
        Integer flowId = null;
        log.info("doMigration2 {}", match.toString());

        if(match.get(MatchField.ETH_TYPE).equals(EthType.ARP)) {
            log.info("PacketIn with ARP");
            MacAddress srcMac = match.get(MatchField.ETH_SRC);
            MacAddress dstMac = match.get(MatchField.ETH_DST);

            if(srcMac != null) {
                ARP arp = new ARP();
                arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
                arp.setProtocolType(Ethernet.TYPE_IPV4);
                arp.setOpCode(ARP.OP_REPLY);

                arp.setHardwareAddressLength((byte)6);
                arp.setProtocolAddressLength((byte)4);

                MacAddress mac = OVXMap.getInstance().getDstMacFromSrcMac(srcMac);

                arp.setSenderHardwareAddress(mac.getBytes());
                arp.setSenderProtocolAddress(match.get(MatchField.ARP_TPA).getBytes());  //arp_spa=10.0.0.1, arp_tpa=10.0.0.2

                arp.setTargetHardwareAddress(srcMac.getBytes());
                arp.setTargetProtocolAddress(match.get(MatchField.ARP_SPA).getBytes());

                Ethernet ethernet = new Ethernet();
                ethernet.setSourceMACAddress(mac.getBytes());
                ethernet.setDestinationMACAddress(srcMac.getBytes());
                ethernet.setEtherType(Ethernet.TYPE_ARP);
                ethernet.setPayload(arp);
                ethernet.setPad(false);

                OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);

                OFActionOutput ofActionOutput = factory.actions().buildOutput()
                        .setPort(match.get(MatchField.IN_PORT))
                        .build();

                OFPacketOut ofPacketOut = factory.buildPacketOut()
                        .setData(ethernet.serialize())
                        .setActions(Collections.singletonList((OFAction)ofActionOutput))
                        .setInPort(OFPort.CONTROLLER)
                        .setBufferId(OFBufferId.NO_BUFFER)
                        .build();

                physicalSwitch.sendMsg(new OVXMessage(ofPacketOut), null);
            }
        }else if(match.get(MatchField.ETH_TYPE).equals(EthType.IPv4)){
            log.info("PacketIn with IPv4");

            Host dstHost = OVXMap.getInstance().getHost(match.get(MatchField.ETH_DST));

            if(dstHost != null) {
                log.info("DstHost is not null [" +  ofPacketIn.getData().length + "]");
                OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);
                OFActionOutput ofActionOutput = factory.actions().buildOutput()
                        .setPort(OFPort.of(dstHost.getPort().getPhysicalPort().getPortNumber()))
                        .build();

                OFPacketOut ofPacketOut = factory.buildPacketOut()
                        .setData(ofPacketIn.getData())
                        .setActions(Collections.singletonList((OFAction) ofActionOutput))
                        .setInPort(OFPort.CONTROLLER)
                        .setBufferId(OFBufferId.NO_BUFFER)
                        .build();

                dstHost.getPort().getPhysicalPort().getParentSwitch().sendMsg(new OVXMessage(ofPacketOut), null);
            }

        }

        doMigration(physicalSwitch, ovxSwitch, match, pInport);
    }*/

    public synchronized void doMigration(PhysicalSwitch physicalSwitch, OVXSwitch ovxSwitch, Match match, short pInport) {
    	this.log.info("In do migration ... ");
        Integer outflowId = null, outPathId = null;
        Integer inflowId = null, inPathId = null;
        VirtualPath outvPath = null;
        VirtualPath invPath = null;
        try {
            OVXNetwork vnet = physicalSwitch.getMap().getVirtualNetwork(ovxSwitch.getTenantId());
            outflowId = vnet.getFlowManager().getFlowValues(
                    match.get(MatchField.ETH_SRC).getBytes(),
                    match.get(MatchField.ETH_DST).getBytes());

            outPathId = PathUtil.getInstance().makePathID(ovxSwitch.getTenantId(), outflowId);
            outvPath = VirtualPathBuilder.getInstance().getVirtualPath(outPathId);

            inflowId = vnet.getFlowManager().getFlowValues(
                    match.get(MatchField.ETH_DST).getBytes(),
                    match.get(MatchField.ETH_SRC).getBytes());

            inPathId = PathUtil.getInstance().makePathID(ovxSwitch.getTenantId(), inflowId);
            this.log.info("{}",outvPath.toString());
            if(outvPath == null){
            	this.log.info("check point 2...");
                return;
            }
            if(!outvPath.isMigrated()){
            	this.log.info("check point 3...");
                outvPath.setMigrated(true);}
            else{
                return;}

            outvPath.setTimestamp(System.currentTimeMillis());
            addMigratedPathID(outflowId);

            invPath = VirtualPathBuilder.getInstance().getVirtualPath(inPathId);
            this.log.info("check point 5...");
            this.log.info("{}",invPath.toString());
            if(invPath == null)
                return;
            this.log.info("check point 6...");
            invPath.setMigrated(true);

            invPath.setTimestamp(System.currentTimeMillis());
            addMigratedPathID(inPathId);

            log.info("Migration is performed");
        } catch (NetworkMappingException e) {
            e.printStackTrace();
        }catch (IndexOutOfBoundException e) {
            e.printStackTrace();
        }
        this.log.info("check point 7...");
        PhysicalPath outOldPath = PhysicalPathBuilder.getInstance().getPhysicalPath(outPathId);
        PhysicalPath outNewPath = findPhysicalPath(physicalSwitch,
                (PhysicalSwitch)outOldPath.getDstSwitch().getSwitch(), outOldPath, pInport);

        PhysicalPathBuilder.getInstance().addPhysicalPath(outNewPath);

        outvPath.setMigratedPhysicalPath(outNewPath);
        if(outNewPath != null){
            //modifyOldPhysicalPathtoController(outOldPath);
            removeOldPhysicalPathFlowEntry(outOldPath);

            installNewPhysicalPathFlowEntry(outNewPath, outOldPath);
        }

        PhysicalPath inOldPath = PhysicalPathBuilder.getInstance().getPhysicalPath(inPathId);
        PhysicalPath inNewPath = findPhysicalPath(outNewPath, inOldPath);

        PhysicalPathBuilder.getInstance().addPhysicalPath(inNewPath);

        invPath.setMigratedPhysicalPath(inNewPath);
        if(inNewPath != null){
            //modifyOldPhysicalPathtoController(inOldPath);

            removeOldPhysicalPathFlowEntry(inOldPath);

            installNewPhysicalPathFlowEntry(inNewPath, inOldPath);
        }

        log.info("Outgoing Path Migration Time " + (System.currentTimeMillis() - outvPath.getTimestamp()));
        log.info(" Ingoing Path Migration Time " + (System.currentTimeMillis() - outvPath.getTimestamp()));
    }

    public void modifyOldPhysicalPathtoController(PhysicalPath oldPath) {
        //log.info("modifyOldPhysicalPathtoController FlowID = " + oldPath.getFlowID());
        OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);
        OFActionOutput output = factory.actions().buildOutput()
                .setPort(OFPort.CONTROLLER)
                .build();

        OFFlowMod flowMod = null;
        OFFlowModify flowModify = null;

        if(oldPath.getSrcSwitch().getMplsFlowMod() != null) {
            flowMod = oldPath.getSrcSwitch().getMplsFlowMod();

            flowModify = factory.buildFlowModify()
                    .setActions(Collections.singletonList((OFAction)output))
                    .setMatch(flowMod.getMatch())
                    .build();

            oldPath.getSrcSwitch().getSwitch().sendMsg(new OVXMessage(flowModify),
                    oldPath.getSrcSwitch().getSwitch());
        }

        if(oldPath.getIntermediate().size() != 0) {
             for(Node node : oldPath.getIntermediate()) {
                //log.info(node.flowMod.toString());
                flowMod = node.getMplsFlowMod();
                flowModify = factory.buildFlowModify()
                        .setActions(Collections.singletonList((OFAction) output))
                        .setMatch(flowMod.getMatch())
                        .build();

                node.getSwitch().sendMsg(new OVXMessage(flowModify), node.getSwitch());
            }
        }

        if(oldPath.getDstSwitch().getMplsFlowMod() != null) {
            //log.info("oldPath.getDstSwitch().flowMod != null");

            flowMod = oldPath.getDstSwitch().getMplsFlowMod();

            flowModify = factory.buildFlowModify()
                    .setActions(Collections.singletonList((OFAction)output))
                    .setMatch(flowMod.getMatch())
                    .build();

            oldPath.getDstSwitch().getSwitch().sendMsg(new OVXMessage(flowModify),
                    oldPath.getDstSwitch().getSwitch());
        }
    }


    public PhysicalPath findPhysicalPath(PhysicalSwitch srcSwitch, PhysicalSwitch dstSwitch,
                                         PhysicalPath pPath, short pInport) {
        ShortestPath routing = new ShortestPath();
        PhysicalPath newpPath = null;

        //log.info("srcSwitch : " + srcSwitch.toString());
        //log.info("dstSwitch : " + dstSwitch.toString());

        LinkedList<PhysicalLink> physicalLinks = routing.computePath(srcSwitch, dstSwitch);

        //log.info("findPhysicalPath Inport : " + pInport);

        if(physicalLinks != null) {
            //log.info("physicalLinks != null {}/{}", pPath.getFlowID(), pPath.getTenantID());
            newpPath = new PhysicalPath(pPath.getFlowID(), pPath.getTenantID(),
                    PathUtil.getInstance().makePathID(pPath.getTenantID(), pPath.getFlowID()),
                    pPath.getvPath());

            newpPath.setSrcHost(pPath.getSrcHost());
            newpPath.setDstHost(pPath.getDstHost());

            if(physicalLinks.size() == 1) {
                //log.info("findPhysicalPath physicalLinks.size() == 1");

                PhysicalLink plink = physicalLinks.get(0);
                //log.info(plink.toString());

                //set srcSwitch
                Node tempNode = new Node(plink.getSrcSwitch());

                tempNode.setInPort(plink.getSrcSwitch().getPort(pInport));
                tempNode.setOutPort(plink.getSrcPort());

                newpPath.setSrcSwitch(tempNode);

                //set dstSwitch
                tempNode = new Node(plink.getDstSwitch());

                tempNode.setInPort(plink.getDstPort());
                tempNode.setOutPort(newpPath.getDstHost().getPort().getPhysicalPort());

                newpPath.setDstSwitch(tempNode);

            }else{
                //log.info("findPhysicalPath physicalLinks.size( {} )", physicalLinks.size());
                Node srcNode = null;
                Node dstNode = null;
                LinkedList<Node> nodes = new LinkedList<>();

                for(PhysicalLink plink : physicalLinks) {
                    if(srcNode == null) {
                        srcNode = new Node(plink.getSrcSwitch());
                    }

                    srcNode.setOutPort(plink.getSrcPort());

                    nodes.add(srcNode);

                    dstNode = new Node(plink.getDstSwitch());
                    dstNode.setInPort(plink.getDstPort());

                    srcNode = dstNode;

                    if(plink.equals(physicalLinks.getLast())) {
                        nodes.add(srcNode);
                    }
                }

                for(Node node : nodes) {
                    if(node.equals(nodes.getFirst())) {
                        node.setInPort(physicalLinks.get(0).getSrcSwitch().getPort(pInport));

                        newpPath.setSrcSwitch(node);
                    }else if(node.equals(nodes.getLast())) {
                        node.setOutPort(newpPath.getDstHost().getPort().getPhysicalPort());

                        newpPath.setDstSwitch(node);
                    }else{
                        newpPath.setIntermediateSwitch(node);
                    }
                }
            }
        }
        return newpPath;
    }

    public PhysicalPath findPhysicalPath(PhysicalPath outNewPath, PhysicalPath inOldPath) {
        ShortestPath routing = new ShortestPath();
        PhysicalPath newpPath = null;

        LinkedList<PhysicalLink> physicalLinks = routing.computePath((PhysicalSwitch)outNewPath.getDstSwitch().getSwitch(),
                (PhysicalSwitch)outNewPath.getSrcSwitch().getSwitch());

        if(physicalLinks != null) {
            //log.info("physicalLinks != null {}/{}", inOldPath.getFlowID(), inOldPath.getTenantID());
            newpPath = new PhysicalPath(inOldPath.getFlowID(), inOldPath.getTenantID(),
                    PathUtil.getInstance().makePathID(inOldPath.getTenantID(), inOldPath.getFlowID()),
                    inOldPath.getvPath());

            newpPath.setSrcHost(inOldPath.getSrcHost());
            newpPath.setDstHost(inOldPath.getDstHost());

            if(physicalLinks.size() == 1) {
                //log.info("findPhysicalPath2 physicalLinks.size() == 1");
                PhysicalLink plink = physicalLinks.get(0);

                log.info(plink.toString());
                //set srcSwitch
                Node tempNode = new Node(plink.getSrcSwitch());

                tempNode.setInPort(outNewPath.getDstSwitch().getOutPort());
                tempNode.setOutPort(plink.getSrcPort());

                newpPath.setSrcSwitch(tempNode);

                //set dstSwitch
                tempNode = new Node(plink.getDstSwitch());

                tempNode.setInPort(plink.getDstPort());
                tempNode.setOutPort(outNewPath.getSrcSwitch().getInPort());

                newpPath.setDstSwitch(tempNode);
            }else{
                Node srcNode = null;
                Node dstNode = null;
                LinkedList<Node> nodes = new LinkedList<>();

                for(PhysicalLink plink : physicalLinks) {
                    if(srcNode == null) {
                        srcNode = new Node(plink.getSrcSwitch());
                    }

                    srcNode.setOutPort(plink.getSrcPort());

                    nodes.add(srcNode);

                    dstNode = new Node(plink.getDstSwitch());
                    dstNode.setInPort(plink.getDstPort());

                    srcNode = dstNode;

                    if(plink.equals(physicalLinks.getLast())) {
                        nodes.add(srcNode);
                    }
                }

                for(Node node : nodes) {
                    if(node.equals(nodes.getFirst())) {
                        node.setInPort(outNewPath.getDstSwitch().getOutPort());

                        //log.info("node.inPort {}", node.inPort.toString());

                        newpPath.setSrcSwitch(node);
                    }else if(node.equals(nodes.getLast())) {
                        node.setOutPort(outNewPath.getSrcSwitch().getInPort());

                        //log.info("node.outPort {}", node.outPort.toString());
                        newpPath.setDstSwitch(node);
                    }else{
                        newpPath.setIntermediateSwitch(node);
                    }
                }

            }
        }
        return newpPath;
    }

    public void installNewPhysicalPathFlowEntry(PhysicalPath nPath, PhysicalPath oPath) {
        MplsForwarding.getInstance().addMplsActions(oPath, nPath);

        nPath.sendSouth();
    }

    public void removeOldPhysicalPathFlowEntry(PhysicalPath pPath) {

        if(pPath.isOriginalPath()) {
            if(pPath.getMplsLabel().getPathIDs().size() == 0) {
                log.info("Remove Old Original PhysicalPath for PathID [{}]", pPath.getPathID());

                sendFlowDeleteStrict(pPath.getSrcSwitch());

                for(Node node : pPath.getIntermediate()) {
                    sendFlowDeleteStrict(node);
                }

                sendFlowDeleteStrict(pPath.getDstSwitch());
            }else{
                log.info("Remove Old Original PhysicalPath Edge Switch for PathID [{}]", pPath.getPathID());

                sendFlowDeleteStrict(pPath.getSrcSwitch());
                sendFlowDeleteStrict(pPath.getDstSwitch());
            }
        }else{
            log.info("Remove Old Referenced PhysicalPath Edge Switch for PathID [{}]", pPath.getPathID());
            sendFlowDeleteStrict(pPath.getSrcSwitch());
            sendFlowDeleteStrict(pPath.getDstSwitch());
        }
    }

    public void sendFlowDeleteStrict(Node node) {
        OFFactory factory = OFFactories.getFactory(OFVersion.OF_13);

        if(node.getMplsFlowMod() != null) {
            OFFlowDeleteStrict ofFlowDeleteStrict = factory.buildFlowDeleteStrict()
                    .setMatch(node.getMplsFlowMod().getMatch())
                    .setFlags(node.getMplsFlowMod().getFlags())
                    .setPriority(node.getMplsFlowMod().getPriority())
                    .build();

            node.getSwitch().sendMsg(new OVXMessage(ofFlowDeleteStrict), node.getSwitch());
        }
    }
}

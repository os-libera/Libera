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

import java.util.*;


import net.onrc.openvirtex.api.service.handlers.tenant.SetOVXmode;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.FlowTable;
import net.onrc.openvirtex.elements.datapath.OVXFlowTable;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.OVXmodes.*;
import net.onrc.openvirtex.exceptions.*;
import net.onrc.openvirtex.messages.actions.*;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.services.forwarding.mpls.MplsForwarding;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPath;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPathBuilder;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPath;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPathBuilder;
import net.onrc.openvirtex.util.OVXUtil;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import net.onrc.openvirtex.elements.datapath.PhysicalFlowTable;
import net.onrc.openvirtex.elements.OVXmodes.OVXmodeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import static org.projectfloodlight.openflow.protocol.match.MatchField.IPV4_DST;
import static org.projectfloodlight.openflow.protocol.match.MatchField.IPV4_SRC;


public class OVXFlowMod extends OVXMessage implements Devirtualizable {

    private final Logger log = LogManager.getLogger(OVXFlowMod.class.getName());

    private OVXSwitch sw = null;
    private final List<OFAction> approvedActions = new LinkedList<OFAction>();
    private OFFlowMod originalFlowMod = null;


    private long ovxCookie = -1;

    public OVXFlowMod(OFMessage msg) {
        super(msg);
    }

    public OFFlowMod getFlowMod() {
        return (OFFlowMod)this.getOFMessage();
    }



    public VirtualPath virtualPath = null;



    @Override
    public synchronized void devirtualize(final OVXSwitch sw) {
        this.log.info("devirtualize start");
        this.log.info(this.getFlowMod().toString());
        //this.log.info(this.getOFMessage().toString());


        List<OFAction> laction = this.getFlowMod().getActions();
        if(laction != null) {
            if(laction.size() == 1) {
                OFAction ofAction = laction.get(0);
                if(ofAction.getType() == OFActionType.OUTPUT) {
                    OFActionOutput ofActionOutput = (OFActionOutput) ofAction;
                    if(ofActionOutput.getPort() != OFPort.CONTROLLER) {
                        //this.log.info("devirtualize");
                        /*if(this.getMplsFlowMod().getCommand()==OFFlowModCommand.DELETE ||
                                this.getMplsFlowMod().getCommand()==OFFlowModCommand.DELETE_STRICT)
                            this.log.info("Before " + this.getOFMessage().toString());*/


                    }else {
                        return;
                    }
                }
            }
        }

        // Drop LLDP-matching messages sent by some applications
        if (this.getFlowMod().getMatch().get(MatchField.ETH_TYPE) == EthType.LLDP) {
            return;
        }


        this.originalFlowMod = this.getFlowMod().createBuilder().build();

        this.sw = sw;
        FlowTable ft = this.sw.getFlowTable();

        int bufferId = OFBufferId.NO_BUFFER.getInt();
        if (sw.getFromBufferMap(this.getFlowMod().getBufferId().getInt()) != null) {
            bufferId = ((OFPacketIn)sw.getFromBufferMap(this.getFlowMod().getBufferId().getInt()).getOFMessage())
                    .getBufferId().getInt();
        }


        //OFMatch?? inport? ???? 0?? ???? ??, ??? OpenFlowj??? MatchField? ???? ???
        //?? ??? ?? ??? inport?? ? ? ??.
        //ONOS? ?? ???? ???? ???? ?? FlowMod ???? ???(ARP, LLDP, IPv4??? Controller? ??? ??)
        //??? in_port ??? ?? ?? ???? ?? ?????

        short inport = 0;

        if(this.getFlowMod().getMatch().get(MatchField.IN_PORT) != null)
        {
            inport = this.getFlowMod().getMatch()
                    .get(MatchField.IN_PORT).getShortPortNumber();
        }
        boolean pflag = ft.handleFlowMods(this.clone());


        OVXMatch ovxMatch = new OVXMatch(this.getFlowMod().getMatch());

        ovxCookie = ((OVXFlowTable) ft).getCookie(this, false);


        if(OVXmodeHandler.getOVXmode()==1){
            ovxMatch.setOVXSwitch(sw);}


        ovxMatch.setCookie(ovxCookie);

        this.setOFMessage(this.getFlowMod().createBuilder()
                .setCookie(U64.of(ovxMatch.getCookie()))
                .build()
        );


        if(OVXmodeHandler.getOVXmode() == 2) {
            //log.info("AggFlow mode");
            //AggFlow: If match received by controller has only mac address, write ip address on match
            try {
                if (ovxMatch.getMatch().get(MatchField.IPV4_SRC).getInt() == 0 || ovxMatch.getMatch().get(MatchField.IPV4_DST).getInt() == 0) {
                    net.onrc.openvirtex.elements.address.IPAddress srcIP = getHostIP(getHostbyMACAddress(ovxMatch.getMatch().get(MatchField.ETH_SRC).getBytes()));
                    IPAddress dstIP = getHostIP(getHostbyMACAddress(ovxMatch.getMatch().get(MatchField.ETH_DST).getBytes()));
                    if (srcIP != null && dstIP != null) {
                        this.modifyMatch(OVXMessageUtil.updateMatch(this.getFlowMod().getMatch(), this.getFlowMod().getMatch().createBuilder()
                                .setExact(MatchField.IPV4_SRC, IPv4Address.of(srcIP.getIp()))
                                .build()));
                        this.modifyMatch(OVXMessageUtil.updateMatch(this.getFlowMod().getMatch(), this.getFlowMod().getMatch().createBuilder()
                                .setExact(MatchField.IPV4_DST, IPv4Address.of(dstIP.getIp()))
                                .build()));
                    }
                }
            } catch (NullPointerException e) {
                log.info("match received by controller= {} , mac value ={}",
                        ovxMatch.getMatch(), ovxMatch.getMatch().get(MatchField.IPV4_DST).getInt(), ovxMatch.getMatch().get(MatchField.IPV4_SRC).getInt());
                log.info("check fetching get host, srcIP={}", getHostIP(getHostbyMACAddress(ovxMatch.getMatch().get(MatchField.ETH_SRC).getBytes())));
            }
        }


        for (final OFAction act : this.getFlowMod().getActions()) {
            try {
                OVXAction action2 = OVXActionUtil.wrappingOVXAction(act);

                ((VirtualizableAction) action2).virtualize(sw, this.approvedActions, ovxMatch);
            } catch (final ActionVirtualizationDenied e) {
                this.log.info("Action {} could not be virtualized; error: {}",
                        act, e.getMessage());
                ft.deleteFlowMod(ovxCookie);
                sw.sendMsg(OVXMessageUtil.makeError(e.getErrorCode(), this), sw);
                return;
            } catch (final DroppedMessageException e) {
                //this.log.info("Dropping ovxFlowMod {} {}", this.getOFMessage().toString(), e);
                ft.deleteFlowMod(ovxCookie);
                // TODO perhaps send error message to controller
                return;
            } catch (final NullPointerException e) {
                this.log.info("Action {} could not be supported={}", act);
                return;
            }
        }


        if(OVXmodeHandler.getOVXmode() == 1) {
            //log.info("LiteVisor mode in flowmod");
            //LiteVisor: added for MPLS
            switch (this.getFlowMod().getCommand()) {
                case ADD:
                    if (ovxMatch.getFlowId() != null) {
                       // log.info("Virtual path ADD case");
                        this.virtualPath = VirtualPathBuilder.getInstance().buildVirtualPath(ovxMatch, originalFlowMod);

                        if (this.virtualPath == null)
                            return;
                    } else {
                        return;
                    }
                    break;
                case MODIFY:
                case MODIFY_STRICT:
                    break;
                case DELETE:
                case DELETE_STRICT:
                    this.originalFlowMod = VirtualPathBuilder.getInstance().removeVirtualPath(this.originalFlowMod, ovxMatch);
                    if (this.originalFlowMod == null) {
                        log.info("Matching FlowMod message does not exist");
                        return;
                    }
                    //Match temp = MplsManager.getInstance().DeleteMplsActions(ovxMatch);

                    break;
            }
        }


        final OVXPort ovxInPort = sw.getPort(inport);
        this.setOFMessage(this.getFlowMod().createBuilder()
                .setBufferId(OFBufferId.of(bufferId))
                .build()
        );

        if (ovxInPort == null) {
            if(this.getFlowMod().getMatch().isFullyWildcarded(MatchField.IN_PORT)) {
                for (OVXPort iport : sw.getPorts().values()) {
                    try {
                        if(OVXmodeHandler.getOVXmode() == 1) {
                            prepAndSendSouth(iport, pflag, ovxMatch);
                        } else { prepAndSendSouth(iport, pflag);}
                    } catch (IndexOutOfBoundException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                this.log.error(
                        "Unknown virtual port id {}; dropping ovxFlowMod {}",
                        inport, this);
                sw.sendMsg(OVXMessageUtil.makeErrorMsg(OFFlowModFailedCode.EPERM, this), sw);
                return;
            }
        } else {

            try {
                if(OVXmodeHandler.getOVXmode() == 1) {
                    prepAndSendSouth(ovxInPort, pflag, ovxMatch);
                } else {
                    prepAndSendSouth(ovxInPort, pflag); }
            } catch (IndexOutOfBoundException e) {
                e.printStackTrace();
            }
        }

    }



    public void modifyMatch(Match match)
    {
        this.setOFMessage(this.getFlowMod().createBuilder()
                .setMatch(match)
                .build()
        );
    }


    //LiteVisor PrepAndsendSouth method called
    private void prepAndSendSouth(OVXPort inPort, boolean pflag, OVXMatch ovxMatch) throws IndexOutOfBoundException {

        if (!inPort.isActive()) {
            log.warn("Virtual network {}: port {} on switch {} is down.",
                    sw.getTenantId(), inPort.getPortNumber(),
                    sw.getSwitchName());
            return;
        }

        this.modifyMatch(
                OVXMessageUtil.updateMatch(
                        this.getFlowMod().getMatch(),
                        this.getFlowMod().getMatch().createBuilder()
                                .setExact(MatchField.IN_PORT, OFPort.of(inPort.getPhysicalPortNumber()))
                                .build()
                )
        );

        OVXMessageUtil.translateXid(this, inPort);

        this.setOFMessage(this.getFlowMod().createBuilder()
                    .setActions(this.approvedActions)
                    .build());

        if (pflag) {
            if (!this.getFlowMod().getFlags().contains(OFFlowModFlags.SEND_FLOW_REM))
                this.getFlowMod().getFlags().add(OFFlowModFlags.SEND_FLOW_REM);
                switch (this.getFlowMod().getCommand()) {
                    case ADD:
                            //log.info("ADD case");
                            if (ovxMatch.getFlowId() != null) {
                        /*this.setOFMessage(PhysicalPathBuilder.getInstance().buildPhysicalPath(
                                this.virtualPath, this.originalFlowMod, this.getMplsFlowMod(), ovxMatch.getSwitchType(),
                                inPort.getPhysicalPort().getParentSwitch()));*/

                                PhysicalPath pPath = PhysicalPathBuilder.getInstance().buildPhysicalPath(
                                        this.virtualPath, this.originalFlowMod, this.getFlowMod(), ovxMatch.getSwitchType(),
                                        inPort.getPhysicalPort().getParentSwitch());

                        /*this.setOFMessage(
                                PhysicalPathBuilder.getInstance().buildPhysicalPath(
                                        this.virtualPath, this.originalFlowMod, this.getFlowMod(), ovxMatch.getSwitchType(),
                                        inPort.getPhysicalPort().getParentSwitch())
                        );*/

                                if (this.virtualPath.isBuild()) {
                                    //pPath.sendSouth();
                                    if (pPath.findPhysicalPath()) {
                                        pPath.addPathIDtoPhysicalSwitch();
                                        MplsForwarding.getInstance().addMplsActions(pPath);
                                        pPath.sendSouth();

                                    }
                                }

                            } else {
                                return;
                            }
                            break;
                    case DELETE:
                    case DELETE_STRICT:

                            if (ovxMatch.getFlowId() != null) {

                                OFFlowMod ofFlowMod = PhysicalPathBuilder.getInstance().removePhysicalPath(this.originalFlowMod, ovxMatch);
                                if (ofFlowMod != null) {
                                    this.modifyMatch(ofFlowMod.getMatch());
                                } else {
                                }
                                sw.sendSouth(this, inPort);
                            } else {
                                return;
                            }

                            break;
                    }
                }
    }



    // prepAndSendSOuth for OVX or AggFlow mode
    private void prepAndSendSouth(OVXPort inPort, boolean pflag) {
        // try{
        if (!inPort.isActive()) {
            log.warn("Virtual network {}: port {} on switch {} is down.",
                    sw.getTenantId(), inPort.getPortNumber(),
                    sw.getSwitchName());
            return;
        }

        this.modifyMatch(
                OVXMessageUtil.updateMatch(
                        this.getFlowMod().getMatch(),
                        this.getFlowMod().getMatch().createBuilder()
                                .setExact(MatchField.IN_PORT, OFPort.of(inPort.getPhysicalPortNumber()))
                                .build()
                )
        );

        OVXMessageUtil.translateXid(this, inPort);

        //AggFlow: Get the physicalFlowTable.
        PhysicalFlowTable phyFlowTable = inPort.getPhysicalPort().getParentSwitch().getEntrytable();

            boolean isedgeOut = true;
            boolean duflag = false;


            try {
                if (inPort.isEdge()) {
                    if (OVXmodeHandler.getOVXmode() == 0) {
                        this.prependRewriteActions();
                    } else {
                        //log.info("Inport is edge, calling prependRewriteActions method:-");



                       /* OVXPort dstPort = sw.getMap()
                                .getVirtualNetwork(sw.getTenantId())
                                .getNeighborPort(inPort);

                        OVXLink link = sw.getMap()
                                .getVirtualNetwork(sw.getTenantId())
                                .getLink(dstPort, inPort);

                        Integer flowId = sw.getMap()
                                .getVirtualNetwork(sw.getTenantId())
                                .getFlowManager()
                                .getFlowId(
                                        this.getFlowMod().getMatch().get(MatchField.ETH_SRC).getBytes(),
                                        this.getFlowMod().getMatch().get(MatchField.ETH_DST).getBytes());


                        OVXLinkUtils lUtils = new OVXLinkUtils(sw.getTenantId(), link.getLinkId(), flowId, link.getSrcSwitch());
*/
                        if (this.getOFMessage().getVersion() == OFVersion.OF_10) {

                         /*  this.getFlowMod().getMatch()
                                    .createBuilder()
                                    .wildcard(MatchField.IN_PORT)
                                    .setExact(MatchField.IN_PORT, OFPort.of(inPort.getPhysicalPortNumber()))
                                    .build();


                            this.getFlowMod().getMatch()
                                    .createBuilder()
                                    .wildcard(MatchField.ETH_SRC)
                                    .setExact(MatchField.ETH_SRC, MacAddress.of(lUtils.getSrcMac().getBytes()))
                                    .build();

                            this.getFlowMod().getMatch()
                                    .createBuilder()
                                    .wildcard(MatchField.ETH_DST)
                                    .setExact(MatchField.ETH_DST, MacAddress.of(lUtils.getDstMac().getBytes()))
                                    .build();

                            this.getFlowMod().getMatch()
                                    .createBuilder()
                                    .wildcard(MatchField.ETH_TYPE)
                                    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                    .build();
*/
                            this.modifyMatch(OVXMessageUtil.updateMatch(this.getFlowMod().getMatch(), this.getFlowMod().getMatch().createBuilder()
                                    .setExact(MatchField.IN_PORT, this.getFlowMod().getMatch().get(MatchField.IN_PORT))
                                    .setExact(MatchField.ETH_SRC, this.getFlowMod().getMatch().get(MatchField.ETH_SRC))
                                    .setExact(MatchField.ETH_DST, this.getFlowMod().getMatch().get(MatchField.ETH_DST))
                                    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                    .setExact(MatchField.IPV4_SRC, this.getFlowMod().getMatch().get(IPV4_SRC))
                                    .setExact(MatchField.IPV4_DST, this.getFlowMod().getMatch().get(IPV4_DST))
                                    .build()));

                            OFAction action = this.factory.actions()
                                    .buildSetDlSrc()
                                    .setDlAddr(MacAddress.of(sw.getTenantId()))
                                    .build();
                            this.approvedActions.add(0, action);
                            log.info("approved action in OF1.0 = {}", approvedActions);

                        } else {
                            /*this.getFlowMod().getMatch()
                                    .createBuilder()
                                    .wildcard(MatchField.IN_PORT)
                                    .setExact(MatchField.IN_PORT, OFPort.of(inPort.getPhysicalPortNumber()))
                                    .build();

                            this.getFlowMod().getMatch()
                                    .createBuilder()
                                    .wildcard(MatchField.ETH_SRC)
                                    .wildcard(MatchField.ETH_DST)
                                    .setExact(MatchField.ETH_SRC, MacAddress.of(lUtils.getSrcMac().getBytes()))
                                    .setExact(MatchField.ETH_DST, MacAddress.of(lUtils.getDstMac().getBytes()))
                                    .build();

                            this.getFlowMod().getMatch()
                                    .createBuilder()
                                    .wildcard(MatchField.ETH_TYPE)
                                    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                   .build();
*/
                            this.modifyMatch(OVXMessageUtil.updateMatch(this.getFlowMod().getMatch(), this.getFlowMod().getMatch().createBuilder()
                                    .setExact(MatchField.IN_PORT, this.getFlowMod().getMatch().get(MatchField.IN_PORT))
                                    .setExact(MatchField.ETH_SRC, this.getFlowMod().getMatch().get(MatchField.ETH_SRC))
                                    .setExact(MatchField.ETH_DST, this.getFlowMod().getMatch().get(MatchField.ETH_DST))
                                    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                    .setExact(MatchField.IPV4_SRC, this.getFlowMod().getMatch().get(IPV4_SRC))
                                    .setExact(MatchField.IPV4_DST, this.getFlowMod().getMatch().get(IPV4_DST))
                                    .build()));
                            OFActionSetField ofActionSetField = this.factory
                                    .actions()
                                    .buildSetField()
                                    .setField(this.factory.oxms().ethSrc(MacAddress.of(sw.getTenantId())))
                                    .build();
                            this.approvedActions.add(0, ofActionSetField);
                            log.info("approved action in OF1.3 = {}", approvedActions);




                        }
                    }
                } else {
                    log.info("Inport is not edge");

                    if (OVXmodeHandler.getOVXmode() == 0) {
                        this.modifyMatch(
                                IPMapper.rewriteMatch(
                                        sw.getTenantId(),
                                        this.getFlowMod().getMatch()
                                )
                        );
                    }
                    // TODO: Verify why we have two send points... and if this is
                    // the right place for the match rewriting
                    if (inPort != null
                            && inPort.isLink()
                            && this.getFlowMod().getMatch().get(MatchField.ETH_DST) != null
                            && this.getFlowMod().getMatch().get(MatchField.ETH_SRC) != null
                            ) {

                        OVXPort dstPort = sw.getMap()
                                .getVirtualNetwork(sw.getTenantId())
                                .getNeighborPort(inPort);

                        OVXLink link = sw.getMap()
                                .getVirtualNetwork(sw.getTenantId())
                                .getLink(dstPort, inPort);
                        // rewrite the OFMatch with the values of the link

                        if (inPort != null && link != null) {


                            Integer flowId = sw.getMap()
                                    .getVirtualNetwork(sw.getTenantId())
                                    .getFlowManager()
                                    .getFlowId(
                                            this.getFlowMod().getMatch().get(MatchField.ETH_SRC).getBytes(),
                                            this.getFlowMod().getMatch().get(MatchField.ETH_DST).getBytes());


                            if (OVXmodeHandler.getOVXmode() == 0) {
                                OVXLinkUtils lUtils = new OVXLinkUtils(sw.getTenantId(), link.getLinkId(), flowId);

                                //this.log.info("before " + this.getFlowMod().getMatch().toString());

                                this.modifyMatch(lUtils.rewriteMatch(this.getFlowMod().getMatch()));

                                //this.log.info("after " + this.getFlowMod().getMatch().toString());
                            }

                            //AggFlow:
                            else {

                                OVXLinkUtils lUtils1 = new OVXLinkUtils(sw.getTenantId(), link.getLinkId(), flowId, link.getSrcSwitch());
                                this.modifyMatch(lUtils1.rewriteMatch(this.getFlowMod().getMatch()));


                                //AggFlow:  Check outPort is edge
                                //When outPort is edge, then check all conditions.
                                isedgeOut = isEdgeOutport();
                                if (isedgeOut) {
                                    //True-->set MAC as (source's MAC, des's MAC) and IP as (src's IP, dst's IP)
                                    //log.info("outPort is edge, check all conditions:-");

                                    this.modifyMatch(OVXMessageUtil.updateMatch(this.getFlowMod().getMatch(), this.getFlowMod().getMatch().createBuilder()
                                            .setExact(MatchField.IN_PORT, this.getFlowMod().getMatch().get(MatchField.IN_PORT))
                                            .setExact(MatchField.ETH_SRC, this.getFlowMod().getMatch().get(MatchField.ETH_SRC))
                                            .setExact(MatchField.ETH_DST, this.getFlowMod().getMatch().get(MatchField.ETH_DST))
                                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                            .setExact(MatchField.IPV4_SRC, this.getFlowMod().getMatch().get(IPV4_SRC))
                                            .setExact(MatchField.IPV4_DST, this.getFlowMod().getMatch().get(IPV4_DST))
                                            .build()));


                                   /* this.getFlowMod().getMatch().createBuilder()
                                            .wildcard(MatchField.IN_PORT)
                                            .setExact(MatchField.IN_PORT, OFPort.of(inPort.getPhysicalPortNumber()))
                                            .build();

                                    this.getFlowMod().getMatch().createBuilder()
                                            .wildcard(MatchField.ETH_SRC)
                                            .wildcard(MatchField.ETH_DST)
                                            .setExact(MatchField.ETH_SRC, MacAddress.of(lUtils1.getSrcMac().getBytes()))
                                            .setExact(MatchField.ETH_DST, MacAddress.of(lUtils1.getDstMac().getBytes()))
                                            .build();

                                    this.getFlowMod().getMatch().createBuilder()
                                            .wildcard(MatchField.ETH_TYPE)
                                            .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                                            .build();

*/



                                    //log.info("match when inport not edge after setting wildcard = {}", this.getFlowMod().getMatch());
                                }
                            }
                        }
                    }
                }
            }catch (NetworkMappingException e) {
                log.warn(
                        "OVXFlowMod. Error retrieving the network with id {} for flowMod {}. Dropping packet...",
                        this.sw.getTenantId(), this);
            } catch (DroppedMessageException e) {
                log.warn(
                        "OVXFlowMod. Error retrieving flowId in network with id {} for flowMod {}. Dropping packet...",
                        this.sw.getTenantId(), this);
            } catch (IndexOutOfBoundException e) {
                e.printStackTrace();
            }

            if(OVXmodeHandler.getOVXmode()==0) {
                this.setOFMessage(this.getFlowMod().createBuilder()
                        .setActions(this.approvedActions)
                        .build());
                if (pflag) {
                    if(!this.getFlowMod().getFlags().contains(OFFlowModFlags.SEND_FLOW_REM))
                        this.getFlowMod().getFlags().add(OFFlowModFlags.SEND_FLOW_REM);
                    sw.sendSouth(this, inPort);
                }
            } else{
                //AggFlow: In core, check that rule is duplicated.
                if (!isedgeOut) {
                    duflag = phyFlowTable.checkduplicate(this);
                    this.log.info("DuFlag is {}", duflag);
                }

                this.setOFMessage(this.getFlowMod().createBuilder()
                        .setActions(this.approvedActions)
                        .build()
                );
                //Rule is not installed in physical switch, then send south.

                if (!duflag && pflag) {

                    if (!this.getFlowMod().getFlags().contains(OFFlowModFlags.SEND_FLOW_REM)) {
                        this.getFlowMod().getFlags().add(OFFlowModFlags.SEND_FLOW_REM);}
                        sw.sendSouth(this, inPort);

                }
            }
    }




    private void prependRewriteActions() {
        if(this.getOFMessage().getVersion() == OFVersion.OF_10)
            prependRewriteActionsVer10();
        else
            prependRewriteActionsVer13();

    }

    private void prependRewriteActionsVer13() {
        if(this.getFlowMod().getMatch().get(MatchField.IPV4_SRC) != null) {
            OFActionSetField ofActionSetField = this.factory.actions().buildSetField()
                    .setField(this.factory.oxms().ipv4Src(IPv4Address.of(
                            IPMapper.getPhysicalIp(sw.getTenantId(),
                                    this.getFlowMod().getMatch().get(MatchField.IPV4_SRC).getInt()))))
                    .build();
            this.approvedActions.add(0, ofActionSetField);
        }

        if(this.getFlowMod().getMatch().get(MatchField.IPV4_DST) != null) {
            OFActionSetField ofActionSetField = this.factory.actions().buildSetField()
                    .setField(this.factory.oxms().ipv4Dst(IPv4Address.of(
                            IPMapper.getPhysicalIp(
                                    sw.getTenantId(),
                                    this.getFlowMod().getMatch().get(MatchField.IPV4_DST).getInt()))))
                    .build();
            this.approvedActions.add(0, ofActionSetField);
        }
    }

    private void prependRewriteActionsVer10() {
        if(this.getFlowMod().getMatch().get(MatchField.IPV4_SRC) != null) {
            OFAction action = this.factory.actions().buildSetNwSrc()
                    .setNwAddr(IPv4Address.of(
                            IPMapper.getPhysicalIp(
                                    sw.getTenantId(),
                                    this.getFlowMod().getMatch().get(MatchField.IPV4_SRC).getInt())))
                    .build();

            this.approvedActions.add(0, action);
        }

        if(this.getFlowMod().getMatch().get(MatchField.IPV4_DST) != null) {
            OFAction action = this.factory.actions().buildSetNwDst()
                    .setNwAddr(IPv4Address.of(
                            IPMapper.getPhysicalIp(sw.getTenantId(),
                                    this.getFlowMod().getMatch().get(MatchField.IPV4_DST).getInt())))
                    .build();

            this.approvedActions.add(0, action);
        }
    }


    public OVXFlowMod clone() {
        OVXFlowMod flowMod = new OVXFlowMod(this.getOFMessage().createBuilder().build());
        return flowMod;
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (this.getFlowMod().getMatch() != null) {
            map.put("match", new OVXMatch(this.getFlowMod().getMatch()).toMap());
        }
        LinkedList<Map<String, Object>> actions = new LinkedList<Map<String, Object>>();
        for (OFAction act : this.getFlowMod().getActions()) {
            try {
                actions.add(OVXUtil.actionToMap(act));
            } catch (UnknownActionException e) {
                log.warn("Ignoring action {} because {}", act, e.getMessage());
            }
        }
        map.put("actionsList", actions);
        map.put("priority", String.valueOf(this.getFlowMod().getPriority()));
        return map;
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }



    /**AggFlow:
     * Check outport which indicate output action is edge.
     * @return true if out port is edge
     */
    private boolean isEdgeOutport(){
        short outport = 0;
        if(this.getFlowMod().getActions().size()==0){
            return false;
        }

        for(final OFAction act : this.getFlowMod().getActions()){
            if(act.getType()==OFActionType.OUTPUT){
                OFActionOutput outact = (OFActionOutput) act;
                outport = outact.getPort().getShortPortNumber();
            }
        }

        OVXPort outPort = sw.getPort(outport);
        if(outPort.isEdge())
        {
            return true;}
        else{
            return false;}
    }

    /**
     * Gets the host ip.
     * @param host
     * @return the host ip address
     */
    private IPAddress getHostIP(Host host)
    {
        if(host!=null){
            return host.getIp();}
        else{
            return null;}
    }

    /**
     * Gets the host instance by MACAddress.
     * @param mac
     * @return the host
     */
    private Host getHostbyMACAddress(byte[] mac){
        OVXMap map = OVXMap.getInstance();

        try {
           // log.info("getHostbyMACAddress", map.getVirtualNetwork(sw.getTenantId()).getHost(MacAddress.of(mac)));
            return map.getVirtualNetwork(sw.getTenantId()).getHost(MacAddress.of(mac));
        } catch (NetworkMappingException e) {
            log.error(e);
        }
        return null;
    }
}

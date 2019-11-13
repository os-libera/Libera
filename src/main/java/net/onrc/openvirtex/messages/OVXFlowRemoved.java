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

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalFlowTable;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.OVXmodes.OVXmodeHandler;
import net.onrc.openvirtex.elements.OVXmodes.OVXmodeManager;
import net.onrc.openvirtex.exceptions.MappingException;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.services.forwarding.mpls.MplsForwarding;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPathBuilder;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPath;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPathBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFMessage;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;

import java.util.List;

public class OVXFlowRemoved extends OVXMessage implements Virtualizable {

    Logger log = LogManager.getLogger(OVXFlowRemoved.class.getName());

    public OVXFlowRemoved(OFMessage msg) {

        super(msg);
    }



    public OFFlowRemoved getFlowRemoved() {


        return (OFFlowRemoved)this.getOFMessage();
    }

    @Override
    public void virtualize(final PhysicalSwitch sw) {
        this.log.debug("virtualize");

        this.log.debug(this.getFlowRemoved().toString());


        long thisCookie = this.getFlowRemoved().getCookie().getValue();
        int tid = (int) ( thisCookie >> 32);


        /*AggFLow:
    	 * Get the physical flow table in this physical switch.
    	 * If this flow was aggregated, all virtual flow revive and each FlowRevmoved send to each controller.
    	 */
        PhysicalFlowTable phyFlowTable = sw.getEntrytable();

        if(OVXmodeHandler.getOVXmode()==1) {
            //for VM
            Integer pathID = MplsForwarding.getInstance().getPathIDFromMatch(this.getFlowRemoved().getMatch(), sw, tid);
            //Integer flowId = null;// = PhysicalPathBuilder.getInstance().getFlowID(this.getFlowRemoved().getCookie());

            //log.info("Cookie {}", getFlowRemoved().getCookie().toString());

            if (pathID != null) {
                log.debug("PathID != null pathID [{}]", pathID);
                VirtualPath vPath = VirtualPathBuilder.getInstance().getVirtualPath(pathID);
                if (vPath != null) {
                    log.debug("vPath != null");
                    if (vPath.isMigrated()) {
                        log.debug("PathID [{}] is migrated", pathID);
                        return;
                    } else {
                        log.debug("PathID [{}] is not migrated", pathID);
                    }
                } else {
                    log.debug("vPath == null");
                    return;
                }
            } else {
                log.info("PathID == null " + this.getFlowRemoved().toString());
            }
        }

        /* a PhysSwitch can be a OVXLink */
        if (!(sw.getMap().hasVirtualSwitch(sw, tid))) {
            return;
        }
        try {
            OVXSwitch vsw = sw.getMap().getVirtualSwitch(sw, tid);
            /*
             * If we are a Big Switch we might receive multiple same-cookie FR's
             * from multiple PhysicalSwitches. Only handle if the FR's newly
             * seen
             */
            if (vsw.getFlowTable().hasFlowMod(thisCookie)) {
                OVXFlowMod fm = vsw.getFlowMod(thisCookie);

                /*
                 * send north ONLY if tenant controller wanted a FlowRemoved for
                 * the FlowMod
                 */

                if(OVXmodeHandler.getOVXmode() != 2) {
                    vsw.deleteFlowMod(thisCookie);
                    if (fm.getFlowMod().getFlags().contains(OFFlowModFlags.SEND_FLOW_REM)) {
                        this.setOFMessage(
                                this.getFlowRemoved().createBuilder()
                                        .setCookie(fm.getFlowMod().getCookie())
                                        .setMatch(fm.getFlowMod().getMatch())
                                        .setPriority(fm.getFlowMod().getPriority())
                                        .setIdleTimeout(fm.getFlowMod().getIdleTimeout())
                                        .build()
                        );
                        log.info("send to flowremoved to controller {}", this.getFlowRemoved().toString());
                        vsw.sendMsg(this, sw);
                    }
                }else {

                 /*AggFLow:  Get the ActionOutput */
                    OFActionOutput outact = null;
                    for (final OFAction act : fm.getFlowMod().getActions()) {
                        if (act.getType() == OFActionType.OUTPUT) {
                            outact = (OFActionOutput) act;
                        }
                    }

                /* All cookie which was aggregated to this flow is retrieved */
                    List<Long> cookieSet = phyFlowTable.removeEntry(new OVXMatch(fm.getFlowMod().getMatch()), outact, thisCookie);

                    if (cookieSet != null) {
                        for (Long cookies : cookieSet) {
                            int temptid = (int) (cookies >> 32);
                            if (sw.getMap().hasVirtualSwitch(sw, temptid)) {
                                vsw = sw.getMap().getVirtualSwitch(sw, temptid);

                                if (vsw.getFlowTable().hasFlowMod(cookies)) {
                                    OVXFlowMod tempFM = vsw.getFlowMod(cookies);
                                    vsw.deleteFlowMod(cookies);

                                    if (tempFM.getFlowMod().getFlags().contains(OFFlowModFlags.SEND_FLOW_REM)) {
                                        this.setOFMessage(
                                                this.getFlowRemoved().createBuilder()
                                                        .setCookie(fm.getFlowMod().getCookie())
                                                        .setMatch(fm.getFlowMod().getMatch())
                                                        .setPriority(fm.getFlowMod().getPriority())
                                                        .setIdleTimeout(fm.getFlowMod().getIdleTimeout())
                                                        .build()
                                        );
                                        vsw.sendMsg(this, sw);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (MappingException e) {
            log.warn("Exception fetching FlowMod from FlowTable: {}", e);
        }
    }
    @Override
    public String toString() {
        return "OVXFlowRemoved: cookie = " + this.getFlowRemoved().getCookie().getValue()
                + " priority = " + this.getFlowRemoved().getPriority()
                + " match = " + this.getFlowRemoved().getMatch().toString()
                + " reason = " + this.getFlowRemoved().getReason();
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }
}

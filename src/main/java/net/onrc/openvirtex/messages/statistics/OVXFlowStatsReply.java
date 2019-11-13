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
package net.onrc.openvirtex.messages.statistics;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.messages.OVXStatisticsReply;
import net.onrc.openvirtex.services.forwarding.mpls.MplsForwarding;
import net.onrc.openvirtex.services.forwarding.mpls.MplsLabel;
import net.onrc.openvirtex.services.path.Node;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPath;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPathBuilder;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPath;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPathBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import net.onrc.openvirtex.elements.OVXmodes.OVXmodeHandler;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class OVXFlowStatsReply extends OVXStatistics implements VirtualizableStatistic {

    Logger log = LogManager.getLogger(OVXFlowStatsReply.class.getName());

    protected OFFlowStatsReply ofFlowStatsReply;
    protected OFFlowStatsEntry ofFlowStatsEntry;

    public OVXFlowStatsReply(OFMessage ofMessage) {
        super(OFStatsType.FLOW);
        this.ofFlowStatsReply = (OFFlowStatsReply)ofMessage;
        this.ofFlowStatsEntry = null;
    }

    public OVXFlowStatsReply(OFMessage ofMessage, OFFlowStatsEntry ofFlowStatsEntry) {
        super(OFStatsType.FLOW);
        this.ofFlowStatsReply = (OFFlowStatsReply)ofMessage;
        this.ofFlowStatsEntry = ofFlowStatsEntry;
    }

    public void setOFMessage(OFMessage ofMessage) {
        this.ofFlowStatsReply = (OFFlowStatsReply)ofMessage;
    }

    public OFMessage getOFMessage() {
        return this.ofFlowStatsReply;
    }

    public OFFlowStatsEntry getOFFlowStatsEntry() {
        return this.ofFlowStatsEntry;
    }

    @Override
    public void virtualizeStatistic(final PhysicalSwitch sw, final OVXStatisticsReply msg) {
        this.log.debug("virtualizeStatistic");
        this.log.debug(msg.getOFMessage().toString());
        if (msg.getOFMessage().getXid() != 0) {
            sw.removeFlowMods(msg);
            return;
        }

        //this.log.info("[{}] virtualizeStatistic Before {}", System.currentTimeMillis(), msg.getOFMessage().toString());


        HashMap<Integer, List<OFFlowStatsEntry>> stats = new HashMap<Integer, List<OFFlowStatsEntry>>();

        OFFlowStatsReply ofFlowStatsReply = (OFFlowStatsReply)msg.getOFMessage();


        if(OVXmodeHandler.getOVXmode() == 1) {

            for (OFFlowStatsEntry stat : ofFlowStatsReply.getEntries()) {
                try {
                    if (!stat.getCookie().equals(U64.of(0))) {                       //except default flowentries
                        int tid = getTidFromCookie(stat.getCookie().getValue());
                        //addToStats(tid, stat, stats);

                        if (stat.getMatch().get(MatchField.ETH_SRC) != null && stat.getMatch().get(MatchField.ETH_DST) != null) {    //for edge switches
                            //int tid = getTidFromCookie(stat.getCookie().getValue());
                            log.debug("For MAC TID [{}] {}/{}", tid,
                                    stat.getMatch().get(MatchField.ETH_SRC).toString(),
                                    stat.getMatch().get(MatchField.ETH_DST).toString()
                            );
                            //addToStats(tid, stat, stats);

                            Integer pathID = MplsForwarding.getInstance().getPathIDFromMatch(stat.getMatch(), sw, tid);

                            if (pathID != null) {
                                VirtualPath vPath = VirtualPathBuilder.getInstance().getVirtualPath(pathID);
                                PhysicalPath pPath = vPath.getPhysicalPath();
                                Node node = pPath.getCorrespondingNode(sw);

                                if (node != null) {
                                    log.debug("PhysicalSwitch is exist {} in pPath", node.toString());
                                    node.setFlowStatsEntry(stat);
                                    //addToStats(tid, stat, stats);
                                } else {
                                    // pPath? ?? mPath? ?? ??
                                    if (vPath.isMigrated()) {
                                        node = vPath.getMigratedPhysicalPath().getCorrespondingNode(sw);

                                        if (node != null) {
                                            log.debug("PhysicalSwitch is exist {} in mPath", node.toString());
                                            node.setFlowStatsEntry(stat);
                                            //addToStats(tid, stat, stats);
                                        }
                                    }
                                }
                            }
                        } else if (stat.getMatch().get(MatchField.MPLS_LABEL) != null) {    //for intermediate switches
                            //this.log.info("MPLS_LABEL " + match.get(MatchField.MPLS_LABEL).toString());
                            MplsLabel label = MplsForwarding.getInstance().getMplsLabel(stat.getMatch().get(MatchField.MPLS_LABEL).getRaw());

                            if (label != null) {
                                //int tid = label.getTenantID();
                                log.debug("For MPLS TID [{}] {}", tid, U32.of(label.getLabelValue()).toString());

                                VirtualPath vPath;
                                PhysicalPath pPath;
                                Node node;

                                OFFlowStatsEntry relatedEntry;
                                OFFlowStatsEntry temp;

                                LinkedList<Integer> pathIDs = new LinkedList<>();
                                pathIDs.addAll(label.getPathIDs());
                                pathIDs.add(label.getOriginalPathID());

                                for (Integer pathID : pathIDs) {
                                    vPath = VirtualPathBuilder.getInstance().getVirtualPath(pathID);
                                    pPath = vPath.getPhysicalPath();
                                    node = pPath.getCorrespondingNode(sw);

                                    if (node != null) {
                                        log.debug("PhysicalSwitch is exist {} in pPath", node.toString());

                                        relatedEntry = getRelatedEntry(tid, pPath);

                                        if (relatedEntry == null) {
                                            log.debug("1. OFFlowStatsEntry is created");
                                            temp = makeFlowStatsEntry(stat, node.getmFlowMod());

                                        } else {
                                            log.debug("2. OFFlowStatsEntry is created");
                                            temp = relatedEntry.createBuilder()
                                                    .setCookie(node.getmFlowMod().getCookie())
                                                    .build();
                                        }

                                        node.setFlowStatsEntry(temp);
                                        //addToStats(tid, temp, stats);
                                    } else {
                                        if (vPath.isMigrated()) {
                                            node = vPath.getMigratedPhysicalPath().getCorrespondingNode(sw);

                                            if (node != null) {
                                                log.debug("PhysicalSwitch is exist {} in mPath", node.toString());
                                                relatedEntry = getRelatedEntry(tid, pPath);

                                                if (relatedEntry == null) {
                                                    log.debug("3. OFFlowStatsEntry is created");
                                                    temp = makeFlowStatsEntry(stat, node.getmFlowMod());

                                                } else {
                                                    log.debug("4. OFFlowStatsEntry is created");
                                                    temp = relatedEntry.createBuilder()
                                                            .setCookie(node.getmFlowMod().getCookie())
                                                            .build();
                                                }
                                                node.setFlowStatsEntry(temp);
                                                //addToStats(tid, temp, stats);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        //this.log.info("[{}] virtualizeStatistic After {}", System.currentTimeMillis(), msg.getOFMessage().toString());
                        //addToStats(tid, stat, stats);
                    }
                } catch (NullPointerException e) {
                    log.info("null value caught");
                }
            }
        }else {
            for (OFFlowStatsEntry stat : ofFlowStatsReply.getEntries()) {
                int tid = getTidFromCookie(stat.getCookie().getValue());
                addToStats(tid, stat, stats);
                sw.setFlowStatistics(stats);
            }
        }
    }

    public OFFlowStatsEntry makeFlowStatsEntry(OFFlowStatsEntry oriEntry, OFFlowMod flowMod) {
        OFFlowStatsEntry temp = OFFactories.getFactory(this.getOFMessage().getVersion()).buildFlowStatsEntry()
                .setDurationSec(oriEntry.getDurationSec())
                .setDurationNsec(oriEntry.getDurationNsec())
                .setTableId(flowMod.getTableId())
                .setPriority(flowMod.getPriority())
                .setByteCount(oriEntry.getByteCount())
                .setPacketCount(oriEntry.getPacketCount())
                .setFlags(flowMod.getFlags())
                .setCookie(flowMod.getCookie())
                .build();

        return temp;

    }

    private OFFlowStatsEntry getRelatedEntry(int tid, PhysicalPath pPath) {

        PhysicalSwitch psw = (PhysicalSwitch)pPath.getSrcSwitch().getSwitch();
        List<OFFlowStatsEntry> entries = psw.getFlowStats(tid);

        if(entries != null) {
            for(OFFlowStatsEntry entry : entries) {
                if(entry.getMatch().get(MatchField.ETH_SRC).equals(pPath.getSrcHost().getMac()) &&
                        entry.getMatch().get(MatchField.ETH_DST).equals(pPath.getDstHost().getMac())) {
                    log.debug("Find matching entry of SrcSwitch for {}", entry.getMatch().toString());
                    return entry;

                }
            }

        }

        psw = (PhysicalSwitch)pPath.getDstSwitch().getSwitch();
        entries = psw.getFlowStats(tid);

        if(entries != null) {
            for(OFFlowStatsEntry entry : entries) {
                if(entry.getMatch().get(MatchField.ETH_SRC).equals(pPath.getSrcHost().getMac()) &&
                        entry.getMatch().get(MatchField.ETH_DST).equals(pPath.getDstHost().getMac())) {
                    log.debug("Find matching entry of DstSwitch for {} ",  entry.getMatch().toString());
                    return entry;

                }
            }
        }
        return null;
    }

    private void addToStats(int tid, OFFlowStatsEntry entry,
                            HashMap<Integer, List<OFFlowStatsEntry>> stats) {
        List<OFFlowStatsEntry> statsList = stats.get(tid);
        if (statsList == null) {
            statsList = new LinkedList<OFFlowStatsEntry>();
        }
        statsList.add(entry);
        stats.put(tid, statsList);
    }

    private int getTidFromCookie(long cookie) {
        return (int) (cookie >> 32);
    }

    @Override
    public int hashCode() {
        return this.ofFlowStatsReply.hashCode();
    }
}

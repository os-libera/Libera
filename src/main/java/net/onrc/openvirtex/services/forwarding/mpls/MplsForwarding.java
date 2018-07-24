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
package net.onrc.openvirtex.services.forwarding.mpls;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.messages.OVXMessageUtil;
import net.onrc.openvirtex.services.path.Node;
import net.onrc.openvirtex.services.path.Path;
import net.onrc.openvirtex.services.path.PathUtil;
import net.onrc.openvirtex.services.path.SwitchType;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPath;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPathBuilder;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPath;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPathBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;

import java.util.*;

public class MplsForwarding {
    private static MplsForwarding instance;

    private static Logger log = LogManager.getLogger(MplsForwarding.class.getName());
    private static OFFactory factory;

    private static LinkedList<MplsLabel> labels;
    private static LinkedList<MplsLabel> removedLabels;

    //for failover
    //private static LinkedList<MplsLabel> failoverLabels;

    public MplsForwarding() {
        MplsForwarding.log.info("Starting MplsForwarding");
        factory = OFFactories.getFactory(OFVersion.OF_13);

        labels = new LinkedList<>();
        removedLabels = new LinkedList<>();

        //failoverLabels = new LinkedList<>();
    }

    public MplsLabel getMplsLabel(int labelvalue) {
        for(MplsLabel label : labels) {
            if(label.getLabelValue() == labelvalue)
                return label;
        }

        return null;
    }

    public MplsLabel getRemovedMplsLabel(int labelvalue) {
        for(MplsLabel label : removedLabels) {
            if(label.getLabelValue() == labelvalue)
                return label;
        }

        return null;
    }

    public synchronized static MplsForwarding getInstance() {
        if (MplsForwarding.instance == null) {
            MplsForwarding.instance = new MplsForwarding();
        }
        return MplsForwarding.instance;
    }

    public MplsLabel isContainedMplsLabel(int value) {
        for(MplsLabel label : labels) {
            if(label.getLabelValue() == value)
                return label;
        }
        return null;
    }

    public MplsLabel isContainedRemovedMplsLabel(int value) {
        for(MplsLabel label : removedLabels) {
            if(label.getLabelValue() == value)
                return label;
        }
        return null;
    }

    /*public void removeMplsLabel(int value) {
        for(MplsLabel label : labels) {
            if(label.getLabelValue() == value) {
                log.info("Label {} is removed",  U32.of(label.getLabelValue()).toString() );
                labels.remove(label);
                break;
            }
        }

    }*/

    public OFFlowMod addMplsActions(PhysicalPath pPath, VirtualPath vPath, OFFlowMod mFlowMod, SwitchType type) {

        int labelValue = makeLabel(vPath.getTenantID(), mFlowMod);
        MplsLabel label = isContainedMplsLabel(labelValue);

        if(label != null) {
            if(label.isOriginalPathID(vPath.getPathID())) {

            }else{

                if(label.isContainedPathID(vPath.getPathID())) {
                }else{
                    label.addPathID(vPath.getPathID());
                    pPath.setNotOriginalPathID(label);

                    //PhysicalPath oriPath = PhysicalPathBuilder.getInstance().getPhysicalPath(label.getOriginalPathFlowID());
                    //oriPath.addReferencedPathFlowID(vPath.getFlowID());

                    log.info("2. Assigned exist MPLS Label [{}] for PathID [{}] Original PathID [{}]",
                            U32.of(label.getLabelValue()).toString(), vPath.getPathID(),
                            label.getOriginalPathID());
                }
            }
        }else{
            label = new MplsLabel(labelValue, vPath.getPathID());
            labels.add(label);
            pPath.setItselfOriginalPathID(label);
            //pPath.setOriginalPath(true);
            //pPath.setMplsLabel(label);

            log.info("1. Assigned MPLS Label [{}] for PathID [{}] Original PathID [{}]",
                     U32.of(label.getLabelValue()).toString(), vPath.getPathID(),
                    label.getOriginalPathID());
        }

        OFFlowMod ofFlowMod = mFlowMod.createBuilder().build();
        List<OFAction> actions = ofFlowMod.getActions();

        Match match = ofFlowMod.getMatch().createBuilder().build();

        switch(type) {
            case INGRESS:
                OFActionSetField actionSetMplsLabel = factory.actions().buildSetField()
                        .setField(factory.oxms().mplsLabel(U32.of(label.getLabelValue())))
                        .build();
                actions.add(0, actionSetMplsLabel);

                OFActionPushMpls actionPushMpls = factory.actions().buildPushMpls()
                        .setEthertype(EthType.MPLS_UNICAST)
                        .build();
                actions.add(0, actionPushMpls);

                ofFlowMod = ofFlowMod.createBuilder().setMatch(match).setActions(actions).build();
                break;
            case INTERMEDIATE:
                match = ofFlowMod.getMatch().createBuilder()
                        .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                        .setExact(MatchField.MPLS_LABEL, U32.of(label.getLabelValue()))
                        .build();

                ofFlowMod = ofFlowMod.createBuilder().setMatch(match).build();
                break;
            case EGRESS:
                OFActionPopMpls actionPopMpls = factory.actions().buildPopMpls()
                        .setEthertype(EthType.IPv4)
                        .build();
                actions.add(0, actionPopMpls);

                match = ofFlowMod.getMatch().createBuilder()
                        .setExact(MatchField.ETH_SRC, match.get(MatchField.ETH_SRC))
                        .setExact(MatchField.ETH_DST, match.get(MatchField.ETH_DST))
                        .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                        .setExact(MatchField.MPLS_LABEL, U32.of(label.getLabelValue()))
                        .build();

                ofFlowMod = ofFlowMod.createBuilder().setMatch(match).setActions(actions).build();
                break;
            default:
                break;
        }
        return ofFlowMod;
    }


    //for migration
    public void addMplsActions(PhysicalPath oldPath, PhysicalPath newPath) {
        int labelValue = makeLabel(oldPath);
        MplsLabel oldLabel = isContainedMplsLabel(labelValue);
        MplsLabel newLabel = null;
        MplsLabel label = null;

        if(oldLabel != null) {
            if (oldPath.isOriginalPath()) {
                log.info("OldPath is OriginalPath");
                if (oldPath.getMplsLabel().getPathIDs().size() == 0) {  // none of reference FlowId
                    log.info("OldPath has no Refs");
                    newLabel = isContainedMplsLabel(makeLabel(newPath));
                    if(newLabel == null){
                        removedLabels.add(oldLabel);
                        labels.remove(oldLabel);

                        newLabel = new MplsLabel(makeLabel(newPath), newPath.getPathID());
                        labels.add(newLabel);
                        newPath.setItselfOriginalPathID(newLabel);

                        log.info("1. ReAssigned new MPLS Label [{}] for Original PathID [{}] with Refs[0]"
                                ,U32.of(newLabel.getLabelValue()).toString(), newPath.getPathID());

                        label = newLabel;
                    }else{
                        newLabel.addPathID(newPath.getPathID());
                        newPath.setNotOriginalPathID(newLabel);

                        log.info("2. ReAssigned exist MPLS Label [{}] for Referenced PathID [{}] with Refs[0]"
                                ,U32.of(newLabel.getLabelValue()).toString(), newPath.getPathID());

                        label = newLabel;
                    }
                } else {
                    log.info("OldPath has Refs[{}]", oldPath.getMplsLabel().getPathIDs().size());

                    int newOriginalPathID = oldLabel.changeOriginalPathID();
                    log.info("Change Original PathID [{}]->[{}] for MPLS Label [{}]", oldPath.getPathID(),
                            newOriginalPathID,
                            U32.of(oldLabel.getLabelValue()).toString());

                    //update OriginPathFlowID for the Flows of label
                    PhysicalPath newOriginalPath = PhysicalPathBuilder.getInstance().getPhysicalPath(newOriginalPathID);
                    newOriginalPath.setItselfOriginalPathID(oldLabel);

                    log.info("Update Referenced {} Flows to new OriginalPathID [{}]", newOriginalPath.getMplsLabel().getPathIDs().size(),
                            newOriginalPath.getPathID());
                    for(Integer pathID : newOriginalPath.getMplsLabel().getPathIDs()) {
                        VirtualPath tempVPath = VirtualPathBuilder.getInstance().getVirtualPath(pathID);
                        if(tempVPath.getMigratedPhysicalPath() != null) {
                            if(tempVPath.getMigratedPhysicalPath().getMplsLabel().equals(newOriginalPath.getMplsLabel())) {
                                int temp = tempVPath.getMigratedPhysicalPath().getOriginalPathID();
                                tempVPath.getMigratedPhysicalPath().setNotOriginalPathID(newOriginalPath.getMplsLabel());

                                log.info("PathID [{}] Migrated Physical Path [{}] -> [{}]", pathID,
                                        temp, oldLabel.getOriginalPathID());
                            }
                        }else{
                            if(tempVPath.getPhysicalPath().getMplsLabel().equals(newOriginalPath.getMplsLabel())) {
                                int temp = tempVPath.getPhysicalPath().getOriginalPathID();

                                tempVPath.getPhysicalPath().setNotOriginalPathID(newOriginalPath.getMplsLabel());

                                log.info("PathID [{}] Physical Path [{}] -> [{}]", pathID,
                                        temp, oldLabel.getOriginalPathID());
                            }
                        }
                    }
                    //nw label for newPath
                    newLabel = isContainedMplsLabel(makeLabel(newPath));

                    if(newLabel == null){
                        newLabel = new MplsLabel(makeLabel(newPath), newPath.getPathID());
                        labels.add(newLabel);
                        newPath.setItselfOriginalPathID(newLabel);

                        log.info("3. ReAssigned new MPLS Label [{}] for Original PathID [{}] with Refs[{}]",
                                U32.of(newLabel.getLabelValue()).toString(), newPath.getPathID(),
                                newOriginalPath.getMplsLabel().getPathIDs().size());

                        label = newLabel;
                    }else{
                        newLabel.addPathID(newPath.getPathID());
                        newPath.setNotOriginalPathID(newLabel);

                        log.info("4. ReAssigned exist MPLS Label [{}] for Referenced PathID [{}] with Refs[{}]",
                                U32.of(newLabel.getLabelValue()).toString(), newPath.getPathID(),
                                newOriginalPath.getMplsLabel().getPathIDs().size());

                        label = newLabel;
                    }
                }
            } else {
                oldLabel.removePathID(oldPath.getPathID());

                newLabel = isContainedMplsLabel(makeLabel(newPath));

                if(newLabel == null) {
                    newLabel = new MplsLabel(makeLabel(newPath), newPath.getPathID());
                    labels.add(newLabel);
                    newPath.setItselfOriginalPathID(newLabel);

                    log.info("5. ReAssigned new MPLS Label [{}] for Original PathID [{}]"
                            ,U32.of(newLabel.getLabelValue()).toString(), newPath.getPathID());

                    label = newLabel;
                }else{
                    newLabel.addPathID(newPath.getPathID());
                    newPath.setNotOriginalPathID(newLabel);

                    log.info("6. ReAssigned exist MPLS Label [{}] for Referenced PathID [{}]"
                            ,U32.of(newLabel.getLabelValue()).toString(), newPath.getPathID());

                    label = newLabel;
                }
            }
        }else{
            log.info("MplsLabel {} is not exist", labelValue);
            return;
        }




        OFFlowMod mplsFlowMod = null;

        if(newPath.getSrcSwitch() != null) {
            mplsFlowMod = oldPath.getSrcSwitch().getMplsFlowMod().createBuilder().build();

            //log.info("getSrcSwitch Before " + ofFlowMod.toString());
            Match match = mplsFlowMod.getMatch().createBuilder().build();

            match = match.createBuilder()
                    .setExact(MatchField.IN_PORT, OFPort.of(newPath.getSrcSwitch().getInPort().getPortNumber()))
                    .build();

            match = OVXMessageUtil.updateMatch(mplsFlowMod.getMatch(), match);

            //log.info("getSrcSwitch After " + match.toString());

            List<OFAction> actions = new LinkedList<>();

            OFActionPushMpls actionPushMpls = factory.actions().buildPushMpls()
                    .setEthertype(EthType.MPLS_UNICAST)
                    .build();
            actions.add(actionPushMpls);

            OFActionSetField actionSetMplsLabel = factory.actions().buildSetField()
                    .setField(factory.oxms().mplsLabel(U32.of(label.getLabelValue())))
                    .build();
            actions.add(actionSetMplsLabel);

            OFActionOutput ofActionOutput = factory.actions().buildOutput()
                    .setPort(OFPort.of(newPath.getSrcSwitch().getOutPort().getPortNumber()))
                    .build();
            actions.add(ofActionOutput);

            mplsFlowMod = mplsFlowMod.createBuilder()
                    .setPriority(mplsFlowMod.getPriority())
                    .setMatch(match)
                    .setActions(actions)
                    .build();

            newPath.getSrcSwitch().setMplsFlowMod(mplsFlowMod.createBuilder().build());
            //newPath.getSrcSwitch().setmFlowMod(oldPath.getSrcSwitch().getmFlowMod());
            //log.info("getSrcSwitch After " + mplsFlowMod.toString());
        }

        if(newPath.getDstSwitch() != null) {
            mplsFlowMod = oldPath.getDstSwitch().getMplsFlowMod().createBuilder().build();

            //log.info("getDstSwitch Before " + ofFlowMod.toString());
            Match match = mplsFlowMod.getMatch().createBuilder().build();

            match = match.createBuilder()
                    .setExact(MatchField.ETH_SRC, newPath.getSrcHost().getMac())
                    .setExact(MatchField.ETH_DST, newPath.getDstHost().getMac())
                    .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                    .setExact(MatchField.MPLS_LABEL, U32.of(label.getLabelValue()))
                    .build();

            match = OVXMessageUtil.updateMatch(mplsFlowMod.getMatch(), match);

            //log.info("getDstSwitch After " + match.toString());

            List<OFAction> actions = new LinkedList<>();

            OFActionPopMpls actionPopMpls = factory.actions().buildPopMpls()
                    .setEthertype(EthType.IPv4)
                    .build();
            actions.add(actionPopMpls);

            OFActionOutput ofActionOutput = factory.actions().buildOutput()
                    .setPort(OFPort.of(newPath.getDstSwitch().getOutPort().getPortNumber()))
                    .build();
            actions.add(ofActionOutput);

            mplsFlowMod = mplsFlowMod.createBuilder()
                    .setPriority(mplsFlowMod.getPriority())
                    .setMatch(match)
                    .setActions(actions)
                    .build();

            newPath.getDstSwitch().setMplsFlowMod(mplsFlowMod.createBuilder().build());
            //newPath.getDstSwitch().setmFlowMod(oldPath.getDstSwitch().getmFlowMod());
            //log.info("getDstSwitch After " + mplsFlowMod.toString());
        }

        if(newPath.getIntermediate().size() != 0) {
            mplsFlowMod = oldPath.getDstSwitch().getMplsFlowMod().createBuilder().build();

            for (Node node : newPath.getIntermediate()) {

                Match match = mplsFlowMod.getMatch().createBuilder().build();

                match = match.createBuilder()
                        .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                        .setExact(MatchField.MPLS_LABEL, U32.of(label.getLabelValue()))
                        .build();

                List<OFAction> actions = new LinkedList<>();

                OFActionOutput ofActionOutput = factory.actions().buildOutput()
                        .setPort(OFPort.of(node.getOutPort().getPortNumber()))
                        .build();
                actions.add(ofActionOutput);

                mplsFlowMod = mplsFlowMod.createBuilder()
                        .setPriority(mplsFlowMod.getPriority())
                        .setMatch(match)
                        .setActions(actions)
                        .build();

                node.setMplsFlowMod(mplsFlowMod.createBuilder().build());
            }
        }
    }


    public synchronized void addMplsActions(PhysicalPath pPath) {

        int labelValue = makeLabel(pPath.getTenantID(), pPath.getSrcSwitch().getmFlowMod());
        MplsLabel label = isContainedMplsLabel(labelValue);

        if(label != null) {
            if(label.isOriginalPathID(pPath.getPathID())) {

            }else{

                if(label.isContainedPathID(pPath.getPathID())) {
                }else{
                    label.addPathID(pPath.getPathID());
                    pPath.setNotOriginalPathID(label);

                    //PhysicalPath oriPath = PhysicalPathBuilder.getInstance().getPhysicalPath(label.getOriginalPathFlowID());
                    //oriPath.addReferencedPathFlowID(vPath.getFlowID());

                    log.info("2. Assigned exist MPLS Label [{}] for PathID [{}] Original PathID [{}]",
                            U32.of(label.getLabelValue()).toString(), pPath.getPathID(),
                            label.getOriginalPathID());
                }
            }
        }else{
            label = new MplsLabel(labelValue, pPath.getPathID());
            labels.add(label);
            pPath.setItselfOriginalPathID(label);
            //pPath.setOriginalPath(true);
            //pPath.setMplsLabel(label);

            log.info("1. Assigned MPLS Label [{}] for PathID [{}] Original PathID [{}]",
                    U32.of(label.getLabelValue()).toString(), pPath.getPathID(),
                    label.getOriginalPathID());
        }

        OFFlowMod flowMod;
        Match match;
        if(pPath.getSrcSwitch() != null && pPath.getSrcSwitch().getMplsFlowMod() == null) {
            //log.info("SrcSwitch mFlowMod {}", pPath.getSrcSwitch().getmFlowMod().toString());
            LinkedList<OFAction> actions = new LinkedList<>();
            flowMod = pPath.getSrcSwitch().getmFlowMod().createBuilder().build();

            OFActionPushMpls actionPushMpls = factory.actions().buildPushMpls()
                    .setEthertype(EthType.MPLS_UNICAST)
                    .build();
            actions.addLast(actionPushMpls);

            OFActionSetField actionSetMplsLabel = factory.actions().buildSetField()
                    .setField(factory.oxms().mplsLabel(U32.of(label.getLabelValue())))
                    .build();
            actions.addLast(actionSetMplsLabel);

            OFActionOutput actionOutput = factory.actions().buildOutput()
                    .setPort(OFPort.of(pPath.getSrcSwitch().getOutPort().getPortNumber()))
                    .build();
            actions.addLast(actionOutput);

            pPath.getSrcSwitch().setMplsFlowMod(
                    flowMod.createBuilder().setActions(actions).build()
            );

            //log.info("SrcSwitch MplsFlowMod {}", pPath.getSrcSwitch().getMplsFlowMod().toString());
        }

        if(pPath.getDstSwitch() != null && pPath.getDstSwitch().getMplsFlowMod() == null) {
            //log.info("DstSwitch mFlowMod {}", pPath.getDstSwitch().getmFlowMod().toString());
            LinkedList<OFAction> actions = new LinkedList<>();
            flowMod = pPath.getDstSwitch().getmFlowMod().createBuilder().build();

            match = pPath.getDstSwitch().getmFlowMod().getMatch().createBuilder().build();

            OFActionPopMpls actionPopMpls = factory.actions().buildPopMpls()
                    .setEthertype(EthType.IPv4)
                    .build();
            actions.addLast(actionPopMpls);

            OFActionOutput actionOutput = factory.actions().buildOutput()
                    .setPort(OFPort.of(pPath.getDstSwitch().getOutPort().getPortNumber()))
                    .build();
            actions.addLast(actionOutput);

            match = match.createBuilder()
                    .setExact(MatchField.ETH_SRC, match.get(MatchField.ETH_SRC))
                    .setExact(MatchField.ETH_DST, match.get(MatchField.ETH_DST))
                    .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                    .setExact(MatchField.MPLS_LABEL, U32.of(label.getLabelValue()))
                    .build();

            pPath.getDstSwitch().setMplsFlowMod(
                    flowMod.createBuilder().setMatch(match).setActions(actions).build()
            );
            //log.info("DstSwitch MplsFlowMod {}", pPath.getSrcSwitch().getMplsFlowMod().toString());
        }

        if(pPath.getIntermediate().size() != 0) {
            //log.info("size {}", pPath.getIntermediate().size());
            flowMod = pPath.getDstSwitch().getMplsFlowMod().createBuilder().build();

            for (Node node : pPath.getIntermediate()) {
                //log.info("InterSwitch mFlowMod {}", node.getmFlowMod().toString());
                if(node.getMplsFlowMod() == null) {
                    LinkedList<OFAction> actions = new LinkedList<>();

                    match = flowMod.getMatch().createBuilder()
                            .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                            .setExact(MatchField.MPLS_LABEL, U32.of(label.getLabelValue()))
                            .build();

                    OFActionOutput ofActionOutput = factory.actions().buildOutput()
                            .setPort(OFPort.of(node.getOutPort().getPortNumber()))
                            .build();
                    actions.addLast(ofActionOutput);

                    node.setMplsFlowMod(
                            flowMod.createBuilder().setMatch(match).setActions(actions).build()

                    );

                    //log.info("InterSwitch MplsFlowMod {}", node.getMplsFlowMod().toString());
                }
            }
        }
    }

    public synchronized void addMplsActionsForFailOver(PhysicalPath newPath, PhysicalPath oldPath) {
        int labelValue = makeLabel(newPath);
        MplsLabel label = isContainedMplsLabel(labelValue);

        if(label.isOriginalPathID(oldPath.getPathID())) {
            //log.info("OriginalPath");
            newPath.setItselfOriginalPathID(label);
        }else{
            //log.info("Not OriginalPath");
            newPath.setNotOriginalPathID(label);
        }

        removedLabels.add(label);

        OFFlowMod mplsFlowMod = null;

        if(newPath.getSrcSwitch() != null) {
            mplsFlowMod = oldPath.getSrcSwitch().getMplsFlowMod().createBuilder().build();

            //log.info("getSrcSwitch Before " + ofFlowMod.toString());
            Match match = mplsFlowMod.getMatch().createBuilder().build();

            match = match.createBuilder()
                    .setExact(MatchField.IN_PORT, OFPort.of(newPath.getSrcSwitch().getInPort().getPortNumber()))
                    .build();

            match = OVXMessageUtil.updateMatch(mplsFlowMod.getMatch(), match);

            //log.info("getSrcSwitch After " + match.toString());

            List<OFAction> actions = new LinkedList<>();

            OFActionPushMpls actionPushMpls = factory.actions().buildPushMpls()
                    .setEthertype(EthType.MPLS_UNICAST)
                    .build();
            actions.add(actionPushMpls);

            OFActionSetField actionSetMplsLabel = factory.actions().buildSetField()
                    .setField(factory.oxms().mplsLabel(U32.of(label.getLabelValue())))
                    .build();
            actions.add(actionSetMplsLabel);

            OFActionOutput ofActionOutput = factory.actions().buildOutput()
                    .setPort(OFPort.of(newPath.getSrcSwitch().getOutPort().getPortNumber()))
                    .build();
            actions.add(ofActionOutput);

            mplsFlowMod = mplsFlowMod.createBuilder()
                    .setPriority(mplsFlowMod.getPriority())
                    .setMatch(match)
                    .setActions(actions)
                    .build();

            newPath.getSrcSwitch().setMplsFlowMod(mplsFlowMod.createBuilder().build());
            //newPath.getSrcSwitch().setmFlowMod(oldPath.getSrcSwitch().getmFlowMod());
            //log.info("getSrcSwitch After " + mplsFlowMod.toString());
        }

        if(newPath.getDstSwitch() != null) {
            mplsFlowMod = oldPath.getDstSwitch().getMplsFlowMod().createBuilder().build();

            //log.info("getDstSwitch Before " + ofFlowMod.toString());
            Match match = mplsFlowMod.getMatch().createBuilder().build();

            match = match.createBuilder()
                    .setExact(MatchField.ETH_SRC, newPath.getSrcHost().getMac())
                    .setExact(MatchField.ETH_DST, newPath.getDstHost().getMac())
                    .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                    .setExact(MatchField.MPLS_LABEL, U32.of(label.getLabelValue()))
                    .build();

            match = OVXMessageUtil.updateMatch(mplsFlowMod.getMatch(), match);

            //log.info("getDstSwitch After " + match.toString());

            List<OFAction> actions = new LinkedList<>();

            OFActionPopMpls actionPopMpls = factory.actions().buildPopMpls()
                    .setEthertype(EthType.IPv4)
                    .build();
            actions.add(actionPopMpls);

            OFActionOutput ofActionOutput = factory.actions().buildOutput()
                    .setPort(OFPort.of(newPath.getDstSwitch().getOutPort().getPortNumber()))
                    .build();
            actions.add(ofActionOutput);

            mplsFlowMod = mplsFlowMod.createBuilder()
                    .setPriority(mplsFlowMod.getPriority())
                    .setMatch(match)
                    .setActions(actions)
                    .build();

            newPath.getDstSwitch().setMplsFlowMod(mplsFlowMod.createBuilder().build());
            //newPath.getDstSwitch().setmFlowMod(oldPath.getDstSwitch().getmFlowMod());
            //log.info("getDstSwitch After " + mplsFlowMod.toString());
        }

        if(newPath.getIntermediate().size() != 0) {
            mplsFlowMod = oldPath.getDstSwitch().getMplsFlowMod().createBuilder().build();

            for (Node node : newPath.getIntermediate()) {

                Match match = mplsFlowMod.getMatch().createBuilder().build();

                match = match.createBuilder()
                        .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                        .setExact(MatchField.MPLS_LABEL, U32.of(label.getLabelValue()))
                        .build();

                List<OFAction> actions = new LinkedList<>();

                OFActionOutput ofActionOutput = factory.actions().buildOutput()
                        .setPort(OFPort.of(node.getOutPort().getPortNumber()))
                        .build();
                actions.add(ofActionOutput);

                mplsFlowMod = mplsFlowMod.createBuilder()
                        .setPriority(mplsFlowMod.getPriority())
                        .setMatch(match)
                        .setActions(actions)
                        .build();

                node.setMplsFlowMod(mplsFlowMod.createBuilder().build());

                //log.info("getDstSwitch After " + node.getMplsFlowMod().toString());
            }
        }

    }


    public int makeLabel(int tenantID, OFFlowMod flowMod) {

        MacAddress srcMacAddress = flowMod.getMatch().get(MatchField.ETH_SRC);
        MacAddress dstMacAddress = flowMod.getMatch().get(MatchField.ETH_DST);
        int srcSwitchID = 0, dstSwitchID = 0;

        OVXNetwork vnet = null;
        Collection<Host> hosts = null;
        try {
            vnet = OVXMap.getInstance().getVirtualNetwork(tenantID);
            hosts = vnet.getHosts();
        } catch (NetworkMappingException e) {
            e.printStackTrace();
        }

        for(Host host : hosts) {
            if (host.getMac().equals(srcMacAddress)) {
                srcSwitchID = host.getPort().getPhysicalPort().getParentSwitch().getSwitchLocID();
                //log.info("srcSwitchID [" + srcSwitchID + "] for " + host.toString());
                break;
            }
        }

        for(Host host : hosts) {
            if (host.getMac().equals(dstMacAddress)) {
                dstSwitchID = host.getPort().getPhysicalPort().getParentSwitch().getSwitchLocID();
                //log.info("dstSwitchID [" + dstSwitchID + "] for " + host.toString());
                break;
            }
        }

        //log.info("1. makeLabel {}/{}", srcSwitchID, dstSwitchID);

        return srcSwitchID << 13 | dstSwitchID << 6 | tenantID;
    }


    public int makeLabel(PhysicalPath pPath) {

        int srcSwitchID = ((PhysicalSwitch)pPath.getSrcSwitch().getSwitch()).getSwitchLocID();
        int dstSwitchID = ((PhysicalSwitch)pPath.getDstSwitch().getSwitch()).getSwitchLocID();

        //log.info("2. makeLabel {}/{}", srcSwitchID, dstSwitchID);

        return srcSwitchID << 13 | dstSwitchID << 6 | pPath.getTenantID();
    }

    public Integer getPathIDFromMatch(Match match, PhysicalSwitch pSw, int tenantID) {
        Integer pathID = null;
        Integer flowID = null;
        if(match.get(MatchField.ETH_SRC) != null && match.get(MatchField.ETH_DST) != null ) {

            try {
                OVXNetwork vnet = pSw.getMap().getVirtualNetwork(tenantID);
                flowID = vnet.getFlowManager().getFlowValues(
                        match.get(MatchField.ETH_SRC).getBytes(),
                        match.get(MatchField.ETH_DST).getBytes());
            } catch (NetworkMappingException e) {
                e.printStackTrace();
            } catch (IndexOutOfBoundException e) {
                e.printStackTrace();
            }
            pathID = PathUtil.getInstance().makePathID(tenantID, flowID);

            log.debug("PathID {} from ETH_SRC {} and ETH_DST {} TenantID {}", flowID,
                    match.get(MatchField.ETH_SRC),
                    match.get(MatchField.ETH_DST), tenantID);

        }else if(match.get(MatchField.MPLS_LABEL) != null) {
            Integer labelvalue = match.get(MatchField.MPLS_LABEL).getRaw();
            MplsLabel label = getRemovedMplsLabel(labelvalue);

            if(label != null) {
                //log.info("size {}", label.getFlowIDs().size());
                if(label.getTenantID() == tenantID) {
                    pathID = label.getOriginalPathID();

                    log.debug("PathID {} from MPLS_LABEL {}", pathID, match.get(MatchField.MPLS_LABEL).toString());
                }
            }
        }
        return pathID;
    }



}

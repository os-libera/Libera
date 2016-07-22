package net.onrc.openvirtex.services;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.DuplicateIndexException;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessageUtil;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.BitSetIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionPushMpls;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetMplsLabel;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.U32;


import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MplsManager {
    private static MplsManager instance;

    public enum MplsNodeType {
        MPLS_INGRESS,
        MPLS_INTERMEDIATE,
        MPLS_EGRESS
    }

    private static Logger log = LogManager.getLogger(MplsManager.class.getName());

    private static BitSetIndex mplsLabelDistributor = null;

    private static boolean isActive;

    private static ConcurrentHashMap<Integer, MplsLabel> flowMplelableMap;
    private static OFFactory factory;

    private MplsManager() {
        MplsManager.log.info("Starting Mpls Manager");
        // PhysicalNetwork.timer = new HashedWheelTimer();
        //this.discoveryManager = new ConcurrentHashMap<Long, SwitchDiscoveryManager>();
        mplsLabelDistributor =  new BitSetIndex(BitSetIndex.IndexType.MPLS_ID);
        flowMplelableMap = new ConcurrentHashMap<Integer, MplsLabel>();

        factory = OFFactories.getFactory(OFVersion.OF_13);
        isActive = true;
    }

    public static MplsManager getInstance() {
        if (MplsManager.instance == null) {
            MplsManager.instance = new MplsManager();
        }
        return MplsManager.instance;
    }

    public List<OFAction> setMplsActions(MplsNodeType type, Integer flowId, OVXMatch match)
            throws IndexOutOfBoundException, DuplicateIndexException, AddressMappingException {
        if (!isActive)
            return null;

        List<OFAction> actions = new LinkedList<>();

        MplsLabel label = flowMplelableMap.get(flowId);

        if(!isHost(match.getMatch()))
            return null;

        switch(type) {
            case MPLS_INGRESS:
                if(label == null){
                    label = new MplsLabel(mplsLabelDistributor.getNewMplsLabel().intValue());

                    flowMplelableMap.put(flowId, label);
                    log.info("Assigned MPLS Label " + label + " for FlowID[" + flowId + "]");
                }

                OFActionPushMpls actionPushMpls = factory.actions().buildPushMpls()
                        .setEthertype(EthType.MPLS_UNICAST)
                        .build();
                actions.add(actionPushMpls);

                OFActionSetField actionSetMplsLabel = factory.actions().buildSetField()
                        .setField(factory.oxms().mplsLabel(U32.of((long)label.toInt())))
                        .build();
                actions.add(actionSetMplsLabel);
                break;
            case MPLS_INTERMEDIATE:
                if(label != null) {
                    match.setMatch(
                            OVXMessageUtil.updateMatch(
                                    match.getMatch(),
                                    match.getMatch().createBuilder()
                                            .setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST)
                                            .setExact(MatchField.MPLS_LABEL, U32.of((long)label.toInt()))
                                            .build())
                    );

                    log.info("updated Match for MPLS FlowID[" + flowId + "]");
                }
                break;
            case MPLS_EGRESS:
                break;
        }

        return actions;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isHost(Match match) throws AddressMappingException {
        if(match.get(MatchField.ETH_SRC) != null) {
            if(OVXMap.getInstance().getMAC(match.get(MatchField.ETH_SRC)) == null)
                return false;
        }

        if(match.get(MatchField.ETH_DST) != null) {
            if(OVXMap.getInstance().getMAC(match.get(MatchField.ETH_DST)) == null)
                return false;
        }

        return true;
    }
}

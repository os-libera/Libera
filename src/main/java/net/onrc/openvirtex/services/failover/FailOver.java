package net.onrc.openvirtex.services.failover;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.messages.OVXPortStatus;
import net.onrc.openvirtex.routing.ShortestPath;
import net.onrc.openvirtex.services.forwarding.mpls.MplsForwarding;
import net.onrc.openvirtex.services.path.Node;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPath;
import net.onrc.openvirtex.services.path.physicalpath.PhysicalPathBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;

import java.util.*;

/**
 * Created by soulcrime on 2017-12-11.
 */
public class FailOver {
    private static FailOver instance;

    private static Logger log = LogManager.getLogger(FailOver.class.getName());
    private static OFFactory factory;
    private List<PhysicalLink> edges;

    public FailOver() {
        log.info("Starting FailOver Manager");
        factory = OFFactories.getFactory(OFVersion.OF_13);
        edges = new ArrayList<PhysicalLink>(PhysicalNetwork.getInstance().getLinks());
    }

    public synchronized static FailOver getInstance() {
        if (FailOver.instance == null) {
            FailOver.instance = new FailOver();
        }
        return FailOver.instance;
    }

    public boolean removeFailedPhysicalLink(PhysicalLink pLink) {
        if(edges.contains(pLink)) {
            log.info("Failed Link is removed {}", pLink.toString());
            edges.remove(pLink);
            return true;
        }else{
            return false;
        }
    }

    /*
     reason=MODIFY, config=[PORT_DOWN], state=[LINK_DOWN],
     */
    public synchronized void processFailOver(PhysicalSwitch pSw, PhysicalPort pPort, OVXPortStatus ovxPortStatus) {
        OFPortDesc portDesc = ovxPortStatus.getPortStatus().getDesc();

        if (ovxPortStatus.isReason(OFPortReason.DELETE)) {
            // try to remove OVXPort, vLinks, routes

        } else if (ovxPortStatus.isReason(OFPortReason.MODIFY)) {
            if (ovxPortStatus.isState(OFPortState.LINK_DOWN)) {
                // for Link Down
                //pPort.udpateOfPort(portDesc);

                PhysicalPort srcPort = pSw.getPort(portDesc.getPortNo().getShortPortNumber());
                PhysicalPort dstPort = PhysicalNetwork.getInstance().getNeighborPort(srcPort);

                if(srcPort != null && dstPort != null) {
                    PhysicalLink pLink = PhysicalNetwork.getInstance().getLink(srcPort, dstPort);

                    if(pLink != null && removeFailedPhysicalLink(pLink)) {

                        HashSet<Integer> pathIDs = pSw.getPathIDs();

                        for(Integer pathID : pathIDs) {
                            PhysicalPath oldPath = PhysicalPathBuilder.getInstance().getPhysicalPath(pathID);

                            if(oldPath != null && oldPath.isUsingFailedLink(pLink)) {
                                PhysicalPath newPath = oldPath.findPhysicalPath(edges);

                                if(newPath != null) {
                                    log.info("{}", newPath.toString());

                                    removeOldPhysicalPathFlowEntry(oldPath);
                                    oldPath.getvPath().setMigrated(true);
                                    oldPath.getvPath().setMigratedPhysicalPath(newPath);

                                    PhysicalPathBuilder.getInstance().removePhysicalPath(pathID);
                                    PhysicalPathBuilder.getInstance().addPhysicalPath(newPath);

                                    MplsForwarding.getInstance().addMplsActionsForFailOver(newPath, oldPath);
                                    newPath.sendSouth();
                                }
                            }
                        }
                    }
                }
            }else if (!ovxPortStatus.isState(OFPortState.LINK_DOWN)){
                //for Link Recovery

            }
        }
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

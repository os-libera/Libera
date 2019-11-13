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
package net.onrc.openvirtex.services.path.physicalpath;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.link.PhysicalLink;
import net.onrc.openvirtex.elements.port.PhysicalPort;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.OVXMessage;
import net.onrc.openvirtex.routing.ShortestPath;
import net.onrc.openvirtex.services.forwarding.mpls.MplsLabel;
import net.onrc.openvirtex.services.path.Node;
import net.onrc.openvirtex.services.path.Path;
import net.onrc.openvirtex.services.path.PathUtil;
import net.onrc.openvirtex.services.path.SwitchType;
import net.onrc.openvirtex.services.forwarding.mpls.MplsForwarding;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFActionPushVlan;
import org.projectfloodlight.openflow.types.U64;

import java.util.*;

public class PhysicalPath extends Path {
    private static Logger log = LogManager.getLogger(PhysicalPath.class.getName());

    private Map<OFFlowMod, Node> oFlowModmFlowModMap;

    private VirtualPath vPath;

    private boolean isMigrated;
    private boolean isOriginalPath;

    private Integer originalPathID;

    private MplsLabel label = null;


    public PhysicalPath(int flowID, int tenantID, int pathID, VirtualPath vPath) {
        super(flowID, tenantID, pathID);
        this.oFlowModmFlowModMap = new HashMap<>();
        this.vPath = vPath;

        isMigrated = false;
        isOriginalPath = false;
        originalPathID = null;
    }


    public MplsLabel getMplsLabel() {
        return this.label;
    }

    public void setItselfOriginalPathID(MplsLabel label) {
        this.isOriginalPath = true;
        this.originalPathID = this.getPathID();
        this.label = label;
    }

    public void setNotOriginalPathID(MplsLabel label) {
        this.isOriginalPath = false;
        this.originalPathID = label.getOriginalPathID();
        this.label = label;
    }

    public int getOriginalPathID() {
        return this.originalPathID;
    }

    public boolean isOriginalPath() {
        return this.isOriginalPath;
    }

    public VirtualPath getvPath() {
        return this.vPath;
    }

    public OFFlowMod getModifiedFlowMod(OFFlowMod ofFlowMod) {
        return this.oFlowModmFlowModMap.get(ofFlowMod).getOriFlowMod();
    }

    public List<PhysicalSwitch> getSwitches() {
        LinkedList<PhysicalSwitch> switches = new LinkedList<>();

        for(Node node : this.oFlowModmFlowModMap.values()) {
            switches.add((PhysicalSwitch)node.getSwitch());
        }

        return switches;
    }

    public Collection<Node> getNodes() {
        return this.oFlowModmFlowModMap.values();
    }

    public String printPhysicalPathInfo() {
        String str = "PathID = " + this.getPathID() + "\n";
        str = str + "FlowID = " + this.getFlowID() + "\n";
        str = str + "TenantID = " + this.getTenantID() + "\n";

        if(this.getSrcSwitch() != null) {
            str = str + "SrcSwitch = " + this.getSrcSwitch().toString() + "\n";
        }

        if(this.getIntermediate().size() != 0) {
            for(Node node : this.getIntermediate()) {
                //log.info("Intermediate = " + node.toString());
                str = str + "Intermediate = " + node.toString() + "\n";
            }
        }

        if(this.getDstSwitch() != null) {
            str = str + "DstSwitch = " + this.getDstSwitch().toString() + "\n";
        }

        if(this.originalPathID != null) {
            str = str + "OriginalPathFlowID = " + this.originalPathID + " [" + this.isOriginalPath + "]\n";
        }

        return str;
    }

    public PhysicalPath buildPhysicalPath(VirtualPath vPath, OFFlowMod oriFlowMod, OFFlowMod mFlowMod, SwitchType type,
                                       PhysicalSwitch psw) throws IndexOutOfBoundException {

        // log.info("FlowID " + this.getFlowID() + " Physical Path is building");
        OFFlowMod mplsFlowMod = null;
        Node node = null;

        //System.out.printf("isBuildPhysicalPath [%s]\n", ofMessage.toString());
        if(vPath.getSrcHost() != null && this.getSrcHost() == null) {
            this.setSrcHost(vPath.getSrcHost());
        }

        if(vPath.getDstHost() != null && this.getDstHost() == null) {
            this.setDstHost(vPath.getDstHost());
        }

        switch(type) {
            case INGRESS:
                if(this.getSrcSwitch() == null) {
                    //log.info("FlowID [" + this.getFlowID() + "] SrcSwitch is set");
                    mplsFlowMod = MplsForwarding.getInstance().addMplsActions(this, vPath, mFlowMod, type);

                    log.info("INGRESS {}", mplsFlowMod.toString());

                    node = new Node(psw, mplsFlowMod, mFlowMod, oriFlowMod, type);

                    this.setSrcSwitch(node);
                    this.oFlowModmFlowModMap.put(oriFlowMod, node);
                }else{
                    mplsFlowMod = this.getSrcSwitch().getMplsFlowMod();
                }
                break;
            case INTERMEDIATE:
                if(this.oFlowModmFlowModMap.get(oriFlowMod) == null) {

                    //log.info("FlowID [" + this.getFlowID() + "] INTERMEDIATE is set");
                    mplsFlowMod = MplsForwarding.getInstance().addMplsActions(this, vPath, mFlowMod, type);

                    this.oFlowModmFlowModMap.put(oriFlowMod, new Node(psw, mplsFlowMod, mFlowMod,
                            oriFlowMod, type));

                    this.getIntermediate().clear();

                    for (Node n : vPath.getIntermediate()) {
                        Node targetNode = this.oFlowModmFlowModMap.get(n.getOriFlowMod());

                        if(targetNode != null) {
                            //log.info("FlowID [" + this.getFlowID() + "] INTERMEDIATE is set {}", targetNode.toString());
                            this.getIntermediate().add(targetNode);
                            log.info("INTERMEDIATE {}", targetNode.getMplsFlowMod().toString());
                        }
                    }
                }else{
                    mplsFlowMod = this.oFlowModmFlowModMap.get(oriFlowMod).getMplsFlowMod();
                }
                break;
            case EGRESS:
                if(this.getDstSwitch() == null) {
                    //log.info("FlowID [" + this.getFlowID() + "] DstSwitch is set");
                    mplsFlowMod = MplsForwarding.getInstance().addMplsActions(this, vPath, mFlowMod, type);

                    log.info("EGRESS {}", mplsFlowMod.toString());

                    node = new Node(psw, mplsFlowMod, mFlowMod, oriFlowMod, type);

                    this.setDstSwitch(node);
                    this.oFlowModmFlowModMap.put(oriFlowMod, node);
                }else{
                    mplsFlowMod = this.getDstSwitch().getMplsFlowMod();
                }
                break;
            case SAME:
                if(this.getSame() == null) {
                    log.info("this.same == null");
                    this.setSame(new Node(psw, mFlowMod, mFlowMod, oriFlowMod, type));
                }else{
                    log.info("this.same != null");
                }
                break;
        }

        return this;
    }


    // PhysicalPath is calculated in this method.
    public PhysicalPath buildPhysicalPath2(VirtualPath vPath, OFFlowMod oriFlowMod, OFFlowMod mFlowMod, SwitchType type,
                                           PhysicalSwitch psw) throws IndexOutOfBoundException {
        OFFlowMod mplsFlowMod = null;
        Node node = null;

        //System.out.printf("buildPhysicalPath2 [%s]\n", mFlowMod.toString());
        if(vPath.getSrcHost() != null && this.getSrcHost() == null) {
            log.info("PhysicalPath[{}] SrcHost is setting", this.getPathID());
            this.setSrcHost(vPath.getSrcHost());
        }

        if(vPath.getDstHost() != null && this.getDstHost() == null) {
            log.info("PhysicalPath[{}] DstHost is setting", this.getPathID());
            this.setDstHost(vPath.getDstHost());
        }

        switch(type) {
            case INGRESS:
                if(this.getSrcSwitch() == null) {

                    node = new Node(psw, null, mFlowMod, oriFlowMod, type);

                    log.info("PhysicalPath[{}] SrcSwitch is setting", this.getPathID());

                    this.setSrcSwitch(node);
                    this.oFlowModmFlowModMap.put(oriFlowMod, node);
                }
                break;
            case INTERMEDIATE:
                if(this.oFlowModmFlowModMap.get(oriFlowMod) == null) {

                    node = new Node(psw, null, mFlowMod, oriFlowMod, type);

                    this.getIntermediate().add(node);
                    log.info("PhysicalPath[{}] InterSwitch {} is setting", this.getPathID(), psw.getSwitchLocID());

                    this.oFlowModmFlowModMap.put(oriFlowMod, node);
                }
                break;
            case EGRESS:
                if(this.getDstSwitch() == null) {

                    node = new Node(psw, null, mFlowMod, oriFlowMod, type);

                    log.info("PhysicalPath[{}] DstSwitch is setting", this.getPathID());

                    this.setDstSwitch(node);
                    this.oFlowModmFlowModMap.put(oriFlowMod, node);
                }
                break;
            case SAME:
                if(this.getSame() == null) {
                    log.info("this.same == null");
                    this.setSame(new Node(psw, mFlowMod, mFlowMod, oriFlowMod, type));
                }else{
                    log.info("this.same != null");
                }
                break;
        }

        return this;
    }

    public synchronized boolean findPhysicalPath() {

        if(this.getSrcSwitch() == null || this.getDstSwitch() == null) {
            return false;
        }

        ShortestPath routing = new ShortestPath();

        PhysicalSwitch srcSwitch = (PhysicalSwitch)this.getSrcSwitch().getSwitch();
        PhysicalSwitch dstSwitch = (PhysicalSwitch)this.getDstSwitch().getSwitch();

        LinkedList<PhysicalLink> physicalLinks = routing.computePath(srcSwitch, dstSwitch);

        //log.info("findPhysicalPath Inport : " + pInport);
        if(physicalLinks != null) {
            //log.info("physicalLinks != null {}/{}", pPath.getFlowID(), pPath.getTenantID());

            if(physicalLinks.size() == 1) {
                //log.info("findPhysicalPath physicalLinks.size() == 1");

                PhysicalLink plink = physicalLinks.get(0);
                //log.info(plink.toString());

                //set srcSwitch
                this.getSrcSwitch().setInPort(this.getSrcHost().getPort().getPhysicalPort());
                this.getSrcSwitch().setOutPort(plink.getSrcPort());

                //set dstSwitch
                this.getDstSwitch().setInPort(plink.getDstPort());
                this.getDstSwitch().setOutPort(this.getDstHost().getPort().getPhysicalPort());

                this.getSrcSwitch().setNextNode(this.getDstSwitch());
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

                    srcNode.setNextNode(dstNode);

                    srcNode = dstNode;

                    if(plink.equals(physicalLinks.getLast())) {
                        nodes.add(srcNode);
                    }
                }

                List<Node> orderedSwitch = new LinkedList<>(this.getIntermediate());
                this.getIntermediate().clear();

                for(Node node : nodes) {
                    if(node.equals(nodes.getFirst())) {
                        this.getSrcSwitch().setInPort(this.getSrcHost().getPort().getPhysicalPort());
                        this.getSrcSwitch().setOutPort(node.getOutPort());
                        this.getSrcSwitch().setNextNode(node.getNextNode());
                    }else if(node.equals(nodes.getLast())) {
                        this.getDstSwitch().setInPort(node.getInPort());
                        this.getDstSwitch().setOutPort(this.getDstHost().getPort().getPhysicalPort());
                        this.getDstSwitch().setNextNode(node.getNextNode());
                    }else{
                        //this.getIntermediate().add(node);
                        for(Node interSwitch : orderedSwitch) {
                            if(interSwitch.getSwitch().equals(node.getSwitch())) {
                                //log.info("find intermediate switch {}", node.getSwitch().getName());
                                node.setmFlowMod(interSwitch.getmFlowMod());
                                node.setOriFlowMod(interSwitch.getOriFlowMod());

                                this.getIntermediate().add(node);
                                orderedSwitch.remove(interSwitch);

                                break;
                            }
                        }
                    }
                }
                //log.info("Inter Size {}", this.getIntermediate().size());
            }
        }

        return true;
    }

    //for link failure
    public synchronized PhysicalPath findPhysicalPath(List<PhysicalLink> edges) {
        PhysicalPath newpPath = null;

        ShortestPath routing = new ShortestPath(edges);

        PhysicalSwitch srcSwitch = (PhysicalSwitch)this.getSrcSwitch().getSwitch();
        PhysicalSwitch dstSwitch = (PhysicalSwitch)this.getDstSwitch().getSwitch();



        LinkedList<PhysicalLink> physicalLinks = routing.computePath(srcSwitch, dstSwitch);

        //log.info("findPhysicalPath Inport : " + pInport);
        if(physicalLinks != null) {
            //log.info("physicalLinks != null {}/{}", pPath.getFlowID(), pPath.getTenantID());

            newpPath = new PhysicalPath(this.getFlowID(), this.getTenantID(),
                    PathUtil.getInstance().makePathID(this.getTenantID(), this.getFlowID()),
                    this.getvPath());

            newpPath.setSrcHost(this.getSrcHost());
            newpPath.setDstHost(this.getDstHost());

            newpPath.setSrcSwitch(this.getSrcSwitch());
            newpPath.setDstSwitch(this.getDstSwitch());

            if(physicalLinks.size() == 1) {
                log.info("findPhysicalPath physicalLinks.size() == 1");

                PhysicalLink plink = physicalLinks.get(0);
                //log.info(plink.toString());

                //set srcSwitch
                newpPath.getSrcSwitch().setInPort(this.getSrcHost().getPort().getPhysicalPort());
                newpPath.getSrcSwitch().setOutPort(plink.getSrcPort());

                //set dstSwitch
                newpPath.getDstSwitch().setInPort(plink.getDstPort());
                newpPath.getDstSwitch().setOutPort(this.getDstHost().getPort().getPhysicalPort());

                newpPath.getSrcSwitch().setNextNode(newpPath.getDstSwitch());
            }else{
                log.info("findPhysicalPath physicalLinks.size( {} )", physicalLinks.size());
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

                    srcNode.setNextNode(dstNode);

                    srcNode = dstNode;

                    if(plink.equals(physicalLinks.getLast())) {
                        nodes.add(srcNode);
                    }
                }

                for(Node node : nodes) {
                    if(node.equals(nodes.getFirst())) {
                        node.setInPort(this.getSrcHost().getPort().getPhysicalPort());

                        node.setOriFlowMod(this.getSrcSwitch().getOriFlowMod());
                        node.setmFlowMod(this.getSrcSwitch().getmFlowMod());

                        newpPath.setSrcSwitch(node);
                    }else if(node.equals(nodes.getLast())) {
                        node.setOutPort(this.getDstHost().getPort().getPhysicalPort());

                        node.setOriFlowMod(this.getDstSwitch().getOriFlowMod());
                        node.setmFlowMod(this.getDstSwitch().getmFlowMod());

                        newpPath.setDstSwitch(node);
                    }else{
                        //?? ???? SrcSwitch? DstSwitch ?? ??? Original FlowMod? Modified FlowMod ???? ???
                        node.setOriFlowMod(this.getDstSwitch().getOriFlowMod());
                        node.setmFlowMod(this.getDstSwitch().getmFlowMod());

                        newpPath.setIntermediateSwitch(node);
                    }
                }
            }
        }

        return newpPath;
    }




    public void sendSouth() {
        //log.info("Send to physical switches for PhysicalPath [" + this.getFlowID() + "]");

        if(this.getDstSwitch() != null && this.getDstSwitch().getMplsFlowMod() != null) {
            //log.info("SendSouth getDstSwitch for FlowID [{}]", this.getFlowID());
            this.getDstSwitch().getSwitch().sendMsg(new OVXMessage(this.getDstSwitch().getMplsFlowMod()), this.getDstSwitch().getSwitch());
        }

        if(this.isOriginalPath) {
            if (this.getIntermediate().size() != 0) {
                for (Node node : this.getIntermediate()) {
                    if(node != null) {
                        //log.info("SendSouth getIntermediate for FlowID [{}]", this.getFlowID());
                        //log.info("getIntermediate " + node.flowMod.toString());
                        node.getSwitch().sendMsg(new OVXMessage(node.getMplsFlowMod()), node.getSwitch());
                    }
                }
            }
        }

        if(this.getSrcSwitch() != null && this.getSrcSwitch().getMplsFlowMod() != null) {
            //log.info("SendSouth getSrcSwitch for FlowID [{}]", this.getFlowID());
            this.getSrcSwitch().getSwitch().sendMsg(new OVXMessage(this.getSrcSwitch().getMplsFlowMod()), this.getSrcSwitch().getSwitch());
        }
    }

    public Node getCorrespondingNode(PhysicalSwitch psw) {
        if(getSrcSwitch() != null && getSrcSwitch().getSwitch().equals(psw)) {
            return getSrcSwitch();
        }else if(getDstSwitch() != null && getDstSwitch().getSwitch().equals(psw)) {
            return getDstSwitch();
        }else if(getIntermediate().size() != 0) {
            for(Node node : getIntermediate()) {
                if(node.getSwitch().equals(psw)) {
                    return node;
                }
            }
        }

        return null;
    }

    public void addPathIDtoPhysicalSwitch() {
        if (this.getSrcSwitch() != null) {
            ((PhysicalSwitch) this.getSrcSwitch().getSwitch()).addPathID(this.getPathID());
        }

        if (this.getDstSwitch() != null) {
            ((PhysicalSwitch) this.getDstSwitch().getSwitch()).addPathID(this.getPathID());
        }

        if (this.getIntermediate() != null) {
            synchronized (this.getIntermediate()) {
                Iterator i = this.getIntermediate().iterator();
                    for (Node node : this.getIntermediate()) {
                        while (i.hasNext()) {
                            i.next();
                        ((PhysicalSwitch) node.getSwitch()).addPathID(this.getPathID());
                    }
                }
            }
        }
    }



    public boolean isUsingFailedLink(PhysicalLink pLink) {
        PhysicalSwitch srcSwitch = pLink.getSrcPort().getParentSwitch();
        PhysicalSwitch dstSwitch = pLink.getDstPort().getParentSwitch();

        LinkedList<Node> nodes = new LinkedList<>();

        nodes.add(this.getSrcSwitch());
        nodes.addAll(this.getIntermediate());
        nodes.add(this.getDstSwitch());

        for(Node node : nodes) {
            if(node.getSwitch().equals(srcSwitch)) {
                //log.info("SrcSwitch is matched {}/{}", node.getSwitch().getName(), srcSwitch.getName());
                if(node.getNextNode() != null && node.getNextNode().getSwitch().equals(dstSwitch)) {
                    //log.info("DstSwitch is matched {}/{}", node.getNextNode().getSwitch().getName(), dstSwitch.getName());
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String toString() {
        String str = "\n";

        LinkedList<Node> nodes = new LinkedList<>();

        nodes.add(this.getSrcSwitch());
        nodes.addAll(this.getIntermediate());
        nodes.add(this.getDstSwitch());

        for(Node node : nodes) {
            if(node.getNextNode() != null) {
                str = str + node.toString() + " -> " + node.getNextNode().toString() + "\n";
            }else{
                str = str + node.toString() + "\n";
            }
        }

        return str;
    }
}

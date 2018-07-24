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
package net.onrc.openvirtex.services.path;

import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.elements.port.Port;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;

public class Node {
    private NodeType type;
    private SwitchType stype;
    private Switch sw = null;
    private OFFlowMod mplsFlowMod = null;
    private OFFlowMod mFlowMod = null;
    private OFFlowMod oriFlowMod = null;
    private Port inPort = null;
    private Port outPort = null;

    private OFFlowStatsEntry flowStatsEntry = null;

    //for failover
    private Node nextNode = null;

    public Node(Switch sw, OFFlowMod mplsFlowMod, OFFlowMod mFlowMod, OFFlowMod oriFlowMod, SwitchType stype) {
        this.sw = sw;
        this.mplsFlowMod = mplsFlowMod;
        this.mFlowMod = mFlowMod;
        this.oriFlowMod = oriFlowMod;
        this.stype = stype;
        this.flowStatsEntry = null;

        /*this.flowStatsEntry = OFFactories.getFactory(mplsFlowMod.getVersion()).buildFlowStatsEntry()
                .setCookie(mplsFlowMod.getCookie())
                .setMatch(mplsFlowMod.getMatch())
                .setInstructions(mplsFlowMod.getInstructions())
                .build();*/
    }

    public Node(Switch sw, OFFlowMod oriFlowMod, SwitchType stype) {
        this.sw = sw;
        this.oriFlowMod = oriFlowMod;
        this.stype = stype;
    }

    public Node getNextNode() {
        return this.nextNode;
    }
    public void setNextNode(Node nextNode) {
        this.nextNode = nextNode;
    }

    public void setFlowStatsEntry(OFFlowStatsEntry entry) {
        this.flowStatsEntry = entry;
    }

    public OFFlowStatsEntry getFlowStatsEntry() {
        return this.flowStatsEntry;
    }

    public void setOriFlowMod(OFFlowMod oriFlowMod) {
        this.oriFlowMod = oriFlowMod;
    }

    public OFFlowMod getOriFlowMod() {
        return this.oriFlowMod;
    }

    public SwitchType getSwitchType() {
        return this.stype;
    }

    public Node(Switch sw) {
        this.sw = sw;
    }

    public void setNodeType(NodeType type) {
        this.type = type;
    }

    public NodeType getNodeType() {
        return this.type;
    }

    public void setSwitch(Switch sw) {
        this.sw = sw;
    }

    public Switch getSwitch() {
        return this.sw;
    }

    public void setMplsFlowMod(OFFlowMod mplsFlowMod) {
        this.mplsFlowMod = mplsFlowMod;
    }

    public OFFlowMod getMplsFlowMod() {
        return this.mplsFlowMod;
    }

    public void setmFlowMod(OFFlowMod flowMod) {
        this.mFlowMod = flowMod;
    }

    public OFFlowMod getmFlowMod() {
        return this.mFlowMod;
    }

    public void setInPort(Port p) {
        this.inPort = p;
    }

    public Port getInPort() {
        return this.inPort;
    }

    public void setOutPort(Port p) {
        this.outPort = p;
    }

    public Port getOutPort() {
        return this.outPort;
    }

    public String toString() {
        String str = "";
        if(sw != null)
            str = "Switch LocID [" + ((PhysicalSwitch)sw).getSwitchLocID() + "]";

        //if(mplsFlowMod != null)
        //    str = str + "FlowMod [" + mplsFlowMod.toString() + "]\n";

        /*if(inPort != null)
            str = str + "inPort [" + inPort.toString() + "]\n";

        if(outPort != null)
            str = str + "outPort [" + outPort.toString() + "]\n";*/

        return str;
    }
}

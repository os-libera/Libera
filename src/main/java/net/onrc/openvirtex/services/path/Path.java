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

import net.onrc.openvirtex.elements.host.Host;

import java.util.LinkedList;
import java.util.List;

public class Path {
    //private int pathID;
    private int pathID;
    private int flowID;
    private int tenantID;
    private Host srcHost;
    private Host dstHost;
    private Node srcSwitch;
    private Node dstSwitch;
    private Node same;
    private List<Node> intermediate;

    private boolean isBuild;

    public Path(int flowID, int tenantID, int pathID) {
        this.pathID = pathID;
        this.flowID = flowID;
        this.tenantID = tenantID;
        this.srcSwitch = null;
        this.intermediate = new LinkedList<>();
        this.dstSwitch = null;
        this.same = null;
        this.isBuild = false;
    }

    public int getPathID() { return this.pathID; }

    public List<Node> getIntermediate() {
        return this.intermediate;
    }

    public int getFlowID() {
        return this.flowID;
    }

    public int getTenantID() {
        return this.tenantID;
    }

    public void setSrcSwitch(Node srcSwitch) {
        this.srcSwitch = srcSwitch;
    }

    public void setIntermediateSwitch(Node intermediateSwitch) {
        this.intermediate.add(intermediateSwitch);
    }

    public Node getSrcSwitch() {
        return this.srcSwitch;
    }

    public void setDstSwitch(Node dstSwitch) {
        this.dstSwitch = dstSwitch;
    }

    public Node getDstSwitch() {
        return this.dstSwitch;
    }

    public void setSame(Node same) {
        this.same = same;
    }

    public Node getSame() {
        return this.same;
    }

    public void setSrcHost(Host host) {
        this.srcHost = host;
    }

    public void setDstHost(Host host) {
        this.dstHost = host;
    }

    public Host getSrcHost() {
        return this.srcHost;
    }

    public Host getDstHost() {
        return this.dstHost;
    }

    public void setBuild(boolean isBuild) {
        this.isBuild = isBuild;
    }

    public synchronized boolean isBuild() {
        return this.isBuild;
    }
}

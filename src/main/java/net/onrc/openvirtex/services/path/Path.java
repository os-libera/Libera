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
package net.onrc.openvirtex.services.path;

import net.onrc.openvirtex.elements.host.Host;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


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
    private LinkedBlockingQueue<Node> intermediate;
    //private List<Node> intermediate;
    private boolean isBuild;

    public Path(int flowID, int tenantID, int pathID) {
        this.pathID = pathID;
        this.flowID = flowID;
        this.tenantID = tenantID;
        this.srcSwitch = null;
        this.intermediate = new LinkedBlockingQueue<Node>();
        this.dstSwitch = null;
        this.same = null;
        this.isBuild = false;

    }



    public int getPathID() { return this.pathID; }

    public synchronized LinkedBlockingQueue<Node> getIntermediate() {

        return this.intermediate;
    }
    //}

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

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

package net.onrc.openvirtex.services.forwarding.mpls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

/**
 * Created by soulcrime on 2017-07-25.
 */
public class MplsLabel {
    private static Logger log = LogManager.getLogger(MplsLabel.class.getName());
    private int label;
    private LinkedList<Integer> pathIDs = new LinkedList<>();

    private Integer originalPathID;

    public MplsLabel(int label, int pathID){
        this.label = label;
        //this.flowIDs.add(flowID);
        this.originalPathID = pathID;
    }

    public boolean isContainedPathID(Integer pathID) {
        return this.pathIDs.contains(pathID);
    }

    public boolean isOriginalPathID(int pathID) {
        if(this.originalPathID == pathID)
            return true;
        else
            return false;
    }

    public void addPathID(Integer pathID) {
        log.info("refenced PathID [" + pathID + "] is added to original PathID [" + this.originalPathID + "]");
        this.pathIDs.add(pathID);
    }

    public LinkedList<Integer> getPathIDs() {
        return this.pathIDs;
    }

    public void removePathID(Integer pathID) {
        if(this.pathIDs.contains(pathID))
            pathIDs.remove(pathID);
    }

    public int getLabelValue() {
        return this.label;
    }

    public int getOriginalPathID() {
        return this.originalPathID;
    }

    public Integer changeOriginalPathID() {
        if(this.pathIDs.size() != 0){
            this.originalPathID = this.pathIDs.getFirst();
            this.pathIDs.remove(this.originalPathID);
            return this.originalPathID;
        }else{
            return null;
        }
    }

    public int getTenantID() {
        return label & 0x3f;
    }

    public int getSrcPhysicalID() {
        return label >> 13;
    }

    public int getDstPhysicalID() {
        return (label & 0x1fc0) >> 6;
    }

    @Override
    public boolean equals(Object o) {
        if(this.label == ((MplsLabel)o).label)
            return true;
        else
            return false;
    }
}

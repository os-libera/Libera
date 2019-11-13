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
import net.onrc.openvirtex.elements.network.OVXNetwork;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.services.forwarding.mpls.MplsForwarding;
import net.onrc.openvirtex.services.forwarding.mpls.MplsLabel;
import net.onrc.openvirtex.services.path.PathUtil;
import net.onrc.openvirtex.services.path.SwitchType;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPath;
import net.onrc.openvirtex.services.path.virtualpath.VirtualPathBuilder;
import net.onrc.openvirtex.util.BitSetIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.U64;

import java.util.concurrent.ConcurrentHashMap;

public class PhysicalPathBuilder {
    private static PhysicalPathBuilder instance;
    private static Logger log = LogManager.getLogger(PhysicalPathBuilder.class.getName());

    private static ConcurrentHashMap<Integer,PhysicalPath> PathIDphysicalPathMap;

    private final BitSetIndex pathCounter;

    private PhysicalPathBuilder() {
        PathIDphysicalPathMap = new ConcurrentHashMap<>();
        this.pathCounter = new BitSetIndex(BitSetIndex.IndexType.PATH_ID);
    }

    /*public Integer getNewPathID() {
        try {
            return pathCounter.getNewIndex();
        } catch (IndexOutOfBoundException e) {
            e.printStackTrace();
        }
        return null;
    }*/

    public synchronized static PhysicalPathBuilder getInstance() {
        if (PhysicalPathBuilder.instance == null) {
            log.info("Starting PhysicalPathBuilder");

            PhysicalPathBuilder.instance = new PhysicalPathBuilder();
        }
        return PhysicalPathBuilder.instance;
    }

    public synchronized PhysicalPath buildPhysicalPath(VirtualPath vPath, OFFlowMod oFlowMod,
                                                    OFFlowMod mFlowMod, SwitchType type,
                                                    PhysicalSwitch psw) throws IndexOutOfBoundException {
        int pathID = PathUtil.getInstance().makePathID(vPath.getTenantID(), vPath.getFlowID());
        PhysicalPath pPath = PathIDphysicalPathMap.get(pathID);
        if(pPath == null){

            pPath = new PhysicalPath(vPath.getFlowID(), vPath.getTenantID(), pathID, vPath);
            vPath.setPhysicalPath(pPath);

            PathIDphysicalPathMap.put(pathID, pPath);
            log.info("PhysicalPath ID [{}] is building", vPath.getPathID());
        }

        //return pPath.buildPhysicalPath(vPath, oFlowMod, mFlowMod, type, psw);
        return pPath.buildPhysicalPath2(vPath, oFlowMod, mFlowMod, type, psw);
    }

    public synchronized OFFlowMod removePhysicalPath(OFFlowMod oriFlowMod, OVXMatch ovxMatch) {
        int pathID = PathUtil.getInstance().makePathID(ovxMatch.getTenantId(), ovxMatch.getFlowId());
        PhysicalPath pPath = PathIDphysicalPathMap.get(pathID);

        if(pPath == null){
            log.info("PhysicalPath ID [{}] does not exist", pathID);
            return null;
        }else{
            //log.info("PhysicalPath ID [{}] exists", ovxMatch.getFlowId());

            VirtualPath vPath = VirtualPathBuilder.getInstance().getVirtualPath(pathID);


            if(vPath == null) {
                PathIDphysicalPathMap.remove(pathID);
                log.info("PhysicalPath ID [{}] is removed", pathID);
            }

            return pPath.getModifiedFlowMod(oriFlowMod);        //path 지우는 것도 따져봐야함
        }
    }

    public PhysicalPath getPhysicalPath(Integer pathId) {
        return PathIDphysicalPathMap.get(pathId);
    }

    public synchronized  void addPhysicalPath(PhysicalPath pPath) {
        PathIDphysicalPathMap.put(pPath.getPathID(), pPath);
    }

    public synchronized void removePhysicalPath(Integer pathId) {
        PathIDphysicalPathMap.remove(pathId);
    }

}

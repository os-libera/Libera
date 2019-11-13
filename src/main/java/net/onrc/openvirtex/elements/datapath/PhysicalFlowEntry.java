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

package net.onrc.openvirtex.elements.datapath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.onrc.openvirtex.messages.OVXFlowMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.match.MatchField;

import net.onrc.openvirtex.protocol.OVXMatch;
//import net.onrc.openvirtex.util.MACAddress;
import org.projectfloodlight.openflow.types.MacAddress;

/**
 * This class is to store physical rule set aggregated rule for efficiency.
 * Rule is matched by Match and Output Number And stored by cookie number.
 */
public class PhysicalFlowEntry {
	
	private Logger log = LogManager.getLogger(PhysicalFlowEntry.class
            .getName());

	protected OVXFlowMod fm;
	//OVXMatch ovxmatch = new OVXMatch(fm.getFlowMod().getMatch());

	/* Entry is represented by Match and outaction */
	private OVXMatch ovxmatch;
	private OFActionOutput ofaction;
  public OFFactory factory;
	
	/* Store all cookie that have same match and out-port */
	private List<Long> cookieSet = Collections.synchronizedList(new ArrayList<Long>());
    
	/**
     * Instantiates a new EntryPair.
     *  @param match the ovxMatch
     * @param action the output action
	 * @param cookie the cookie
	 */
	public PhysicalFlowEntry(OVXMatch match, OFActionOutput action, long cookie){
		this.ovxmatch = match;
		this.ofaction = action;
    this.factory = OFFactories.getFactory(action.getVersion());
		this.cookieSet.add(cookie);
		log.debug("This cookie is : {} ", cookie);
	}

	/** @return original OFMatch */
	public OVXMatch getMatch() {
		return this.ovxmatch;
	}
	
	/**
	 * Get the Match of this entry.
	 * 
	 * @return the match
	 */
	//public OVXMatch getMatch(){
	//	return this.ovxmatch ;
	//}
	
	/**
	 * Get the output action of this entry.
	 * 
	 * @return the output action
	 */
	public OFActionOutput getAction(){
		return this.ofaction;
	}

	/**
	 * Add cookie to the cookie set
	 * 
	 * @param cookie
	 */
	public void addCookie(long cookie){
		this.cookieSet.add(cookie);
	}
	
	/**
	 * Get the cookie set
	 * 
	 * @return the cookie set
	 */
	public List<Long> getCookieSet(){
		return this.cookieSet;
	}
	
	/**
	 * Check the entry with the existed entry
	 * Same entry has same ovxmatch and outport of output action
	 * 
	 * @param entity
	 * @return if same condition, return true. otherwise return false.
	 */
	public boolean equals(PhysicalFlowEntry entity){
		
		if(entity == null)
			return false;
		
		if(Arrays.equals(this.ovxmatch.getMatch().get(MatchField.ETH_DST).getBytes(), entity.ovxmatch.getMatch().get(MatchField.ETH_DST).getBytes())
				&& (this.ovxmatch.getMatch() == entity.ovxmatch.getMatch())
				&& (this.ofaction.getPort() == entity.ofaction.getPort())){
			return true;
		}
		return false;
	}
	

	/*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
	@Override
	public String toString() {
		String ret = "";
		ret +="Cookie : ";
		for(Long c : cookieSet){
			ret += c + "\t";
		}
		ret += "\nInport : "+ this.ovxmatch.getMatch().get(MatchField.IN_PORT)
				+ "\nMAC Destination Addresse: "+ MacAddress.of((this.ovxmatch.getMatch().get(MatchField.ETH_DST)).toString()
				+ "\nOutput port : " + this.ofaction.getPort()
				+"\n");

        ret += "Entry \n========================\n" + ret
                + "========================\n";
        
        return ret;
	}
	
}
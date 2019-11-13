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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.openflow.protocol.OFMatch;
//import org.openflow.protocol.Wildcards.Flag;
//import org.openflow.protocol.action.OFAction;
//import org.openflow.protocol.action.OFActionOutput;
//import org.openflow.protocol.action.OFActionType;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.MacAddress;
import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.protocol.OVXMatch;

import static org.projectfloodlight.openflow.protocol.match.MatchField.IPV4_DST;
import static org.projectfloodlight.openflow.protocol.match.MatchField.IPV4_SRC;

/**
 * Physical flow table that manage to aggregate rule and store that.
 * 
 * @author byyu
 *
 */
public class PhysicalFlowTable {
	
	private static Logger log = LogManager.getLogger(PhysicalFlowTable.class.getName());

	// Physical switch tied to this table
	private PhysicalSwitch physw;
	protected long newcookie;
	// The FlowMod this Entry represents
	protected OVXFlowMod fm;
	// Aggregated rule set installed in physical switch.
	private Set<PhysicalFlowEntry> entry = new HashSet<PhysicalFlowEntry>();


    /**
     * Instantiates a new flow table associated to the given
     * physical switch.
     *
	 * @param sw the physical switch
	 */
	public PhysicalFlowTable(PhysicalSwitch sw){
				this.physw = sw;
	}



	//public long getCookie() {
	//	return fm.getFlowMod().getCookie().getValue();
	//}

	/**
	 * Add an entity to this physical table.
	 * 
	 * @param match
	 * @param action
	 */
	public void addEntry(OVXMatch match, OFActionOutput action){

		//long cookie = fm.getFlowMod().getCookie().getValue();
		PhysicalFlowEntry entity = new PhysicalFlowEntry(match, action, match.getCookie());
		entry.add(entity);
	}



	/**
	 * Add an entity to this physical table. 
	 * In this case, we need to extend matching field and increase the priority of this FlowMod packet, 
	 * because the rule that has different out put with same in port already is installed.
	 *
	 * @param fm
	 * @param match
	 * @param action
	 */
	private void addEntry(OVXFlowMod fm, OVXMatch match, OFActionOutput action){


		int prio = fm.getFlowMod().getPriority();
		fm.setOFMessage(
				fm.getFlowMod().createBuilder()
						.setPriority(++prio)
						.build());

		/*match.setWildcards((OFMatch.OFPFW_ALL) & (~OFMatch.OFPFW_IN_PORT)
								& (~OFMatch.OFPFW_DL_SRC)
								& (~OFMatch.OFPFW_DL_DST)
								& (~OFMatch.OFPFW_DL_TYPE)
								& (~OFMatch.OFPFW_NW_DST_MASK)
								& (~OFMatch.OFPFW_NW_SRC_MASK)); */

		match.getMatch().createBuilder()
				//.wildcard(MatchField.IN_PORT)
				//.wildcard(MatchField.ETH_SRC)
				//.wildcard(MatchField.ETH_DST)
				//.wildcard(MatchField.ETH_TYPE)
				//.wildcard(MatchField.IPV4_SRC)
				//.wildcard(MatchField.IPV4_DST)
				.setExact(MatchField.IN_PORT, fm.getFlowMod().getMatch().get(MatchField.IN_PORT))
				.setExact(MatchField.ETH_SRC, fm.getFlowMod().getMatch().get(MatchField.ETH_SRC))
				.setExact(MatchField.ETH_TYPE, fm.getFlowMod().getMatch().get(MatchField.ETH_TYPE))
				.setExact(MatchField.IPV4_SRC, fm.getFlowMod().getMatch().get(IPV4_SRC))
				.setExact(MatchField.IPV4_DST, fm.getFlowMod().getMatch().get(IPV4_DST))
				.build();

		fm.setOFMessage(fm.getFlowMod().createBuilder()
				.setMatch(match.getMatch())
				.build());


		addEntry(match, action);
	}
	
	/**
	 * Remove the flow entry and return the all cookies correspond to this FlowRemoved packet.
	 * 
	 * @param match
	 * @param action
	 * @param cookie
	 * @return the cookie set that assigned same rule
	 */
	public List<Long> removeEntry(OVXMatch match, OFActionOutput action, long cookie){
		PhysicalFlowEntry newEntity = new PhysicalFlowEntry(match, action, cookie);
		for(PhysicalFlowEntry entity : entry){
			if(entity.equals(newEntity)){
				List<Long> cookieList = entity.getCookieSet();
				entry.remove(entity);
				return cookieList;
			}
		}
		return null;
	}

	/**
	 * Check whether the FlowMod is duplicated with the installed rules.
	 * If both have same input and same output, this FlowMod is duplicated.
	 * Otherwise, both have same input but have different output, the match field of this FlowMod is extended and stored in the table.
	 * And, If both have different input or no one is installed, this FlowMod is stored in the table.
	 * 
	 * @param fm
	 * @return result of checking duplication. If the FlowMod is sent to physical switch, false is returned.
	 */
	public boolean checkduplicate(OVXFlowMod fm){
		//log.info("Start checking duplicate at {}", this.physw);
		OVXMatch match = new OVXMatch(fm.getFlowMod().getMatch());
		OFActionOutput outaction = null;

		fm.setOFMessage(fm.getFlowMod().createBuilder()
				.setCookie(U64.of(newcookie))
				.build());

		//match.setCookie(fm.getFlowMod().getCookie());

		OVXMatch oldMatch;
		
		short oldoutport, outport=0;
		
		//Gets the ActionOutput from the FlowMod.
		for(OFAction action : fm.getFlowMod().getActions()){
			if(action.getType()==OFActionType.OUTPUT){
	    		outaction = (OFActionOutput) action;
	    		outport = outaction.getPort().getShortPortNumber();
			}
		}
		
		//This FlowMod is not for forwarding.
		if(outport==0){
			return false;
		}
		
		for(PhysicalFlowEntry entity : entry){

			oldMatch = entity.getMatch();
			oldoutport = entity.getAction().getPort().getShortPortNumber();
			if(oldMatch.getMatch() == match.getMatch()){
				if(outport == oldoutport){
					if(oldMatch == match){
						if(!oldMatch.getMatch().isFullyWildcarded(MatchField.IPV4_DST)){
							if (oldMatch.getMatch().get(MatchField.IPV4_DST) == match.getMatch().get(MatchField.IPV4_DST)){
								entity.addCookie(newcookie);
								return true;
							}
						}else{
							entity.addCookie(newcookie);
							return true;
						}
					}
				}else{
					if(oldMatch == match){
						addEntry(fm, match, outaction);
						return false;
					}
					
				}
			}
		}
		
		addEntry(match, outaction);
		return false;
	}
	
	public Set<PhysicalFlowEntry> getFlowEntry(){
		return this.entry;
	}


}

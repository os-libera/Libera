/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ****************************************************************************
 * Libera HyperVisor development based OpenVirteX for SDN 2.0
 *
 *   OpenFlow Version Up with OpenFlowj
 *
 * This is updated by Libera Project team in Korea University
 *
 * Author: Seong-Mun Kim (bebecry@gmail.com)
 ******************************************************************************/
package net.onrc.openvirtex.messages.actions;

import net.onrc.openvirtex.elements.address.IPMapper;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.protocol.OVXMatch;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.types.IPv4Address;

import java.util.List;

public class OVXActionSetNwSrc extends OVXAction implements VirtualizableAction {

    private OFActionSetNwSrc ofActionSetNwSrc;

    public OVXActionSetNwSrc(OFAction ofAction)
    {
        super(ofAction);
        this.ofActionSetNwSrc = (OFActionSetNwSrc)ofAction;
    }

    @Override
    public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OVXMatch match)
            throws ActionVirtualizationDenied, DroppedMessageException {
        this.ofActionSetNwSrc = this.ofActionSetNwSrc.createBuilder()
                .setNwAddr(IPv4Address.of(
                        IPMapper.getPhysicalIp(
                                sw.getTenantId(),
                                this.ofActionSetNwSrc.getNwAddr().getInt())
                        )
                )
                .build();

        approvedActions.add(this.ofActionSetNwSrc);
    }

    @Override
    public int hashCode() {
        return this.getAction().hashCode();
    }
}

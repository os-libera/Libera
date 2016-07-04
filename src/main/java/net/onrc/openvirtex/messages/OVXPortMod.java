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
package net.onrc.openvirtex.messages;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.port.PhysicalPort;

import org.projectfloodlight.openflow.protocol.OFBadRequestCode;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortMod;
import org.projectfloodlight.openflow.types.OFPort;

public class OVXPortMod extends OVXMessage implements Devirtualizable {

    public OVXPortMod(OFMessage msg) {
        super(msg);
    }

    public OFPortMod getPortMod() {
        return (OFPortMod)this.getOFMessage();
    }

    @Override
    public void devirtualize(final OVXSwitch sw) {
        // TODO Auto-generated method stub
        // assume port numbers are virtual
        final OVXPort p = sw.getPort(
                this.getPortMod().getPortNo().getShortPortNumber()
        );

        if (p == null) {
            sw.sendMsg(
                    OVXMessageUtil.makeErrorMsg(OFBadRequestCode.EPERM, this),
                    sw
            );
            return;
        }
        // set physical port number - anything else to do?
        final PhysicalPort phyPort = p.getPhysicalPort();

        this.setOFMessage(this.getPortMod().createBuilder()
                .setPortNo(OFPort.of(phyPort.getPortNumber()))
                .build()
        );

        OVXMessageUtil.translateXid(this, p);
    }

    @Override
    public int hashCode() {
        return this.getOFMessage().hashCode();
    }

}

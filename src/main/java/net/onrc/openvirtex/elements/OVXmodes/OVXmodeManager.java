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

package net.onrc.openvirtex.elements.OVXmodes;

//import org.openflow.protocol.OFMatch;
//import org.openflow.protocol.Wildcards.Flag;
import com.sun.org.apache.xerces.internal.impl.dv.xs.AnyURIDV;
import net.onrc.openvirtex.api.service.handlers.tenant.SetOVXmode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class OVXmodeManager {
    private HashMap<Integer, Number> OVXmodeMap = null;
    Logger log = LogManager.getLogger(OVXmodeManager.class.getName());
    private static AtomicReference<OVXmodeManager> OVXmodeInstance = new AtomicReference<>();


/*
    Anu
    */

    public int OVXmoderewriteMatch(int mode){
        this.log.info("Inside OVXmoderewriteMatch. mode value passed ={}", mode);
        switch(mode){
            case 0 :
            { log.info("OVX mode case");
                return 0;}
            case 1 :
            { log.info("LiteVisor mode case");
                return 1;}
            case 2 :
            { log.info("AggFlow mode case");
                return 2;}
            default:
            { log.info("Default mode case");
                return 0;}
        }
    }

    public static OVXmodeManager getInstance() {
        OVXmodeManager.OVXmodeInstance.compareAndSet(null, new OVXmodeManager());
        return OVXmodeManager.OVXmodeInstance.get();
    }


}

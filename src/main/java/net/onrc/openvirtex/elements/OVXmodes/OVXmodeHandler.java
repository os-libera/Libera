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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class OVXmodeHandler {



    private static Map<Integer, Integer> OVXmodeMap;
    Logger log = LogManager.getLogger(OVXmodeHandler.class.getName());

    private static AtomicReference<OVXmodeHandler> OVXmodeInstance = new AtomicReference<>();
    OVXmodeManager ovxmodeManager = new OVXmodeManager();
    private static Integer mode;

    private OVXmodeHandler() {
        OVXmodeMap = new HashMap<Integer, Integer>();
    }


    //Called while entering OVX mode in CMD
    public void setOVXmode(int tenantId, int OVXmode) {
        this.log.info("Inside setOVXmode method; tenantid={}, mode={}", tenantId, OVXmode);
            OVXmodeMap.put(tenantId, OVXmode);
            mode = OVXmodeMap.get(tenantId);
            log.info("value stored in OVX mode map={}", mode);
    }

    public static int getOVXmode() {
        try{
                return mode;

        } catch(NullPointerException e){ mode=0;}
        return mode;
    }


    public int processOVXmode(int mode) {
        Integer OVXmode = null;
        log.info("Inside processOVXmode method, mode={}",mode);
        if(OVXmodeMap.isEmpty() || mode == 0){
            log.info("OVXmodemap is empty={} ", mode);
            OVXmode = 0;
        }else{
            log.info("OVXmodemap is not 0 or empty ={} ", mode);
            OVXmode = getOVXmode();
        }
        OVXmode= ovxmodeManager.OVXmoderewriteMatch(OVXmode);
           //mode=0;
        return OVXmode;
    }

    public static OVXmodeHandler getInstance() {
        OVXmodeHandler.OVXmodeInstance.compareAndSet(null, new OVXmodeHandler());
        return OVXmodeHandler.OVXmodeInstance.get();
    }

}



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

package net.onrc.openvirtex.services.path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by soulcrime on 2017-07-28.
 */
public class PathUtil {

    private static PathUtil instance;
    private static Logger log = LogManager.getLogger(PathUtil.class.getName());

    public synchronized static PathUtil getInstance() {
        if (PathUtil.instance == null) {
            log.info("Starting PathUtil");

            PathUtil.instance = new PathUtil();
        }
        return PathUtil.instance;
    }

    public int makePathID(int tenantID, int flowID) {
        return tenantID << 20 | flowID;
    }
}

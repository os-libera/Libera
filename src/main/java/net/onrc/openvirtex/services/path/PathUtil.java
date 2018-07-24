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

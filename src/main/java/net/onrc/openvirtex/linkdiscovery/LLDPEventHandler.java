package net.onrc.openvirtex.linkdiscovery;

import net.onrc.openvirtex.elements.datapath.Switch;
import net.onrc.openvirtex.messages.OVXMessage;

/**
 * Created by Administrator on 2016-04-21.
 */
public interface LLDPEventHandler {
    public void handleLLDP(OVXMessage msg, Switch sw);
}

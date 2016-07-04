package net.onrc.openvirtex.messages.actions;

import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.protocol.OVXMatch;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;

import java.util.List;

/**
 * Created by Administrator on 2016-06-20.
 */
public class OVXActionSetField extends OVXAction implements VirtualizableAction {
    private OFActionSetField ofActionSetField;
    public OVXActionSetField(OFAction ofAction) {
        super(ofAction);
        this.ofActionSetField = (OFActionSetField)ofAction;
    }

    @Override
    public void virtualize(OVXSwitch sw, List<OFAction> approvedActions, OVXMatch match)
            throws ActionVirtualizationDenied, DroppedMessageException {

    }
}

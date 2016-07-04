package net.onrc.openvirtex.util;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.projectfloodlight.openflow.types.MacAddress;

/**
 * Created by Administrator on 2016-05-02.
 */
public class MacAddressSerializer implements JsonSerializer<MacAddress> {

    @Override
    public JsonElement serialize(MacAddress mac, Type t,
                                 JsonSerializationContext c) {
        final JsonPrimitive res = new JsonPrimitive(mac.toString());
        return res;
    }
}

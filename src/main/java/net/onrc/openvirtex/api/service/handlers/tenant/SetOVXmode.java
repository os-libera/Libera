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

package net.onrc.openvirtex.api.service.handlers.tenant;

import java.util.*;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.onrc.openvirtex.api.service.handlers.ApiHandler;
import net.onrc.openvirtex.api.service.handlers.HandlerUtils;
import net.onrc.openvirtex.api.service.handlers.TenantHandler;
import net.onrc.openvirtex.exceptions.MissingRequiredField;
import net.onrc.openvirtex.elements.OVXmodes.OVXmodeHandler;
import net.onrc.openvirtex.exceptions.OVXmodeValueException;


public class SetOVXmode extends ApiHandler<Map<String, Object>>{
    Logger log = LogManager.getLogger(SetOVXmode.class.getName());


    @Override
    public JSONRPC2Response process(final Map<String, Object> params){
        JSONRPC2Response resp = null;

        try{
            final Number tenantId = HandlerUtils.<Number>fetchField(TenantHandler.TENANT, params, true, null);
            Number OVXmode = HandlerUtils.<Number>fetchField(TenantHandler.OVXMODE, params, true, null);

            HandlerUtils.isValidTenantId(tenantId.intValue());
            HandlerUtils.isValidOVXmode(OVXmode.intValue());


            this.log.info("Tenant Id of the virtual network {} and OVX mode {}",
                    tenantId.intValue(), OVXmode);

            OVXmodeHandler ovxmodeHandler = OVXmodeHandler.getInstance();
            ovxmodeHandler.setOVXmode(tenantId.intValue(), OVXmode.intValue());


            log.info("OVX mode being set={}", OVXmode.intValue());
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(TenantHandler.OVXMODE, OVXmode.intValue());
            reply.put(TenantHandler.TENANT, tenantId.intValue());
            log.info("reply value ={}", reply);

            resp = new JSONRPC2Response(reply, 0);

        }catch(MissingRequiredField e){
            resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),this.cmdName() + ": Unable to set this OVX mode in the virtual network : "+e.getMessage()), 0);
        } catch (OVXmodeValueException e) {
            resp = new JSONRPC2Response(new JSONRPC2Error(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), this.cmdName()
                    + ": Invalid OVX mode : " + e.getMessage()), 0);
        }

        return resp;
    }



    @Override
    public JSONRPC2ParamsType getType(){
        return JSONRPC2ParamsType.OBJECT;
    }
}
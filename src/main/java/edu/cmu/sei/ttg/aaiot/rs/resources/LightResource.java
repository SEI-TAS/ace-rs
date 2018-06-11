/*
AAIoT Source Code

Copyright 2018 Carnegie Mellon University. All Rights Reserved.

NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS"
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER
INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM
USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM
PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.

[DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see
Copyright notice for non-US Government use and distribution.

This Software includes and/or makes use of the following Third-Party Software subject to its own license:

1. ace-java (https://bitbucket.org/lseitz/ace-java/src/9b4c5c6dfa5ed8a3456b32a65a3affe08de9286b/LICENSE.md?at=master&fileviewer=file-view-default)
Copyright 2016-2018 RISE SICS AB.
2. zxing (https://github.com/zxing/zxing/blob/master/LICENSE) Copyright 2018 zxing.
3. sarxos webcam-capture (https://github.com/sarxos/webcam-capture/blob/master/LICENSE.txt) Copyright 2017 Bartosz Firyn.
4. 6lbr (https://github.com/cetic/6lbr/blob/develop/LICENSE) Copyright 2017 CETIC.

DM18-0702
*/

package edu.cmu.sei.ttg.aaiot.rs.resources;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import se.sics.ace.Constants;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by sebastianecheverria on 8/3/17.
 */
public class LightResource extends CoapResource implements IIoTResource
{
    public LightResource() {
        super("light");
        getAttributes().setTitle("Temperature Resource");
    }

    @Override
    public void handleGET(CoapExchange exchange)
    {
        int minVal = 0;
        int maxVal = 1;
        int currVal = ThreadLocalRandom.current().nextInt(minVal, maxVal + 1);

        CBORObject lightsStatus = CBORObject.NewMap();
        lightsStatus.Add(1, currVal);
        exchange.respond(CoAP.ResponseCode.CONTENT, lightsStatus.EncodeToBytes());
    }

    public Set<Short> getActions(String scopeName)
    {
        Set<Short> actions = new HashSet<>();
        actions.add(Constants.GET);
        return actions;
    }

    public List<String> getScopeNames()
    {
        ArrayList<String> scopeNames = new ArrayList<>();
        scopeNames.add("r_light");
        return scopeNames;
    }

    public Map<String, Set<Short>> getScopeHandler(String scopeName)
    {
        Map<String, Set<Short>> resourceMap = new HashMap<>();
        resourceMap.put(this.getName(), this.getActions(scopeName));
        return resourceMap;
    }


}


package edu.cmu.sei.ttg.aaiot.rs.resources;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

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

    public Set<String> getActions()
    {
        Set<String> actions = new HashSet<>();
        actions.add("GET");
        return actions;
    }

    public List<String> getScopeNames()
    {
        ArrayList<String> scopeNames = new ArrayList<>();
        scopeNames.add("r_light");
        return scopeNames;
    }

    public Map<String, Set<String>> getScopeHandler()
    {
        Map<String, Set<String>> resourceMap = new HashMap<>();
        resourceMap.put(this.getName(), this.getActions());
        return resourceMap;
    }


}


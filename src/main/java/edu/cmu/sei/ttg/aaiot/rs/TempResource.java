package edu.cmu.sei.ttg.aaiot.rs;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Sebastian on 2017-05-19.
 */
public class TempResource extends CoapResource {
    public TempResource() {
        super("temp");
        getAttributes().setTitle("Temperature Resource");
    }

    @Override
    public void handleGET(CoapExchange exchange)
    {
        int minTemp = 21;
        int maxTemp = 36;
        int currentTemp = ThreadLocalRandom.current().nextInt(minTemp, maxTemp + 1);

        CBORObject temperature = CBORObject.NewMap();
        temperature.Add(1, currentTemp);
        temperature.Add(2, "C");
        exchange.respond(CoAP.ResponseCode.CONTENT, temperature.EncodeToBytes());
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
        scopeNames.add("r_temp");
        return scopeNames;
    }

    public Map<String, Set<String>> getScopeHandler()
    {
        Map<String, Set<String>> tempResourceMap = new HashMap<>();
        tempResourceMap.put(this.getName(), this.getActions());
        return tempResourceMap;
    }


}

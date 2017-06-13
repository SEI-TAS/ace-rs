import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        exchange.respond("35.0 C");
    }

    @Override
    public void handlePOST(CoapExchange exchange)
    {
        exchange.respond("35.0 C");
    }

    public Set<String> getActions()
    {
        Set<String> actions = new HashSet<>();
        actions.add("GET");
        actions.add("POST");

        return actions;
    }

    public String getScopeName()
    {
        return "r_temp";
    }

    public Map<String, Set<String>> getScopeHandler()
    {
        Map<String, Set<String>> tempResourceMap = new HashMap<>();
        tempResourceMap.put(this.getName(), this.getActions());
        return tempResourceMap;
    }


}

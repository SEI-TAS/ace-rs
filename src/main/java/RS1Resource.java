import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * Created by Sebastian on 2017-05-19.
 */
public class RS1Resource extends CoapResource {
    public RS1Resource() {
        super("temp");
        getAttributes().setTitle("Temperature Resource");
    }

    @Override
    public void handleGET(CoapExchange exchange)
    {
        exchange.respond("35.0 C");
    }
}

import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.KissValidator;

import java.util.*;

/**
 * Created by Sebastian on 2017-05-02.
 */
public class main {
   public static void main(String[] args)
    {
        try {
            Map<String, Map<String, Set<String>>> myScopes = new HashMap<>();

            TempResource tempResource = new TempResource();
            myScopes.put(tempResource.getScopeName(), tempResource.getScopeHandler());

            CoapsRS rsServer = new CoapsRS(myScopes);
            rsServer.setAS("TestAS", "coaps://localhost/authz-info/");

            // Add actual resources.
            rsServer.add(tempResource);

            System.out.println("Starting server");
            rsServer.start();

        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

}

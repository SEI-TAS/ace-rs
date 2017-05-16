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

            Set<String> actions = new HashSet<>();
            actions.add("GET");
            Map<String, Set<String>> myResource = new HashMap<>();
            myResource.put("temp", actions);
            Map<String, Map<String, Set<String>>> myScopes = new HashMap<>();
            myScopes.put("r_temp", myResource);

            myResource.clear();
            myResource.put("co2", actions);
            myScopes.put("r_co2", myResource);

            KissValidator valid = new KissValidator(Collections.singleton("rs1"), myScopes);

            Set<String> resources = new HashSet<>();
            resources.add("temp");
            resources.add("co2");

            CoapsRS rsServer = new CoapsRS(new KissTime(), resources, valid, valid, null);

            System.out.println("Starting server");
            rsServer.start();

        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

}

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
            actions.add("POST");

            Map<String, Set<String>> tempResource = new HashMap<>();
            tempResource.put("temp", actions);

            Map<String, Map<String, Set<String>>> myScopes = new HashMap<>();
            myScopes.put("r_temp", tempResource);

            KissValidator validator = new KissValidator(Collections.singleton("rs1"), myScopes);

            CoapsRS rsServer = new CoapsRS(new KissTime(), validator, validator);

            System.out.println("Starting server");
            rsServer.start();

        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

}

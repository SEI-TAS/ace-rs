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
            for(String scopeName : tempResource.getScopeNames())
                myScopes.put(scopeName, tempResource.getScopeHandler());

            CoapsRS rsServer = new CoapsRS(myScopes);

            // This should be called as the result of pairing.
            byte[] sharedKey256Bytes = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};
            rsServer.setAS("AAIoT_AS", "coaps://localhost/authz-info/", sharedKey256Bytes);

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

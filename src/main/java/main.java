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
    static byte[] key256 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};

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

            CBORObject keyData = CBORObject.NewMap();
            keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
            keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(key256));
            OneKey sharedKey = new OneKey(keyData);

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

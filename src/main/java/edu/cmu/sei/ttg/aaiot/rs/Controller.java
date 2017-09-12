package edu.cmu.sei.ttg.aaiot.rs;

import edu.cmu.sei.ttg.aaiot.credentials.FileASCredentialStore;
import edu.cmu.sei.ttg.aaiot.pairing.PairingResource;
import edu.cmu.sei.ttg.aaiot.rs.resources.IIoTResource;
import edu.cmu.sei.ttg.aaiot.rs.resources.LightResource;
import edu.cmu.sei.ttg.aaiot.rs.resources.TempResource;
import edu.cmu.sei.ttg.aaiot.tokens.RevokedTokenChecker;
import org.eclipse.californium.core.CoapResource;
import se.sics.ace.AceException;

import java.io.IOException;
import java.util.*;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller
{
    private static final String PAIRING_KEY_ID = "pairing";
    private static final byte[] PAIRING_KEY = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final String RS_ID = "rs1";

    private FileASCredentialStore credentialStore;

    private CoapsRS rsServer = null;
    Map<String, Map<String, Set<String>>> myScopes = new HashMap<>();
    ArrayList<IIoTResource> resources = new ArrayList<>();

    public void run() throws COSE.CoseException, IOException, AceException
    {
        credentialStore = new FileASCredentialStore();

        // Set up our static resources.
        resources.add(new TempResource());
        resources.add(new LightResource());
        for(IIoTResource resource : resources)
        {
            for (String scopeName : resource.getScopeNames())
                myScopes.put(scopeName, resource.getScopeHandler());
        }

        Scanner scanner = new Scanner(System.in);
        while(true) {
            try
            {
                System.out.println("");
                System.out.println("Choose (p)air and restart server, (s)tart server, or (q)uit: ");
                char choice = scanner.next().charAt(0);

                switch (choice)
                {
                    case 'p':
                        boolean success = pair();

                        if (success)
                        {
                            System.out.println("Finished pairing procedure!");
                            setupCoapRS();
                            System.out.println("Server restarted!");
                        } else
                        {
                            System.out.println("Pairing aborted.");
                        }

                        break;
                    case 's':
                        setupCoapRS();
                        break;
                    case 'q':
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid command.");
                }
            }
            catch(Exception ex)
            {
                System.out.println("Error processing command: " + ex.toString());
            }
        }
    }

    // Handles the first part of the pair user command.
    public boolean pair()
    {
        try
        {
            PairingResource pairingManager = new PairingResource(PAIRING_KEY_ID, PAIRING_KEY, RS_ID, getScopeString(), credentialStore);
            return pairingManager.pair();
        }
        catch(Exception ex)
        {
            System.out.println("Error pairing: " + ex.toString());
            return false;
        }
    }

    // Starts the RS.
    private void setupCoapRS() throws COSE.CoseException, IOException, AceException
    {
        if(rsServer != null)
        {
            rsServer.close();
        }

        rsServer = new CoapsRS(RS_ID, myScopes);
        rsServer.setAS(credentialStore.getASid(), credentialStore.getASIP().getHostAddress(), credentialStore.getRawASPSK());

        // Add actual resources.
        for (IIoTResource resource : resources)
        {
            rsServer.add((CoapResource) resource);
        }

        System.out.println("Starting server");
        rsServer.start();
    }

    private Set<String> getScopes()
    {
        return new HashSet<>(myScopes.keySet());
    }

    private String getScopeString()
    {
        return String.join(";", getScopes());
    }

}

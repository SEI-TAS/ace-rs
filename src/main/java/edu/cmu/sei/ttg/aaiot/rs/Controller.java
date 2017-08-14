package edu.cmu.sei.ttg.aaiot.rs;

import edu.cmu.sei.ttg.aaiot.credentials.FileCredentialStore;
import edu.cmu.sei.ttg.aaiot.rs.pairing.PairingManager;
import edu.cmu.sei.ttg.aaiot.rs.resources.IIoTResource;
import edu.cmu.sei.ttg.aaiot.rs.resources.LightResource;
import edu.cmu.sei.ttg.aaiot.rs.resources.TempResource;
import org.eclipse.californium.core.CoapResource;
import se.sics.ace.AceException;

import java.io.IOException;
import java.util.*;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller
{
    private static final String rsId = "rs1";

    private FileCredentialStore credentialStore;

    private CoapsRS rsServer = null;
    Map<String, Map<String, Set<String>>> myScopes = new HashMap<>();
    ArrayList<IIoTResource> resources = new ArrayList<>();

    public void run() throws COSE.CoseException, IOException, AceException
    {
        credentialStore = new FileCredentialStore();

        TempResource tempResource = new TempResource();
        resources.add(tempResource);

        LightResource lightResource = new LightResource();
        resources.add(lightResource);

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

    public boolean pair()
    {
        try
        {
            PairingManager pairingManager = new PairingManager(rsId, getScopes(), credentialStore);
            pairingManager.startPairing();
            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Error pairing: " + ex.toString());
            return false;
        }
    }

    private void setupCoapRS() throws COSE.CoseException, IOException, AceException
    {
        if(rsServer != null)
        {
            rsServer.close();
        }

        rsServer = new CoapsRS(rsId, myScopes);
        rsServer.setAS(credentialStore.getASid(), "coaps://" + credentialStore.getASIP().getHostAddress() + "/authz-info/", credentialStore.getRawASPSK());

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

}

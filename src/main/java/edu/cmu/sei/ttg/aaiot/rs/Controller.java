package edu.cmu.sei.ttg.aaiot.rs;

import edu.cmu.sei.ttg.aaiot.rs.pairing.ICredentialStore;
import edu.cmu.sei.ttg.aaiot.rs.pairing.PairingManager;
import org.eclipse.californium.core.CoapResource;
import se.sics.ace.AceException;

import java.io.IOException;
import java.util.*;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller implements ICredentialStore
{
    private static final String rsId = "rs1";

    private String asId;
    private byte[] psk;

    private CoapsRS rsServer = null;
    Map<String, Map<String, Set<String>>> myScopes = new HashMap<>();
    ArrayList<CoapResource> resources = new ArrayList<>();

    public void run() throws COSE.CoseException, IOException, AceException
    {
        TempResource tempResource = new TempResource();
        resources.add(tempResource);
        for(String scopeName : tempResource.getScopeNames())
            myScopes.put(scopeName, tempResource.getScopeHandler());


        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("");
            System.out.println("Choose (p)air and restart server, or (q)uit: ");
            char choice = scanner.next().charAt(0);

            switch (choice) {
                case 'p':
                    pair();
                    System.out.println("Paired!");
                    setupCoapRS();
                    System.out.println("Server restarted!");
                    break;
                case 'q':
                    System.exit(0);
                default:
                    System.out.println("Invalid command.");
            }
        }
    }

    public void pair() throws IOException
    {
        PairingManager pairingManager = new PairingManager(this);
        pairingManager.startPairing();
    }

    private void setupCoapRS() throws COSE.CoseException, IOException, AceException
    {
        if(rsServer != null)
        {
            rsServer.close();
        }

        rsServer = new CoapsRS(rsId, myScopes);

        // TODO: should get URL from AS as well.
        rsServer.setAS(asId, "coaps://localhost/authz-info/", psk);

        // Add actual resources.
        for (CoapResource resource : resources)
        {
            rsServer.add(resource);
        }

        System.out.println("Starting server");
        rsServer.start();
    }

    @Override
    public String getId()
    {
        return rsId;
    }

    @Override
    public boolean storeAS(String asId, byte[] psk)
    {
        try
        {
            this.asId = asId;
            this.psk = psk;
            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Error storing AS key: " + ex.toString());
            return false;
        }
    }

    @Override
    public Set<String> getScopes()
    {
        return new HashSet<>(myScopes.keySet());
    }

}

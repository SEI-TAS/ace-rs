package edu.cmu.sei.ttg.aaiot.rs;

import edu.cmu.sei.ttg.aaiot.rs.pairing.ICredentialStore;
import edu.cmu.sei.ttg.aaiot.rs.pairing.PairingManager;
import se.sics.ace.AceException;

import java.io.IOException;
import java.util.*;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller implements ICredentialStore
{
    private static final String rsId = "rs1";

    private CoapsRS rsServer;
    Map<String, Map<String, Set<String>>> myScopes = new HashMap<>();

    public void run() throws COSE.CoseException, IOException, AceException
    {
        TempResource tempResource = new TempResource();
        for(String scopeName : tempResource.getScopeNames())
            myScopes.put(scopeName, tempResource.getScopeHandler());

        rsServer = new CoapsRS(rsId, myScopes);

        // Add actual resources.
        rsServer.add(tempResource);

        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("");
            System.out.println("Choose (p)air and start server, or (q)uit: ");
            char choice = scanner.next().charAt(0);

            switch (choice) {
                case 'p':
                    pair();
                    System.out.println("Paired!");
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
            // TODO: should get URL from AS as well.
            rsServer.setAS(asId, "coaps://localhost/authz-info/", psk);
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

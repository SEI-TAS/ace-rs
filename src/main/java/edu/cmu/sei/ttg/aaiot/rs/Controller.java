/*
AAIoT Source Code

Copyright 2018 Carnegie Mellon University. All Rights Reserved.

NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS"
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER
INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM
USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM
PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.

[DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see
Copyright notice for non-US Government use and distribution.

This Software includes and/or makes use of the following Third-Party Software subject to its own license:

1. ace-java (https://bitbucket.org/lseitz/ace-java/src/9b4c5c6dfa5ed8a3456b32a65a3affe08de9286b/LICENSE.md?at=master&fileviewer=file-view-default)
Copyright 2016-2018 RISE SICS AB.
2. zxing (https://github.com/zxing/zxing/blob/master/LICENSE) Copyright 2018 zxing.
3. sarxos webcam-capture (https://github.com/sarxos/webcam-capture/blob/master/LICENSE.txt) Copyright 2017 Bartosz Firyn.
4. 6lbr (https://github.com/cetic/6lbr/blob/develop/LICENSE) Copyright 2017 CETIC.

DM18-0702
*/

package edu.cmu.sei.ttg.aaiot.rs;

import edu.cmu.sei.ttg.aaiot.config.Config;
import edu.cmu.sei.ttg.aaiot.credentials.FileASCredentialStore;
import edu.cmu.sei.ttg.aaiot.pairing.PairingResource;
import edu.cmu.sei.ttg.aaiot.rs.resources.*;
import org.eclipse.californium.core.CoapResource;
import se.sics.ace.AceException;

import java.io.IOException;
import java.util.*;

/**
 * Created by Sebastian on 2017-07-11.
 */
public class Controller
{
    // Config file used to load configurations.
    private static final String CONFIG_FILE = "./config.json";

    private static final byte[] PAIRING_KEY = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    //private static final byte[] TEST_KEY = {(byte) 0xb1, (byte) 0xb2, (byte) 0xb3, 0x04, 0x05, 0x06, 0x07, 0x08,
    //        0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};

    private static final String BASE_PATH = "ace";

    private FileASCredentialStore credentialStore;

    private String rsId = "";
    private CoapsRS rsServer = null;
    private Map<String, Map<String, Set<Short>>> myScopes = new HashMap<>();
    private ArrayList<IIoTResource> resources = new ArrayList<>();

    public void run() throws COSE.CoseException, IOException
    {
        try
        {
            Config.load(CONFIG_FILE);
        }
        catch(Exception ex)
        {
            System.out.println("Error loading config file: " + ex.toString());
            throw new RuntimeException("Error loading config file: " + ex.toString());
        }

        rsId = Config.data.get("id");

        credentialStore = new FileASCredentialStore();

        // TODO: For testing purposes only:
        //credentialStore.storeAS("AS", TEST_KEY, InetAddress.getByName("127.0.0.1"));

        // Set up our static resources.
        resources.add(new TempResource());
        resources.add(new LightResource());
        resources.add(new HelloWorldResource());
        resources.add(new LockResource());

        for(IIoTResource resource : resources)
        {
            Map<String, Set<Short>> actionsByScope = resource.getActionsByScope();
            for (String scopeName : actionsByScope.keySet())
            {
                Map<String, Set<Short>> resourceActions = new HashMap<>();
                String fullResourceName = BASE_PATH + "/" + ((CoapResource) resource).getName();
                System.out.println(fullResourceName);
                resourceActions.put(fullResourceName, actionsByScope.get(scopeName));
                myScopes.put(scopeName, resourceActions);
            }
        }

        Scanner scanner = new Scanner(System.in);
        while(true) {
            try
            {
                System.out.println("");
                System.out.println("Choose (p)air and restart server, (s)tart server, (v) start server with revocation check, or (q)uit: ");
                char choice = scanner.next().charAt(0);

                switch (choice)
                {
                    case 'p':
                        boolean success = pair();

                        if (success)
                        {
                            System.out.println("Finished pairing procedure!");
                            setupCoapRS(false);
                            System.out.println("Server restarted!");
                        } else
                        {
                            System.out.println("Pairing aborted.");
                        }

                        break;
                    case 's':
                        setupCoapRS(false);
                        break;
                    case 'v':
                        setupCoapRS(true);
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
                //ex.printStackTrace();
            }
        }
    }

    // Handles the first part of the pair user command.
    public boolean pair()
    {
        try
        {
            PairingResource pairingManager = new PairingResource(PAIRING_KEY, rsId, getScopeString(), credentialStore);
            return pairingManager.pair();
        }
        catch(Exception ex)
        {
            System.out.println("Error pairing: " + ex.toString());
            return false;
        }
    }

    // Starts the RS.
    private void setupCoapRS(boolean startRevocationChecker) throws IOException, AceException
    {
        if(rsServer != null)
        {
            rsServer.close();
        }

        if(credentialStore.getASid() == null)
        {
            throw new AceException("Server can't be started since there is no paired AS.");
        }

        rsServer = new CoapsRS(rsId, myScopes);
        rsServer.setAS(credentialStore.getASid(), credentialStore.getASIP().getHostAddress(), credentialStore.getRawASPSK());

        // Add actual resources.
        CoapResource baseResource = new CoapResource(BASE_PATH);
        for (IIoTResource resource : resources)
        {
            baseResource.add((CoapResource) resource);
        }
        rsServer.add(baseResource);

        System.out.println("Starting server");
        rsServer.start(startRevocationChecker);
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

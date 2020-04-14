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

/**
 * Created by Sebastian on 2017-05-05.
 */
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Logger;

import COSE.*;
import edu.cmu.sei.ttg.aaiot.config.Config;
import edu.cmu.sei.ttg.aaiot.tokens.IRemovedTokenTracker;
import edu.cmu.sei.ttg.aaiot.tokens.RevokedTokenChecker;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;

import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.coap.rs.CoapAuthzInfo;
import se.sics.ace.coap.rs.CoapDeliverer;
import se.sics.ace.coap.rs.dtlsProfile.DtlspPskStore;
import se.sics.ace.cwt.CwtCryptoCtx;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.KissValidator;
import se.sics.ace.rs.AsRequestCreationHints;
import se.sics.ace.rs.AudienceValidator;
import se.sics.ace.rs.AuthzInfo;
import se.sics.ace.rs.ScopeValidator;
import se.sics.ace.rs.TokenRepository;

/**
 * A resource server listening to CoAP requests over DTLS.
 *
 * Create an instance of this server with the constructor then call
 * CoapsAS.start();
 *
 * @author Sebastian Echeverria
 *
 */
public class CoapsRS extends CoapServer implements AutoCloseable, IRemovedTokenTracker
{

    /**
     * The logger
     */
    private static final Logger LOGGER = Logger.getLogger(CoapsRS.class.getName());

    private String name;
    private static final String TOKEN_FILE_PATH = "tokenRepo.json";
    private static final int AS_COAPS_PORT = 5684;

    private RevokedTokenChecker checker;

    private String asServerName;
    private byte[] asPsk;
    private int rsCoapPort;
    private int rsCoapsPort;

    private AuthzInfo authInfoHandler = null;
    private CoapAuthzInfo authInfoEndpoint;

    private AudienceValidator audienceValidator;
    private ScopeValidator scopeValidator;

    private CwtCryptoCtx ctx;
    private TokenRepository tokenRepo;

    /**
     * Constructor.
     *
     * @param myScopes the scopes to set up
     *
     */
    public CoapsRS(String name, Map<String, Map<String, Set<Short>>> myScopes)
    {
        this.name = name;
        Set<String> audiences = new HashSet<>();
        audiences.add(name);
        KissValidator validator = new KissValidator(audiences, myScopes);
        audienceValidator = validator;
        scopeValidator = validator;

        rsCoapPort = Integer.parseInt(Config.data.get("local_coap_port"));
        rsCoapsPort = Integer.parseInt(Config.data.get("local_coaps_port"));
    }

    public void setAS(String asName, String asServerName, byte[] asPSK) throws AceException, IOException
    {
        this.asServerName = asServerName;
        this.asPsk = asPSK;

        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, AlgorithmID.AES_CCM_16_64_128, AlgorithmID.Direct);
        ctx = CwtCryptoCtx.encrypt0(asPSK, coseP.getAlg().AsCBOR());

        TokenRepository.create(scopeValidator, TOKEN_FILE_PATH, ctx);
        tokenRepo = TokenRepository.getInstance();

        List<String> issuers = Collections.singletonList(asName);
        this.authInfoHandler = new AuthzInfo(tokenRepo, issuers, new KissTime(), null, audienceValidator, ctx);
        this.authInfoEndpoint = new CoapAuthzInfo(this.authInfoHandler);
        add(this.authInfoEndpoint);

        // DTLS endpoint.
        addEndpoint(getCoapsEndpoint());

        // Non-DTLS endpoint for authz-info posts.
        addEndpoint(new CoapEndpoint.Builder().setPort(rsCoapPort).build());

        String asURI = "coap://" + asServerName + "/authz-info/";
        AsRequestCreationHints arch = new AsRequestCreationHints(asURI, null, false, false);
        CoapDeliverer dpd = new CoapDeliverer(getRoot(), tokenRepo, null, arch);
        setMessageDeliverer(dpd);
    }

    private CoapEndpoint getCoapsEndpoint()
    {
        LOGGER.info("Starting CoapsRS with PSK only");
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder().setAddress(new InetSocketAddress(rsCoapsPort));
        config.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});

        DtlspPskStore pskStore = new DtlspPskStore(this.authInfoHandler);
        config.setPskStore(pskStore);

        DTLSConnector connector = new DTLSConnector(config.build());
        CoapEndpoint endpoint = new CoapEndpoint.Builder().setConnector(connector)
                .setNetworkConfig(NetworkConfig.getStandard()).build();
        return endpoint;
    }

    @Override
    public void start()
    {
        start(false);
    }

    public void start(boolean checkForRevokedTokens)
    {
        super.start();

        if(checkForRevokedTokens)
        {
            System.out.println("Starting revoked tokens checker.");
            try
            {
                checker = new RevokedTokenChecker(this.asServerName, AS_COAPS_PORT, this.name, this.asPsk, this, TokenRepository.getInstance());
            } catch (AceException ex)
            {
                throw new RuntimeException(ex.toString());
            }

            checker.startChecking();
        }
    }

    @Override
    public void close() throws AceException, IOException {
        LOGGER.info("Closing down CoapsRS ...");
        tokenRepo.close();
        new PrintWriter(TOKEN_FILE_PATH).close();
        this.stop();

        if(this.checker != null)
        {
            this.checker.stopChecking();
        }
    }

    @Override
    public void notifyRemovedToken(String s, String s1)
    {
        // Nothing for now, we are not tracking this.
    }
}

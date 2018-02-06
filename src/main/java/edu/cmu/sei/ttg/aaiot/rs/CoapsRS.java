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
import se.sics.ace.rs.AsInfo;
import se.sics.ace.coap.rs.CoapAuthzInfo;
import se.sics.ace.coap.rs.CoapDeliverer;
import se.sics.ace.coap.rs.dtlsProfile.DtlspPskStore;
import se.sics.ace.cwt.CwtCryptoCtx;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.KissValidator;
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
    private static final int AS_PORT = 5684;

    private static final int RS_COAP_PORT = 5690;

    private RevokedTokenChecker checker;

    private String asServerName;
    private byte[] asPsk;

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
     * @throws AceException
     * @throws CoseException
     *
     */
    public CoapsRS(String name, Map<String, Map<String, Set<String>>> myScopes)
            throws AceException, CoseException, IOException {

        this.name = name;
        KissValidator validator = new KissValidator(Collections.singleton(name), myScopes);
        audienceValidator = validator;
        scopeValidator = validator;
    }

    public void setAS(String asName, String asServerName, byte[] asPSK) throws AceException, CoseException, IOException
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
        addEndpoint(new CoapEndpoint(new InetSocketAddress(RS_COAP_PORT)));

        String asURI = "coap://" + asServerName + "/authz-info/";
        AsInfo asi = new AsInfo(asURI);
        CoapDeliverer dpd = new CoapDeliverer(getRoot(), tokenRepo, null, asi);
        setMessageDeliverer(dpd);
    }

    private CoapEndpoint getCoapsEndpoint() throws CoseException, IOException
    {
        LOGGER.info("Starting CoapsRS with PSK only");
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(new InetSocketAddress(5685));
        config.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});

        DtlspPskStore pskStore = new DtlspPskStore(this.authInfoHandler);
        config.setPskStore(pskStore);

        DTLSConnector connector = new DTLSConnector(config.build());
        CoapEndpoint endpoint = new CoapEndpoint(connector, NetworkConfig.getStandard());
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
                checker = new RevokedTokenChecker(this.asServerName, AS_PORT, this.name, this.asPsk, this, TokenRepository.getInstance());
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

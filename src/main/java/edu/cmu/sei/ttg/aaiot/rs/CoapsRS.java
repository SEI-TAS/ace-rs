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
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskClient;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;

import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.Constants;
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
public class CoapsRS extends CoapServer implements AutoCloseable {

    /**
     * The logger
     */
    private static final Logger LOGGER = Logger.getLogger(CoapsRS.class.getName());

    private String name;
    private static final String TOKEN_FILE_PATH = "src/main/resources/testTokens.json";

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

        addEndpoint(getCoapsEndpoint());

        String asURI = "coaps://" + asServerName + "/authz-info/";
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

    // Sends an introspection request only to check if the token is still marked as valid or not. If invalid, this could
    // be from a revoked or an expired token.
    public boolean isTokenActive(CBORObject token) throws AceException
    {
        CoapsPskClient client = new CoapsPskClient(this.asServerName, 5684, this.name, this.asPsk);

        Map<String, CBORObject> params = new HashMap<>();
        params.put("token", token);
        CBORObject cborParams = Constants.abbreviate(params);

        CBORObject reply = client.sendRequest("introspect", "post", cborParams);

        Map<String, CBORObject> mapReply = Constants.unabbreviate(reply);
        boolean isActive = mapReply.get("active").AsBoolean();
        return isActive;
    }

    @Override
    public void close() throws AceException, IOException {
        LOGGER.info("Closing down CoapsRS ...");
        tokenRepo.close();
        new PrintWriter(TOKEN_FILE_PATH).close();
        this.stop();
        //this.authInfoEndpoint.close();
    }
}

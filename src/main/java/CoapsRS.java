/**
 * Created by Sebastian on 2017-05-05.
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Logger;

import COSE.*;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;

import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.TimeProvider;
import se.sics.ace.as.PDP;
import se.sics.ace.coap.as.CoapAceEndpoint;
import se.sics.ace.coap.rs.dtlsProfile.AsInfo;
import se.sics.ace.coap.rs.dtlsProfile.DtlspAuthzInfo;
import se.sics.ace.coap.rs.dtlsProfile.DtlspDeliverer;
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

    static byte[] sharedKey256Bytes = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};

    private AuthzInfo authInfoResource = null;
    private DtlspAuthzInfo authInfoEndpoint;

    /**
     * Constructor.
     *
     * @param time
     * @param asymmetricKey
     * @throws AceException
     * @throws CoseException
     *
     */
    public CoapsRS(TimeProvider time, Set<String> resources,
                   AudienceValidator audValidator, ScopeValidator scopeValidator,
                   OneKey asymmetricKey) throws AceException, CoseException, IOException {

        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, AlgorithmID.AES_CCM_16_128_256, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(sharedKey256Bytes, coseP.getAlg().AsCBOR());

        TokenRepository.create(scopeValidator, "src/test/resources/tokens.json", ctx);
        TokenRepository tokenRepo = TokenRepository.getInstance();
        List<String> issuers = Collections.singletonList("TestAS");
        this.authInfoResource = new AuthzInfo(tokenRepo, issuers, time, null, audValidator, ctx);
        this.authInfoEndpoint = new DtlspAuthzInfo(this.authInfoResource);
        add(this.authInfoEndpoint);

        addEndpoint(getCoapsEndpoint());

        AsInfo asi = new AsInfo("coaps://blah/authz-info/");
        DtlspDeliverer dpd = new DtlspDeliverer(getRoot(), tokenRepo, null, asi);
        //setMessageDeliverer(dpd);
    }

    private CoapEndpoint getCoapsEndpoint() throws CoseException, IOException
    {
        LOGGER.info("Starting CoapsRS with PSK only");
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(new InetSocketAddress(5685));
        config.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});

        //DtlspPskStore store = new DtlspPskStore(this.authInfoResource);
        // Add clientA to store, so it can be known by RS. This should be the result of pairing or something...
        config.setPskStore(new StaticPskStore("clientA", sharedKey256Bytes));

        DTLSConnector connector = new DTLSConnector(config.build());
        CoapEndpoint endpoint = new CoapEndpoint(connector, NetworkConfig.getStandard());
        return endpoint;
    }

    @Override
    public void close() throws Exception {
        LOGGER.info("Closing down CoapsRS ...");
        //this.authInfoEndpoint.close();
    }
}

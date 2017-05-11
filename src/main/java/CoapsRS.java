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

import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.TimeProvider;
import se.sics.ace.as.PDP;
import se.sics.ace.coap.as.CoapAceEndpoint;
import se.sics.ace.coap.rs.dtlsProfile.DtlspAuthzInfo;
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

    private static byte[] key128 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

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

        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, coseP.getAlg().AsCBOR());

        TokenRepository.create(scopeValidator, "src/test/resources/tokens.json", ctx);
        TokenRepository tokenRepo = TokenRepository.getInstance();
        List<String> issuers = Collections.singletonList("TestAS");
        this.authInfoResource = new AuthzInfo(tokenRepo, issuers, time, null, audValidator, ctx);
        this.authInfoEndpoint = new DtlspAuthzInfo(this.authInfoResource);
        add(this.authInfoEndpoint);

        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(
                new InetSocketAddress(CoAP.DEFAULT_COAP_SECURE_PORT));
        if (asymmetricKey != null && asymmetricKey.get(KeyKeys.KeyType) == KeyKeys.KeyType_EC2 ) {
            LOGGER.info("Starting CoapsRS with PSK and RPK");
            config.setSupportedCipherSuites(new CipherSuite[]{
                    CipherSuite.TLS_PSK_WITH_AES_128_CCM_8,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
        } else {
            LOGGER.info("Starting CoapsRS with PSK only");
            config.setSupportedCipherSuites(new CipherSuite[]{
                    CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});
        }

        DtlspPskStore store = new DtlspPskStore(this.authInfoResource);
        config.setPskStore(store);
        if (asymmetricKey != null) {
            config.setIdentity(asymmetricKey.AsPrivateKey(), asymmetricKey.AsPublicKey());
        }

        DTLSConnector connector = new DTLSConnector(config.build());
        addEndpoint(new CoapEndpoint(connector, NetworkConfig.getStandard()));
    }

    @Override
    public void close() throws Exception {
        LOGGER.info("Closing down CoapsRS ...");
        //this.authInfoEndpoint.close();
    }
}

package edu.cmu.sei.ttg.aaiot.rs.pairing;

import java.util.Set;

/**
 * Created by sebastianecheverria on 7/25/17.
 */
public interface ICredentialStore
{
    String getId();
    Set<String> getScopes();
    boolean storeAS(String id, byte[] psk);
}

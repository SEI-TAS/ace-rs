package edu.cmu.sei.ttg.aaiot.rs.resources;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sebastianecheverria on 8/3/17.
 */
public interface IIoTResource
{
    Set<String> getActions();

    List<String> getScopeNames();

    Map<String, Set<String>> getScopeHandler();
}

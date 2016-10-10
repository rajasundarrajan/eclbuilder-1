package org.hpccsystems.dsp.service;

import java.util.List;
import java.util.Map;

import org.hpcc.HIPIE.Contract;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.PluginException;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.User;

public interface PluginService {

    List<String> getAllPluginRepos(User user) throws PluginException;

    Map<String, List<Plugin>> getAllPlugins(User user, String repoName) throws PluginException;
    
    List<String> getAllRepos() throws PluginException;
    
    Contract getDatasourceContract() throws HipieException ;
}

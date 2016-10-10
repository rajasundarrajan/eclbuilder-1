package org.hpccsystems.dsp.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.servlet.ServletContext;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.usergroupservice.IUserGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;

@Service("authenticationService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final String INVALID_CREDENTIALS = "invalidCredentials";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    @Override
    public User getCurrentUser() {
        return (User) getCurrentSession().getAttribute(Constants.USER);
    }

    @Override
    public void logout(Object object) {
        getCurrentSession().invalidate();
    }

    /**
     * Fetches the user for the specified account. In case of a the stub
     * authentication, returns a new User object populated with the user id and
     * password
     * 
     * @param account
     *            The id of the user
     * @param password
     *            The password
     * @param context
     *            Servlet context. If this is null, current application context
     *            is used
     * @return A User object
     * @throws AuthenticationException
     * @throws Exception
     */
    @Override
    public User fetchUser(String account, String password, ServletContext context) throws AuthenticationException {
        IUserGroupService ugsvc = null;
        ServletContext servletContext = context == null ? (ServletContext) getCurrentSession().getWebApp().getServletContext() : context;
        if (isBlank(account)) {
            return null;
        }

        if (isBlank(HipieSingleton.getUgsDomain())) {
            throw new AuthenticationException(Labels.getLabel("mbsusergroupdomainnotfound"));
        }

        try {
            ugsvc = HipieSingleton.getHipie().getPermissionsManager().getAuthManager();
        } catch (Exception ex) {
            throw new AuthenticationException(Labels.getLabel(INVALID_CREDENTIALS), ex);
        }
        User user = null;
        
        
        
        return user;
    }

    @Override
    public User fetchUser(String account, String password) throws AuthenticationException {
        return fetchUser(account, password, null);
    }

    @Override
    public void setRedirectURL(String url, String params) {
        getCurrentSession().setAttribute(AuthenticationService.URL, url);
        getCurrentSession().setAttribute(AuthenticationService.PARAMS, params);
    }

    @Override
    public void clearRedirectURL() {
        getCurrentSession().setAttribute(AuthenticationService.URL, null);
        getCurrentSession().setAttribute(AuthenticationService.PARAMS, null);
    }

    @Override
    public String getRedictURL() {
        return (String) getCurrentSession().getAttribute(AuthenticationService.URL);
    }

    private Session getCurrentSession() {
        return Sessions.getCurrent();
    }

    @Override
    public String getRedirectParams() {
        return (String) getCurrentSession().getAttribute(AuthenticationService.PARAMS);
    }

    @Override
    public boolean hasRedirectURL() {
        return getCurrentSession().hasAttribute(AuthenticationService.PARAMS) && getCurrentSession().hasAttribute(AuthenticationService.URL);
    }

}

package org.hpccsystems.dsp.ramps.controller;

import java.io.IOException;
import java.util.Map;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.BookmarkEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Initiator;
import org.zkoss.zkplus.spring.SpringUtil;

public class AuthenticatorInit implements Initiator {

    private static final String LOGIN_URL = "/login.zhtml";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatorInit.class);
    
    public static void processBookmark(Event event) {
        BookmarkEvent bmEvent = (BookmarkEvent) event;
        LOGGER.debug("Bookmark received  - {}",  bmEvent.getBookmark());
    }
    
    @Override
    public void doInit(Page page, Map<String, Object> arg1) {
        page.addEventListener(Events.ON_BOOKMARK_CHANGE, AuthenticatorInit::processBookmark);
        
        User user = (User) Sessions.getCurrent().getAttribute(Constants.USER);
        
        //Adding composition id if present to session.
        String requestPath = Executions.getCurrent().getDesktop().getRequestPath();
        Map<String, String[]> parameterMap = Executions.getCurrent().getParameterMap();

        if(parameterMap.containsKey(Constants.COMPOSITION)) {
            AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean("authenticationService");
            if(!parameterMap.isEmpty()) {
                authenticationService.setRedirectURL(requestPath, parameterMap.get(Constants.COMPOSITION)[0]);
            }
            
        }
        
        if (user == null) {
            LOGGER.debug("Redirect url - {} \n Params - {}", requestPath, parameterMap);
            try {
                Executions.forward(LOGIN_URL);
            } catch (IOException e) {
                LOGGER.error("Request formarding failed", e);
                LOGGER.info("Redirecting to login page");
                Executions.sendRedirect(LOGIN_URL);
            }
            return;
        }
    }

}

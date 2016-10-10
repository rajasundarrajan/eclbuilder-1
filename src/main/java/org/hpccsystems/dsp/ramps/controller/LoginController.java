package org.hpccsystems.dsp.ramps.controller;

import java.time.Instant;
import java.util.Collection;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Perspective;
import org.hpccsystems.dsp.entity.Permission;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.log.ApplicationAccess;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.SettingsService;
import org.hpccsystems.dsp.service.UserService;
import org.hpccsystems.usergroupservice.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class LoginController extends SelectorComposer<Component> {

    private static final String DSP_DEV_PERMISSION_NOT_DEFINED = "dspDevPermissionNotDefined";
    private static final String FAILED = "Failed.";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    @Wire
    Textbox account;
    @Wire
    Textbox password;
    @Wire
    Button login;
    @Wire
    Button cancel;
    @Wire
    Label message;
    @Wire
    Listbox language;
    @Wire
    Listitem listItemEnglish, listItemChinese;

    @Override
    public void doAfterCompose(final Component comp) throws Exception {
        super.doAfterCompose(comp);
        // Redirecting if the user is already logged in.
        if (((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser() != null) {
            Executions.sendRedirect("/");
            return;
        }
        account.focus();
    }

    @Listen("onClick = #login; onOK = #loginWin")
    public void onLogin(Event event) {
        long startTime = Instant.now().toEpochMilli();
        disableLogin();
        String accnt = account.getValue();
        String pwd = password.getValue();
        try {
            // On Successful LDAP Authentication the user object is returned,if
            // not the authentication exception would be thrown.
            User user = ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).fetchUser(accnt, pwd);
            if (user != null) {
                if(!user.isGlobalAdmin()){
                    if (((SettingsService) SpringUtil.getBean("settingsService")).isMaintenanceEnabled()) {
                        message.setValue(Labels.getLabel("applicationUnderMaintenance"));
                        return;
                    }
                }
            
                // Check whether the user has the DSPDEV permissions,if not
                // throw error message and return.
                if (!user.canLogin()) {
                    message.setValue(Labels.getLabel(DSP_DEV_PERMISSION_NOT_DEFINED));
                    enableLogin();
                    doDBLog(accnt, startTime, FAILED + Labels.getLabel(DSP_DEV_PERMISSION_NOT_DEFINED));
                    return;
                }
                LOGGER.debug("User is admin:{}", user.isGlobalAdmin());
                LOGGER.debug("User permissions {}", StringUtils.join(user.getPermissions(), ","));
                LOGGER.debug("User groups {}", StringUtils.join(user.getGroups(), ","));
                // get the DSP level user permission from DB and set in user
                // object
                org.hpccsystems.usergroupservice.User mbsUser = user.getMbsUser();
                Collection<Group> userGroups = mbsUser.getGroups();
                Permission userGroupsPermission = ((UserService)SpringUtil.getBean(Constants.USER_SERVICE)).getUserPermission(userGroups);
                user.setPermission(userGroupsPermission);
                setPerspective(user);
                // User is successfully authenticated set the user object in
                // session and redirect to home page
                getCurrentSession().setAttribute(Constants.USER, user);
                //Adding user to native session for pass-through validation
                HttpSession nativeSession = (HttpSession) Sessions.getCurrent().getNativeSession();
                nativeSession.setAttribute(Constants.USER, user);
                
                doDBLog(accnt, startTime, "Success");
                message.setSclass("warn login-success");
                message.setValue(Labels.getLabel("welcome") +" "+ user.getName());                
                Executions.sendRedirect("/");
            } else {
                message.setValue(Labels.getLabel("invalidCredentials"));
                enableLogin();
            }
        } catch (DatabaseException ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            enableLogin();
            message.setValue(ex.getMessage());
            doDBLog(accnt, startTime, FAILED + ex.getMessage());
        } catch (AuthenticationException ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            enableLogin();
            message.setValue(ex.getMessage());
            doDBLog(accnt, startTime, FAILED + ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            enableLogin();
            message.setValue(Labels.getLabel("errorAuthentication"));
            doDBLog(accnt, startTime, FAILED + ex.getMessage());
        }

    }

    @Listen("onClick = #cancel")
    public void onClickCancel() {
        account.setValue("");
        password.setValue("");
        message.setValue(" ");
    }

    private void disableLogin() {
        account.setDisabled(true);
        password.setDisabled(true);
        login.setDisabled(true);
    }

    private void enableLogin() {
        account.setDisabled(false);
        password.setDisabled(false);
        login.setDisabled(false);
    }

    private void doDBLog(String accnt, long startTime, String msg) {
        if (LOGGER.isDebugEnabled()) {
            ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).log(new ApplicationAccess(accnt, ApplicationAccess.ACTION_LOGIN, startTime, msg));
        }
    }

    private void setPerspective(User user) {
        if (user.getMbsUser() != null && user.getMbsUser().getIsGlobalAdmin()) {
            user.addPerspective(Perspective.ADMINISTRATION);
        }
        // If the permissions are not configured in db the permission object
        // would be null
        if (user.getPermission() != null) {
            if (user.getPermission().canViewDashboard()) {
                user.addPerspective(Perspective.DASHBOARD);
            }
            if (user.getPermission().canViewRamps()) {
                user.addPerspective(Perspective.RAMPS);
            }
        }
    }

    private Session getCurrentSession() {
        return Sessions.getCurrent();
    }
}

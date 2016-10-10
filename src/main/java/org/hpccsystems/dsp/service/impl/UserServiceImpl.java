package org.hpccsystems.dsp.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;

import org.hpccsystems.dsp.Perspective;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.entity.Permission;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.UserService;
import org.hpccsystems.usergroupservice.Group;
import org.hpccsystems.usergroupservice.IUserGroupService;
import org.hpccsystems.usergroupservice.UserGroupFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Sessions;

@Service("userService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserServiceImpl implements UserService {

    private DSPDao dspDao;

    @Autowired
    public void setRampsDao(DSPDao dspDao) {
        this.dspDao = dspDao;
    }

    @Override
    public Collection<Group> getAllGroups() throws AuthenticationException {
        ServletContext sc = (ServletContext) Sessions.getCurrent().getWebApp().getServletContext();
        IUserGroupService ugsvc;
        try {
            ugsvc = UserGroupFactory.GetService(sc);
            return ugsvc.getAllGroups();
        } catch (Exception e) {
            throw new AuthenticationException(Labels.getLabel("errorFetchingGroups"), e);
        }
    }

    @Override
    public Collection<org.hpccsystems.usergroupservice.User> getAllUsers() throws AuthenticationException {
        ServletContext sc = (ServletContext) Sessions.getCurrent().getWebApp().getServletContext();
        try {
            IUserGroupService ugsvc = UserGroupFactory.GetService(sc);
            return ugsvc.getAllUsers();
        } catch (Exception e) {
            throw new AuthenticationException(Labels.getLabel("errorFetchingUsers"), e);
        }

    }

    @Override
    public void saveRampsUserGroups(String groupCode, Permission permission) throws DatabaseException {
        dspDao.saveRampsUserGroups(groupCode, permission.getRampsPermission());
    }
    
    @Override
    public void saveDashboardUserGroups(String groupCode, Permission permission) throws DatabaseException {
        dspDao.saveDashboardUserGroups(groupCode, permission.getDashboardPermission().getUiPermission());
    }

    @Override
    public Map<String, Permission> getGroupPermissions(Collection<Group> userGroups) throws DatabaseException {
        Map<String, Permission> groupPermission = new LinkedHashMap<String, Permission>();
        List<String> groupCodes = new ArrayList<String>();
        // Including a try-catch to handle run-time errors
        userGroups.stream().forEach(group -> groupCodes.add(group.getName()));
        groupPermission = dspDao.getGroupPermissions(groupCodes);

        return groupPermission;
    }

    @Override
    public Permission getUserPermission(Collection<Group> userGroups) throws DatabaseException {
        Map<String, Permission> allGroup = getGroupPermissions(userGroups);
        Iterator<Permission> allGroupIterator = allGroup.values().iterator();
        Permission userPermission = null;
        if (allGroupIterator.hasNext()) {
            userPermission = allGroupIterator.next();
        }
        while(allGroupIterator.hasNext()){
            userPermission.updatePermission(allGroupIterator.next());
        }
        return userPermission;
    }

    @Override
    public Map<String, Permission> getPersPermissions(Collection<Group> userGroups) throws DatabaseException {
        Map<String, Permission> persPermission = new HashMap<String, Permission>();

        for (Group group : userGroups) {
            String groupName = group.getName();
            persPermission.put(groupName, dspDao.getPersPermissions(groupName));

        }

        return persPermission;
    }

    @Override
    public void savePersPermission(Map<String, Permission> persPermissions) throws DatabaseException {
        for (Entry<String, Permission> persPermission : persPermissions.entrySet()) {
            dspDao.savePersPermission(persPermission.getKey(), persPermission.getValue());
        }
    }

    @Override
    public Perspective getLastViewPerspective(User user) {
        String lastView = dspDao.getLastViewPerspective(user.getId());
        if(lastView != null){
            for(Perspective pers : Perspective.values()){
                if(pers.name().equals(lastView) && user.canAccess(pers)){
                    return pers;
                }
            }
        }
        return null;
    }

    @Override
    public void setLastViewPerspective(String userId, String perspective) {
        dspDao.setLastViewPerspective(userId, perspective);
    }
    
    @Override
    public List<String> getAdvancedModeGroups() {
        return dspDao.getAdvancedModeGroups();
    }
    
    @Override
    public List<String> getConvertToCompGroups() {
        return dspDao.getConvertToCompGroups();
    }
}

package org.hpccsystems.dsp.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hpccsystems.dsp.Perspective;
import org.hpccsystems.dsp.entity.Permission;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.usergroupservice.Group;

public interface UserService {

    Collection<Group> getAllGroups() throws AuthenticationException;

    Collection<org.hpccsystems.usergroupservice.User> getAllUsers() throws AuthenticationException;

    void saveRampsUserGroups(String groupCode, Permission value) throws DatabaseException;
    
    void saveDashboardUserGroups(String groupCode, Permission value) throws DatabaseException;

    Map<String, Permission> getGroupPermissions(Collection<Group> userGroups) throws DatabaseException;

    Permission getUserPermission(Collection<Group> userGroups) throws DatabaseException;

    Map<String, Permission> getPersPermissions(Collection<Group> userGroups) throws DatabaseException;

    void savePersPermission(Map<String, Permission> persPermissions) throws DatabaseException;

    Perspective getLastViewPerspective(User user);
    
    void setLastViewPerspective(String userId, String perspective);

    List<String> getAdvancedModeGroups();
    
    List<String> getConvertToCompGroups();
}

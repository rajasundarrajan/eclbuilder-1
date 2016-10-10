package org.hpccsystems.dsp.ramps.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.dude.Permission;
import org.hpcc.HIPIE.dude.Permission.PermissionLevel;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.ramps.entity.CompositionPermisson;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;

public class PermissionsViewModel implements Serializable{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsViewModel.class);
   
    private List<CompositionPermisson> permissions;

    private String userAccessError;

    private String groupAccessError;

    private Collection<String> filteredUsers;
    
    public String getUserAccessError() {
        return userAccessError;
    }

    public void setUserAccessError(String userAccessError) {
        this.userAccessError = userAccessError;
    }

    public String getGroupAccessError() {
        return groupAccessError;
    }

    public void setGroupAccessError(String groupAccessError) {
        this.groupAccessError = groupAccessError;
    }

    public Collection<String> getAllGroups() {

        Collection<String> groups = new ArrayList<>();
        try {
            groups = RampsUtil.retriveAllGroups();
        } catch (AuthenticationException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            setGroupAccessError(Labels.getLabel("noGroupsToShow"));
        }
        return groups;
    }
    
    public Collection<String> getFilteredUsers() {
        return filteredUsers;
    }

    public void setFilteredUsers(Collection<String> filteredUsers) {
        this.filteredUsers = filteredUsers;
    }

    public List<CompositionPermisson> getPermissions() {
        return permissions;
    }

    @NotifyChange("permissions")
    public void setPermissions(List<CompositionPermisson> permissions) {
        this.permissions = permissions;
    }
  

    @Init
    public void init(@ExecutionArgParam("compositionPermission") List<Permission> permissionsfromInclude) {
        List<CompositionPermisson> cmpPermissions = new ArrayList<CompositionPermisson>();
        for (Permission permission : permissionsfromInclude) {
          CompositionPermisson cmpPermission = new CompositionPermisson();
          cmpPermission.setPermission(permission);
          if(PermissionLevel.CUSTOM.equals(permission.getPermissionLevel())){
              cmpPermission.setCustomComposition(true);
          }else if(PermissionLevel.PRIVATE.equals(permission.getPermissionLevel())){
              cmpPermission.setPrivateComposition(true);
          }else if(PermissionLevel.PUBLIC.equals(permission.getPermissionLevel())){
              cmpPermission.setPublicComposition(true);
          }
          
          loadCustomPermission(permission,cmpPermission);
          cmpPermissions.add(cmpPermission);
        }
        permissions =cmpPermissions;

    }
   
    @Command
    public void givePermissions(@BindingParam("level") String level,
            @BindingParam("selectedPermission") CompositionPermisson cmpPermission) {
        if(PermissionLevel.PUBLIC.toString().equalsIgnoreCase(level)){
            cmpPermission.getPermission().setPermissionLevel(PermissionLevel.PUBLIC);
            removeCustompermissions(cmpPermission);
            checkRadioButtons(cmpPermission,true,false,false);
        }else if(PermissionLevel.PRIVATE.toString().equalsIgnoreCase(level)){
            cmpPermission.getPermission().setPermissionLevel(PermissionLevel.PRIVATE);
            removeCustompermissions(cmpPermission);
            checkRadioButtons(cmpPermission,false,true,false);
            
        }else if(PermissionLevel.CUSTOM.toString().equalsIgnoreCase(level)){
            cmpPermission.getPermission().setPermissionLevel(PermissionLevel.CUSTOM);
            checkRadioButtons(cmpPermission,false,false,true);
            
        }

    }
    
    @Command
    public void giveGroupPermissions(@ContextParam(ContextType.TRIGGER_EVENT) SelectEvent<Component,String> event,
            @BindingParam("selectedPermission") CompositionPermisson cmpPermission) {
        cmpPermission.getPermission().getGroups().clear();
        event.getSelectedObjects()
            .forEach(item -> cmpPermission.getPermission().getGroups().add(item));
        cmpPermission.getSelectedGroups().clear();
        cmpPermission.getSelectedGroups().addAll(cmpPermission.getPermission().getGroups());
      
    }
    
    @SuppressWarnings("rawtypes")
    @Command
    public void chooseUsers(@ContextParam(ContextType.TRIGGER_EVENT) SelectEvent<?, Object> event,
            @BindingParam("selectedPermission") CompositionPermisson cmpPermission) {
        Iterator<Object> selectedObjItr = event.getSelectedObjects().iterator();
        String user = null;
        if (selectedObjItr.hasNext()) {
            user = selectedObjItr.next().toString();
        }
        if (user != null) {
            cmpPermission.getPermission().getUserIds().add(user);
            if (cmpPermission.getSelectedUsers() == null) {
                Collection<String> selUsers = new ArrayList<>();
                cmpPermission.setSelectedUsers(selUsers);
            }
            if (!cmpPermission.getSelectedUsers().contains(user)) {
                cmpPermission.getSelectedUsers().add(user);
            }
        }
        
        ((ListModelList) ((Listbox) event.getTarget()).getModel()).clearSelection();
        //Hiding the popup after selection
        Bandbox bandbox = (Bandbox) event.getTarget().getParent().getParent();
        bandbox.setOpen(false);
    }
    
    @Command
    public void onDeleteUser(@BindingParam("user") String user, @BindingParam("selectedPermission") CompositionPermisson cmpPermission) {
        cmpPermission.getSelectedUsers().remove(user);
        cmpPermission.getPermission().getUserIds().remove(user);

    }
  
    
    @NotifyChange("filteredUsers")
    @Command
    public void onChangingUsers(@ContextParam(ContextType.TRIGGER_EVENT) InputEvent event){
        Collection<String> users = new ArrayList<>();
        try {
            Collection<String> seachableUsers =CompositionUtil.getAllUsers();
            if(seachableUsers != null){
                users.addAll(seachableUsers.stream()
                        .filter(user -> StringUtils.containsIgnoreCase(user, event.getValue()))
                        .collect(Collectors.toList()));
                if (getFilteredUsers() != null) {
                    getFilteredUsers().clear();
                }
                setFilteredUsers(users);  
            }
        } catch (AuthenticationException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            setUserAccessError(Labels.getLabel("noUsersToShow"));
        }
    }
    
    private void removeCustompermissions(CompositionPermisson cmpPermission){
        if(cmpPermission.getSelectedGroups() != null && !cmpPermission.getSelectedGroups().isEmpty() ){
            cmpPermission.getSelectedGroups().clear(); 
        } 
        cmpPermission.getPermission().getUserIds().clear();
        
        if(cmpPermission.getSelectedUsers() != null && !cmpPermission.getSelectedUsers().isEmpty() ){
            cmpPermission.getSelectedUsers().clear();
        }
        cmpPermission.getPermission().getGroups().clear();
    }
    
    private void loadCustomPermission(Permission permission,CompositionPermisson cmpPermission){
        if(permission.getGroups()!= null && !permission.getGroups().isEmpty()){
            cmpPermission.setSelectedGroups(permission.getGroups()); 
        }
        
        if(permission.getUserIds() != null && !permission.getUserIds() .isEmpty()){
            cmpPermission.setSelectedUsers(permission.getUserIds());  
        }
    }
    
    private void checkRadioButtons(CompositionPermisson cmpPermission,boolean isPublic,boolean isPrivate, boolean isCustom){
        cmpPermission.setPrivateComposition(isPrivate);
        cmpPermission.setPublicComposition(isPublic);
        cmpPermission.setCustomComposition(isCustom);
    }

}

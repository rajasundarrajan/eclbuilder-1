package org.hpccsystems.dsp.ramps.entity;


import java.io.Serializable;
import java.util.Collection;
import org.hpcc.HIPIE.dude.Permission;

public class CompositionPermisson implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private Permission permission;
    private boolean customComposition;
    private boolean publicComposition;
    private boolean privateComposition;
    private Collection<String> selectedGroups;
    private Collection<String> selectedUsers;
   
    public Collection<String> getSelectedUsers() {
        return selectedUsers;
    }


    public void setSelectedUsers(Collection<String> selectedUsers) {
        this.selectedUsers = selectedUsers;
    }


    public void setSelectedGroups(Collection<String> selectedGroups) {
        this.selectedGroups = selectedGroups;
    }

    
    public Collection<String> getSelectedGroups() {
            return  selectedGroups;  
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public boolean isCustomComposition() {
        return customComposition;
    }

    public void setCustomComposition(boolean customComposition) {
        this.customComposition = customComposition;
    }

    public boolean isPublicComposition() {
        return publicComposition;
    }

    public void setPublicComposition(boolean publicComposition) {
        this.publicComposition = publicComposition;
    }

    public boolean isPrivateComposition() {
        return privateComposition;
    }

    public void setPrivateComposition(boolean privateComposition) {
        this.privateComposition = privateComposition;
    }

  }

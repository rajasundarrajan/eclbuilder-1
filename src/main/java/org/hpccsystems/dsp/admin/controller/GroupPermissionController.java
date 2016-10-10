package org.hpccsystems.dsp.admin.controller;

import java.util.Set;
import java.util.TreeSet;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.usergroupservice.Group;
import org.hpccsystems.usergroupservice.IUserGroupService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;


@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class GroupPermissionController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    @Wire
    private Grid groupPermissionGrid;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);     
        ListModelList<Group> model = new ListModelList<Group>();
        
        IUserGroupService ugsvc = HipieSingleton.getHipie().getPermissionsManager().getAuthManager();
        model.addAll(ugsvc.getAllGroups());
        groupPermissionGrid.setModel(model);
        groupPermissionGrid.setRowRenderer(new RowRenderer<Group>() {
            @Override
            public void render(Row row, Group mbsGroup, int index) throws Exception {
                row.appendChild(new Label(mbsGroup.getMachineName().toLowerCase()));
                Set<String> permissions=new TreeSet<String>();
                mbsGroup.getPermissions().stream().forEach(permission->permissions.add(permission.getCode().toLowerCase()));
                
                Listbox listbox = new Listbox();
                listbox.setNonselectableTags("*");
                ListModelList<String> modelList = new ListModelList<String>();
                modelList.addAll(permissions);
                listbox.setModel(modelList);
                row.appendChild(listbox);
            }
        });
    }
}

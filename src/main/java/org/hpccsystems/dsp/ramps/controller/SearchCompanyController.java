package org.hpccsystems.dsp.ramps.controller;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.ramps.entity.Company;
import org.hpccsystems.dsp.ramps.factory.CompanyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class SearchCompanyController extends SelectorComposer<Vlayout> {

    private static final String SEARCH_BOXRESULT = "searchBoxresult";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCompanyController.class);

    @Wire
    private Textbox companyName;
    @Wire
    private Intbox gcid;
    @Wire
    private Listbox searchResult;
    @Wire
    private Hlayout confirmHbox;
    @Wire
    private Textbox gcidTextbox;
    @Wire
    private Button confirmBtn;
    @Wire
    private Label notifyLabel;

    private ListModelList<Company> companyModel = new ListModelList<Company>();
    private Component parent;
    private static final String PARENT_EDIT_WIN_ID = "gcidEditWindow";
    private static final String PARENT_LAYOUT_WIN_ID = "layoutGcidEditWindow";
    private Company selectedCompany = null;
    private boolean showConfirmationBox = false;
    private boolean isLayoutParentWin;
    private boolean isEditParentWin;

    @Override
    public void doAfterCompose(Vlayout comp) throws Exception {
        super.doAfterCompose(comp);
        confirmBtn.setDisabled(true);
        searchResult.setModel(companyModel);
        searchResult.setItemRenderer(this::companyItemRender);

        parent = this.getSelf().getParent().getParent();

        if (PARENT_EDIT_WIN_ID.equals(parent.getId())) {
            isEditParentWin = true;
        } else if (PARENT_LAYOUT_WIN_ID.equals(parent.getId())) {
            isLayoutParentWin = true;
        }

        if (isEditParentWin || isLayoutParentWin) {
            showConfirmationBox = true;
        }

        if (showConfirmationBox) {
            confirmHbox.setVisible(true);
        }

        if (isEditParentWin) {
            notifyLabel.setVisible(true);
        }
    }

    private void companyItemRender(Listitem item, Object data, int index) {
        Company company = (Company) data;
        Label companygcidLab = new Label(String.valueOf(company.getGcId()));
        Listcell gcidComp = new Listcell();
        companygcidLab.setParent(gcidComp);
        gcidComp.setSclass(SEARCH_BOXRESULT);
        item.appendChild(gcidComp);
        Label companyNameLab = new Label(company.getName());
        Listcell nameComp = new Listcell();
        nameComp.appendChild(companyNameLab);
        nameComp.setSclass(SEARCH_BOXRESULT);
        item.appendChild(nameComp);
    }

    @Listen("onClick = #searchBtn")
    public void getMatchingCompany() {
        // Ignoring validation for stub users
        ServletContext servletContext = (ServletContext) Sessions.getCurrent().getWebApp().getServletContext();
        if (!(Constants.STUB.equalsIgnoreCase(servletContext.getInitParameter(Constants.USER_GROUP_SVS_TYPE)))
                && StringUtils.isEmpty(companyName.getValue()) && gcid.getValue() == null) {
            Clients.showNotification(Labels.getLabel("companyOrGcid"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER,
                    5000, true);
            return;
        }
        Company company = new Company();
        if (!StringUtils.isEmpty(companyName.getValue())) {
            company.setName(companyName.getValue());
        }
        company.setGcId(gcid.getValue());
        try {
            companyModel.clear();
            companyModel.addAll(CompanyFactory.getCompanyDao().getMatchingCompany(company));
        } catch (DatabaseException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
    }

    @Listen("onSelect = #searchResult")
    public void selectCompany() {
        confirmBtn.setDisabled(false);
        selectedCompany = companyModel.getSelection().iterator().next();
        if (showConfirmationBox) {
            gcidTextbox.setText(String.valueOf(selectedCompany.getGcId()));
        } else {
            Events.postEvent(Constants.EVENTS.ON_SELECT_COMPANY_ID, this.getSelf().getParent(), selectedCompany);
        }
    }

    @Listen("onClick = #confirmBtn")
    public void changeGCID() {
        if (!StringUtils.isEmpty(gcidTextbox.getValue())) {
            if (isLayoutParentWin) {
                Events.postEvent(Constants.EVENTS.ON_CONFIRM_GCID, parent.getParent(), selectedCompany);
                ((Window) parent).detach();
            } else {
                Events.postEvent(Constants.EVENTS.ON_CONFIRM_GCID, parent, selectedCompany);
            }
        }
    }
}

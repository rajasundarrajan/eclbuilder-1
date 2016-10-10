package org.hpccsystems.dsp.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.hpccsystems.dsp.dao.CompanyDao;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.ramps.entity.Company;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Sessions;

@Service("companyDao")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CompanyDaoImpl implements CompanyDao {

    private static final String STUB_COMPANY = "StubCompanies";
    private  List<Company> allCompany;
    
    @Override
    public List<Company> getMatchingCompany(Company company) throws DatabaseException {
        List<Company> filteredCompany  = new ArrayList<Company>();
        if(allCompany == null){
            ServletContext servletContext = (ServletContext) Sessions.getCurrent().getWebApp().getServletContext();
            String companyStr = servletContext.getInitParameter(STUB_COMPANY);
            String[] compArray = companyStr.split(",");
            List<String> compList = Arrays.asList(compArray);
            allCompany = new ArrayList<Company>();
           

            compList.forEach(item -> {
                String[] roles = item.split(":");
                allCompany.add(new Company(roles[1].trim(), Integer.valueOf(roles[0].trim())));
            });
        }

        // filter the companies based on search inputs
        if (!StringUtils.isEmpty(company.getName()) && company.getGcId() != null) {
            filteredCompany.addAll(allCompany
                    .stream()
                    .filter(comp -> StringUtils.containsIgnoreCase(comp.getName(), company.getName())
                            && StringUtils.containsIgnoreCase(String.valueOf(comp.getGcId()), String.valueOf(company.getGcId())))
                    .collect(Collectors.toList()));
        } else if (!StringUtils.isEmpty(company.getName())) {
            filteredCompany.addAll(allCompany.stream().filter(comp -> StringUtils.containsIgnoreCase(comp.getName(), company.getName()))
                    .collect(Collectors.toList()));
        } else if(company.getGcId() != null){
            filteredCompany.addAll(allCompany.stream()
                    .filter(comp -> StringUtils.containsIgnoreCase(String.valueOf(comp.getGcId()), String.valueOf(company.getGcId())))
                    .collect(Collectors.toList()));
        }else{
            filteredCompany.addAll(allCompany);
        }
        
        if (filteredCompany.isEmpty()) {
            throw new DatabaseException(Labels.getLabel("companyIdsNotAvailable"));
        }

        return filteredCompany;
    }

    @Override
    public Map<String, String> getGCIDComplianceValues(String gcid,Collection<String> complianceTags) throws DatabaseException {
        return null;
        
    }
}

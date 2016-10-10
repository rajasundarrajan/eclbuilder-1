package org.hpccsystems.dsp.ramps.factory;

import javax.servlet.ServletContext;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.CompanyDao;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zkplus.spring.SpringUtil;

public class CompanyFactory {
    
    private static CompanyDao companyDao;
    
    private CompanyFactory() {
        super();
    }
    static{
         ServletContext servletContext = (ServletContext) Sessions.getCurrent().getWebApp().getServletContext();
        if(Constants.STUB.equalsIgnoreCase(servletContext.getInitParameter(Constants.USER_GROUP_SVS_TYPE))) {
            companyDao=(CompanyDao)SpringUtil.getBean("companyDao");
        } else {
            companyDao=(CompanyDao)SpringUtil.getBean("mbsCompanyDao");
        }       
    }
    public static CompanyDao getCompanyDao(){
        return companyDao;
    }

}

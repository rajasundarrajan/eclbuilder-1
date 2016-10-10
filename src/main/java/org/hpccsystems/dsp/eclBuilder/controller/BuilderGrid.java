package org.hpccsystems.dsp.eclBuilder.controller;

import java.util.List;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.zkoss.zkplus.spring.SpringUtil;


public class BuilderGrid {
	
	   private List<Builder> builders;

		public List<Builder> getBuilders(){
	    	try {
				builders = ((DSPDao)SpringUtil.getBean("dspDao")).getECLBuilders(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId());
			} catch (DatabaseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        return builders;

		}
}

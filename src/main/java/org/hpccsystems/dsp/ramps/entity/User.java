package org.hpccsystems.dsp.ramps.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Perspective;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.service.UserService;
import org.hpccsystems.usergroupservice.Group;
import org.hpccsystems.usergroupservice.Permission;
import org.zkoss.zkplus.spring.SpringUtil;

public class User implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String password;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}

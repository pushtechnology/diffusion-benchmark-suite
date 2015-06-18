package com.pushtechnology.benchmarks.control.clients;

import static com.pushtechnology.benchmarks.util.PropertiesUtil.getProperty;

import java.util.Properties;

import com.pushtechnology.benchmarks.experiments.CommonExperimentSettings;

public class ControlClientSettings extends CommonExperimentSettings{

	private String principal;
	private String password;

	public ControlClientSettings(Properties settings) {
        super(settings);
        principal =
            getProperty(settings, "principal", "admin");
        password =
                getProperty(settings, "password", "password");
	}


	public String getPrincipal() {
		return principal;
	}

	public String getPassword() {
		return password;
	}
	
}

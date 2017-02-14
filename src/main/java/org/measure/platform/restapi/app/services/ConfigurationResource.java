package org.measure.platform.restapi.app.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "api/configuration")
public class ConfigurationResource {
	
	@Value("${measure.kibana.adress}")
	private String kibanaAdress;
	
	@RequestMapping(value = "/kibana-adress", method = RequestMethod.GET)
	public String getKibanaAdress() {		
		return kibanaAdress;
	}

}

package org.measure.platform.restapi.app.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;

import org.measure.platform.core.api.IRemoteCatalogueService;
import org.measure.platform.restapi.app.services.dto.MeasureAgent;
import org.measure.smm.measure.model.SMMMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;


@RestController
@RequestMapping(value = "api/remote-measure")
public class RemoteRegistrationResource {	
	
    private final Logger log = LoggerFactory.getLogger(RemoteRegistrationResource.class);

    
	@Inject
	IRemoteCatalogueService remoteCatalogue;
	
	@PutMapping("/registration")
    @Timed
    public void registerMeasure(@Valid @RequestBody SMMMeasure measureDefinition)  {
		log.info("Register Measure[" + measureDefinition.getCallbackLable() + "] :" + measureDefinition.getName());
		this.remoteCatalogue.registerRemoteMeasure(measureDefinition);
    }
	
	
    /**
     * GET  /agents : get all the agents.
     */
    @GetMapping("/agent-list")
    @Timed
    public List<MeasureAgent> getAllAgents() {
    	Map<String,MeasureAgent> map = new HashMap<>();
    	for(SMMMeasure measure : this.remoteCatalogue.getAllMeasures()){
    		
        	
    		MeasureAgent agent =  map.get(measure.getCallbackAdress());		
    		if(agent == null){
    			agent = new MeasureAgent();
    			agent.setAgentName(measure.getCallbackLable());
    			agent.setAgentAdress(measure.getCallbackAdress());
    			map.put(measure.getCallbackAdress(), agent);
    		}		
    		agent.getProvidedMeasures().add(measure);
    	}
    	List<MeasureAgent> agents = new ArrayList<>(map.values());
    	agents.sort(new Comparator<MeasureAgent>() {
			@Override
			public int compare(MeasureAgent o1, MeasureAgent o2) {
				return o1.getAgentName().compareTo(o2.getAgentName());
			}
		});
    	
        return  agents;
    }
}
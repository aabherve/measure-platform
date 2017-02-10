package org.measure.platform.core.impl;

import java.util.ArrayList;
import java.util.List;

import org.measure.platform.core.api.IRemoteCatalogueService;
import org.measure.smm.measure.model.SMMMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(value = "singleton")
public class RemoteCatalogueService implements IRemoteCatalogueService {

	private final Logger log = LoggerFactory.getLogger(RemoteCatalogueService.class);

	private List<SMMMeasure> registredMeasures = new ArrayList<>();

	@Override
	public void registerRemoteMeasure(SMMMeasure remoteMeasure) {	
		for(SMMMeasure measure : registredMeasures){
			if(measure.getCallbackAdress().equals(remoteMeasure.getCallbackAdress()) && measure.getName().equals(remoteMeasure.getName()) ){
				return;
			}
		}
		this.registredMeasures.add(remoteMeasure);
		log.info("Register Remote Measure \"" +remoteMeasure.getName()+ "\" form " + remoteMeasure.getCallbackLable());
	}

	@Override
	public List<SMMMeasure> getAllMeasures() {
		return this.registredMeasures;
	}

	@Override
	public void unregisterRemoteMeasure(String measureName, String callbackAdress) {
		for(SMMMeasure measure : new ArrayList<>(this.registredMeasures)){
			if(measure.getCallbackAdress().equals(callbackAdress) && measure.getName().equals(measureName)){
				this.registredMeasures.remove(measure);
			}
		}
	}

}

package org.measure.platform.core.api;

import java.util.List;

import org.measure.smm.measure.model.SMMMeasure;

public interface IRemoteCatalogueService {
	
	public void registerRemoteMeasure(SMMMeasure remoteMeasure);
	public void unregisterRemoteMeasure(String measureName,String callbackAdress);	
	public List<SMMMeasure> getAllMeasures();

}

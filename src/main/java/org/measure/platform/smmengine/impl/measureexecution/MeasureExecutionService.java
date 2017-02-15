package org.measure.platform.smmengine.impl.measureexecution;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.measure.platform.core.api.IMeasureCatalogueService;
import org.measure.platform.core.api.entitys.MeasureInstanceService;
import org.measure.platform.core.api.entitys.MeasurePropertyService;
import org.measure.platform.core.api.entitys.MeasureReferenceService;
import org.measure.platform.core.entity.MeasureInstance;
import org.measure.platform.core.entity.MeasureProperty;
import org.measure.platform.core.entity.MeasureReference;
import org.measure.platform.measurementstorage.api.IMeasurementStorage;
import org.measure.platform.smmengine.api.ILoggerService;
import org.measure.platform.smmengine.api.IMeasureExecutionService;
import org.measure.smm.log.MeasureLog;
import org.measure.smm.measure.api.IDerivedMeasure;
import org.measure.smm.measure.api.IDirectMeasure;
import org.measure.smm.measure.api.IMeasure;
import org.measure.smm.measure.api.IMeasurement;
import org.measure.smm.remote.RemoteExecutionResult;
import org.measure.smm.remote.RemoteMeasureInstanceData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MeasureExecutionService implements IMeasureExecutionService {

	@Inject
	private IMeasureCatalogueService measureCatalogue;

	@Inject
	private MeasureInstanceService measureInstanceService;

	@Inject
	private MeasurePropertyService measurePropertyService;

	@Inject
	private MeasureReferenceService measureReferenceService;

	@Inject
	private IMeasurementStorage measurementStorage;

	@Override
	public MeasureLog executeMeasure(MeasureInstance measureData, IMeasure measureImpl) {

		MeasureLog log = new MeasureLog();
		log.setExectionDate(new Date());
		log.setMeasureInstanceName(measureData.getInstanceName());
		log.setMeasureName(measureData.getMeasureName());

		try {

			List<IMeasurement> measurements = new ArrayList<>();
			if (measureData.isIsRemote()) {
				measurements.addAll(executeRemoteMeasure(measureData, log,true));
			} else {
				measurements.addAll(executeLocalMeasure(measureData, measureImpl, log,true));
			}

			for (IMeasurement measurement : measurements) {
				measurementStorage.putMeasurement(measureData.getInstanceName(), measureData.isManageLastMeasurement(),
						measurement);
			}
		} catch (Exception e) {
			log.setExceptionMessage(e.getMessage());
			log.setSuccess(false);
		}

		return log;
	}

	@Override
	public MeasureLog executeMeasure(Long measureInstanceId) {
		MeasureInstance measureData = measureInstanceService.findOne(measureInstanceId);

		MeasureLog log = new MeasureLog();
		log.setExectionDate(new Date());
		log.setMeasureInstanceName(measureData.getInstanceName());
		log.setMeasureName(measureData.getMeasureName());

		try {

			List<IMeasurement> measurements = new ArrayList<>();
			if (measureData.isIsRemote()) {
				measurements.addAll(executeRemoteMeasure(measureData, log,true));
			} else {
				IMeasure measureImpl = measureCatalogue.getMeasureImplementation(measureData.getMeasureName());
				measurements.addAll(executeLocalMeasure(measureData, measureImpl, log,true));
			}

			for (IMeasurement measurement : measurements) {
				measurementStorage.putMeasurement(measureData.getInstanceName(), measureData.isManageLastMeasurement(),
						measurement);
			}
		} catch (Exception e) {
			log.setExceptionMessage(e.getMessage());
			log.setSuccess(false);
		}
		
		return log;
	}

	@Override
	public MeasureLog testMeasure(Long measureInstanceId) {
		MeasureInstance measureData = measureInstanceService.findOne(measureInstanceId);
		MeasureLog log = new MeasureLog();

		log.setExectionDate(new Date());
		log.setMeasureInstanceName(measureData.getInstanceName());
		log.setMeasureName(measureData.getMeasureName());

		if (measureData.isIsRemote()) {
			executeRemoteMeasure(measureData, log,false);
		} else {
			IMeasure measureImpl = measureCatalogue.getMeasureImplementation(measureData.getMeasureName());
			executeLocalMeasure(measureData, measureImpl, log,false);
		}
		return log;
	}

	private List<IMeasurement> executeRemoteMeasure(MeasureInstance measure, MeasureLog log,boolean storeProp) {
		RestTemplate restTemplate = new RestTemplate();
		try {

			String url = "http://" + measure.getRemoteAdress() + "/api/measure-agent/measure-execution";

			RemoteMeasureInstanceData data = new RemoteMeasureInstanceData();
			data.setInstanceName(measure.getInstanceName());
			data.setMeasureName(measure.getMeasureName());

			Map<String, String> properties = initialiseProperties(measure, null);
			data.setProperties(properties);

			RemoteExecutionResult result = restTemplate.postForObject(url, data, RemoteExecutionResult.class);

			if (result != null) {
				if (storeProp)
					storeUpdatedProperties(measure,result.getUpdatedProperties());
				
				log.setLog(result.getExecutionLog());
				return log.getMesurement();
			} else {
				log.setSuccess(false);
				log.setExceptionMessage("No Result");
			}
		} catch (Exception e) {
			log.setSuccess(false);
			log.setExceptionMessage(e.getMessage());
		}

		return new ArrayList<>();
	}

	private List<IMeasurement> executeLocalMeasure(MeasureInstance measure, IMeasure measureImpl, MeasureLog log,boolean storeProp) {
		try {

			Map<String,String> properties = initialiseProperties(measure, log);
			for (Entry<String, String> entry : properties.entrySet()) {
				measureImpl.getProperties().put(entry.getKey(), entry.getValue());
			}

			Date start = new Date();
			List<IMeasurement> measurements = new ArrayList<>();
			if (measureImpl instanceof IDirectMeasure) {		
				measurements.addAll(executeDirectMeasure((IDirectMeasure) measureImpl));
			} else if (measureImpl instanceof IDerivedMeasure) {

				List<MeasureReference> references = new ArrayList<>();
				for (MeasureReference reference : measureReferenceService.findByInstance(measure)) {
					references.add(reference);
				}
				measurements.addAll(executeDerivedMeasure((IDerivedMeasure) measureImpl, references, log));	
			}
			
			if(storeProp){
				Map<String,String> updatedProperties = new HashMap<>();
				for(Entry<String,String> entry : measureImpl.getProperties().entrySet()){
					if(!entry.getValue().equals(properties.get(entry.getKey()))){
						updatedProperties.put(entry.getKey(), entry.getValue());
					}
				}
				storeUpdatedProperties(measure,updatedProperties);
			}
			
			log.setExecutionTime(new Date().getTime() - start.getTime());
			log.setMesurement(measurements);
			log.setSuccess(true);
			
			return measurements;
			
		} catch (Exception e) {
			log.setSuccess(false);
			log.setExceptionMessage(e.getMessage());
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	private List<IMeasurement> executeDirectMeasure(IDirectMeasure directMeasure) throws Exception {
		return directMeasure.getMeasurement();
	}

	private List<IMeasurement> executeDerivedMeasure(IDerivedMeasure derivedMeasure, List<MeasureReference> references,
			MeasureLog log) throws Exception {
		for (MeasureReference ref : references) {
			List<IMeasurement> measurements = measurementStorage.getMeasurement(ref.getReferencedInstance().getInstanceName(), ref.getNumberRef(), ref.getFilterExpression());
			for (IMeasurement measurement : measurements) {
				derivedMeasure.addMeasureInput(ref.getReferencedInstance().getInstanceName(), ref.getRole(),measurement);
				log.getInputs().add(log.new MeasureTestInput(ref.getRole(), measurement));
			}
		}

		// Execute Measure
		return derivedMeasure.calculateMeasurement();
	}

	private HashMap<String, String> initialiseProperties(MeasureInstance measureData, MeasureLog log) {
		HashMap<String, String> properties = new HashMap<>();
		for (MeasureProperty property : measurePropertyService.findByInstance(measureData)) {
			properties.put(property.getPropertyName(), property.getPropertyValue());
			if (log != null) {
				log.getParameters()
						.add(log.new MeasureTestParameters(property.getPropertyName(), property.getPropertyValue()));
			}
		}
		return properties;
	}

	private void storeUpdatedProperties(MeasureInstance measureData, Map<String, String> updatedProperties) {
		for (MeasureProperty property : new ArrayList<>(measurePropertyService.findByInstance(measureData))) {
			if (updatedProperties.containsKey(property.getPropertyName())) {
				property.setPropertyValue(updatedProperties.get(property.getPropertyName()));
				measurePropertyService.save(property);
			}
		}
	}
}

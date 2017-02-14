package org.measure.platform.smmengine.impl.measureexecution;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			if (measureData.isIsRemote()) {
				Map<String, String> updatedProperties = new HashMap<>();
				List<IMeasurement> measurements = executeRemoteMeasure(measureData, updatedProperties, log);
				storeUpdatedProperties(measureData, updatedProperties);
				for (IMeasurement measurement : measurements) {
					measurementStorage.putMeasurement(measureData.getInstanceName(),
							measureData.isManageLastMeasurement(), measurement);
				}

			} else {
				List<IMeasurement> measurements = executeLocalMeasure(measureData, measureImpl, log);
				for (IMeasurement measurement : measurements) {
					measurementStorage.putMeasurement(measureData.getInstanceName(),
							measureData.isManageLastMeasurement(), measurement);
				}
				storeUpdatedProperties(measureData, measureImpl.getUpdatedProperties());

			}
		} catch (Exception e) {

			log.setExceptionMessage(e.getMessage());
			log.setSuccess(false);
		}

		return log;
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
			String updatedValue = updatedProperties.get(property.getPropertyName());
			if (updatedValue != null) {
				property.setPropertyValue(updatedValue);
				measurePropertyService.save(property);
			}
		}

	}

	@Override
	public MeasureLog executeMeasure(Long measureInstanceId) {
		MeasureInstance measureData = measureInstanceService.findOne(measureInstanceId);
		MeasureLog log = new MeasureLog();

		log.setExectionDate(new Date());
		log.setMeasureInstanceName(measureData.getInstanceName());
		log.setMeasureName(measureData.getMeasureName());

		try {

			if (measureData.isIsRemote()) {
				Map<String, String> updatedProperties = new HashMap<>();
				List<IMeasurement> measurements = executeRemoteMeasure(measureData,updatedProperties, log);
				storeUpdatedProperties(measureData, updatedProperties);
				for (IMeasurement measurement : measurements) {
					measurementStorage.putMeasurement(measureData.getInstanceName(),
							measureData.isManageLastMeasurement(), measurement);
				}
			} else {
				IMeasure measureImpl = measureCatalogue.getMeasureImplementation(measureData.getMeasureName());
				List<IMeasurement> measurements = executeLocalMeasure(measureData, measureImpl, log);
				storeUpdatedProperties(measureData, measureImpl.getUpdatedProperties());

				for (IMeasurement measurement : measurements) {
					measurementStorage.putMeasurement(measureData.getInstanceName(),
							measureData.isManageLastMeasurement(), measurement);
				}
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
			executeRemoteMeasure(measureData, new HashMap<String, String>(), log);
		} else {
			IMeasure measureImpl = measureCatalogue.getMeasureImplementation(measureData.getMeasureName());
			executeLocalMeasure(measureData, measureImpl, log);
		}
		return log;
	}

	private List<IMeasurement> executeRemoteMeasure(MeasureInstance measure, Map<String, String> updatedProperties,
			MeasureLog log) {
		RestTemplate restTemplate = new RestTemplate();
		try {

			String url = "http://" + measure.getRemoteAdress() + "/api/measure-agent/measure-execution";

			RemoteMeasureInstanceData data = new RemoteMeasureInstanceData();
			data.setInstanceName(measure.getInstanceName());
			data.setMeasureName(measure.getMeasureName());
			data.setProperties(initialiseProperties(measure, null));

			RemoteExecutionResult result = restTemplate.postForObject(url, data, RemoteExecutionResult.class);

			if (result != null) {
				updatedProperties.putAll(result.getUpdatedProperties());
				log.setLog(result.getExecutionLog());
				return log.getMesurement();
			} else {
				log.setSuccess(false);
				log.setExceptionMessage("No Result");
			}
		} catch (Exception e) {

			log.setSuccess(false);
			log.setExceptionMessage(e.getMessage());
			System.out.println("Log1 " + log.getExceptionMessage());
			e.printStackTrace();
		}

		return new ArrayList<>();
	}

	private List<IMeasurement> executeLocalMeasure(MeasureInstance measure, IMeasure measureImpl, MeasureLog log) {
		try {
			measureImpl.setProperties(initialiseProperties(measure, log));

			if (measureImpl instanceof IDirectMeasure) {
				Date start = new Date();
				List<IMeasurement> measurements = executeDirectMeasure((IDirectMeasure) measureImpl);
				log.setExecutionTime(new Date().getTime() - start.getTime());
				log.setMesurement(measurements);
				log.setSuccess(true);
				return measurements;
			} else if (measureImpl instanceof IDerivedMeasure) {

				List<MeasureReference> references = new ArrayList<>();
				for (MeasureReference reference : measureReferenceService.findByInstance(measure)) {
					references.add(reference);
				}

				Date start = new Date();
				List<IMeasurement> measurements = executeDerivedMeasure((IDerivedMeasure) measureImpl, references, log);
				log.setExecutionTime(new Date().getTime() - start.getTime());
				log.setMesurement(measurements);
				log.setSuccess(true);
				return measurements;
			}
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
		// Get Input Measurements
		for (MeasureReference ref : references) {
			System.out.println(ref.getFilterExpression());
			List<IMeasurement> measurements = measurementStorage.getMeasurement(
					ref.getReferencedInstance().getInstanceName(), ref.getNumberRef(), ref.getFilterExpression());
			for (IMeasurement measurement : measurements) {
				derivedMeasure.addMeasureInput(ref.getReferencedInstance().getInstanceName(), ref.getRole(),
						measurement);
				log.getInputs().add(log.new MeasureTestInput(ref.getRole(), measurement));
			}
		}

		// Execute Measure
		return derivedMeasure.calculateMeasurement();
	}

}

package org.measure.platform.smmengine.impl.sheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.measure.platform.core.api.IMeasureCatalogueService;
import org.measure.platform.core.entity.MeasureInstance;
import org.measure.platform.measurementstorage.api.IMeasurementStorage;
import org.measure.platform.smmengine.api.ILoggerService;
import org.measure.platform.smmengine.api.IShedulingService;
import org.measure.platform.smmengine.impl.measureexecution.MeasureExecutionService;
import org.measure.smm.log.MeasureLog;
import org.measure.smm.measure.api.IMeasure;
import org.measure.smm.measure.api.IMeasurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class SchedulingService implements IShedulingService {

	@Autowired
	private TaskScheduler taskScheduler;

	@Inject
	private MeasureExecutionService measureExecutionService;

	@Inject
	private IMeasurementStorage measurementStorage;

	@Inject
	private IMeasureCatalogueService measureCatalogue;

	@Inject
	private ILoggerService logger;

	private Map<Long, ScheduledFuture> jobs;

	@PostConstruct
	public void doSomething() {
		this.jobs = new HashMap<>();
	}

	@Override
	public synchronized Boolean scheduleMeasure(MeasureInstance measure) {
		if (measure.isIsShedule() != null && measure.isIsShedule() && measure.getShedulingExpression() != null
				&& measure.getShedulingExpression().matches("\\d+")) {
			Integer rate = Integer.valueOf(measure.getShedulingExpression());

			IMeasure measureImpl = measureCatalogue.getMeasureImplementation(measure.getMeasureName());

			ScheduledFuture job = taskScheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {

					MeasureLog log = measureExecutionService.executeMeasure(measure, measureImpl);
					logger.addMeasureExecutionLog(log);

					if (!log.isSuccess()) {
						removeMeasure(measure.getId());
					}

				}
			}, rate);
			this.jobs.put(measure.getId(), job);
			return true;
		}

		return false;
	}

	@Override
	public synchronized Boolean removeMeasure(Long measureInstanceId) {
		ScheduledFuture job = jobs.get(measureInstanceId);
		if (job != null) {
			job.cancel(true);
			this.jobs.remove(measureInstanceId);
		}
		return true;
	}

	@Override
	public synchronized Boolean isShedule(Long measureInstanceId) {
		return jobs.containsKey(measureInstanceId);
	}

}
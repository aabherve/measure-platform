package org.measure.platform.smmengine.api;

import org.measure.platform.core.entity.MeasureInstance;

public interface IShedulingService {

	Boolean scheduleMeasure(MeasureInstance measure);

	Boolean removeMeasure(Long measureInstanceId);

	Boolean isShedule(Long measureInstanceId);

}

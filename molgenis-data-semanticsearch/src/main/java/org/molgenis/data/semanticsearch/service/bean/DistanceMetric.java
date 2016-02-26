package org.molgenis.data.semanticsearch.service.bean;

import static org.molgenis.data.semanticsearch.string.AttributeToMapUtil.attributeToMap;

import java.util.Map;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_DistanceMetric.class)
public abstract class DistanceMetric
{
	public static DistanceMetric create(AttributeMetaData attrOne, AttributeMetaData attrTwo, boolean valid,
			double logDistance)
	{
		return new AutoValue_DistanceMetric(attributeToMap(attrOne), attributeToMap(attrTwo), valid, logDistance);
	}

	public abstract Map<String, Object> getAttrOne();

	public abstract Map<String, Object> getAttrTwo();

	public abstract boolean isValid();

	public abstract double getLogDistance();
}

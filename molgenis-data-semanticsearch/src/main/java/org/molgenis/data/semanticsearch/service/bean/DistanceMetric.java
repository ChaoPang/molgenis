package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.data.AttributeMetaData;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DistanceMetric implements Comparable<DistanceMetric>
{
	public static DistanceMetric create(AttributeMetaData targetAttribute, AttributeMetaData sourceAttribute,
			boolean valid, double logDistance)
	{
		return new AutoValue_DistanceMetric(targetAttribute, sourceAttribute, valid, logDistance);
	}

	public abstract AttributeMetaData getTargetAttribute();

	public abstract AttributeMetaData getSourceAttribute();

	public abstract boolean isValid();

	public abstract double getLogDistance();

	public int compareTo(DistanceMetric o)
	{
		return Double.compare(getLogDistance(), o.getLogDistance());
	}
}

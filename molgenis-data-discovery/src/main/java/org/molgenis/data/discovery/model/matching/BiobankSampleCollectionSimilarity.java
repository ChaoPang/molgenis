package org.molgenis.data.discovery.model.matching;

import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankSampleCollectionSimilarity.class)
public abstract class BiobankSampleCollectionSimilarity implements Comparable<BiobankSampleCollectionSimilarity>
{
	public abstract BiobankSampleCollection getTarget();

	public abstract BiobankSampleCollection getSource();

	public abstract float getSimilarity();

	public static BiobankSampleCollectionSimilarity create(BiobankSampleCollection target,
			BiobankSampleCollection source, float similarity)
	{
		return new AutoValue_BiobankSampleCollectionSimilarity(target, source, similarity);
	}

	public int compareTo(BiobankSampleCollectionSimilarity o)
	{
		int compareTo = getTarget().getName().compareTo(o.getTarget().getName());
		if (compareTo == 0)
		{
			compareTo = getSource().getName().compareTo(o.getSource().getName());
		}
		return compareTo;
	}
}

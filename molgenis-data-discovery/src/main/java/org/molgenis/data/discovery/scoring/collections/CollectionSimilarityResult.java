package org.molgenis.data.discovery.scoring.collections;

import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_CollectionSimilarityResult.class)
public abstract class CollectionSimilarityResult implements Comparable<CollectionSimilarityResult>
{
	public abstract BiobankSampleCollection getTarget();

	public abstract BiobankSampleCollection getSource();

	public abstract float getSimilarity();

	public abstract int getCoverage();

	public static CollectionSimilarityResult create(BiobankSampleCollection target, BiobankSampleCollection source,
			float similarity, int coverage)
	{
		return new AutoValue_CollectionSimilarityResult(target, source, similarity, coverage);
	}

	public int compareTo(CollectionSimilarityResult o)
	{
		int compareTo = getTarget().getName().compareTo(o.getTarget().getName());
		if (compareTo == 0)
		{
			compareTo = getSource().getName().compareTo(o.getSource().getName());
		}
		return compareTo;
	}
}

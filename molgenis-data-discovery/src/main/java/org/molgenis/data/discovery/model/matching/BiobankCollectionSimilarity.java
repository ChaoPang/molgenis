package org.molgenis.data.discovery.model.matching;

import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankCollectionSimilarity.class)
public abstract class BiobankCollectionSimilarity
{
	public abstract String getIdentifier();

	public abstract BiobankSampleCollection getTarget();

	public abstract BiobankSampleCollection getSource();

	public abstract double getSimilarity();

	public abstract BiobankUniverse getBiobankUniverse();

	public static BiobankCollectionSimilarity create(String identifier, BiobankSampleCollection target,
			BiobankSampleCollection source, double similarity, BiobankUniverse biobankUniverse)
	{
		return new AutoValue_BiobankCollectionSimilarity(identifier, target, source, similarity, biobankUniverse);
	}
}

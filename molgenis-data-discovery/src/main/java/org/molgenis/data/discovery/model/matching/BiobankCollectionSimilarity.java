package org.molgenis.data.discovery.model.matching;

import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankCollectionSimilarity.class)
public abstract class BiobankCollectionSimilarity
{
	public enum SimilarityOption
	{
		SEMANTIC("Semantic"), AVERAGE("Average"), CURATED("Curated"), GENERATED("Generated");

		private String label;

		SimilarityOption(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	public abstract String getIdentifier();

	public abstract BiobankSampleCollection getTarget();

	public abstract BiobankSampleCollection getSource();

	public abstract double getSimilarity();

	public abstract int getCoverage();

	public abstract BiobankUniverse getBiobankUniverse();

	public abstract SimilarityOption getSimilarityOption();

	public static BiobankCollectionSimilarity create(String identifier, BiobankSampleCollection target,
			BiobankSampleCollection source, double similarity, int coverage, BiobankUniverse biobankUniverse,
			SimilarityOption similarityOption)
	{
		return new AutoValue_BiobankCollectionSimilarity(identifier, target, source, similarity, coverage,
				biobankUniverse, similarityOption);
	}
}

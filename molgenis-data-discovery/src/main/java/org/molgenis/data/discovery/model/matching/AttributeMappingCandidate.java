package org.molgenis.data.discovery.model.matching;

import static java.util.Collections.emptyList;

import java.util.List;

import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeMappingCandidate.class)
public abstract class AttributeMappingCandidate implements Comparable<AttributeMappingCandidate>
{
	public abstract String getIdentifier();

	public abstract BiobankUniverse getBiobankUniverse();

	public abstract BiobankSampleAttribute getTarget();

	public abstract BiobankSampleAttribute getSource();

	public abstract MatchingExplanation getExplanation();

	public abstract List<AttributeMappingDecision> getDecisions();

	public static AttributeMappingCandidate create(String identifier, BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, BiobankSampleAttribute source, MatchingExplanation explanation)
	{
		return new AutoValue_AttributeMappingCandidate(identifier, biobankUniverse, target, source, explanation,
				emptyList());
	}

	public static AttributeMappingCandidate create(String identifier, BiobankUniverse biobankUniverse,
			BiobankSampleAttribute target, BiobankSampleAttribute source, MatchingExplanation explanation,
			List<AttributeMappingDecision> decisions)
	{
		return new AutoValue_AttributeMappingCandidate(identifier, biobankUniverse, target, source, explanation,
				decisions);
	}

	@Override
	public int compareTo(AttributeMappingCandidate o2)
	{
		return Double.compare(o2.getExplanation().getNgramScore(), getExplanation().getNgramScore());
	}
}

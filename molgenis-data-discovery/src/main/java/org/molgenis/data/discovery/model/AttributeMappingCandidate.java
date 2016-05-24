package org.molgenis.data.discovery.model;

import java.util.List;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeMappingCandidate.class)
public abstract class AttributeMappingCandidate
{
	public abstract String getIdentifier();

	public abstract BiobankSampleAttribute getTarget();

	public abstract BiobankSampleAttribute getSource();

	public abstract MatchingExplanation getExplanation();

	public abstract List<AttributeMappingDecision> getDecisions();

	public static AttributeMappingCandidate create(String identifier, BiobankSampleAttribute target,
			BiobankSampleAttribute source, MatchingExplanation explanation, List<AttributeMappingDecision> decisions)
	{
		return new AutoValue_AttributeMappingCandidate(identifier, target, source, explanation, decisions);
	}
}

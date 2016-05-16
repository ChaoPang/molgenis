package org.molgenis.data.discovery.model;

import java.util.List;

import org.molgenis.data.EntityMetaData;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeMappingCandidate.class)
public abstract class AttributeMappingCandidate
{
	public abstract String getIdentifier();

	public abstract String getTarget();

	public abstract String getSource();

	public abstract EntityMetaData getTargetEntity();

	public abstract EntityMetaData getSourceEntity();

	public abstract MappingExplanation getExplanation();

	public abstract List<AttributeMappingDecision> getDecisions();

	public static AttributeMappingCandidate create(String identifier, String target, String source,
			EntityMetaData targetEntity, EntityMetaData sourceEntity, MappingExplanation explanation,
			List<AttributeMappingDecision> decisions)
	{
		return new AutoValue_AttributeMappingCandidate(identifier, target, source, targetEntity, sourceEntity,
				explanation, decisions);
	}
}

package org.molgenis.data.discovery.model.matching;

import com.google.auto.value.AutoValue;
import org.molgenis.data.discovery.meta.matching.AttributeMappingDecisionMetaData.DecisionOptions;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.gson.AutoGson;

import javax.annotation.Nullable;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeMappingDecision.class)
public abstract class AttributeMappingDecision
{
	public abstract String getIdentifier();

	public abstract DecisionOptions getDecision();

	@Nullable
	public abstract String getComment();

	public abstract String getOwner();

	public abstract BiobankUniverse getBiobankUniverse();

	public static AttributeMappingDecision create(String identifier, DecisionOptions decision, String comment,
			String owner, BiobankUniverse biobankUniverse)
	{
		return new AutoValue_AttributeMappingDecision(identifier, decision, comment, owner, biobankUniverse);
	}
}

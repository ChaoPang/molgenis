package org.molgenis.data.discovery.model;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.discovery.meta.AttributeMappingDecisionMetaData.DecisionOptions;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeMappingDecision.class)
public abstract class AttributeMappingDecision
{
	public abstract String getIdentifier();

	public abstract DecisionOptions getDecision();

	public abstract String getComment();

	public abstract MolgenisUser getOwner();

	public static AttributeMappingDecision create(String identifier, DecisionOptions decision, String comment,
			MolgenisUser owner)
	{
		return new AutoValue_AttributeMappingDecision(identifier, decision, comment, owner);
	}
}

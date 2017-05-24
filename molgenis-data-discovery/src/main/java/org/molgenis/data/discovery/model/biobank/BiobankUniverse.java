package org.molgenis.data.discovery.model.biobank;

import com.google.auto.value.AutoValue;
import org.molgenis.auth.User;
import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.SemanticType;

import java.util.List;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankUniverse.class)
public abstract class BiobankUniverse
{
	public abstract String getIdentifier();

	public abstract String getName();

	public abstract List<BiobankSampleCollection> getMembers();

	public abstract User getOwner();

	public abstract List<SemanticType> getKeyConcepts();

	public abstract List<BiobankUniverseMemberVector> getVectors();

	public static BiobankUniverse create(String identifier, String name, List<BiobankSampleCollection> members,
			User owner, List<SemanticType> keyConcepts, List<BiobankUniverseMemberVector> vectors)
	{
		return new AutoValue_BiobankUniverse(identifier, name, members, owner, keyConcepts, vectors);
	}
}

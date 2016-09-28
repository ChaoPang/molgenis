package org.molgenis.data.discovery.model.biobank;

import java.util.List;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.SemanticType;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankUniverse.class)
public abstract class BiobankUniverse
{
	public abstract String getIdentifier();

	public abstract String getName();

	public abstract List<BiobankSampleCollection> getMembers();

	public abstract MolgenisUser getOwner();

	public abstract List<SemanticType> getKeyConcepts();

	public abstract List<BiobankUniverseMemberVector> getVectors();

	public static BiobankUniverse create(String identifier, String name, List<BiobankSampleCollection> members,
			MolgenisUser owner, List<SemanticType> keyConcepts, List<BiobankUniverseMemberVector> vectors)
	{
		return new AutoValue_BiobankUniverse(identifier, name, members, owner, keyConcepts, vectors);
	}
}

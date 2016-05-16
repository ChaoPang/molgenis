package org.molgenis.data.discovery.model;

import java.util.List;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.EntityMetaData;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankUniverse.class)
public abstract class BiobankUniverse
{
	public abstract String getIdentifier();

	public abstract String getName();

	public abstract List<EntityMetaData> getMembers();

	public abstract MolgenisUser getOwner();

	public static BiobankUniverse create(String identifier, String name, List<EntityMetaData> members,
			MolgenisUser owner)
	{
		return new AutoValue_BiobankUniverse(identifier, name, members, owner);
	}
}

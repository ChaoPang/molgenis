package org.molgenis.data.discovery.model.biobank;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankSampleCollection.class)
public abstract class BiobankSampleCollection
{
	public abstract String getName();

	public static BiobankSampleCollection create(String name)
	{
		return new AutoValue_BiobankSampleCollection(name);
	}
}

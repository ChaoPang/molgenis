package org.molgenis.data.discovery.model.network;

import javax.validation.constraints.NotNull;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_VisNetworkRequest.class)
public abstract class VisNetworkRequest
{
	@NotNull
	public abstract String getBiobankUniverseIdentifier();

	@NotNull
	public abstract String getSimilarityOption();

	public static VisNetworkRequest create(String biobankUniverseIdentifier, String similarityOption)
	{
		return new AutoValue_VisNetworkRequest(biobankUniverseIdentifier, similarityOption);
	}
}

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
	public abstract NetworkOption getNetworkOption();

	public static VisNetworkRequest create(String biobankUniverseIdentifier, String networkOption)
	{
		return new AutoValue_VisNetworkRequest(biobankUniverseIdentifier, NetworkOption.valueOf(networkOption));
	}
}

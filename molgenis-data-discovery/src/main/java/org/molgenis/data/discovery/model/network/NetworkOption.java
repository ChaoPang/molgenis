package org.molgenis.data.discovery.model.network;

public enum NetworkOption
{
	SEMANTIC("Semantic"), CURATED("Curated"), GENERATED("Generated");

	private String label;

	NetworkOption(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}

package org.molgenis.data.discovery.model.network;

public class NetworkConfiguration
{
	public static String NODE_SHAPE = "circle";

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
}

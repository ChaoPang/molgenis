package org.molgenis.data.discovery.scoring;

public interface ScoringModel
{
	public abstract float score(String document1, String document2, boolean strictMatch);
}

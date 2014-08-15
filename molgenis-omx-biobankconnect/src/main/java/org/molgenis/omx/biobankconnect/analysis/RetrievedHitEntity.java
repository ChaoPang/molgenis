package org.molgenis.omx.biobankconnect.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.molgenis.omx.biobankconnect.utils.HitEntity;

public class RetrievedHitEntity
{
	private final String featureName;
	private final List<HitEntity> hits;

	public RetrievedHitEntity(String featureName)
	{
		this.featureName = featureName;
		hits = new ArrayList<HitEntity>();
	}

	public void addHit(String mappedFeatureName, double score)
	{
		hits.add(new HitEntity(mappedFeatureName, score));
	}

	public String getFeatureName()
	{
		return featureName;
	}

	public List<HitEntity> getHits()
	{
		try
		{
			Collections.sort(hits);
		}
		catch (Exception e)
		{
			System.out.println("Variable : " + featureName + "\t error : " + e.getMessage());
		}
		return hits;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((featureName == null) ? 0 : featureName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		RetrievedHitEntity other = (RetrievedHitEntity) obj;
		if (featureName == null)
		{
			if (other.featureName != null) return false;
		}
		else if (!featureName.equals(other.featureName)) return false;
		return true;
	}
}

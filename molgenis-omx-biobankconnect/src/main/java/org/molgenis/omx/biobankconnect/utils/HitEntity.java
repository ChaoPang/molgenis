package org.molgenis.omx.biobankconnect.utils;

public class HitEntity implements Comparable<HitEntity>
{
	private final String variableName;
	private double score;

	public HitEntity(String variableName, double score)
	{
		this.variableName = variableName;
		this.score = score;
	}

	public void setScore(double score)
	{
		this.score = score;
	}

	public double getScore()
	{
		return score;
	}

	@Override
	public int compareTo(HitEntity o)
	{
		if (getScore() > o.getScore()) return -1;
		if (getScore() == o.getScore()) return 0;
		return 1;
	}

	public String getVariableName()
	{
		return variableName;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		HitEntity other = (HitEntity) obj;
		if (variableName == null)
		{
			if (other.variableName != null) return false;
		}
		else if (!variableName.equals(other.variableName)) return false;
		return true;
	}
}

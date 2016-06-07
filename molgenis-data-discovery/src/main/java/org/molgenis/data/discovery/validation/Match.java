package org.molgenis.data.discovery.validation;

public class Match
{
	private final String target;
	private final String source;
	private final Integer rank;

	public Match(String target, String source, Integer rank)
	{
		this.target = target;
		this.source = source;
		this.rank = rank;
	}

	public String getTarget()
	{
		return target;
	}

	public String getSource()
	{
		return source;
	}

	public Integer getRank()
	{
		return rank;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rank == null) ? 0 : rank.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Match other = (Match) obj;
		if (rank == null)
		{
			if (other.rank != null) return false;
		}
		else if (!rank.equals(other.rank)) return false;
		if (source == null)
		{
			if (other.source != null) return false;
		}
		else if (!source.equals(other.source)) return false;
		if (target == null)
		{
			if (other.target != null) return false;
		}
		else if (!target.equals(other.target)) return false;
		return true;
	}
}

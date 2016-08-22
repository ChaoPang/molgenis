package org.molgenis.data.discovery.validation;

import org.molgenis.data.semanticsearch.semantic.Hit;

class SourceAttributeMatch
{
	private final String sourceAttribute;
	private final Hit<String> hit;

	public SourceAttributeMatch(Hit<String> hit)
	{
		this.sourceAttribute = hit.getResult();
		this.hit = hit;
	}

	public String getSourceAttribute()
	{
		return sourceAttribute;
	}

	public Hit<String> getHit()
	{
		return hit;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceAttribute == null) ? 0 : sourceAttribute.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SourceAttributeMatch other = (SourceAttributeMatch) obj;
		if (sourceAttribute == null)
		{
			if (other.sourceAttribute != null) return false;
		}
		else if (!sourceAttribute.equals(other.sourceAttribute)) return false;
		return true;
	}
}

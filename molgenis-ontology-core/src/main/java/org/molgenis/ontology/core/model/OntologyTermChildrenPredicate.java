package org.molgenis.ontology.core.model;

import java.util.function.BiPredicate;

import org.molgenis.ontology.core.service.OntologyService;

import static java.util.Objects.requireNonNull;

public class OntologyTermChildrenPredicate implements BiPredicate<OntologyTerm, OntologyTerm>
{
	private final Integer searchLevel;
	private final Boolean allChildren;
	private final OntologyService ontologyService;

	public OntologyTermChildrenPredicate(Integer searchLevel, Boolean allChildren, OntologyService ontologyService)
	{
		this.searchLevel = requireNonNull(searchLevel);
		this.allChildren = requireNonNull(allChildren);
		this.ontologyService = requireNonNull(ontologyService);
	}

	@Override
	public boolean test(OntologyTerm parentOntologyTerm, OntologyTerm childOntologyTerm)
	{
		return ontologyService.getOntologyTermDistance(parentOntologyTerm, childOntologyTerm) <= searchLevel
				|| allChildren;
	}

	public Integer getSearchLevel()
	{
		return searchLevel;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((searchLevel == null) ? 0 : searchLevel.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		OntologyTermChildrenPredicate other = (OntologyTermChildrenPredicate) obj;
		if (searchLevel == null)
		{
			if (other.searchLevel != null) return false;
		}
		else if (!searchLevel.equals(other.searchLevel)) return false;
		return true;
	}
}

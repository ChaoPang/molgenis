package org.molgenis.data.semanticsearch.service;

import java.util.List;
import java.util.Set;

import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;

public interface TagGroupGenerator
{
	public abstract List<TagGroup> findTagGroups(String queryString, List<String> ontologyIds);

	public abstract List<TagGroup> generateTagGroups(Set<String> queryWords, List<TagGroup> relevantOntologyTermHits);

	public abstract List<TagGroup> applyTagMatchingCriteria(List<OntologyTerm> relevantOntologyTerms,
			Set<String> searchTerms);
}

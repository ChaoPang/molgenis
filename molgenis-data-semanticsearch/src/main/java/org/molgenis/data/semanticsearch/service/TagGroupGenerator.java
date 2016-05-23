package org.molgenis.data.semanticsearch.service;

import java.util.List;
import java.util.Set;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.ontology.core.model.OntologyTerm;

public interface TagGroupGenerator
{
	public abstract List<OntologyTermHit> findTagGroups(AttributeMetaData attributeMetaData,
			EntityMetaData entityMetadata, Set<String> queryWords, List<String> ontologyIds);

	public abstract List<OntologyTermHit> findTagGroups(String queryString, List<String> ontologyIds);

	public abstract List<OntologyTermHit> generateTagGroups(Set<String> queryWords,
			List<OntologyTermHit> relevantOntologyTermHits);

	public abstract List<OntologyTermHit> applyTagMatchingCriteria(List<OntologyTerm> relevantOntologyTerms,
			Set<String> searchTerms);
}

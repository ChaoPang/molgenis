package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.ontology.core.model.OntologyTerm;

public class SemanticSearchQuery
{
	private final Hit<String> stringQuery;

	private final Hit<OntologyTerm> ontologyTermQuery;

	public SemanticSearchQuery(Hit<String> stringQuery, Hit<OntologyTerm> ontologyTermQuery)
	{
		this.stringQuery = stringQuery;
		this.ontologyTermQuery = ontologyTermQuery;
	}
}

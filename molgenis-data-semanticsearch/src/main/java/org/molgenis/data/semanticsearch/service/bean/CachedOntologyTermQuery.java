package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_CachedOntologyTermQuery.class)
public abstract class CachedOntologyTermQuery
{
	public abstract OntologyTerm getOntologyTerm();

	public abstract QueryExpansionParam getQueryExpansionParam();

	public static CachedOntologyTermQuery create(OntologyTerm ontologyTerm, QueryExpansionParam queryExpansionParam)
	{
		return new AutoValue_CachedOntologyTermQuery(ontologyTerm, queryExpansionParam);
	}
}

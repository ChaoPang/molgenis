package org.molgenis.data.discovery.model;

import java.util.List;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_MappingExplanation.class)
public abstract class MappingExplanation
{
	public abstract String getIdentifier();

	public abstract List<OntologyTerm> getOntologyTerms();

	public abstract String getQueryString();

	public abstract String getMatchedWords();

	public abstract double getNgramScore();

	public static MappingExplanation create(String identifier, List<OntologyTerm> ontologyTerms, String queryString,
			String matchedWords, double ngramScore)
	{
		return new AutoValue_MappingExplanation(identifier, ontologyTerms, queryString, matchedWords, ngramScore);
	}
}

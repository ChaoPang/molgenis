package org.molgenis.data.semanticsearch.explain.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_ExplainedQueryString.class)
public abstract class ExplainedQueryString
{
	public abstract String getMatchedWords();

	public abstract String getQueryString();

	public abstract OntologyTerm getOntologyTerm();

	public abstract float getScore();

	public static ExplainedQueryString create(String matchedWords, String queryString, OntologyTerm ontologyTerm,
			float score)
	{
		return new AutoValue_ExplainedQueryString(matchedWords, queryString, ontologyTerm, score);
	}
}
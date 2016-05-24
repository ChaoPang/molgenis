package org.molgenis.data.semanticsearch.explain.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

import edu.umd.cs.findbugs.annotations.Nullable;

@AutoValue
@AutoGson(autoValueClass = AutoValue_AttributeMatchExplanation.class)
public abstract class AttributeMatchExplanation
{
	public abstract String getMatchedWords();

	public abstract String getQueryString();

	@Nullable
	public abstract OntologyTerm getOntologyTerm();

	public abstract float getScore();

	public static AttributeMatchExplanation create(String matchedWords, String queryString, OntologyTerm ontologyTerm,
			float score)
	{
		return new AutoValue_AttributeMatchExplanation(matchedWords, queryString, ontologyTerm, score);
	}
}
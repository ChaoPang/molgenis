package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermHit.class)
public abstract class OntologyTermHit
{
	public static OntologyTermHit create(OntologyTerm ontologyTerm, String joinedSynonym, String matchedWords)
	{
		return new AutoValue_OntologyTermHit(ontologyTerm, joinedSynonym, matchedWords);
	}

	public abstract OntologyTerm getOntologyTerm();

	public abstract String getJoinedSynonym();

	public abstract String getMatchedWords();
}

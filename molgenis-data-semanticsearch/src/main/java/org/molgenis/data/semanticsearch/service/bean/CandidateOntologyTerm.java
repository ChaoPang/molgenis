package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_CandidateOntologyTerm.class)
public abstract class CandidateOntologyTerm
{
	public static CandidateOntologyTerm create(OntologyTerm ontologyTerm, String matchedSynonym)
	{
		return new AutoValue_CandidateOntologyTerm(ontologyTerm, matchedSynonym);
	}

	public abstract OntologyTerm getOntologyTerm();

	public abstract String getMatchedSynonym();
}

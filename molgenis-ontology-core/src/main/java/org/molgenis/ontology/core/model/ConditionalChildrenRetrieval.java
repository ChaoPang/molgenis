package org.molgenis.ontology.core.model;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_ConditionalChildrenRetrieval.class)
public abstract class ConditionalChildrenRetrieval
{
	public abstract OntologyTerm getOntologyTerm();

	public abstract OntologyTermChildrenPredicate getContinuePredicate();

	public static ConditionalChildrenRetrieval create(OntologyTerm ontologyTerm,
			OntologyTermChildrenPredicate continuePredicate)
	{
		return new AutoValue_ConditionalChildrenRetrieval(ontologyTerm, continuePredicate);
	}
}

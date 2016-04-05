package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermHit.class)
public abstract class OntologyTermHit
{
	public static OntologyTermHit create(OntologyTerm ontologyTerm, String joinedSynonym)
	{
		return new AutoValue_OntologyTermHit(ontologyTerm, joinedSynonym);
	}

	public abstract OntologyTerm getOntologyTerm();

	public abstract String getJoinedSynonym();
}

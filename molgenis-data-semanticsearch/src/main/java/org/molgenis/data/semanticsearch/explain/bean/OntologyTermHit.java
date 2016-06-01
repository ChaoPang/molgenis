package org.molgenis.data.semanticsearch.explain.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermHit.class)
public abstract class OntologyTermHit
{
	public abstract OntologyTerm getOrigin();

	public abstract OntologyTerm getOntologyTerm();

	public static OntologyTermHit create(OntologyTerm origin, OntologyTerm ontologyTerm)
	{
		return new AutoValue_OntologyTermHit(origin, ontologyTerm);
	}
}

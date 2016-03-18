package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermHit.class)
public abstract class OntologyTermHit
{
	public static OntologyTermHit create(OntologyTerm ontologyTerm, String matchedSynonym)
	{
		return new AutoValue_OntologyTermHit(ontologyTerm, matchedSynonym);
	}

	public abstract OntologyTerm getOntologyTerm();

	public abstract String getMatchedSynonym();
	//
	// public static OntologyTermHit and(OntologyTermHit... terms)
	// {
	// if (terms == null || terms.length == 0)
	// {
	// return null;
	// }
	// if (terms.length == 1)
	// {
	// return terms[0];
	// }
	//
	// OntologyTerm ontologyTerm =
	// OntologyTerm.and(stream(terms).map(OntologyTermHit::getOntologyTerm).toArray(OntologyTerm[]::new));
	//
	// OntologyTerm.create(join(stream(terms).map(OntologyTermHit::getOntologyTerm).map(OntologyTerm::getIRI).toArray(),
	// ','),
	// "(" + join(stream(terms).map(OntologyTermHit::getOntologyTerm).map(OntologyTerm::getLabel).toArray(), " and ") +
	// ")");
	//
	// return
	// }
}

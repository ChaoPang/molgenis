package org.molgenis.data.semanticsearch.explain.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermHit.class)
public abstract class OntologyTermHit implements Comparable<OntologyTermHit>
{
	public abstract OntologyTerm getOrigin();

	public abstract OntologyTerm getOntologyTerm();

	public abstract Double getSimilarity();

	public static OntologyTermHit create(OntologyTerm origin, OntologyTerm ontologyTerm)
	{
		return new AutoValue_OntologyTermHit(origin, ontologyTerm, 1.0);
	}

	public static OntologyTermHit create(OntologyTerm origin, OntologyTerm ontologyTerm, Double similarity)
	{
		return new AutoValue_OntologyTermHit(origin, ontologyTerm, similarity);
	}

	public int compareTo(OntologyTermHit other)
	{
		return Double.compare(other.getSimilarity(), getSimilarity());
	}
}

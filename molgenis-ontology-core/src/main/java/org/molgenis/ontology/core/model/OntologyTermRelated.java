package org.molgenis.ontology.core.model;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermRelated.class)
public abstract class OntologyTermRelated
{
	public abstract OntologyTerm getTarget();

	public abstract OntologyTerm getSource();

	public abstract int getStopLevel();

	public static OntologyTermRelated create(OntologyTerm target, OntologyTerm source, int stopLevel)
	{
		return new AutoValue_OntologyTermRelated(target, source, stopLevel);
	}
}

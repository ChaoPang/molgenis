package org.molgenis.ontology.core.model;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OrientedOntologyTerm.class)
public abstract class OrientedOntologyTerm
{
	public abstract OntologyTerm getOntologyTerm();

	public abstract String getNodePath();

	public abstract boolean isRoot();

	public static OrientedOntologyTerm create(OntologyTerm ontologyTerm, String nodePath, boolean root)
	{
		return new AutoValue_OrientedOntologyTerm(ontologyTerm, nodePath, root);
	}
}

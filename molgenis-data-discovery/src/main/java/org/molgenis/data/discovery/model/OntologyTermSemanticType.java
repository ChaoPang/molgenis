package org.molgenis.data.discovery.model;

import java.util.List;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermSemanticType.class)
public abstract class OntologyTermSemanticType
{
	public abstract String getId();

	public abstract OntologyTerm getOntologyTerm();

	public abstract List<SemanticType> getSemanticTypes();

	public static OntologyTermSemanticType create(String id, OntologyTerm ontologyTerm,
			List<SemanticType> semanticTypes)
	{
		return new AutoValue_OntologyTermSemanticType(id, ontologyTerm, semanticTypes);
	}
}

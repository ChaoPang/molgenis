package org.molgenis.data.semanticsearch.service;

import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.semanticsearch.semantic.OntologyTag;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTagObject;

import java.util.List;
import java.util.Map;

public interface OntologyTagService extends TagService<OntologyTagObject, Ontology>
{
	OntologyTag addAttributeTag(String entityTypeId, String attributeName, String relationIRI,
			List<String> ontologyTermIRIs);

	void removeAttributeTag(String entityTypeId, String attributeName, String relationIRI, String ontologyTermIRI);

	Map<String, OntologyTag> tagAttributesInEntity(String entity, Map<Attribute, OntologyTagObject> tags);
}

package org.molgenis.data.semanticsearch.service;

import java.util.List;
import java.util.Map;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.semantic.OntologyTag;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.common.collect.Multimap;

public interface OntologyTagService extends TagService<OntologyTerm, Ontology>
{

	OntologyTag addAttributeTag(String entityName, String attributeName, String relationIRI,
			List<String> ontologyTermIRIs);

	void removeAttributeTag(String entityName, String attributeName, String relationIRI, String ontologyTermIRI);

	public Map<String, OntologyTag> tagAttributesInEntity(String entity, Map<AttributeMetaData, OntologyTerm> tags);

	public Map<String, List<OntologyTerm>> getTagsForAttributes(EntityMetaData entityMetaData,
			List<AttributeMetaData> attributes);

	public Multimap<String, OntologyTag> batchTagAttributesInEntity(String entity,
			Multimap<AttributeMetaData, OntologyTerm> attributeTags);
}

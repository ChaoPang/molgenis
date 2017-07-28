package org.molgenis.data.semanticsearch.service;

import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedMatchCandidate;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTagObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SemanticSearchService
{
	/**
	 * Find all relevant source attributes with an explanation based on ontology terms and search terms
	 *
	 * @param sourceEntityType
	 * @param queryTerms
	 * @param tagGroups
	 * @return AttributeMetaData of resembling attributes, sorted by relevance
	 */
	Map<Attribute, ExplainedMatchCandidate<Attribute>> findAttributes(EntityType sourceEntityType,
			Set<String> queryTerms, List<TagGroup> tagGroups);

	/**
	 * A decision tree for getting the relevant attributes
	 * <p>
	 * 1. First find attributes based on searchTerms. 2. Second find attributes based on ontology terms from tags 3.
	 * Third find attributes based on target attribute label.
	 *
	 * @return Attribute of resembling attributes, sorted by relevance
	 */
	Map<Attribute, ExplainedMatchCandidate<Attribute>> decisionTreeToFindRelevantAttributes(EntityType sourceEntityType,
			Attribute targetAttribute, Collection<OntologyTagObject> ontologyTagTermsFromTags, Set<String> searchTerms);

	/**
	 * Finds {@link OntologyTagObject}s that can be used to tag an attribute.
	 *
	 * @param entity      name of the entity
	 * @param ontologyIDs IDs of ontologies to take the {@link OntologyTagObject}s from.
	 * @return {@link Map} of {@link Hit}s for {@link OntologyTagObject} results
	 */
	Map<Attribute, Hit<OntologyTagObject>> findTags(String entity, List<String> ontologyIDs);
}

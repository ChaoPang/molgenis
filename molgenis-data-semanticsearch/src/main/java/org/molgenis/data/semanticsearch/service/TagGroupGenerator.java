package org.molgenis.data.semanticsearch.service;

import java.util.List;
import java.util.Set;

import org.molgenis.data.semanticsearch.explain.criteria.MatchingCriterion;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;

public interface TagGroupGenerator
{
	/**
	 * Generate a list of {@link TagGroup}s based on one query {@link String}
	 * 
	 * @param queryString
	 * @param ontologyIds
	 * @return a list of {@link TagGroup}s
	 */
	public abstract List<TagGroup> generateTagGroups(String queryString, List<String> ontologyIds);

	/**
	 * Combine the qualified {@link TagGroup}s to obtain the composite {@link TagGroup}s (consisting of multiple
	 * ontology terms)
	 * 
	 * @param queryWords
	 * @param relevantTagGroups
	 * @return a list of combined {@link TagGroup}s
	 */
	public abstract List<TagGroup> combineTagGroups(Set<String> queryWords, List<TagGroup> relevantTagGroups);

	/**
	 * Filter the relevant {@link OntologyTerm}s by applying the {@link MatchingCriterion} and generate a list of
	 * {@link TagGroup}s based on the qualified {@link OntologyTerm}s
	 * 
	 * @param relevantOntologyTerms
	 * @param searchTerms
	 * @param matchingCriterion
	 * @return
	 */
	public abstract List<TagGroup> applyTagMatchingCriterion(List<OntologyTerm> relevantOntologyTerms,
			Set<String> searchTerms, MatchingCriterion matchingCriterion);
}

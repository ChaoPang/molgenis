package org.molgenis.data.semanticsearch.explain.criteria;

import java.util.Set;

import org.molgenis.ontology.core.model.OntologyTerm;

public interface MatchingCriterion
{
	/**
	 * apply the matching criteria to determine whether or not the {@link OntologyTerm} is a good candidate
	 * 
	 * @param words
	 * @param ontologyTerm
	 * @return
	 */
	public abstract boolean apply(Set<String> words, OntologyTerm ontologyTerm);
}

package org.molgenis.data.semanticsearch.explain.criteria.impl;

import static java.util.stream.Collectors.toSet;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.collectLowerCaseTerms;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitRemoveStopWords;

import java.util.Set;

import org.molgenis.data.semanticsearch.explain.criteria.MatchingCriterion;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.utils.Stemmer;

public class StrictMatchingCriterion implements MatchingCriterion
{
	@Override
	public boolean apply(Set<String> words, OntologyTerm ontologyTerm)
	{
		Set<String> ontologyTermSynonyms = collectLowerCaseTerms(ontologyTerm);
		for (String synonym : ontologyTermSynonyms)
		{
			Set<String> wordsInSynonym = splitRemoveStopWords(synonym).stream().map(Stemmer::stem).collect(toSet());
			if (wordsInSynonym.size() != 0 && words.containsAll(wordsInSynonym)) return true;
		}
		return false;
	}
}

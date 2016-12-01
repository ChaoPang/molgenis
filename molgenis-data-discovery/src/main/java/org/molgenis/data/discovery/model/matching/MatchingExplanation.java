package org.molgenis.data.discovery.model.matching;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

@AutoValue
@AutoGson(autoValueClass = AutoValue_MatchingExplanation.class)
public abstract class MatchingExplanation
{
	public abstract String getIdentifier();

	public abstract List<OntologyTerm> getTargetOntologyTerms();

	public abstract List<OntologyTerm> getSourceOntologyTerms();

	public abstract String getQueryString();

	public abstract String getMatchedTargetWords();

	public abstract String getMatchedSourceWords();

	public abstract double getVsmScore();

	public abstract double getNgramScore();

	public static MatchingExplanation create(String identifier, List<OntologyTerm> targetOntologyTerms,
			List<OntologyTerm> sourceOntologyTerms, String queryString, String matchedTargetWords,
			String matchedSourceWords, double vsmScore, double ngramScore)
	{
		return new AutoValue_MatchingExplanation(identifier, targetOntologyTerms, sourceOntologyTerms, queryString,
				matchedTargetWords, matchedSourceWords, vsmScore, ngramScore);
	}

	public String getMatchedWords()
	{
		return getMatchedTargetWords() + ' ' + getMatchedSourceWords();
	}

	public List<OntologyTerm> getOntologyTerms()
	{
		return concat(getTargetOntologyTerms().stream(), getSourceOntologyTerms().stream()).distinct()
				.collect(toList());
	}
}

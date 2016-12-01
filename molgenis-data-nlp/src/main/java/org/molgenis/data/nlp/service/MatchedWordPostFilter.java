package org.molgenis.data.nlp.service;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.Entity;
import org.molgenis.data.nlp.beans.MatchResult;
import org.molgenis.data.nlp.beans.Phrase;
import org.molgenis.ontology.utils.Stemmer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class MatchedWordPostFilter
{
	private final SentenceAnalysisService sentenceAnalysisService;
	private final static String ILLEGAL_CHARS_REGEX = "[^\\p{IsAlphabetic}0-9]+";

	public MatchedWordPostFilter()
	{
		this.sentenceAnalysisService = new SentenceAnalysisService();
	}

	public List<Entity> filter(List<Entity> candidateMatchesForOneTarget)
	{
		List<Entity> candidateMatches = new ArrayList<>();
		for (Entity candidateMatch : candidateMatchesForOneTarget)
		{
			String targetLabel = candidateMatch.getString("targetlabel");
			String matchedTargetWords = candidateMatch.getString("matchedtargetwords");
			String sourceLabel = candidateMatch.getString("sourcelabel");
			String matchedSourceWords = candidateMatch.getString("matchedsourcewords");
			MatchResult targetMatchResult = getMatchResult(targetLabel, matchedTargetWords);
			MatchResult sourceMatchResult = getMatchResult(sourceLabel, matchedSourceWords);
			MatchResult combine = MatchResult.combine(targetMatchResult, sourceMatchResult);

			if (combine.isParitialMatch())
			{
				candidateMatches.add(candidateMatch);
			}
		}

		return candidateMatches;
	}

	private MatchResult getMatchResult(String label, String matchedWords)
	{
		List<Phrase> phrases = sentenceAnalysisService.getPhrases(label);

		boolean anyFullMatch = phrases.stream().map(Phrase::getAllPhrases).flatMap(List::stream)
				.anyMatch(nounPhrase -> match(nounPhrase, matchedWords));

		if (anyFullMatch)
		{
			return MatchResult.create(label, MatchResult.Result.FULL_MATCH);
		}
		else
		{
			boolean anyPartialMatch = phrases.stream().map(Phrase::getCorePhrase)
					.anyMatch(nounPhrase -> match(nounPhrase, matchedWords));

			if (anyPartialMatch)
			{
				return MatchResult.create(label, MatchResult.Result.PARTIAL_MATCH);
			}
		}

		return MatchResult.create(label, MatchResult.Result.NO_MATCH);
	}

	private boolean match(String nounPhrase, String matchedWords)
	{
		Set<String> nounPhraseTokens = splitIntoUniqueTerms(nounPhrase).stream().map(Stemmer::stem)
				.filter(StringUtils::isNotBlank).collect(toSet());

		Set<String> matchedWordTokens = splitIntoUniqueTerms(matchedWords).stream().map(Stemmer::stem)
				.filter(StringUtils::isNotBlank).collect(toSet());

		return matchedWordTokens.containsAll(nounPhraseTokens);
	}

	private Set<String> splitIntoUniqueTerms(String description)
	{
		return Sets.newLinkedHashSet(Stream.of(description.split(ILLEGAL_CHARS_REGEX)).map(StringUtils::lowerCase)
				.filter(StringUtils::isNotBlank).collect(toList()));
	}
}

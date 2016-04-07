package org.molgenis.ontology.utils;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.tartarus.snowball.ext.PorterStemmer;

import com.google.common.base.Splitter;

public class Stemmer
{
	private final static Splitter SPLITTER = Splitter.onPattern("[^\\p{IsAlphabetic}]+");
	private final static String ILLEGAL_REGEX_PATTERN = "[^a-zA-Z0-9 ]";
	private final static String SINGLE_SPACE_CHAR = " ";

	/**
	 * Remove illegal characters from the string and stem each single word
	 * 
	 * @param phrase
	 * @return a string that consists of stemmed words
	 */
	public static String cleanStemPhrase(String phrase)
	{
		List<String> stemmedWords = newArrayList(replaceIllegalCharacter(phrase).split(SINGLE_SPACE_CHAR));
		return stemAndJoin(stemmedWords);
	}

	public static String stem(String word)
	{
		PorterStemmer porterStemmer = new PorterStemmer();
		porterStemmer.setCurrent(word);
		porterStemmer.stem();
		return porterStemmer.getCurrent();
	}

	public static String stemAndJoin(Iterable<String> terms)
	{
		return stream(terms.spliterator(), false).filter(w -> !STOPWORDSLIST.contains(w.toLowerCase()))
				.map(Stemmer::stem).filter(StringUtils::isNotBlank).collect(joining(SINGLE_SPACE_CHAR));
	}

	public static Set<String> splitAndStem(String phrase)
	{
		return newHashSet(stream(SPLITTER.split(phrase.toLowerCase()).spliterator(), false)
				.filter(w -> !STOPWORDSLIST.contains(w)).map(Stemmer::stem).filter(StringUtils::isNotBlank)
				.collect(toSet()));
	}

	public static String replaceIllegalCharacter(String string)
	{
		return string.replaceAll(ILLEGAL_REGEX_PATTERN, " ").replaceAll(" +", " ").trim().toLowerCase();
	}
}
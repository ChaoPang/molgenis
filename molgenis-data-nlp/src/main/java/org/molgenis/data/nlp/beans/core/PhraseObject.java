package org.molgenis.data.nlp.beans.core;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class PhraseObject implements Comparable<PhraseObject>
{
	private final int startingIndex;
	private final int endingIndex;
	private final KeyConceptModifier keyConceptModifier;
	private final KeyConcept keyConcept;

	public PhraseObject(KeyConcept keyConcept, KeyConceptModifier keyConceptModifier)
	{
		this.keyConcept = keyConcept;
		this.keyConceptModifier = keyConceptModifier;

		List<Word> allWords = Stream.concat(keyConcept.getWords().stream(), keyConceptModifier.getWords().stream())
				.distinct().sorted().collect(toList());

		this.startingIndex = allWords.stream().mapToInt(Word::getBeginPosition).min().orElse(-1);
		this.endingIndex = allWords.stream().mapToInt(Word::getEndPosition).max().orElse(-1);
	}

	public int getBeginPosition()
	{
		return startingIndex;
	}

	public int getEndPosition()
	{
		return endingIndex;
	}

	public String getKeyConceptString()
	{
		return keyConcept.getKeyConceptString();
	}

	public List<String> getModifiers()
	{
		return keyConceptModifier.getAllModifiers();
	}

	public boolean isNotEmpty()
	{
		return !keyConcept.getWords().isEmpty();
	}

	public boolean isEmpty()
	{
		return keyConcept.getWords().isEmpty();
	}

	public List<Word> getAllWordObjects()
	{
		List<Word> keyConceptWords = keyConcept.getWords();
		List<Word> keyConceptModifiers = keyConceptModifier.getWords();
		return Stream.concat(keyConceptModifiers.stream(), keyConceptWords.stream()).sorted().collect(toList());
	}

	public List<String> getAllWordTexts()
	{
		List<Word> keyConceptWords = keyConcept.getWords();
		List<Word> keyConceptModifiers = keyConceptModifier.getWords();
		return Stream.concat(keyConceptModifiers.stream(), keyConceptWords.stream()).sorted().map(Word::getText)
				.collect(toList());
	}

	public String toString()
	{
		if (isEmpty()) return StringUtils.EMPTY;

		return String.format("Modifiers:%s; KeyConcept: %s; Start: %s; End: %s", keyConceptModifier.getAllModifiers(),
				keyConcept.getKeyConceptString(), getBeginPosition(), getEndPosition());
	}

	public KeyConceptModifier getKeyConceptModifier()
	{
		return keyConceptModifier;
	}

	public KeyConcept getKeyConcept()
	{
		return keyConcept;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + endingIndex;
		result = prime * result + startingIndex;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PhraseObject other = (PhraseObject) obj;
		if (endingIndex != other.endingIndex) return false;
		if (startingIndex != other.startingIndex) return false;
		return true;
	}

	@Override
	public int compareTo(PhraseObject o)
	{
		return Integer.compare(getBeginPosition(), o.getBeginPosition());
	}
}

package org.molgenis.data.nlp.beans.core;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class KeyConceptModifier
{
	private final List<Word> keyConceptModifiers;

	public KeyConceptModifier(List<Word> words)
	{
		this.keyConceptModifiers = extractKeyConceptModifiers(words);
	}

	protected abstract List<Word> extractKeyConceptModifiers(List<Word> words);

	public List<Word> getWords()
	{
		return keyConceptModifiers;
	}

	public String getKeyModifier()
	{
		// Looking for the modifier pattern Noun + Verb/Adj (e.g. Lipid Lowering)
		OptionalInt min = keyConceptModifiers.stream().filter(Word::isNoun).mapToInt(keyConceptModifiers::indexOf)
				.min();
		if (min.isPresent())
		{
			return keyConceptModifiers.stream().skip(min.getAsInt()).map(Word::getText).collect(joining(" "));
		}
		return EMPTY;
	}

	public List<String> getAllModifiers()
	{
		List<String> allModifiers = new ArrayList<>();

		int startingIndex = keyConceptModifiers.size() - 1;

		OptionalInt min = keyConceptModifiers.stream().filter(Word::isNoun).mapToInt(keyConceptModifiers::indexOf)
				.min();

		String keyModifier = EMPTY;
		if (min.isPresent())
		{
			startingIndex = min.getAsInt() - 1;
			keyModifier = keyConceptModifiers.stream().skip(min.getAsInt()).map(Word::getText).collect(joining(" "));
		}

		for (int index = startingIndex; index >= 0; index--)
		{
			allModifiers.add(isBlank(keyModifier) ? keyConceptModifiers.get(index).getText() :
					keyConceptModifiers.get(index).getText() + " " + keyModifier);
		}
		return allModifiers;
	}
}

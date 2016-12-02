package org.molgenis.data.nlp.beans.noun;

import org.molgenis.data.nlp.beans.core.KeyConceptModifier;
import org.molgenis.data.nlp.beans.core.Word;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class NounKeyConceptModifier extends KeyConceptModifier
{
	public NounKeyConceptModifier(List<Word> words)
	{
		super(words);
	}

	protected List<Word> extractKeyConceptModifiers(List<Word> words)
	{
		List<Word> keyConceptNounModifierWords = new ArrayList<>();

		if (!words.isEmpty() && words.get(words.size() - 1).isNoun())
		{
			int firstNounIndex = words.stream().filter(Word::isNoun).mapToInt(word -> words.indexOf(word)).min()
					.orElse(words.size() - 1);
			keyConceptNounModifierWords.addAll(words.stream().limit(firstNounIndex).collect(toList()));
		}

		return keyConceptNounModifierWords;
	}
}

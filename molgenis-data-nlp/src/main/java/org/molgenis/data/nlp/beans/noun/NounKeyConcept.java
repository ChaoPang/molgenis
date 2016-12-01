package org.molgenis.data.nlp.beans.noun;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.molgenis.data.nlp.beans.core.KeyConcept;
import org.molgenis.data.nlp.beans.core.Word;

public class NounKeyConcept extends KeyConcept
{
	public NounKeyConcept(List<Word> words)
	{
		super(words);
	}

	protected List<Word> extractKeyConceptWords(List<Word> words)
	{
		List<Word> keyConceptNounWords = new ArrayList<>();

		if (words.get(words.size() - 1).isNoun())
		{
			int firstNounIndex = words.stream().filter(Word::isNoun).mapToInt(word -> words.indexOf(word)).min()
					.orElse(words.size() - 1);

			keyConceptNounWords.addAll(words.stream().skip(firstNounIndex).collect(toList()));
		}

		return keyConceptNounWords;
	}
}

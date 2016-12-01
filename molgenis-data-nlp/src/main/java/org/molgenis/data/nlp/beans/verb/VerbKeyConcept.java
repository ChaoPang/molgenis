package org.molgenis.data.nlp.beans.verb;

import java.util.ArrayList;
import java.util.List;

import org.molgenis.data.nlp.beans.core.KeyConcept;
import org.molgenis.data.nlp.beans.core.Word;

public class VerbKeyConcept extends KeyConcept
{
	public VerbKeyConcept(List<Word> words)
	{
		super(words);
	}

	protected List<Word> extractKeyConceptWords(List<Word> words)
	{
		List<Word> keyConceptVerbWords = new ArrayList<>();
		for (Word currentWord : words)
		{
			if (currentWord.isVerb())
			{
				keyConceptVerbWords.add(currentWord);
			}
			else break;
		}
		return keyConceptVerbWords;
	}
}

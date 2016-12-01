package org.molgenis.data.nlp.beans.noun;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import org.molgenis.data.nlp.beans.core.KeyConcept;
import org.molgenis.data.nlp.beans.core.PhraseObject;
import org.molgenis.data.nlp.beans.core.Word;
import org.molgenis.data.nlp.beans.core.KeyConceptModifier;

import edu.stanford.nlp.trees.Tree;

public class NounPhraseObjectFactory
{
	public static PhraseObject create(List<Tree> nounPhraseElements)
	{
		List<Word> words = nounPhraseElements.stream().map(Word::new).sorted().collect(toList());
		// If any of the words are of duration type, we remove all the preceding words and the words
		List<Word> durationTypeOfWords = words.stream().filter(Word::isDurationType).collect(toList());
		if (!durationTypeOfWords.isEmpty())
		{
			words.removeAll(durationTypeOfWords);
		}
		KeyConcept nounKeyConcept = new NounKeyConcept(words);
		KeyConceptModifier nounKeyConceptModifier = new NounKeyConceptModifier(words);
		return new PhraseObject(nounKeyConcept, nounKeyConceptModifier);
	}

	public static PhraseObject create(PhraseObject phrase1, PhraseObject phrase2)
	{
		PhraseObject prev;
		PhraseObject next;

		if (phrase1.getEndPosition() > phrase2.getEndPosition())
		{
			prev = phrase2;
			next = phrase1;
		}
		else
		{
			prev = phrase1;
			next = phrase2;
		}

		List<Word> combinedWords = Stream.concat(prev.getAllWordObjects().stream(), next.getAllWordObjects().stream())
				.distinct().sorted().collect(toList());

		return new PhraseObject(new NounKeyConcept(combinedWords), new NounKeyConceptModifier(combinedWords));
	}
}

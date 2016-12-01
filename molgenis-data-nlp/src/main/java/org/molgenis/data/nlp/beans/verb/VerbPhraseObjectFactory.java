package org.molgenis.data.nlp.beans.verb;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.molgenis.data.nlp.beans.core.KeyConcept;
import org.molgenis.data.nlp.beans.core.KeyConceptModifier;
import org.molgenis.data.nlp.beans.core.PhraseObject;
import org.molgenis.data.nlp.beans.core.Word;

import edu.stanford.nlp.trees.Tree;

public class VerbPhraseObjectFactory
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
		KeyConcept nounKeyConcept = new VerbKeyConcept(words);
		KeyConceptModifier nounKeyConceptModifier = new VerbKeyConceptModifier(words);
		return new PhraseObject(nounKeyConcept, nounKeyConceptModifier);
	}
}

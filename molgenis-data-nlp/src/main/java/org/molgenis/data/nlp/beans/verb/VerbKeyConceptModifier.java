package org.molgenis.data.nlp.beans.verb;

import static java.util.Collections.emptyList;

import java.util.List;

import org.molgenis.data.nlp.beans.core.Word;
import org.molgenis.data.nlp.beans.core.KeyConceptModifier;

public class VerbKeyConceptModifier extends KeyConceptModifier
{
	public VerbKeyConceptModifier(List<Word> words)
	{
		super(words);
	}

	protected List<Word> extractKeyConceptModifiers(List<Word> words)
	{
		return emptyList();
	}
}

package org.molgenis.data.nlp.beans.core;

import java.util.List;

import static java.util.stream.Collectors.joining;

public abstract class KeyConcept
{
	private final List<Word> keyConceptWords;

	public KeyConcept(List<Word> words)
	{
		this.keyConceptWords = extractKeyConceptWords(words);
	}

	// The connected noun words at the end of the Noun Phrase group becomes the key concept noun words
	protected abstract List<Word> extractKeyConceptWords(List<Word> words);

	public List<Word> getWords()
	{
		return keyConceptWords;
	}

	public String getKeyConceptString()
	{
		return keyConceptWords.stream().map(Word::getText).collect(joining(" "));
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((keyConceptWords == null) ? 0 : keyConceptWords.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		KeyConcept other = (KeyConcept) obj;
		if (keyConceptWords == null)
		{
			if (other.keyConceptWords != null) return false;
		}
		else if (!keyConceptWords.equals(other.keyConceptWords)) return false;
		return true;
	}
}

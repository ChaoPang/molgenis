package org.molgenis.data.nlp.beans.core;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.Tree;

public class Word implements Comparable<Word>
{
	private static final String NOUN_TAG = "NN";
	private static final String ADJ_TAG = "JJ";
	private static final String VERB_TAG = "VB";
	private static final String RECOGNIZED_ENTITY_DATE = "DATE";
	private static final String RECOGNIZED_ENTITY_DURATION = "DURATION";
	private static final String RECOGNIZED_ENTITY_TIME = "TIME";

	private final String originalText;
	private final String posTagger;
	private final String recognizedEntityName;
	private final int beginPosition;
	private final int endPosition;

	public Word(Tree lastLevelNode)
	{
		CoreLabel coreLabel = lastLevelNode.taggedLabeledYield().get(0);
		this.originalText = coreLabel.originalText();
		this.posTagger = coreLabel.tag();
		this.recognizedEntityName = coreLabel.ner();
		this.beginPosition = coreLabel.beginPosition();
		this.endPosition = coreLabel.endPosition();
	}

	public Word(IndexedWord indexedWord)
	{
		this.originalText = indexedWord.originalText();
		this.posTagger = indexedWord.tag();
		this.recognizedEntityName = indexedWord.ner();
		this.beginPosition = indexedWord.beginPosition();
		this.endPosition = indexedWord.endPosition();
	}

	public boolean isNoun()
	{
		return posTagger.startsWith(NOUN_TAG);
	}

	public boolean isVerb()
	{
		return posTagger.startsWith(VERB_TAG);
	}

	public boolean isAdj()
	{
		return posTagger.startsWith(ADJ_TAG);
	}

	public boolean isDurationType()
	{
		return recognizedEntityName.equals(RECOGNIZED_ENTITY_DURATION) || recognizedEntityName
				.equals(RECOGNIZED_ENTITY_DATE) || recognizedEntityName.equals(RECOGNIZED_ENTITY_TIME);
	}

	public String getText()
	{
		return originalText;
	}

	public int getBeginPosition()
	{
		return beginPosition;
	}

	public int getEndPosition()
	{
		return endPosition;
	}

	@Override
	public int compareTo(Word o)
	{
		return Integer.compare(getBeginPosition(), o.getBeginPosition());
	}

	public String toString()
	{
		return originalText;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + beginPosition;
		result = prime * result + endPosition;
		result = prime * result + ((originalText == null) ? 0 : originalText.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Word other = (Word) obj;
		if (beginPosition != other.beginPosition) return false;
		if (endPosition != other.endPosition) return false;
		if (originalText == null)
		{
			if (other.originalText != null) return false;
		}
		else if (!originalText.equals(other.originalText)) return false;
		return true;
	}
}

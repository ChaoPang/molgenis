package org.molgenis.data.nlp.service;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import org.molgenis.data.nlp.beans.Phrase;
import org.molgenis.data.nlp.beans.core.PhraseObject;
import org.molgenis.data.nlp.beans.core.Word;
import org.molgenis.data.nlp.relations.Relation;
import org.molgenis.data.nlp.relations.WordRelation;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public abstract class AbstractExtractPhraseService
{
	public abstract List<Phrase> getPhrases(String text);

	protected abstract List<PhraseObject> createPhraseObjects(Tree tree);

	protected List<WordRelation> createWordRelations(SemanticGraph semanticGraph)
	{
		List<WordRelation> wordRelations = new ArrayList<>();
		for (SemanticGraphEdge semanticGraphEdge : semanticGraph.edgeListSorted())
		{
			IndexedWord governor = semanticGraphEdge.getGovernor();
			IndexedWord dependent = semanticGraphEdge.getDependent();
			GrammaticalRelation grammaticalRelation = semanticGraphEdge.getRelation();

			Word subjectWord = new Word(governor);
			Relation create = Relation.create(grammaticalRelation);
			Word objectWord = new Word(dependent);

			wordRelations.add(WordRelation.create(subjectWord, create, objectWord));
		}

		return wordRelations;
	}

	/**
	 * Because the qualified phrases can be overlapped with each other, we want to remove the phrases that are contained
	 * in the other qualified phrases
	 *
	 * @param phraseObjects
	 * @return
	 */
	protected List<PhraseObject> removeOverlappingPhraseObjects(List<PhraseObject> phraseObjects)
	{
		List<PhraseObject> phraseObjectsToRemove = new ArrayList<>();
		for (int i = 0; i < phraseObjects.size(); i++)
		{
			int firstStartingIndex = phraseObjects.get(i).getBeginPosition();
			int firstEndingIndex = phraseObjects.get(i).getEndPosition();

			for (int j = 0; j < phraseObjects.size(); j++)
			{
				if (i != j)
				{
					int secondStartingIndex = phraseObjects.get(j).getBeginPosition();
					int secondEndingIndex = phraseObjects.get(j).getEndPosition();

					if (firstStartingIndex <= secondStartingIndex && firstEndingIndex >= secondEndingIndex)
					{
						phraseObjectsToRemove.add(phraseObjects.get(j));
					}
				}
			}
		}
		return phraseObjects.stream().filter(phraseObject -> !phraseObjectsToRemove.contains(phraseObject))
				.collect(toList());
	}

	/**
	 * Replace the useless patterns in the text {@link String}
	 *
	 * @param text
	 * @return
	 */
	protected String preprocessText(String text)
	{
		return text.toLowerCase().replaceAll("\\(\\s*repeat\\s*\\)\\s*\\(\\d+\\)", EMPTY)
				.replaceAll("\\s*etc\\.", EMPTY).replaceAll("\\/", " \\/ ").replaceAll("\\(", " \\(")
				.replaceAll("\\)", "\\) ").replaceAll("\\s\\d{1,2}\\w\\s+", " ");
	}
}
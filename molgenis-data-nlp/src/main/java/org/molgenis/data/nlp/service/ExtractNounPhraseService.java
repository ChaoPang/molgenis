package org.molgenis.data.nlp.service;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import org.molgenis.data.nlp.beans.Phrase;
import org.molgenis.data.nlp.beans.core.PhraseObject;
import org.molgenis.data.nlp.beans.core.Word;
import org.molgenis.data.nlp.beans.noun.NounPhraseObjectFactory;
import org.molgenis.data.nlp.relations.PrepositionalPhraseRelation;
import org.molgenis.data.nlp.relations.WordRelation;
import org.molgenis.data.nlp.utils.NlpUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.molgenis.data.nlp.utils.NlpUtils.*;

public class ExtractNounPhraseService extends AbstractExtractPhraseService
{
	private final StanfordCoreNLP pipeline;

	public ExtractNounPhraseService(StanfordCoreNLP pipeline)
	{
		this.pipeline = pipeline;
	}

	public static void main(String[] args) throws IOException
	{
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		ExtractNounPhraseService converter = new ExtractNounPhraseService(pipeline);
		String string = "high blood cholesterol level";
		List<Phrase> nounPhrases = converter.getPhrases(string);
		System.out.format("The input: %s%nThe phrases: %s%n%n", string, nounPhrases);
	}

	public List<Phrase> getPhrases(String text)
	{
		List<Phrase> phrases = new ArrayList<>();

		String processedText = preprocessText(text);

		Annotation document = new Annotation(processedText);

		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for (CoreMap sentence : sentences)
		{
			Tree tree = sentence.get(TreeAnnotation.class);

			List<PhraseObject> nounPhraseObjects = createPhraseObjects(tree);

			List<PrepositionalPhraseRelation> propositionalPhraseRelations = concat(
					extractPropositionOfRelations(tree, nounPhraseObjects).stream(),
					extractPropositionInRelations(tree, nounPhraseObjects).stream()).collect(toList());

			// Remove the nounModifiers from the nounPhraseObjects
			nounPhraseObjects.removeAll(
					propositionalPhraseRelations.stream().flatMap(node -> node.getAllModifiers().stream())
							.collect(toList()));

			SemanticGraph semanticGraph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);

			List<WordRelation> wordRelations = createWordRelations(semanticGraph);

			nounPhraseObjects = removeSubjectNounsFromPhraseObjects(nounPhraseObjects, wordRelations);

			List<Phrase> convertPhraseObjectsToNounPhrases = convertPhraseObjectsToNounPhrases(nounPhraseObjects,
					propositionalPhraseRelations);

			phrases.addAll(convertPhraseObjectsToNounPhrases);
		}

		return phrases;
	}

	private List<PhraseObject> removeSubjectNounsFromPhraseObjects(List<PhraseObject> nounPhraseObjects,
			List<WordRelation> wordRelations)
	{
		WordRelation subjectRelation = wordRelations.stream()
				.filter(wordRelation -> wordRelation.getRelation().isSubject()).findFirst().orElse(null);

		WordRelation objectRelation = wordRelations.stream()
				.filter(wordRelation -> wordRelation.getRelation().isObject()).findFirst().orElse(null);

		if (nonNull(subjectRelation) && nonNull(objectRelation))
		{
			Word subject = subjectRelation.getDependentWord();
			Word object = objectRelation.getDependentWord();

			if (subject.isNoun() && object.isNoun())
			{
				nounPhraseObjects = nounPhraseObjects.stream()
						.filter(phraseObject -> !phraseObject.getKeyConcept().getWords().contains(subject))
						.collect(toList());
			}
		}

		return nounPhraseObjects;
	}

	/**
	 * Find the qualified phrases from which the nouns can be extracted
	 *
	 * @param tree
	 * @return
	 */
	protected List<PhraseObject> createPhraseObjects(Tree tree)
	{
		List<PhraseObject> phraseObjects = new ArrayList<>();

		List<Tree> qualifiedPhrases = new ArrayList<>();
		// Get Verb Phrases that are contained in a Noun Phrase
		qualifiedPhrases.addAll(findNounsInVerbPhrasesBottomUpApproach(tree));
		// Get Noun Phrases that contain Nouns
		qualifiedPhrases.addAll(findNounPhrasesBottomUpApproach(tree));

		for (Tree qualifiedPhrase : qualifiedPhrases)
		{
			List<Tree> wordGroup = new ArrayList<>();

			for (Tree lastLevelNode : getLastLevelNodes(qualifiedPhrase))
			{
				if ((isTagVerb(lastLevelNode) || isTagAdjective(lastLevelNode)) && isPrevWordGroupMemberNotNoun(
						wordGroup))
				{
					wordGroup.add(lastLevelNode);
				}
				else if (isTagNoun(lastLevelNode) || isInnerPhraseConjunctionAmpersand(lastLevelNode)
						|| isTagCardinalNumber(lastLevelNode))
				{
					wordGroup.add(lastLevelNode);
				}
				else if (!wordGroup.isEmpty())
				{
					phraseObjects.add(NounPhraseObjectFactory.create(wordGroup));
					wordGroup.clear();
				}
			}

			if (!wordGroup.isEmpty())
			{
				phraseObjects.add(NounPhraseObjectFactory.create(wordGroup));
				wordGroup.clear();
			}
		}

		List<PhraseObject> removeOverlappingPhraseObjects = removeOverlappingPhraseObjects(
				phraseObjects.stream().filter(PhraseObject::isNotEmpty).distinct().sorted().collect(toList()));

		return removeOverlappingPhraseObjects;
	}

	/**
	 * Create a list of {@link Phrase}s based on the collected {@link PhraseObject}s
	 *
	 * @param nounPhraseNodes
	 * @param propositionalPhraseRelations
	 * @return
	 */
	private List<Phrase> convertPhraseObjectsToNounPhrases(List<PhraseObject> nounPhraseNodes,
			List<PrepositionalPhraseRelation> propositionalPhraseRelations)
	{
		List<Phrase> nounPhrases = new ArrayList<>();

		// If the phrases are sitting next to each other in a sentence, they should be connected.
		List<PhraseObject> linkedPhraseObjects = new ArrayList<>();

		for (PhraseObject phraseObject : nounPhraseNodes)
		{
			if (linkedPhraseObjects.isEmpty())
			{
				linkedPhraseObjects.add(phraseObject);
			}
			else if (isCurrentPhraseAdjacentToPreviousPhrase(phraseObject, linkedPhraseObjects))
			{
				linkedPhraseObjects.add(phraseObject);
			}
			else
			{
				nounPhrases.add(createNounPhrase(linkedPhraseObjects, propositionalPhraseRelations));
				// Reset the list
				linkedPhraseObjects.clear();
				// Add the new phraseObject to the list
				linkedPhraseObjects.add(phraseObject);
			}
		}

		if (!linkedPhraseObjects.isEmpty())
		{
			nounPhrases.add(createNounPhrase(linkedPhraseObjects, propositionalPhraseRelations));
		}

		return nounPhrases;
	}

	/**
	 * Find the Noun Phrases that contain nouns
	 *
	 * @param tree
	 * @return
	 */
	private List<Tree> findNounPhrasesBottomUpApproach(Tree tree)
	{
		List<Tree> nounPhrases = getLastLevelNounNodes(tree).stream().map(leaf -> leaf.parent(tree)).distinct()
				.collect(toList());

		return nounPhrases;
	}

	/**
	 * Find the Verb Phrases that contain a verb and noun phrases
	 *
	 * @param tree
	 * @return
	 */
	private List<Tree> findNounsInVerbPhrasesBottomUpApproach(Tree tree)
	{
		List<Tree> verbPhrases = new ArrayList<>();

		List<Tree> verbPhraseNodes = tree.getLeaves().stream().map(leaf -> leaf.parent(tree))
				.filter(NlpUtils::isTagVerb).map(leaf -> leaf.parent(tree)).filter(NlpUtils::isTagVerbPhrase)
				.collect(toList());

		for (Tree verbPhraseNode : verbPhraseNodes)
		{
			Tree parent = verbPhraseNode;
			while (true)
			{
				if (isTagNounPhrase(parent))
				{
					verbPhrases.add(verbPhraseNode);
					break;
				}

				if (parent.depth() == tree.depth())
				{
					break;
				}
				parent = parent.parent(tree);
			}
		}

		return verbPhrases;
	}

	/**
	 * Check if the previous {@link PhraseObject} is the preceding neighbor of the current {@link PhraseObject}
	 *
	 * @param phraseObject
	 * @param linkedPhraseObjects
	 * @return
	 */
	private boolean isCurrentPhraseAdjacentToPreviousPhrase(PhraseObject phraseObject,
			List<PhraseObject> linkedPhraseObjects)
	{
		return linkedPhraseObjects.size() > 0
				&& linkedPhraseObjects.get(linkedPhraseObjects.size() - 1).getEndPosition() + 1 == phraseObject
				.getBeginPosition();
	}

	private boolean isPrevWordGroupMemberNotNoun(List<Tree> wordGroup)
	{
		return wordGroup.isEmpty() || !isTagNoun(wordGroup.get(wordGroup.size() - 1));
	}

	private Phrase createNounPhrase(List<PhraseObject> linkedPhraseObjects,
			List<PrepositionalPhraseRelation> prepositionalPhraseRelations)
	{
		List<PhraseObject> prepositionalPhraseModifiers = new ArrayList<>();

		for (PrepositionalPhraseRelation prepositionalPhraseRelation : prepositionalPhraseRelations)
		{
			for (PhraseObject phraseObject : linkedPhraseObjects)
			{
				if (prepositionalPhraseRelation.contains(phraseObject))
				{
					prepositionalPhraseModifiers.addAll(prepositionalPhraseRelation.getNounModifiers(phraseObject));
				}
			}
		}

		PhraseObject combinedPhraseObject = linkedPhraseObjects.get(0);
		for (int i = 1; i < linkedPhraseObjects.size(); i++)
		{
			combinedPhraseObject = NounPhraseObjectFactory.create(combinedPhraseObject, linkedPhraseObjects.get(i));
		}

		List<String> prepositionalPhraseWords = prepositionalPhraseModifiers.stream().map(PhraseObject::getAllWordTexts)
				.flatMap(List::stream).distinct().collect(toList());

		String keyConceptString = combinedPhraseObject.getKeyConceptString();

		List<String> modifiers = combinedPhraseObject.getModifiers();

		return Phrase.create(keyConceptString, modifiers, prepositionalPhraseWords);
	}

	/**
	 * Capture the prepositional phrases such as 'height in cm' and 'meat on bread'.
	 *
	 * @param treeNode
	 * @param totalNounPhraseNodes
	 * @return
	 */
	private List<PrepositionalPhraseRelation> extractPropositionInRelations(Tree treeNode,
			List<PhraseObject> totalNounPhraseNodes)
	{
		List<Tree> prepositionalPhraseNodes = treeNode.getLeaves().stream().map(leaf -> leaf.parent(treeNode))
				.filter(NlpUtils::isTagConjunctionIn).map(leaf -> leaf.parent(treeNode))
				.filter(NlpUtils::isGroupInPropositionPhrase).collect(toList());

		List<PrepositionalPhraseRelation> PrepositionalPhraseRelations = createPropositionaRelations(treeNode,
				totalNounPhraseNodes, prepositionalPhraseNodes, true);

		return PrepositionalPhraseRelations;
	}

	/**
	 * Capture the prepositional phrases such as 'history of hypertension'.
	 *
	 * @param treeNode
	 * @param totalNounPhraseNodes
	 * @return
	 */
	private List<PrepositionalPhraseRelation> extractPropositionOfRelations(Tree treeNode,
			List<PhraseObject> totalNounPhraseNodes)
	{
		List<Tree> prepositionalPhraseNodes = getLastLevelNodes(treeNode).stream().filter(NlpUtils::isTagConjunctionIn)
				.map(leaf -> leaf.parent(treeNode)).filter(NlpUtils::isGroupOfPropositionPhrase).collect(toList());

		List<PrepositionalPhraseRelation> PrepositionalPhraseRelations = createPropositionaRelations(treeNode,
				totalNounPhraseNodes, prepositionalPhraseNodes, false);

		return PrepositionalPhraseRelations;
	}

	/**
	 * Create a {@link List} of {@link PrepositionalPhraseRelation}s for the nouns and their corresponding prepositional
	 * phrases. Depending on the type of prepositional phrases such as 'history of hypertension' and 'height in cm', the
	 * position of the core nouns are different. For example, for the phrase 'history of hypertension', the core noun is
	 * hypertension that sits in the prepositional phrase whereas for the phrase 'height in cm', the core noun is height
	 * that sits outside the prepositional phrase.
	 *
	 * @param treeNode
	 * @param totalNounPhraseNodes
	 * @param prepositionalPhraseNodes
	 * @param isCoreNounBeforePrepositionPhrase
	 * @return
	 */
	private List<PrepositionalPhraseRelation> createPropositionaRelations(Tree treeNode,
			List<PhraseObject> totalNounPhraseNodes, List<Tree> prepositionalPhraseNodes,
			boolean isCoreNounBeforePrepositionPhrase)
	{
		List<PrepositionalPhraseRelation> PrepositionalPhraseRelations = new ArrayList<>();

		for (Tree prepositionalPhraseNode : prepositionalPhraseNodes)
		{
			// Check if the preceding neighbor is of NP type
			if (isPrecedingSiblingNounPhrase(prepositionalPhraseNode, treeNode))
			{
				Tree parentNodeIncludingPrepositionalPhrase = prepositionalPhraseNode.parent(treeNode);

				List<PhraseObject> nounsInPrepositionalPhrase = createPhraseObjects(prepositionalPhraseNode);

				List<Integer> nounsInPrepositionalPhraseEndPositions = createPhraseObjects(
						parentNodeIncludingPrepositionalPhrase).stream()
						.filter(nounPhraseNode -> !nounsInPrepositionalPhrase.contains(nounPhraseNode))
						.map(PhraseObject::getEndPosition).collect(Collectors.toList());

				List<PhraseObject> nounsBeforePrepositionalPhrase = totalNounPhraseNodes.stream()
						.filter(phraseNode -> nounsInPrepositionalPhraseEndPositions
								.contains(phraseNode.getEndPosition())).collect(toList());

				PrepositionalPhraseRelation prepositionalPhraseRelation;
				if (isCoreNounBeforePrepositionPhrase)
				{
					prepositionalPhraseRelation = new PrepositionalPhraseRelation(nounsInPrepositionalPhrase,
							nounsBeforePrepositionalPhrase);
				}
				else
				{
					prepositionalPhraseRelation = new PrepositionalPhraseRelation(nounsBeforePrepositionalPhrase,
							nounsInPrepositionalPhrase);
				}
				PrepositionalPhraseRelations.add(prepositionalPhraseRelation);
			}
		}
		return PrepositionalPhraseRelations;
	}

	private boolean isPrecedingSiblingNounPhrase(Tree prepositionalPhraseNode, Tree root)
	{
		List<Tree> childrenAsList = prepositionalPhraseNode.parent(root).getChildrenAsList();
		int indexOf = childrenAsList.indexOf(prepositionalPhraseNode);
		if (indexOf == 0)
		{
			return false;
		}
		else
		{
			List<Tree> lastLevelNodes = getLastLevelNodes(childrenAsList.get(indexOf - 1));
			return indexOf >= 1 && isTagNoun(lastLevelNodes.get(lastLevelNodes.size() - 1));
		}
	}
}
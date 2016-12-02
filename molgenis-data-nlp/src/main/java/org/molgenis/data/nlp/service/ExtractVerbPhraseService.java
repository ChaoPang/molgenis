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
import org.molgenis.data.nlp.beans.verb.VerbPhraseObjectFactory;
import org.molgenis.data.nlp.beans.relations.Relation;
import org.molgenis.data.nlp.beans.relations.WordRelation;
import org.molgenis.data.nlp.utils.NlpUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.util.stream.Collectors.toList;
import static org.molgenis.data.nlp.utils.NlpUtils.getLastLevelNodes;
import static org.molgenis.data.nlp.utils.NlpUtils.isTagVerb;

public class ExtractVerbPhraseService extends AbstractExtractPhraseService
{
	private final StanfordCoreNLP pipeline;

	public ExtractVerbPhraseService(StanfordCoreNLP pipeline)
	{
		this.pipeline = pipeline;
	}

	public static void main(String[] args) throws IOException
	{
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		ExtractVerbPhraseService converter = new ExtractVerbPhraseService(pipeline);
		String string = "Indicator of whether the participant currently smokes cigars. Current is defined as today and up to the last 12 months.";
		List<Phrase> nounPhrases = converter.getPhrases(string);
		System.out.format("The input: %s%nThe phrases: %s%n%n", string, nounPhrases);
	}

	@Override
	public List<Phrase> getPhrases(String text)
	{
		List<Phrase> verbPhrases = new ArrayList<>();

		String processedText = preprocessText(text);
		Annotation document = new Annotation(processedText);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences)
		{
			SemanticGraph semanticGraph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);

			List<WordRelation> wordRelations = createWordRelations(semanticGraph);

			Tree tree = sentence.get(TreeAnnotation.class);

			List<PhraseObject> phraseObjects = createPhraseObjects(tree);

			List<Word> auxillaryVerbs = findAuxillaryVerbs(wordRelations);

			phraseObjects = removeAuxillaryVerbsFromPhraseObjects(phraseObjects, auxillaryVerbs);

			verbPhrases.addAll(createVerbPhrases(phraseObjects));
		}

		return verbPhrases;
	}

	private List<Phrase> createVerbPhrases(List<PhraseObject> phraseObjects)
	{
		List<Phrase> collect = phraseObjects.stream().map(phraseObject -> Phrase
				.create(phraseObject.getKeyConcept().getKeyConceptString(), phraseObject.getModifiers()))
				.collect(toList());
		return collect;
	}

	private List<PhraseObject> removeAuxillaryVerbsFromPhraseObjects(List<PhraseObject> phraseObjects,
			List<Word> auxillaryVerbs)
	{
		List<PhraseObject> filteredPhraseObjects = new ArrayList<>();

		for (PhraseObject phraseObject : phraseObjects)
		{
			if (!auxillaryVerbs.containsAll(phraseObject.getKeyConcept().getWords()))
			{
				filteredPhraseObjects.add(phraseObject);
			}
		}

		return filteredPhraseObjects;
	}

	private List<Word> findAuxillaryVerbs(List<WordRelation> wordRelations)
	{
		List<Word> verbWords = new ArrayList<>();
		for (WordRelation wordRelation : wordRelations)
		{
			Relation relation = wordRelation.getRelation();
			Word subjectWord = wordRelation.getGovernorWord();
			Word objectWord = wordRelation.getDependentWord();
			if (relation.isAuxiliary() && objectWord.isVerb())
			{
				verbWords.add(objectWord);
			}

			if (relation.isComplementaryClausal() && objectWord.isVerb() && subjectWord.isVerb())
			{
				verbWords.add(subjectWord);
			}
		}
		return verbWords;
	}

	protected List<PhraseObject> createPhraseObjects(Tree tree)
	{
		List<PhraseObject> phraseObjects = new ArrayList<>();

		List<Tree> qualifiedPhrases = findVerbPhrasesBottomUpApproach(tree);

		for (Tree qualifiedPhrase : qualifiedPhrases)
		{
			List<Tree> wordGroup = new ArrayList<>();

			for (Tree lastLevelNode : getLastLevelNodes(qualifiedPhrase))
			{
				if (isTagVerb(lastLevelNode))
				{
					wordGroup.add(lastLevelNode);
				}
				else if (!wordGroup.isEmpty())
				{
					phraseObjects.add(VerbPhraseObjectFactory.create(wordGroup));
					wordGroup.clear();
				}
			}

			if (!wordGroup.isEmpty())
			{
				phraseObjects.add(VerbPhraseObjectFactory.create(wordGroup));
				wordGroup.clear();
			}
		}

		List<PhraseObject> removeOverlappingPhraseObjects = removeOverlappingPhraseObjects(
				phraseObjects.stream().filter(PhraseObject::isNotEmpty).distinct().sorted().collect(toList()));

		return removeOverlappingPhraseObjects;

	}

	private List<Tree> findVerbPhrasesBottomUpApproach(Tree tree)
	{
		return tree.getLeaves().stream().map(leaf -> leaf.parent(tree)).filter(NlpUtils::isTagVerb)
				.map(lastLevelNode -> lastLevelNode.parent(tree)).collect(toList());
	}
}

package org.molgenis.data.nlp.utils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.LabeledWord;
import edu.stanford.nlp.trees.Tree;

public class NlpUtils
{
	public static final String AMPERSAND_CHAR = "&";
	public static final String NOUN_PHRASE_TAG = "NP";
	public static final String VERB_PHRASE_TAG = "VP";
	public static final String NOUN_TAG = "NN";
	public static final String ADJ_TAG = "JJ";
	public static final String VERB_TAG = "VB";
	public static final String SUBORDIATING_CONJUNCTION = "IN";
	public static final String PROPOSITION_TAG = "PP";
	public static final String PERSON_PRONOUN_TAG = "PRP";
	public static final String CONNECTING_CONJUNCTION = "CC";
	public static final String CARDINAL_NUMBER = "CD";
	public static final List<String> LIST_OF_PROPOSITION_IN = Arrays.asList("in", "for", "into", "with", "by", "on");

	public static boolean isTagNoun(Tree tree)
	{
		return tree.value().startsWith(NOUN_TAG);
	}

	public static boolean isTagVerb(Tree tree)
	{
		return tree.value().startsWith(VERB_TAG);
	}

	public static boolean isTagAdjective(Tree tree)
	{
		return tree.value().startsWith(ADJ_TAG);
	}

	public static boolean isTagNounPhrase(Tree tree)
	{
		return tree.value().equals(NOUN_PHRASE_TAG);
	}

	public static boolean isTagVerbPhrase(Tree tree)
	{
		return tree.value().equals(VERB_PHRASE_TAG);
	}

	public static boolean isTagPersonalPronoun(Tree tree)
	{
		return tree.value().equals(PERSON_PRONOUN_TAG);
	}

	public static boolean isTagProposition(Tree tree)
	{
		return tree.value().equals(PROPOSITION_TAG);
	}

	public static boolean isTagConjunctionIn(Tree tree)
	{
		return tree.value().equals(SUBORDIATING_CONJUNCTION);
	}

	public static boolean isTagCardinalNumber(Tree tree)
	{
		return tree.value().equals(CARDINAL_NUMBER);
	}

	public static boolean isInnerPhraseConjunctionAmpersand(Tree tree)
	{
		CoreLabel coreLabel = tree.taggedLabeledYield().get(0);
		String originalText = coreLabel.originalText();
		return tree.value().equals(CONNECTING_CONJUNCTION) && (originalText.equals(AMPERSAND_CHAR));
	}

	public static boolean isInnerPhraseConjunctionOr(Tree tree)
	{
		CoreLabel coreLabel = tree.taggedLabeledYield().get(0);
		String originalText = coreLabel.originalText();
		return tree.value().equals(CONNECTING_CONJUNCTION) && (originalText.equals("or"));
	}

	public static boolean isPropositionIn(LabeledWord labeledWord)
	{
		String originalText = labeledWord.word();
		return labeledWord.tag().value().equals(SUBORDIATING_CONJUNCTION) && isNotBlank(originalText)
				&& (LIST_OF_PROPOSITION_IN.contains(originalText.toLowerCase()));
	}

	public static boolean isPropositionOf(LabeledWord labeledWord)
	{
		String originalText = labeledWord.word();
		return labeledWord.tag().value().equals(SUBORDIATING_CONJUNCTION) && isNotBlank(originalText)
				&& originalText.equalsIgnoreCase("of");
	}

	public static boolean isGroupOfPropositionPhrase(Tree tree)
	{
		boolean hasNounPhrase = tree.getChildrenAsList().stream().anyMatch(NlpUtils::isTagNounPhrase);
		boolean hasPropositionOf = tree.getChildrenAsList().stream()
				.anyMatch(child -> isPropositionOf(child.labeledYield().get(0)));

		return isTagProposition(tree) && hasNounPhrase && hasPropositionOf;
	}

	public static boolean isGroupInPropositionPhrase(Tree tree)
	{
		boolean hasNounPhrase = tree.getChildrenAsList().stream().anyMatch(NlpUtils::isTagNounPhrase);
		boolean hasPropositionIn = tree.getChildrenAsList().stream()
				.anyMatch(child -> isPropositionIn(child.labeledYield().get(0)));

		return isTagProposition(tree) && hasNounPhrase && hasPropositionIn;
	}

	/**
	 * Get the last level Node that contains the tag information and original text
	 *
	 * @param tree
	 * @return
	 */
	public static List<Tree> getLastLevelNodes(Tree tree)
	{
		return tree.getLeaves().stream().map(leaf -> leaf.parent(tree)).collect(Collectors.toList());
	}

	/**
	 * Get the last level Noun node that contains the tag information and original text
	 *
	 * @param tree
	 * @return
	 */
	public static List<Tree> getLastLevelNounNodes(Tree tree)
	{
		return tree.getLeaves().stream().map(leaf -> leaf.parent(tree)).filter(NlpUtils::isTagNoun)
				.collect(Collectors.toList());
	}
}

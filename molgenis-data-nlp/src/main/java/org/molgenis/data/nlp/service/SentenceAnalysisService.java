package org.molgenis.data.nlp.service;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.molgenis.data.nlp.beans.Phrase;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class SentenceAnalysisService
{
	private final ExtractNounPhraseService extractNounPhraseService;
	private final ExtractVerbPhraseService extractVerbPhraseService;

	public SentenceAnalysisService()
	{
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		this.extractNounPhraseService = new ExtractNounPhraseService(pipeline);
		this.extractVerbPhraseService = new ExtractVerbPhraseService(pipeline);
	}

	public List<Phrase> getPhrases(String text)
	{
		List<Phrase> collect = Stream.concat(extractNounPhraseService.getPhrases(text).stream(),
				extractVerbPhraseService.getPhrases(text).stream()).collect(Collectors.toList());
		return collect;
	}

	public static void main(String[] args) throws IOException
	{
		SentenceAnalysisService converter = new SentenceAnalysisService();
		String string = "Indicator of whether the participant currently uses blood glucose lowering medication.";
		List<Phrase> nounPhrases = converter.getPhrases(string);
		System.out.format("The input: %s%nThe phrases: %s%n%n", string, nounPhrases);
	}
}

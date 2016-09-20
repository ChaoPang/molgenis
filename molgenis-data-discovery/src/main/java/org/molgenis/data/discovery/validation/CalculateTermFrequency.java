package org.molgenis.data.discovery.validation;

import static org.molgenis.ontology.ic.TermFrequencyEntityMetaData.FREQUENCY;
import static org.molgenis.ontology.ic.TermFrequencyEntityMetaData.OCCURRENCE;
import static org.molgenis.ontology.ic.TermFrequencyEntityMetaData.TERM;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.csv.CsvWriter;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.data.support.MapEntity;
import org.molgenis.ontology.utils.Stemmer;

public class CalculateTermFrequency
{
	public static void main(String args[]) throws IOException
	{
		File file = new File("/Users/chaopang/Desktop/MaelstromVariables.csv");
		File outputFile = new File("/Users/chaopang/Desktop/TermFrequency.csv");
		List<String> readLines = FileUtils.readLines(file);
		CsvWriter csvWriter = new CsvWriter(outputFile);
		csvWriter.writeAttributeNames(Arrays.asList(TERM, FREQUENCY, OCCURRENCE));
		int count = readLines.size();
		Map<String, Integer> termFrequency = new HashMap<>();
		for (String line : readLines)
		{
			List<String> splitIntoTerms = SemanticSearchServiceUtils.splitIntoTerms(line).stream()
					.map(StringUtils::trim).map(Stemmer::stem).filter(StringUtils::isNotBlank)
					.filter(term -> !term.matches("\\d*")).collect(Collectors.toList());

			for (String term : splitIntoTerms)
			{
				if (!termFrequency.containsKey(term))
				{
					termFrequency.put(term, 0);
				}
				termFrequency.put(term, termFrequency.get(term) + 1);
			}
		}

		for (Entry<String, Integer> entrySet : termFrequency.entrySet())
		{
			String term = entrySet.getKey();
			Integer occurrence = entrySet.getValue();
			float frequency = (float) Math.log10((double) count / occurrence);
			MapEntity entity = new MapEntity();
			entity.set(TERM, term);
			entity.set(FREQUENCY, frequency);
			entity.set(OCCURRENCE, occurrence);
			csvWriter.add(entity);
		}

		csvWriter.close();
	}
}

package org.molgenis.data.semanticsearch.string;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.CandidateOntologyTerm;

import static java.util.Objects.requireNonNull;

public class OntologyTermComparator implements Comparator<Hit<CandidateOntologyTerm>>
{
	private final Stemmer stemmer;

	public OntologyTermComparator(Stemmer stemmer)
	{
		this.stemmer = requireNonNull(stemmer);
	}

	@Override
	public int compare(Hit<CandidateOntologyTerm> o2, Hit<CandidateOntologyTerm> o1)
	{
		String synonym1 = o1.getResult().getMatchedSynonym();
		String synonym2 = o2.getResult().getMatchedSynonym();

		float score1 = o1.getScore();
		float score2 = o2.getScore();

		int compare = Float.compare(score1, score2);

		// two elements have the same score, they need to be sorted according to other standards
		if (compare == 0)
		{
			if (synonymEquals(synonym1, synonym2))
			{
				// if the next ontologyterm is matched based on its label rather than any of the synonyms, the
				// order of the next ontologyterm should be higher than the previous one
				if (!isOntologyTermNameMatched(o1) && isOntologyTermNameMatched(o2))
				{
					return -1;
				}

				// if both of the ontology terms are matched based on one of the synonyms rather than the name,
				// the order of these two elements need to be re-sorted based on the synonym information content
				if (!isOntologyTermNameMatched(o1) && !isOntologyTermNameMatched(o2))
				{
					float informationContent1 = calculateInformationContent(synonym1,
							o1.getResult().getOntologyTerm().getSynonyms());
					float informationContent2 = calculateInformationContent(synonym2,
							o2.getResult().getOntologyTerm().getSynonyms());
					return Float.compare(informationContent1, informationContent2);
				}
			}
		}
		return compare;
	}

	float calculateInformationContent(String bestMatchingSynonym, List<String> synonyms)
	{
		int count = 0;
		String joinedSynonym = StringUtils.join(synonyms, StringUtils.EMPTY).toLowerCase();
		Pattern pattern = Pattern.compile(Pattern.quote(bestMatchingSynonym.toLowerCase()));
		Matcher matcher = pattern.matcher(joinedSynonym);
		while (matcher.find())
		{
			count++;
		}
		float contributedLength = count * bestMatchingSynonym.length();
		return contributedLength / joinedSynonym.length();
	}

	boolean synonymEquals(String synonym1, String synonym2)
	{
		return synonym1.equals(synonym2)
				|| stemmer.cleanStemPhrase(synonym1).equalsIgnoreCase(stemmer.cleanStemPhrase(synonym2));
	}

	boolean isOntologyTermNameMatched(Hit<CandidateOntologyTerm> hit)
	{
		return hit.getResult().getOntologyTerm().getLabel().equalsIgnoreCase(hit.getResult().getMatchedSynonym());
	}
}
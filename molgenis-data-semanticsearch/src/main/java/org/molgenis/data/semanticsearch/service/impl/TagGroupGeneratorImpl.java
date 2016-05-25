package org.molgenis.data.semanticsearch.service.impl;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.spell.StringDistance;
import org.elasticsearch.common.base.Joiner;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.semanticsearch.utils.OntologyTermComparator;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.Stemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import static java.util.Objects.requireNonNull;

public class TagGroupGeneratorImpl implements TagGroupGenerator
{
	private static final Logger LOG = LoggerFactory.getLogger(TagGroupGeneratorImpl.class);

	private final OntologyService ontologyService;

	public final static int MAX_NUM_TAGS = 50;
	private final static String ILLEGAL_CHARS_REGEX = "[^\\p{L}'a-zA-Z0-9\\.~]+";
	private Joiner termJoiner = Joiner.on(' ');

	@Autowired
	public TagGroupGeneratorImpl(OntologyService ontologyService)
	{
		this.ontologyService = requireNonNull(ontologyService);
	}

	@Override
	public List<TagGroup> findTagGroups(String queryString, List<String> ontologyIds)
	{
		Set<String> queryWords = splitRemoveStopWords(queryString);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findTagGroups({},{},{})", ontologyIds, queryWords, MAX_NUM_TAGS);
		}

		List<OntologyTerm> relevantOntologyTerms = ontologyService.findOntologyTerms(ontologyIds, queryWords,
				MAX_NUM_TAGS);

		List<TagGroup> orderOntologyTermHits = Lists
				.newArrayList(applyTagMatchingCriteria(relevantOntologyTerms, queryWords));

		orderOntologyTermHits.addAll(matchCommonWordsToOntologyTerms(queryWords, ontologyIds, orderOntologyTermHits));

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", orderOntologyTermHits);
		}

		List<TagGroup> ontologyTermHit = generateTagGroups(queryWords, orderOntologyTermHits);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("OntologyTermHit: {}", ontologyTermHit);
		}

		return ontologyTermHit;
	}

	/**
	 * Finds the best combinations of @{link OntologyTerm}s based on the given search terms
	 * 
	 * @param queryWords
	 * @param relevantOntologyTerms
	 * @return
	 */
	@Override
	public List<TagGroup> generateTagGroups(Set<String> queryWords, List<TagGroup> relevantTagGroups)
	{
		relevantTagGroups.sort(new OntologyTermComparator());

		List<TagGroup> allTagGroups = new ArrayList<>();

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Hits: {}", relevantTagGroups);
		}

		while (relevantTagGroups.size() > 0)
		{
			// 1. Create a list of ontology term candidates with the best matching synonym known
			// 2. Loop through the list of candidates and collect all the possible candidates (all best combinations of
			// ontology terms)
			// 3. Compute a list of possible ontology terms.
			Multimap<String, OntologyTerm> ontologyTermGroups = LinkedHashMultimap.create();

			for (TagGroup tagGroup : ImmutableList.copyOf(relevantTagGroups))
			{
				if (ontologyTermGroups.size() == 0)
				{
					ontologyTermGroups.put(tagGroup.getMatchedWords(), tagGroup.getOntologyTerm());
				}
				else
				{
					if (ontologyTermGroups.containsKey(tagGroup.getMatchedWords()))
					{
						ontologyTermGroups.put(tagGroup.getMatchedWords(), tagGroup.getOntologyTerm());
					}
					else
					{
						Set<String> involvedSynonyms = ontologyTermGroups.keys().elementSet();
						Set<String> jointTerms = Sets.union(involvedSynonyms,
								splitRemoveStopWords(tagGroup.getMatchedWords()));
						float previousScore = round(distanceFrom(termJoiner.join(involvedSynonyms), queryWords));
						float joinedScore = round(distanceFrom(termJoiner.join(jointTerms), queryWords));
						if (joinedScore > previousScore)
						{
							ontologyTermGroups.put(tagGroup.getMatchedWords(), tagGroup.getOntologyTerm());
						}
					}
				}
			}

			String joinedSynonym = termJoiner.join(ontologyTermGroups.keySet());
			float newScore = round(distanceFrom(joinedSynonym, queryWords));

			// If the new tag group is as of good quality as the previous one, then we add this new
			// tag group to the list
			if (allTagGroups.size() == 0 || (allTagGroups.size() < 3 && newScore >= allTagGroups.get(0).getScore()))
			{
				List<TagGroup> newTagGroups = createTagGroups(ontologyTermGroups).stream()
						.map(ontologyTerm -> TagGroup.create(ontologyTerm, joinedSynonym, newScore)).collect(toList());

				allTagGroups.addAll(newTagGroups);

				// Remove the ontology term hits that have been stored in the potential combination map
				relevantTagGroups = relevantTagGroups.stream()
						.filter(hit -> !ontologyTermGroups.containsValue(hit.getOntologyTerm()))
						.collect(Collectors.toList());
			}
			else break;
		}

		if (LOG.isDebugEnabled())
		{
			LOG.debug("result: {}", allTagGroups);
		}

		return allTagGroups;
	}

	@Override
	public List<TagGroup> applyTagMatchingCriteria(List<OntologyTerm> relevantOntologyTerms, Set<String> searchWords)
	{
		if (relevantOntologyTerms.size() > 0)
		{
			Set<String> stemmedSearchTerms = searchWords.stream().map(Stemmer::stem).collect(Collectors.toSet());

			List<TagGroup> orderedIndividualOntologyTermHits = relevantOntologyTerms.stream()
					.filter(ontologyTerm -> matchingCriteria(stemmedSearchTerms, ontologyTerm))
					.map(ontologyTerm -> createTagGroup(stemmedSearchTerms, ontologyTerm))
					.sorted(new OntologyTermComparator()).collect(Collectors.toList());

			return orderedIndividualOntologyTermHits;
		}

		return Collections.emptyList();
	}

	/**
	 * All the words from the synonym
	 * 
	 * @param keywordsFromAttribute
	 * @param ontologyTerm
	 * @return
	 */
	private boolean matchingCriteria(Set<String> keywordsFromAttribute, OntologyTerm ontologyTerm)
	{
		Set<String> ontologyTermSynonyms = collectLowerCaseTerms(ontologyTerm);
		for (String synonym : ontologyTermSynonyms)
		{
			Set<String> splitIntoTerms = splitRemoveStopWords(synonym).stream().map(Stemmer::stem).collect(toSet());
			if (splitIntoTerms.size() != 0 && keywordsFromAttribute.containsAll(splitIntoTerms)) return true;
		}
		return false;
	}

	/**
	 * This method finds the common query words that are not matched to any of the relevant ontology terms yet. When a
	 * big ontology e.g. UMLS is used for tagging attributes, the top ontology terms are usually matched to the
	 * important words, not the common words. For example 'History of hypertension' may result in the list of relevant
	 * ontology terms where the first 100 ontology terms are matched on the word hypertension.
	 * 
	 * @param queryWords
	 * @param ontologyIds
	 * @param relevantOntologyTerms
	 * @return
	 */
	private List<TagGroup> matchCommonWordsToOntologyTerms(Set<String> queryWords, List<String> ontologyIds,
			List<TagGroup> relevantOntologyTerms)
	{
		if (relevantOntologyTerms.size() > 0 && queryWords.size() > 0)
		{
			String joinedMatchedSynonyms = termJoiner
					.join(relevantOntologyTerms.stream().map(hit -> hit.getMatchedWords()).collect(toSet()));

			Set<String> joinedSynonymStemmedWords = Stemmer.splitAndStem(joinedMatchedSynonyms);

			Set<String> commonQueryWords = queryWords.stream().map(Stemmer::stem)
					.filter(w -> isNotBlank(w) && !joinedSynonymStemmedWords.contains(w)).collect(toSet());

			List<OntologyTerm> additionalOntologyTerms = ontologyService.findOntologyTerms(ontologyIds,
					commonQueryWords, MAX_NUM_TAGS);

			if (additionalOntologyTerms.size() > 0)
			{
				return applyTagMatchingCriteria(additionalOntologyTerms, commonQueryWords);
			}
		}

		return emptyList();
	}

	private TagGroup createTagGroup(Set<String> stemmedSearchTerms, OntologyTerm ontologyTerm)
	{
		Hit<String> bestMatchingSynonym = bestMatchingSynonym(ontologyTerm, stemmedSearchTerms);
		return TagGroup.create(ontologyTerm, bestMatchingSynonym.getResult(), bestMatchingSynonym.getScore());
	}

	List<OntologyTerm> createTagGroups(Multimap<String, OntologyTerm> candidates)
	{
		List<OntologyTerm> ontologyTerms = new ArrayList<>();
		for (Entry<String, Collection<OntologyTerm>> entry : candidates.asMap().entrySet())
		{
			if (ontologyTerms.size() == 0)
			{
				ontologyTerms.addAll(entry.getValue());
			}
			else
			{
				// the pairwise combinations of any sets of ontology terms
				ontologyTerms = ontologyTerms.stream()
						.flatMap(ot1 -> entry.getValue().stream().map(ot2 -> OntologyTerm.and(ot1, ot2)))
						.collect(Collectors.toList());
			}
		}
		return ontologyTerms;
	}

	/**
	 * Computes the best matching synonym which is closest to a set of search terms.<br/>
	 * Will stem the {@link OntologyTerm} 's synonyms and the search terms, and then compute the maximum
	 * {@link StringDistance} between them. 0 means disjunct, 1 means identical
	 * 
	 * @param ontologyTerm
	 *            the {@link OntologyTerm}
	 * @param searchTerms
	 *            the search terms
	 * @return the maximum {@link StringDistance} between the ontologyterm and the search terms
	 */
	Hit<String> bestMatchingSynonym(OntologyTerm ontologyTerm, Set<String> searchTerms)
	{
		List<Hit<String>> collect = collectLowerCaseTerms(ontologyTerm).stream()
				.map(synonym -> Hit.create(synonym, distanceFrom(synonym, searchTerms))).collect(Collectors.toList());
		for (Hit<String> hit : collect)
		{
			Set<String> synoymTokens = Stemmer.splitAndStem(hit.getResult());
			if (searchTerms.containsAll(synoymTokens)) return hit;
		}
		return collect.get(0);
	}

	float distanceFrom(String synonym, Set<String> searchTerms)
	{
		String s1 = Stemmer.stemAndJoin(splitRemoveStopWords(synonym));
		String s2 = Stemmer.stemAndJoin(searchTerms);
		float distance = (float) stringMatching(s1, s2) / 100;
		LOG.debug("Similarity between: {} and {} is {}", s1, s2, distance);
		return distance;
	}

	Set<String> splitRemoveStopWords(String description)
	{
		return newLinkedHashSet(stream(description.split(ILLEGAL_CHARS_REGEX)).map(StringUtils::lowerCase)
				.filter(w -> !STOPWORDSLIST.contains(w) && isNotBlank(w)).collect(toList()));
	}

	Set<String> collectLowerCaseTerms(OntologyTerm ontologyTerm)
	{
		Set<String> allTerms = Sets.newLinkedHashSet();
		allTerms.addAll(ontologyTerm.getSynonyms().stream().map(StringUtils::lowerCase).collect(Collectors.toList()));
		allTerms.add(ontologyTerm.getLabel().toLowerCase());
		return allTerms;
	}

	float round(float score)
	{
		return Math.round(score * 100000) / 100000.0f;
	}
}
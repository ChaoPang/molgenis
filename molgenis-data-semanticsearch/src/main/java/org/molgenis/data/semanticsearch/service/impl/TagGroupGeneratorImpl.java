package org.molgenis.data.semanticsearch.service.impl;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.splitIntoTerms;
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
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.spell.StringDistance;
import org.elasticsearch.common.base.Joiner;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semantic.Relation;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
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
	private final OntologyTagService ontologyTagService;

	public final static int MAX_NUM_TAGS = 50;
	private final static String ILLEGAL_CHARS_REGEX = "[^\\p{L}'a-zA-Z0-9\\.~]+";
	private Joiner termJoiner = Joiner.on(' ');

	@Autowired
	public TagGroupGeneratorImpl(OntologyService ontologyService, OntologyTagService ontologyTagService)
	{
		this.ontologyService = requireNonNull(ontologyService);
		this.ontologyTagService = requireNonNull(ontologyTagService);
	}

	@Override
	public List<OntologyTermHit> findTagGroups(AttributeMetaData attributeMetaData, EntityMetaData entityMetadata,
			Set<String> queryWords, List<String> ontologyIds)
	{
		if (entityMetadata != null)
		{
			Multimap<Relation, OntologyTerm> tagsForAttribute = ontologyTagService.getTagsForAttribute(entityMetadata,
					attributeMetaData);
			if (!tagsForAttribute.isEmpty())
			{
				return tagsForAttribute.values().stream()
						.map(ot -> OntologyTermHit.create(ot, ot.getLabel(), ot.getLabel(), 1.0f)).collect(toList());
			}
		}

		List<OntologyTerm> relevantOntologyTerms;
		// If the user search query is not empty, then it overrules the existing tags
		if (queryWords != null && !queryWords.isEmpty())
		{
			Set<String> escapedSearchTerms = queryWords.stream().filter(StringUtils::isNotBlank)
					.map(QueryParser::escape).collect(toSet());
			relevantOntologyTerms = ontologyService.findExcatOntologyTerms(ontologyIds, escapedSearchTerms,
					MAX_NUM_TAGS);
		}
		else
		{
			queryWords = splitRemoveStopWords(attributeMetaData.getDescription() == null ? attributeMetaData.getLabel()
					: attributeMetaData.getDescription());
			relevantOntologyTerms = ontologyService.findOntologyTerms(ontologyIds, queryWords, MAX_NUM_TAGS);
		}

		List<OntologyTermHit> orderOntologyTerms = Lists
				.newArrayList(applyTagMatchingCriteria(relevantOntologyTerms, queryWords));

		orderOntologyTerms.addAll(matchCommonWordsToOntologyTerms(queryWords, ontologyIds, orderOntologyTerms));

		List<OntologyTermHit> ontologyTermHits = generateTagGroups(queryWords, orderOntologyTerms);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", ontologyTermHits);
		}

		return ontologyTermHits;
	}

	@Override
	public List<OntologyTermHit> findTagGroups(String queryString, List<String> ontologyIds)
	{
		Set<String> queryWords = splitRemoveStopWords(queryString);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findTagGroups({},{},{})", ontologyIds, queryWords, MAX_NUM_TAGS);
		}

		List<OntologyTerm> relevantOntologyTerms = ontologyService.findOntologyTerms(ontologyIds, queryWords,
				MAX_NUM_TAGS);

		List<OntologyTermHit> orderOntologyTermHits = Lists
				.newArrayList(applyTagMatchingCriteria(relevantOntologyTerms, queryWords));

		orderOntologyTermHits.addAll(matchCommonWordsToOntologyTerms(queryWords, ontologyIds, orderOntologyTermHits));

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", orderOntologyTermHits);
		}

		List<OntologyTermHit> ontologyTermHit = generateTagGroups(queryWords, orderOntologyTermHits);

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
	public List<OntologyTermHit> generateTagGroups(Set<String> queryWords,
			List<OntologyTermHit> relevantOntologyTermHits)
	{
		relevantOntologyTermHits.sort(new OntologyTermComparator());

		List<OntologyTermHit> allTagGroups = new ArrayList<>();

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Hits: {}", relevantOntologyTermHits);
		}

		while (relevantOntologyTermHits.size() > 0)
		{
			// 1. Create a list of ontology term candidates with the best matching synonym known
			// 2. Loop through the list of candidates and collect all the possible candidates (all best combinations of
			// ontology terms)
			// 3. Compute a list of possible ontology terms.
			Multimap<String, OntologyTerm> ontologyTermGroups = LinkedHashMultimap.create();

			for (OntologyTermHit ontologyTermHit : ImmutableList.copyOf(relevantOntologyTermHits))
			{
				if (ontologyTermGroups.size() == 0)
				{
					ontologyTermGroups.put(ontologyTermHit.getJoinedSynonym(), ontologyTermHit.getOntologyTerm());
				}
				else
				{
					if (ontologyTermGroups.containsKey(ontologyTermHit.getJoinedSynonym()))
					{
						ontologyTermGroups.put(ontologyTermHit.getJoinedSynonym(), ontologyTermHit.getOntologyTerm());
					}
					else
					{
						Set<String> involvedSynonyms = ontologyTermGroups.keys().elementSet();
						Set<String> jointTerms = Sets.union(involvedSynonyms,
								splitRemoveStopWords(ontologyTermHit.getJoinedSynonym()));
						float previousScore = round(distanceFrom(termJoiner.join(involvedSynonyms), queryWords));
						float joinedScore = round(distanceFrom(termJoiner.join(jointTerms), queryWords));
						if (joinedScore > previousScore)
						{
							ontologyTermGroups.put(ontologyTermHit.getJoinedSynonym(),
									ontologyTermHit.getOntologyTerm());
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
				String matchedWords = termJoiner.join(getMatchedWords(termJoiner.join(queryWords), joinedSynonym));
				List<OntologyTermHit> newTagGroups = createTagGroups(ontologyTermGroups).stream().map(
						ontologyTerm -> OntologyTermHit.create(ontologyTerm, joinedSynonym, matchedWords, newScore))
						.collect(toList());

				allTagGroups.addAll(newTagGroups);

				// Remove the ontology term hits that have been stored in the potential combination map
				relevantOntologyTermHits = relevantOntologyTermHits.stream()
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
	public List<OntologyTermHit> applyTagMatchingCriteria(List<OntologyTerm> relevantOntologyTerms,
			Set<String> searchTerms)
	{
		if (relevantOntologyTerms.size() > 0)
		{
			Set<String> stemmedSearchTerms = searchTerms.stream().map(Stemmer::stem).collect(Collectors.toSet());

			List<OntologyTermHit> orderedIndividualOntologyTermHits = relevantOntologyTerms.stream()
					.filter(ontologyTerm -> matchingCriteria(stemmedSearchTerms, ontologyTerm))
					.map(ontologyTerm -> createOntologyTermHit(stemmedSearchTerms, ontologyTerm))
					.sorted(new OntologyTermComparator()).collect(Collectors.toList());

			return orderedIndividualOntologyTermHits;
		}

		return Collections.emptyList();
	}

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
	private List<OntologyTermHit> matchCommonWordsToOntologyTerms(Set<String> queryWords, List<String> ontologyIds,
			List<OntologyTermHit> relevantOntologyTerms)
	{
		if (relevantOntologyTerms.size() > 0 && queryWords.size() > 0)
		{
			String joinedMatchedSynonyms = termJoiner
					.join(relevantOntologyTerms.stream().map(hit -> hit.getJoinedSynonym()).collect(toSet()));

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

	private OntologyTermHit createOntologyTermHit(Set<String> stemmedSearchTerms, OntologyTerm ontologyTerm)
	{
		Hit<String> bestMatchingSynonym = bestMatchingSynonym(ontologyTerm, stemmedSearchTerms);
		Set<String> matchedWords = getMatchedWords(termJoiner.join(stemmedSearchTerms),
				bestMatchingSynonym.getResult());
		return OntologyTermHit.create(ontologyTerm, bestMatchingSynonym.getResult(), termJoiner.join(matchedWords),
				bestMatchingSynonym.getScore());
	}

	private Set<String> getMatchedWords(String string1, String string2)
	{
		Set<String> stemmedWords = Stemmer.splitAndStem(string1);
		Set<String> matchedWords = Sets.newLinkedHashSet(splitIntoTerms(string2).stream()
				.filter(word -> stemmedWords.contains(Stemmer.stem(word))).collect(Collectors.toList()));
		return matchedWords;
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
package org.molgenis.data.semanticsearch.service.impl;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.spell.StringDistance;
import org.elasticsearch.common.base.Joiner;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.explain.criteria.MatchingCriterion;
import org.molgenis.data.semanticsearch.explain.criteria.impl.StrictMatchingCriterion;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.semanticsearch.utils.OntologyTermHitComparator;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.Stemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.google.common.collect.Sets.union;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils.collectLowerCaseTerms;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.cleanStemPhrase;

public class TagGroupGeneratorImpl implements TagGroupGenerator
{
	private static final Logger LOG = LoggerFactory.getLogger(TagGroupGeneratorImpl.class);

	private final OntologyService ontologyService;

	public final static int MAX_NUM_TAGS = 3000;
	public final static MatchingCriterion STRICT_MATCHING_CRITERION = new StrictMatchingCriterion();
	private final static String ILLEGAL_CHARS_REGEX = "[^\\p{L}'a-zA-Z0-9\\.~]+";
	private Joiner termJoiner = Joiner.on(' ');

	@Autowired
	public TagGroupGeneratorImpl(OntologyService ontologyService)
	{
		this.ontologyService = requireNonNull(ontologyService);
	}

	@Override
	public List<TagGroup> generateTagGroups(String queryString, List<String> ontologyIds)
	{
		List<SemanticType> globalKeyConcepts = ontologyService.getAllSemanticTypes().stream()
				.filter(SemanticType::isGlobalKeyConcept).collect(Collectors.toList());

		return generateTagGroups(queryString, ontologyIds, globalKeyConcepts);
	}

	@Override
	public List<TagGroup> generateTagGroups(String queryString, List<String> ontologyIds,
			List<SemanticType> globalKeyConcepts)
	{
		Set<String> queryWords = removeIllegalCharactersAndStopWords(queryString);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findTagGroups({},{},{})", ontologyIds, queryWords, MAX_NUM_TAGS);
		}

		List<OntologyTerm> relevantOntologyTerms = ontologyService
				.findOntologyTerms(ontologyIds, queryWords, MAX_NUM_TAGS);

		List<OntologyTermHit> candidateTagGroups = applyTagMatchingCriterion(relevantOntologyTerms, queryWords,
				STRICT_MATCHING_CRITERION).stream()
				.filter(ontologyTermHit -> containsKeyConcepts(globalKeyConcepts, ontologyTermHit)).collect(toList());

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", candidateTagGroups);
		}

		List<TagGroup> tagGroups = combineTagGroups(queryWords, candidateTagGroups);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("OntologyTermHit: {}", tagGroups);
		}

		return tagGroups;
	}

	/**
	 * Finds the best combinations of @{link OntologyTerm}s based on the given search terms
	 *
	 * @param queryWords
	 * @param ontologyTermHits
	 * @return
	 */
	@Override
	public List<TagGroup> combineTagGroups(Set<String> queryWords, List<OntologyTermHit> ontologyTermHits)
	{
		ontologyTermHits.sort(new OntologyTermHitComparator());

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Hits: {}", ontologyTermHits);
		}
		List<TagGroup> combinedTagGroups = new ArrayList<>();

		Multimap<String, OntologyTermHit> ontologyTermGroups = LinkedHashMultimap.create();
		ontologyTermHits.stream().forEach(hit -> ontologyTermGroups.put(hit.getMatchedWords(), hit));

		List<String> matchedWords = newArrayList(ontologyTermGroups.keySet());
		Set<String> combinedGroups = new HashSet<>();

		// 1. Create a list of ontology term candidates with the best matching synonym known
		// 2. Loop through the list of candidates and collect all the possible candidates (all best combinations of
		// ontology terms)
		// 3. Compute a list of possible ontology terms.
		for (int i = 0; i < matchedWords.size(); i++)
		{
			if (!combinedGroups.contains(matchedWords.get(i)))
			{
				Multimap<String, OntologyTermHit> ontologyTermsToCombine = LinkedHashMultimap.create();
				ontologyTermsToCombine.putAll(matchedWords.get(i), ontologyTermGroups.get(matchedWords.get(i)));

				for (int j = 0; j < matchedWords.size(); j++)
				{
					if (matchedWords.get(i).equals(matchedWords.get(j))) continue;

					String previousJoinedTerm = termJoiner.join(ontologyTermsToCombine.keySet());
					Set<String> previousJoinedMatchedWords = removeIllegalCharactersAndStopWords(previousJoinedTerm);
					Set<String> nextMatchedWords = removeIllegalCharactersAndStopWords(matchedWords.get(j));

					String temporaryJoinedTerm = termJoiner.join(union(previousJoinedMatchedWords, nextMatchedWords));

					float currentScore = round(distanceFrom(matchedWords.get(j), queryWords));
					float previousScore = round(distanceFrom(previousJoinedTerm, queryWords));
					float joinedScore = round(distanceFrom(temporaryJoinedTerm, queryWords));

					if (joinedScore >= previousScore && joinedScore >= currentScore)
					{
						ontologyTermsToCombine.putAll(matchedWords.get(j), ontologyTermGroups.get(matchedWords.get(j)));
					}
				}

				combinedGroups.addAll(ontologyTermsToCombine.keySet());

				String joinedSynonym = termJoiner.join(ontologyTermsToCombine.keySet());
				float newScore = round(distanceFrom(joinedSynonym, queryWords));
				float upperBound = combinedTagGroups.isEmpty() ? 0.0f : combinedTagGroups.get(0).getScore() * 0.8f;
				if (newScore >= upperBound)
				{
					List<TagGroup> newTagGroups = createTagGroups(ontologyTermsToCombine).stream()
							.map(list -> TagGroup.create(list, joinedSynonym, newScore)).collect(toList());
					combinedTagGroups.addAll(newTagGroups);
				}
				else break;
			}
		}

		return combinedTagGroups.stream().limit(20).collect(toList());
	}

	@Override
	public List<OntologyTermHit> applyTagMatchingCriterion(List<OntologyTerm> relevantOntologyTerms,
			Set<String> queryWords, MatchingCriterion matchingCriterion)
	{
		if (relevantOntologyTerms.size() > 0)
		{
			Set<String> stemmedSearchTerms = queryWords.stream().map(Stemmer::stem).collect(toSet());

			List<OntologyTermHit> orderedIndividualOntologyTermHits = relevantOntologyTerms.stream()
					.filter(ontologyTerm -> matchingCriterion.apply(stemmedSearchTerms, ontologyTerm))
					.map(ontologyTerm -> createTagGroup(stemmedSearchTerms, ontologyTerm))
					.sorted(new OntologyTermHitComparator()).collect(toList());

			// Remove the low ranking ontologyterms that are the parents of the high ranking ontologyterms
			List<OntologyTermHit> copyOfOntologyTermHits = newArrayList(orderedIndividualOntologyTermHits);
			for (int i = orderedIndividualOntologyTermHits.size() - 1; i > 1; i--)
			{
				OntologyTerm lowRankingOntologyTerm = orderedIndividualOntologyTermHits.get(i).getOntologyTerm();

				for (int j = i - 1; j > 0; j--)
				{
					OntologyTerm highRankingOntologyTerm = orderedIndividualOntologyTermHits.get(j).getOntologyTerm();

					if (ontologyService.isDescendant(highRankingOntologyTerm, lowRankingOntologyTerm))
					{
						copyOfOntologyTermHits.remove(orderedIndividualOntologyTermHits.get(i));
					}
				}
			}

			return copyOfOntologyTermHits;
		}
		return emptyList();
	}

	private OntologyTermHit createTagGroup(Set<String> stemmedSearchTerms, OntologyTerm ontologyTerm)
	{
		Hit<String> bestMatchingSynonym = bestMatchingSynonym(ontologyTerm, stemmedSearchTerms);
		return OntologyTermHit
				.create(ontologyTerm, cleanStemPhrase(bestMatchingSynonym.getResult()), bestMatchingSynonym.getScore());
	}

	List<List<OntologyTerm>> createTagGroups(Multimap<String, OntologyTermHit> candidates)
	{
		List<List<OntologyTerm>> ontologyTermGroups = new ArrayList<>();

		for (Collection<OntologyTermHit> values : candidates.asMap().values())
		{
			List<OntologyTerm> atomicOntologyTermGroup = values.stream().map(OntologyTermHit::getOntologyTerm)
					.collect(toList());

			if (ontologyTermGroups.isEmpty())
			{
				ontologyTermGroups.addAll(atomicOntologyTermGroup.stream().map(Lists::newArrayList).collect(toList()));
			}
			else
			{
				List<List<OntologyTerm>> tempOntologyTermGroups = new ArrayList<>();

				for (OntologyTerm ontologyTerm : atomicOntologyTermGroup)
				{
					List<List<OntologyTerm>> copyOfOntologyTermGroups = ontologyTermGroups.stream()
							.map(list -> newArrayList(list)).collect(Collectors.toList());

					copyOfOntologyTermGroups.forEach(list -> list.add(ontologyTerm));

					tempOntologyTermGroups.addAll(copyOfOntologyTermGroups);
				}

				ontologyTermGroups = newArrayList(tempOntologyTermGroups);
			}
		}

		return ontologyTermGroups;
	}

	/**
	 * Computes the best matching synonym which is closest to a set of search terms.<br/>
	 * Will stem the {@link OntologyTerm} 's synonyms and the search terms, and then compute the maximum
	 * {@link StringDistance} between them. 0 means disjunct, 1 means identical
	 *
	 * @param ontologyTerm the {@link OntologyTerm}
	 * @param queryWords   the search terms
	 * @return the maximum {@link StringDistance} between the ontologyterm and the search terms
	 */
	Hit<String> bestMatchingSynonym(OntologyTerm ontologyTerm, Set<String> queryWords)
	{
		List<Hit<String>> collect = collectLowerCaseTerms(ontologyTerm).stream()
				.map(term -> termJoiner.join(removeIllegalCharactersAndStopWords(term)))
				.filter(term -> queryWords.containsAll(Stemmer.splitAndStem(term)))
				.map(synonym -> Hit.create(synonym, distanceFrom(synonym, queryWords))).sorted(reverseOrder())
				.collect(toList());

		return collect.get(0);
	}

	float distanceFrom(String term, Set<String> searchTerms)
	{
		String s1 = Stemmer.stemAndJoin(removeIllegalCharactersAndStopWords(term));
		String s2 = Stemmer.stemAndJoin(searchTerms);
		float distance = (float) stringMatching(s1, s2) / 100;
		LOG.debug("Similarity between: {} and {} is {}", s1, s2, distance);
		return distance;
	}

	Set<String> removeIllegalCharactersAndStopWords(String description)
	{
		return newLinkedHashSet(stream(description.split(ILLEGAL_CHARS_REGEX)).map(StringUtils::lowerCase)
				.filter(word -> !STOPWORDSLIST.contains(word)).filter(StringUtils::isNotBlank).collect(toList()));
	}

	float round(float score)
	{
		return Math.round(score * 100000) / 100000.0f;
	}

	private boolean containsKeyConcepts(List<SemanticType> keyConcepts, OntologyTermHit ontologyTermHit)
	{
		if (keyConcepts.isEmpty()) return true;
		return ontologyTermHit.getOntologyTerm().getSemanticTypes().stream().allMatch(keyConcepts::contains);
	}
}
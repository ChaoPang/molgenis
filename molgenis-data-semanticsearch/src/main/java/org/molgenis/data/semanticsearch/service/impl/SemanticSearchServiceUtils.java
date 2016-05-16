package org.molgenis.data.semanticsearch.service.impl;

import static autovalue.shaded.com.google.common.common.collect.Iterables.get;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.lang.Math.pow;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.data.QueryRule.Operator.DIS_MAX;
import static org.molgenis.data.QueryRule.Operator.FUZZY_MATCH;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.DESCRIPTION;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.LABEL;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.cleanStemPhrase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.spell.StringDistance;
import org.elasticsearch.common.base.Joiner;
import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.semantic.Relation;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.string.OntologyTermComparator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OntologyTermChildrenPredicate;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.ic.TermFrequencyService;
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

public class SemanticSearchServiceUtils
{
	private static final float LEXICAL_QUERY_BOOSTVALUE = 2.0f;

	private static final Logger LOG = LoggerFactory.getLogger(SemanticSearchServiceUtils.class);

	private final TermFrequencyService termFrequencyService;
	private final DataService dataService;
	private final OntologyService ontologyService;
	private final OntologyTagService ontologyTagService;

	public static final int MAX_NUM_TAGS = 50;
	public static final String UNIT_ONTOLOGY_IRI = "http://purl.obolibrary.org/obo/uo.owl";

	private final static String CARET_CHARACTER = "^";
	private final static String ESCAPED_CARET_CHARACTER = "\\^";
	private final static String ILLEGAL_CHARS_REGEX = "[^\\p{L}'a-zA-Z0-9\\.~]+";
	private Joiner termJoiner = Joiner.on(' ');

	@Autowired
	public SemanticSearchServiceUtils(DataService dataService, OntologyService ontologyService,
			OntologyTagService ontologyTagService, TermFrequencyService termFrequencyService)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologyService = requireNonNull(ontologyService);
		this.ontologyTagService = requireNonNull(ontologyTagService);
		this.termFrequencyService = requireNonNull(termFrequencyService);
	}

	public List<Hit<OntologyTermHit>> findOntologyTermsForAttr(AttributeMetaData attribute,
			EntityMetaData entityMetadata, Set<String> searchTerms, List<String> ontologyIds)
	{
		if (entityMetadata != null)
		{
			Multimap<Relation, OntologyTerm> tagsForAttribute = ontologyTagService.getTagsForAttribute(entityMetadata,
					attribute);
			if (!tagsForAttribute.isEmpty())
			{
				return tagsForAttribute.values().stream()
						.map(ot -> Hit.create(OntologyTermHit.create(ot, ot.getLabel(), ot.getLabel()), 1.0f))
						.collect(toList());
			}
		}

		List<OntologyTerm> relevantOntologyTerms;
		// If the user search query is not empty, then it overrules the existing tags
		if (searchTerms != null && !searchTerms.isEmpty())
		{
			Set<String> escapedSearchTerms = searchTerms.stream().filter(StringUtils::isNotBlank)
					.map(QueryParser::escape).collect(toSet());
			relevantOntologyTerms = ontologyService.findExcatOntologyTerms(ontologyIds, escapedSearchTerms,
					MAX_NUM_TAGS);
		}
		else
		{
			searchTerms = splitRemoveStopWords(
					attribute.getDescription() == null ? attribute.getLabel() : attribute.getDescription());
			relevantOntologyTerms = ontologyService.findOntologyTerms(ontologyIds, searchTerms, MAX_NUM_TAGS);
		}

		List<Hit<OntologyTermHit>> orderOntologyTerms = Lists
				.newArrayList(filterAndSortOntologyTerms(relevantOntologyTerms, searchTerms));

		orderOntologyTerms.addAll(findOntologyTermsForMissingTerms(searchTerms, ontologyIds, orderOntologyTerms));

		List<Hit<OntologyTermHit>> ontologyTermHits = combineOntologyTerms(searchTerms, orderOntologyTerms);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", ontologyTermHits);
		}

		return ontologyTermHits;
	}

	public List<Hit<OntologyTermHit>> findOntologyTermsForQueryString(String queryString, List<String> ontologyIds)
	{
		Set<String> searchTerms = splitRemoveStopWords(queryString);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findOntologyTermCombination({},{},{})", ontologyIds, searchTerms, MAX_NUM_TAGS);
		}

		List<OntologyTerm> relevantOntologyTerms = ontologyService.findOntologyTerms(ontologyIds, searchTerms,
				MAX_NUM_TAGS);

		List<Hit<OntologyTermHit>> orderOntologyTermHits = Lists
				.newArrayList(filterAndSortOntologyTerms(relevantOntologyTerms, searchTerms));

		orderOntologyTermHits.addAll(findOntologyTermsForMissingTerms(searchTerms, ontologyIds, orderOntologyTermHits));

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", orderOntologyTermHits);
		}

		List<Hit<OntologyTermHit>> ontologyTermHit = combineOntologyTerms(searchTerms, orderOntologyTermHits);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("OntologyTermHit: {}", ontologyTermHit);
		}

		return ontologyTermHit;
	}

	public List<Hit<OntologyTermHit>> filterAndSortOntologyTerms(List<OntologyTerm> relevantOntologyTerms,
			Set<String> searchTerms)
	{
		if (relevantOntologyTerms.size() > 0)
		{
			Set<String> stemmedSearchTerms = searchTerms.stream().map(Stemmer::stem).collect(Collectors.toSet());

			List<Hit<OntologyTermHit>> orderedIndividualOntologyTermHits = relevantOntologyTerms.stream()
					.filter(ontologyTerm -> filterOntologyTerm(stemmedSearchTerms, ontologyTerm))
					.map(ontologyTerm -> createOntologyTermHit(stemmedSearchTerms, ontologyTerm))
					.sorted(new OntologyTermComparator()).collect(Collectors.toList());

			return orderedIndividualOntologyTermHits;
		}

		return Collections.emptyList();
	}

	private Hit<OntologyTermHit> createOntologyTermHit(Set<String> stemmedSearchTerms, OntologyTerm ontologyTerm)
	{
		Hit<String> bestMatchingSynonym = bestMatchingSynonym(ontologyTerm, stemmedSearchTerms);
		Set<String> matchedWords = getMatchedWords(termJoiner.join(stemmedSearchTerms),
				bestMatchingSynonym.getResult());
		OntologyTermHit candidate = OntologyTermHit.create(ontologyTerm, bestMatchingSynonym.getResult(),
				termJoiner.join(matchedWords));
		return Hit.<OntologyTermHit> create(candidate, bestMatchingSynonym.getScore());
	}

	private Set<String> getMatchedWords(String join, String result)
	{
		Set<String> splitAndStem = Stemmer.splitAndStem(join);
		Set<String> splitAndStem2 = Stemmer.splitAndStem(result);
		splitAndStem.retainAll(splitAndStem2);
		return splitAndStem;
	}

	private boolean filterOntologyTerm(Set<String> keywordsFromAttribute, OntologyTerm ontologyTerm)
	{
		Set<String> ontologyTermSynonyms = collectLowerCaseTerms(ontologyTerm);
		for (String synonym : ontologyTermSynonyms)
		{
			Set<String> splitIntoTerms = splitRemoveStopWords(synonym).stream().map(Stemmer::stem).collect(toSet());
			if (splitIntoTerms.size() != 0 && keywordsFromAttribute.containsAll(splitIntoTerms)) return true;
		}
		return false;
	}

	private List<Hit<OntologyTermHit>> findOntologyTermsForMissingTerms(Set<String> searchTerms,
			List<String> ontologyIds, List<Hit<OntologyTermHit>> potentialCombinations)
	{
		if (potentialCombinations.size() > 0 && searchTerms.size() > 0)
		{
			String joinedMatchedSynonyms = termJoiner.join(potentialCombinations.stream()
					.map(hit -> hit.getResult().getJoinedSynonym()).collect(Collectors.toSet()));

			Set<String> joinedSynonymStemmedWords = Stemmer.splitAndStem(joinedMatchedSynonyms);

			Set<String> missingSearchTerms = searchTerms.stream().map(Stemmer::stem)
					.filter(w -> isNotBlank(w) && !joinedSynonymStemmedWords.contains(w)).collect(toSet());

			List<OntologyTerm> ontologyTermsForMissingPart = ontologyService.findOntologyTerms(ontologyIds,
					missingSearchTerms, MAX_NUM_TAGS);

			if (ontologyTermsForMissingPart.size() > 0)
			{
				return filterAndSortOntologyTerms(ontologyTermsForMissingPart, missingSearchTerms);

			}
		}

		return emptyList();
	}

	/**
	 * Finds the best combinations of @{link OntologyTerm}s based on the given search terms
	 * 
	 * @param searchTerms
	 * @param relevantOntologyTerms
	 * @return
	 */
	public List<Hit<OntologyTermHit>> combineOntologyTerms(Set<String> searchTerms,
			List<Hit<OntologyTermHit>> individualOntologyTermHits)
	{
		individualOntologyTermHits.sort(new OntologyTermComparator());

		List<Hit<OntologyTermHit>> combinedOntologyTermHits = new ArrayList<>();

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Hits: {}", individualOntologyTermHits);
		}

		while (individualOntologyTermHits.size() > 0)
		{
			// 1. Create a list of ontology term candidates with the best matching synonym known
			// 2. Loop through the list of candidates and collect all the possible candidates (all best combinations of
			// ontology terms)
			// 3. Compute a list of possible ontology terms.
			Multimap<String, OntologyTerm> potentialCombinations = LinkedHashMultimap.create();

			for (Hit<OntologyTermHit> hit : ImmutableList.copyOf(individualOntologyTermHits))
			{
				OntologyTermHit ontologyTermHit = hit.getResult();

				if (potentialCombinations.size() == 0)
				{
					potentialCombinations.put(ontologyTermHit.getJoinedSynonym(), ontologyTermHit.getOntologyTerm());
				}
				else
				{
					if (potentialCombinations.containsKey(ontologyTermHit.getJoinedSynonym()))
					{
						potentialCombinations.put(ontologyTermHit.getJoinedSynonym(),
								ontologyTermHit.getOntologyTerm());
					}
					else
					{
						Set<String> involvedSynonyms = potentialCombinations.keys().elementSet();
						Set<String> jointTerms = Sets.union(involvedSynonyms,
								splitRemoveStopWords(ontologyTermHit.getJoinedSynonym()));
						float previousScore = round(distanceFrom(termJoiner.join(involvedSynonyms), searchTerms));
						float joinedScore = round(distanceFrom(termJoiner.join(jointTerms), searchTerms));
						if (joinedScore > previousScore)
						{
							potentialCombinations.put(ontologyTermHit.getJoinedSynonym(),
									ontologyTermHit.getOntologyTerm());
						}
					}
				}
			}

			String joinedSynonym = termJoiner.join(potentialCombinations.keySet());
			String matchedWords = termJoiner.join(getMatchedWords(termJoiner.join(searchTerms), joinedSynonym));

			List<Hit<OntologyTermHit>> collect = createOntologyTermPairwiseCombination(potentialCombinations).stream()
					.map(ontologyTerm -> Hit.create(OntologyTermHit.create(ontologyTerm, joinedSynonym, matchedWords),
							distanceFrom(joinedSynonym, searchTerms)))
					.collect(toList());

			// If the new combination of ontology terms is as of good quality as the previous one, then we add this new
			// combination to the list
			if (combinedOntologyTermHits.size() == 0 || (combinedOntologyTermHits.size() < 2
					&& collect.get(0).getScore() >= combinedOntologyTermHits.get(0).getScore()))
			{
				combinedOntologyTermHits.addAll(collect);

				// Remove the ontology term hits that have been stored in the potential combination map
				individualOntologyTermHits = individualOntologyTermHits.stream()
						.filter(hit -> !potentialCombinations.containsValue(hit.getResult().getOntologyTerm()))
						.collect(Collectors.toList());
			}
			else break;
		}

		if (LOG.isDebugEnabled())
		{
			LOG.debug("result: {}", combinedOntologyTermHits);
		}

		return combinedOntologyTermHits;
	}

	List<OntologyTerm> createOntologyTermPairwiseCombination(Multimap<String, OntologyTerm> candidates)
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
	 * A helper function to create a list of queryTerms based on the information from the targetAttribute as well as
	 * user defined searchTerms. If the user defined searchTerms exist, the targetAttribute information will not be
	 * used.
	 * 
	 * @param targetAttribute
	 * @param userQueries
	 * @return list of queryTerms
	 */
	public Set<String> getQueryTermsFromAttribute(AttributeMetaData targetAttribute, Set<String> userQueries)
	{
		Set<String> queryTerms = new HashSet<>();
		if (userQueries != null && !userQueries.isEmpty())
		{
			queryTerms.addAll(userQueries);
		}
		else if (targetAttribute != null)
		{
			if (isNotBlank(targetAttribute.getLabel()))
			{
				queryTerms.add(targetAttribute.getLabel());
			}
			if (isNotBlank(targetAttribute.getDescription()))
			{
				queryTerms.add(targetAttribute.getDescription());
			}
		}
		return queryTerms.stream().map(this::escapeCharsExcludingCaretChar).collect(toSet());
	}

	/**
	 * Create a disMaxJunc query rule based on the given search terms as well as the information from given ontology
	 * terms
	 *
	 * @param ontologyTermHits
	 * @param searchTerms
	 *
	 * @return disMaxJunc queryRule
	 */
	public QueryRule createDisMaxQueryRule(Set<String> searchTerms, List<Hit<OntologyTermHit>> ontologyTermHits,
			QueryExpansionParameter ontologyExpansionParameters)
	{
		List<QueryRule> rules = new ArrayList<>();

		if (searchTerms != null)
		{
			List<String> queryTerms = searchTerms.stream().filter(StringUtils::isNotBlank).map(this::parseQueryString)
					.map(this::boostLexicalQuery).collect(toList());

			QueryRule createDisMaxQueryRuleForTerms = createDisMaxQueryRuleForTerms(queryTerms,
					LEXICAL_QUERY_BOOSTVALUE);

			if (createDisMaxQueryRuleForTerms != null)
			{
				rules.add(createDisMaxQueryRuleForTerms);
			}
		}

		Multimap<String, Hit<OntologyTerm>> combiniationGroupWithSameJoinedSynonym = LinkedHashMultimap.create();

		ontologyTermHits.forEach(hit -> combiniationGroupWithSameJoinedSynonym.put(hit.getResult().getJoinedSynonym(),
				Hit.create(hit.getResult().getOntologyTerm(), hit.getScore())));

		for (String joinedSynonym : combiniationGroupWithSameJoinedSynonym.keySet())
		{
			List<Hit<OntologyTerm>> ontologyTermCombinations = Lists
					.newArrayList(combiniationGroupWithSameJoinedSynonym.get(joinedSynonym));

			QueryRule queryRuleForOntologyTerms = createQueryRuleForOntologyTerms(ontologyTermCombinations,
					ontologyExpansionParameters);

			if (queryRuleForOntologyTerms != null)
			{
				rules.add(queryRuleForOntologyTerms);
			}
		}

		QueryRule disMaxQueryRule = null;
		if (rules.size() > 0)
		{
			disMaxQueryRule = new QueryRule(rules);
			disMaxQueryRule.setOperator(DIS_MAX);
		}

		return disMaxQueryRule;
	}

	/**
	 * Creates a disMax query rule that only contains all information from the ontology terms as well as their children
	 * 
	 * @param ontologyTermHits
	 * @return
	 */
	public QueryRule createQueryRuleForOntologyTerms(List<Hit<OntologyTerm>> ontologyTermHits,
			QueryExpansionParameter ontologyExpansionParameters)
	{
		QueryRule queryRule = null;

		if (ontologyTermHits.size() > 0)
		{
			float score = ontologyTermHits.get(0).getScore();

			// Put ontologyTerms with the same synonym in a map
			Multimap<OntologyTerm, OntologyTerm> ontologyTermGroups = groupOntologyTermsBySynonym(ontologyTermHits);

			Set<OntologyTerm> ontologyTermGroupKeys = ontologyTermGroups.keySet();

			if (ontologyTermGroupKeys.size() > 1)
			{
				Map<OntologyTerm, Float> ontologyTermGroupWeight = calculateBoostValueForOntologyTermGroup(
						ontologyTermGroups);

				Function<OntologyTerm, QueryRule> ontologyTermGroupToQueryRule = groupKey -> {

					List<String> queryTermsFromSameGroup = ontologyTermGroups.get(groupKey).stream()
							.flatMap(ot -> getExpandedQueriesFromOntologyTerm(ot, ontologyExpansionParameters).stream())
							.collect(toList());

					return createDisMaxQueryRuleForTerms(queryTermsFromSameGroup,
							ontologyTermGroupWeight.get(groupKey));
				};

				queryRule = createShouldQueryRule(
						ontologyTermGroupKeys.stream().map(ontologyTermGroupToQueryRule).collect(toList()), score);
			}
			else
			{
				OntologyTerm firstOntologyTermGroupKey = get(ontologyTermGroupKeys, 0);

				List<String> queryTerms = ontologyTermGroups.get(firstOntologyTermGroupKey).stream()
						.flatMap(ot -> getExpandedQueriesFromOntologyTerm(ot, ontologyExpansionParameters).stream())
						.collect(toList());

				queryRule = createDisMaxQueryRuleForTerms(queryTerms, score);
			}
		}

		return queryRule;
	}

	private Map<OntologyTerm, Float> calculateBoostValueForOntologyTermGroup(
			Multimap<OntologyTerm, OntologyTerm> ontologyTermGroups)
	{
		Function<OntologyTerm, Double> groupKeyToGroupWeight = key -> ontologyTermGroups.get(key).stream()
				.map(this::collectLowerCaseTerms).map(this::getBestInverseDocumentFrequency)
				.mapToDouble(tf -> (double) tf).max().orElse(1.0d);

		Map<OntologyTerm, Double> ontologyTermGroupWeight = ontologyTermGroups.keySet().stream()
				.collect(Collectors.toMap(key -> key, groupKeyToGroupWeight));

		double maxIdfValue = ontologyTermGroupWeight.values().stream().mapToDouble(Double::doubleValue).max()
				.orElse(1.0d);

		return ontologyTermGroupWeight.entrySet().stream()
				.collect(toMap(Entry::getKey, e -> new Float(e.getValue() / maxIdfValue)));
	}

	private Multimap<OntologyTerm, OntologyTerm> groupOntologyTermsBySynonym(List<Hit<OntologyTerm>> ontologyTermHits)
	{
		Multimap<OntologyTerm, OntologyTerm> multiMap = LinkedHashMultimap.create();
		ontologyService.getAtomicOntologyTerms(ontologyTermHits.get(0).getResult()).forEach(ot -> multiMap.put(ot, ot));

		ontologyTermHits.stream().skip(1)
				.flatMap(hit -> ontologyService.getAtomicOntologyTerms(hit.getResult()).stream())
				.filter(ot -> !multiMap.containsKey(ot)).forEach(atomicOntologyTerm -> {

					OntologyTerm ontologyTermInTheMap = multiMap.keySet().stream()
							.filter(ot -> hasSameSynonyms(ot, atomicOntologyTerm)).findFirst().orElse(null);

					if (ontologyTermInTheMap != null)
					{
						multiMap.put(ontologyTermInTheMap, atomicOntologyTerm);
					}
				});

		return multiMap;
	}

	/**
	 * Create disMaxJunc query rule based a list of queryTerm. All queryTerms are lower cased and stop words are removed
	 * 
	 * @param queryTerms
	 * @return disMaxJunc queryRule
	 */
	public QueryRule createDisMaxQueryRuleForTerms(List<String> queryTerms, Float boostValue)
	{
		List<QueryRule> rules = new ArrayList<QueryRule>();
		newLinkedHashSet(queryTerms).stream().filter(StringUtils::isNotEmpty).map(this::escapeCharsExcludingCaretChar)
				.forEach(query -> {
					rules.add(new QueryRule(LABEL, FUZZY_MATCH, query));
					rules.add(new QueryRule(DESCRIPTION, FUZZY_MATCH, query));
				});

		QueryRule finalDisMaxQuery = null;
		if (rules.size() > 0)
		{
			finalDisMaxQuery = new QueryRule(rules);
			finalDisMaxQuery.setOperator(Operator.DIS_MAX);
		}

		if (finalDisMaxQuery != null && boostValue != null && boostValue.intValue() != 0)
		{
			finalDisMaxQuery.setValue(boostValue);
		}

		return finalDisMaxQuery;
	}

	public QueryRule createShouldQueryRule(List<QueryRule> queryRules, Float boostValue)
	{
		QueryRule shouldQueryRule = null;
		if (queryRules.size() > 0)
		{
			shouldQueryRule = new QueryRule(new ArrayList<QueryRule>());
			shouldQueryRule.setOperator(Operator.SHOULD);
			shouldQueryRule.getNestedRules().addAll(queryRules);
		}

		if (shouldQueryRule != null && boostValue != null && boostValue > 0)
		{
			shouldQueryRule.setValue(boostValue);
		}

		return shouldQueryRule;
	}

	/**
	 * Create a list of string queries based on the information collected from current ontologyterm including label,
	 * synonyms and child ontologyterms
	 * 
	 * @param ontologyTerm
	 * @return
	 */
	public List<String> getExpandedQueriesFromOntologyTerm(OntologyTerm ontologyTerm,
			QueryExpansionParameter ontologyExpansionParameters)
	{
		List<String> queryTerms = collectLowerCaseTerms(ontologyTerm).stream().map(this::parseQueryString)
				.collect(toList());

		if (ontologyExpansionParameters.isChildExpansionEnabled())
		{
			OntologyTermChildrenPredicate predicate = new OntologyTermChildrenPredicate(
					ontologyExpansionParameters.getExpansionLevel(), false, ontologyService);

			Function<OntologyTerm, Stream<String>> mapChildOntologyTermToQueries = child -> collectLowerCaseTerms(child)
					.stream().map(query -> parseBoostQueryString(query,
							pow(0.5, ontologyService.getOntologyTermDistance(ontologyTerm, child))));

			List<String> queryTermsFromChildOntologyTerms = ontologyService.getChildren(ontologyTerm, predicate)
					.stream().flatMap(mapChildOntologyTermToQueries).collect(Collectors.toList());

			queryTerms.addAll(queryTermsFromChildOntologyTerms);

			List<String> queryTermsFromParentOntologyTerms = ontologyService.getParents(ontologyTerm, predicate)
					.stream().flatMap(mapChildOntologyTermToQueries).collect(Collectors.toList());

			queryTerms.addAll(queryTermsFromParentOntologyTerms);
		}

		return queryTerms;
	}

	/**
	 * A helper function that gets identifiers of all the attributes from one entityMetaData
	 * 
	 * @param sourceEntityMetaData
	 * @return
	 */
	public List<String> getAttributeIdentifiers(EntityMetaData sourceEntityMetaData)
	{
		Entity entityMetaDataEntity = dataService.findOne(EntityMetaDataMetaData.ENTITY_NAME,
				new QueryImpl().eq(EntityMetaDataMetaData.FULL_NAME, sourceEntityMetaData.getName()));

		if (entityMetaDataEntity == null) throw new MolgenisDataAccessException(
				"Could not find EntityMetaDataEntity by the name of " + sourceEntityMetaData.getName());

		List<String> attributeIdentifiers = new ArrayList<String>();

		recursivelyCollectAttributeIdentifiers(entityMetaDataEntity.getEntities(EntityMetaDataMetaData.ATTRIBUTES),
				attributeIdentifiers);

		return attributeIdentifiers;
	}

	public AttributeMetaData entityToAttributeMetaData(Entity attributeEntity, EntityMetaData entityMetaData)
	{
		String attributeName = attributeEntity.getString(AttributeMetaDataMetaData.NAME);
		AttributeMetaData attribute = entityMetaData.getAttribute(attributeName);
		if (attribute == null)
		{
			throw new MolgenisDataAccessException("The attributeMetaData : " + attributeName
					+ " does not exsit in EntityMetaData : " + entityMetaData.getName());
		}
		return attribute;
	}

	public String parseQueryString(String queryString)
	{
		return termJoiner.join(splitRemoveStopWords(queryString));
	}

	public String parseBoostQueryString(String queryString, double boost)
	{
		return termJoiner.join(
				splitRemoveStopWords(queryString).stream().map(w -> w + CARET_CHARACTER + boost).collect(toList()));
	}

	public String escapeCharsExcludingCaretChar(String string)
	{
		return QueryParser.escape(string).replace(ESCAPED_CARET_CHARACTER, CARET_CHARACTER);
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
	public Hit<String> bestMatchingSynonym(OntologyTerm ontologyTerm, Set<String> searchTerms)
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

	public float distanceFrom(String synonym, Set<String> searchTerms)
	{
		String s1 = Stemmer.stemAndJoin(splitRemoveStopWords(synonym));
		String s2 = Stemmer.stemAndJoin(searchTerms);
		float distance = (float) stringMatching(s1, s2) / 100;
		LOG.debug("Similarity between: {} and {} is {}", s1, s2, distance);
		return distance;
	}

	public Set<String> splitIntoTerms(String description)
	{
		return newLinkedHashSet(stream(description.split(ILLEGAL_CHARS_REGEX)).map(StringUtils::lowerCase)
				.filter(StringUtils::isNotBlank).collect(toList()));
	}

	public Set<String> splitRemoveStopWords(String description)
	{
		return newLinkedHashSet(stream(description.split(ILLEGAL_CHARS_REGEX)).map(StringUtils::lowerCase)
				.filter(w -> !STOPWORDSLIST.contains(w) && isNotBlank(w)).collect(toList()));
	}

	public Set<String> collectLowerCaseTerms(OntologyTerm ontologyTerm)
	{
		Set<String> allTerms = Sets.newLinkedHashSet();
		allTerms.addAll(ontologyTerm.getSynonyms().stream().map(StringUtils::lowerCase).collect(Collectors.toList()));
		allTerms.add(ontologyTerm.getLabel().toLowerCase());
		return allTerms;
	}

	private String boostLexicalQuery(String queryTerm)
	{
		Map<String, Float> collect = splitIntoTerms(queryTerm).stream()
				.collect(Collectors.toMap(t -> t, t -> termFrequencyService.getTermFrequency(t)));

		double max = collect.values().stream().mapToDouble(value -> (double) value).max().orElse(1.0d);

		Map<String, Float> weightedBoostValue = collect.entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, entry -> new Float(entry.getValue() / max)));

		List<String> boostedWords = splitIntoTerms(queryTerm).stream()
				.map(term -> term + CARET_CHARACTER + weightedBoostValue.get(term)).collect(Collectors.toList());

		return termJoiner.join(boostedWords);
	}

	private boolean hasSameSynonyms(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2)
	{
		List<String> stemmedSynonymsOfOt1 = collectLowerCaseTerms(ontologyTerm1).stream().map(Stemmer::cleanStemPhrase)
				.collect(toList());

		return collectLowerCaseTerms(ontologyTerm2).stream()
				.anyMatch(synonym -> stemmedSynonymsOfOt1.contains(cleanStemPhrase(synonym)));
	}

	private void recursivelyCollectAttributeIdentifiers(Iterable<Entity> attributeEntities,
			List<String> attributeIdentifiers)
	{
		for (Entity attributeEntity : attributeEntities)
		{
			if (!attributeEntity.getString(AttributeMetaDataMetaData.DATA_TYPE)
					.equals(MolgenisFieldTypes.COMPOUND.toString()))
			{
				attributeIdentifiers.add(attributeEntity.getString(AttributeMetaDataMetaData.IDENTIFIER));
			}
			Iterable<Entity> entities = attributeEntity.getEntities(AttributeMetaDataMetaData.PARTS);

			if (entities != null)
			{
				recursivelyCollectAttributeIdentifiers(entities, attributeIdentifiers);
			}
		}
	}

	private Float getBestInverseDocumentFrequency(Set<String> terms)
	{
		Optional<String> findFirst = terms.stream().sorted(new Comparator<String>()
		{
			public int compare(String o1, String o2)
			{
				return Integer.compare(o1.length(), o2.length());
			}
		}).findFirst();

		return findFirst.isPresent() ? termFrequencyService.getTermFrequency(findFirst.get()) : null;
	}

	private float round(float score)
	{
		return Math.round(score * 100000) / 100000.0f;
	}
}
package org.molgenis.data.semanticsearch.service.impl;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.lang.Math.pow;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.molgenis.data.QueryRule.Operator.DIS_MAX;
import static org.molgenis.data.semanticsearch.service.bean.OntologyTermHit.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.molgenis.data.semanticsearch.string.OntologyTermComparator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.molgenis.ontology.utils.Stemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.LinkedHashMultimap;
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

	public static final int MAX_NUM_TAGS = 20;
	public static final String UNIT_ONTOLOGY_IRI = "http://purl.obolibrary.org/obo/uo.owl";

	private final static char SPACE_CHAR = ' ';
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

	public List<Hit<OntologyTerm>> findOntologyTermsForAttr(AttributeMetaData attribute, EntityMetaData entityMetadata,
			Set<String> searchTerms, List<String> ontologyIds)
	{
		Multimap<Relation, OntologyTerm> tagsForAttribute = ontologyTagService.getTagsForAttribute(entityMetadata,
				attribute);
		if (!tagsForAttribute.isEmpty())
		{
			return tagsForAttribute.values().stream().map(ot -> Hit.create(ot, 1.0f)).collect(Collectors.toList());
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

		// TODO: we need to add the part of the string, which didn't get annotated, to the annotation result as a fake
		// ontology term
		List<Hit<OntologyTerm>> ontologyTermHits = combineOntologyTerms(searchTerms, relevantOntologyTerms).stream()
				.map(hit -> Hit.create(hit.getResult().getOntologyTerm(), hit.getScore())).collect(toList());

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", ontologyTermHits);
		}

		return ontologyTermHits;
	}

	public List<Hit<OntologyTermHit>> findOntologyTermCombination(String queryString, List<String> ontologyIds)
	{
		Set<String> searchTerms = splitRemoveStopWords(queryString);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findOntologyTermCombination({},{},{})", ontologyIds, searchTerms, MAX_NUM_TAGS);
		}

		List<OntologyTerm> candidates = ontologyService.findOntologyTerms(ontologyIds, searchTerms, MAX_NUM_TAGS);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", candidates);
		}

		List<Hit<OntologyTermHit>> ontologyTermHit = combineOntologyTerms(searchTerms, candidates);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("OntologyTermHit: {}", ontologyTermHit);
		}

		return ontologyTermHit;
	}

	/**
	 * Finds the best combinations of @{link OntologyTerm}s based on the given search terms
	 * 
	 * @param searchTerms
	 * @param relevantOntologyTerms
	 * @return
	 */
	public List<Hit<OntologyTermHit>> combineOntologyTerms(Set<String> searchTerms,
			List<OntologyTerm> relevantOntologyTerms)
	{
		Set<String> stemmedSearchTerms = searchTerms.stream().map(Stemmer::stem).filter(StringUtils::isNotBlank)
				.collect(toSet());

		List<Hit<OntologyTermHit>> hits = relevantOntologyTerms.stream()
				.filter(ontologyTerm -> filterOntologyTerm(stemmedSearchTerms, ontologyTerm))
				.map(ontologyTerm -> createOntologyTermHit(stemmedSearchTerms, ontologyTerm))
				.sorted(new OntologyTermComparator()).collect(Collectors.toList());

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Hits: {}", hits);
		}

		// 1. Create a list of ontology term candidates with the best matching synonym known
		// 2. Loop through the list of candidates and collect all the possible candidates (all best combinations of
		// ontology terms)
		// 3. Compute a list of possible ontology terms.
		Multimap<String, OntologyTerm> candidates = LinkedHashMultimap.create();

		for (Hit<OntologyTermHit> hit : hits)
		{
			OntologyTermHit ontologyTermHit = hit.getResult();

			if (candidates.size() == 0)
			{
				candidates.put(ontologyTermHit.getJoinedSynonym(), ontologyTermHit.getOntologyTerm());
			}
			else
			{
				if (candidates.containsKey(ontologyTermHit.getJoinedSynonym()))
				{
					candidates.put(ontologyTermHit.getJoinedSynonym(), ontologyTermHit.getOntologyTerm());
				}
				else
				{
					Set<String> involvedSynonyms = candidates.keys().elementSet();
					Set<String> jointTerms = Sets.union(involvedSynonyms,
							splitRemoveStopWords(ontologyTermHit.getJoinedSynonym()));
					float previousScore = round(distanceFrom(termJoiner.join(involvedSynonyms), searchTerms));
					float joinedScore = round(distanceFrom(termJoiner.join(jointTerms), searchTerms));
					if (joinedScore > previousScore)
					{
						candidates.put(ontologyTermHit.getJoinedSynonym(), ontologyTermHit.getOntologyTerm());
					}
				}
			}
		}

		String joinedSynonym = termJoiner.join(candidates.keySet());

		List<Hit<OntologyTermHit>> ontologyTermHits = createOntologyTermPairwiseCombination(candidates).stream()
				.map(ontologyTerm -> Hit.create(OntologyTermHit.create(ontologyTerm, joinedSynonym),
						distanceFrom(joinedSynonym, searchTerms)))
				.collect(toList());

		if (LOG.isDebugEnabled())
		{
			LOG.debug("result: {}", ontologyTermHits);
		}

		return ontologyTermHits;
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
		else
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
	 * @param ontologyTerms
	 * @param searchTerms
	 *
	 * @return disMaxJunc queryRule
	 */
	public QueryRule createDisMaxQueryRule(Set<String> searchTerms, List<Hit<OntologyTerm>> ontologyTerms,
			boolean expand)
	{
		List<QueryRule> rules = new ArrayList<>();

		if (searchTerms != null)
		{
			List<String> queryTerms = searchTerms.stream().filter(StringUtils::isNotBlank).map(this::parseQueryString)
					.collect(toList());

			QueryRule createDisMaxQueryRuleForTerms = createDisMaxQueryRuleForTerms(queryTerms,
					LEXICAL_QUERY_BOOSTVALUE);

			if (createDisMaxQueryRuleForTerms != null)
			{
				rules.add(createDisMaxQueryRuleForTerms);
			}
		}

		List<QueryRule> queryRulesForOntologyTerms = createQueryRulesForOntologyTerms(ontologyTerms, expand);
		if (queryRulesForOntologyTerms.size() > 0)
		{
			rules.addAll(queryRulesForOntologyTerms);
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
	 * @param ontologyTerms
	 * @return
	 */
	public List<QueryRule> createQueryRulesForOntologyTerms(List<Hit<OntologyTerm>> ontologyTerms, boolean expand)
	{
		List<QueryRule> queryRules = new ArrayList<>();

		for (Hit<OntologyTerm> ontologyTermHit : ontologyTerms)
		{
			OntologyTerm ontologyTerm = ontologyTermHit.getResult();
			float score = ontologyTermHit.getScore();

			QueryRule queryRule = null;

			List<OntologyTerm> atomicOntologyTerms = ontologyService.getAtomicOntologyTerms(ontologyTerm);
			// Create a should query because it is a composite ontology term
			if (atomicOntologyTerms.size() > 1)
			{
				List<QueryRule> shouldQueryRules = new ArrayList<>();
				for (OntologyTerm atomicOntologyTerm : atomicOntologyTerms)
				{
					List<String> queryTerms = getQueryTermsFromOntologyTerm(atomicOntologyTerm, expand);
					Float termFrequency = getBestInverseDocumentFrequency(queryTerms);
					QueryRule boostedDisMaxQueryRuleForTerms = createDisMaxQueryRuleForTerms(queryTerms, termFrequency);

					if (boostedDisMaxQueryRuleForTerms != null)
					{
						shouldQueryRules.add(boostedDisMaxQueryRuleForTerms);
					}
				}
				queryRule = createShouldQueryRule(shouldQueryRules, score);
			}
			else if (atomicOntologyTerms.size() == 1) // Create a disMaxJunc query if the ontologyTerm is an atomic one
			{
				queryRule = createDisMaxQueryRuleForTerms(
						getQueryTermsFromOntologyTerm(atomicOntologyTerms.get(0), expand), score);
			}

			if (queryRule != null)
			{
				queryRules.add(queryRule);
			}
		}

		return queryRules;
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
		queryTerms.stream().filter(StringUtils::isNotEmpty).map(this::escapeCharsExcludingCaretChar).forEach(query -> {
			rules.add(new QueryRule(AttributeMetaDataMetaData.LABEL, Operator.FUZZY_MATCH, query));
			rules.add(new QueryRule(AttributeMetaDataMetaData.DESCRIPTION, Operator.FUZZY_MATCH, query));
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

		if (shouldQueryRule != null && boostValue != null && boostValue.intValue() != 0)
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
	public List<String> getQueryTermsFromOntologyTerm(OntologyTerm ontologyTerm, boolean expand)
	{
		List<String> queryTerms = getLowerCaseTermsFromOntologyTerm(ontologyTerm).stream().map(this::parseQueryString)
				.collect(toList());
		if (expand)
		{
			ontologyService.getLevelThreeChildren(ontologyTerm).forEach(childOt -> {
				double boostedNumber = ontologyService.getOntologyTermSemanticRelatedness(ontologyTerm, childOt);
				List<String> collect = getLowerCaseTermsFromOntologyTerm(childOt).stream()
						.map(query -> parseBoostQueryString(query, pow(boostedNumber, 2))).collect(Collectors.toList());
				queryTerms.addAll(collect);
			});
		}
		return queryTerms;
	}

	/**
	 * A helper function to collect synonyms as well as label of ontologyterm in lower case
	 * 
	 * @param ontologyTerm
	 * @return a list of synonyms plus label
	 */
	public Set<String> getLowerCaseTermsFromOntologyTerm(OntologyTerm ontologyTerm)
	{
		Set<String> allTerms = Sets.newLinkedHashSet();
		allTerms.addAll(ontologyTerm.getSynonyms().stream().map(StringUtils::lowerCase).collect(Collectors.toList()));
		allTerms.add(ontologyTerm.getLabel().toLowerCase());
		return allTerms;
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
		Function<? super String, ? extends String> deBoostStopWordMapper = w -> STOPWORDSLIST.contains(w)
				? w + CARET_CHARACTER + 0.1f : w;
		Set<String> searchTerms = stream(queryString.toLowerCase().split(ILLEGAL_CHARS_REGEX))
				.filter(StringUtils::isNotBlank).map(deBoostStopWordMapper).collect(toSet());

		return join(searchTerms, SPACE_CHAR);
	}

	public String parseBoostQueryString(String queryString, double boost)
	{
		Function<? super String, ? extends String> boostWordMapper = w -> STOPWORDSLIST.contains(w)
				? w + CARET_CHARACTER + 0.1f : w + CARET_CHARACTER + boost;

		Set<String> searchTerms = stream(queryString.toLowerCase().split(ILLEGAL_CHARS_REGEX))
				.filter(StringUtils::isNotBlank).map(boostWordMapper).collect(toSet());

		return join(searchTerms, SPACE_CHAR);
	}

	public String escapeCharsExcludingCaretChar(String string)
	{
		return QueryParser.escape(string).replace(ESCAPED_CARET_CHARACTER, CARET_CHARACTER);
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
				ontologyTerms = ontologyTermUnion(ontologyTerms, entry.getValue());
			}
		}
		return ontologyTerms;
	}

	List<OntologyTerm> ontologyTermUnion(Collection<OntologyTerm> listOne, Collection<OntologyTerm> listTwo)
	{
		List<OntologyTerm> newList = new ArrayList<>(listOne.size() * listTwo.size());
		for (OntologyTerm ot1 : listOne)
		{
			for (OntologyTerm ot2 : listTwo)
			{
				newList.add(OntologyTerm.and(ot1, ot2));
			}
		}
		return newList;
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
		List<Hit<String>> collect = getLowerCaseTermsFromOntologyTerm(ontologyTerm).stream()
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

	private Float getBestInverseDocumentFrequency(List<String> terms)
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

	private Hit<OntologyTermHit> createOntologyTermHit(Set<String> stemmedSearchTerms, OntologyTerm ontologyTerm)
	{
		Hit<String> bestMatchingSynonym = bestMatchingSynonym(ontologyTerm, stemmedSearchTerms);
		OntologyTermHit candidate = create(ontologyTerm, bestMatchingSynonym.getResult());
		return Hit.<OntologyTermHit> create(candidate, bestMatchingSynonym.getScore());
	}

	private boolean filterOntologyTerm(Set<String> keywordsFromAttribute, OntologyTerm ontologyTerm)
	{
		Set<String> ontologyTermSynonyms = getLowerCaseTermsFromOntologyTerm(ontologyTerm);
		for (String synonym : ontologyTermSynonyms)
		{
			Set<String> splitIntoTerms = splitRemoveStopWords(synonym).stream().map(Stemmer::stem).collect(toSet());
			if (splitIntoTerms.size() != 0 && keywordsFromAttribute.containsAll(splitIntoTerms)) return true;
		}
		return false;
	}

	private float round(float score)
	{
		return Math.round(score * 100000) / 100000.0f;
	}
}
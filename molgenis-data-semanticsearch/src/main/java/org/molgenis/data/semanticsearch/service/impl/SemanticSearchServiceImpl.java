package org.molgenis.data.semanticsearch.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.spell.StringDistance;
import org.elasticsearch.common.base.Joiner;
import org.elasticsearch.common.collect.Lists;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.semantic.Relation;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.string.NGramDistanceAlgorithm;
import org.molgenis.data.semanticsearch.string.OntologyTermComparator;
import org.molgenis.data.semanticsearch.string.Stemmer;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;

import static java.util.Objects.requireNonNull;

import autovalue.shaded.com.google.common.common.collect.Sets;

public class SemanticSearchServiceImpl implements SemanticSearchService
{
	private static final Logger LOG = LoggerFactory.getLogger(SemanticSearchServiceImpl.class);

	private final DataService dataService;
	private final OntologyService ontologyService;
	private final OntologyTagService ontologyTagService;
	private final SemanticSearchServiceHelper semanticSearchServiceHelper;

	public static final int MAX_NUM_TAGS = 20;
	private Splitter termSplitter = Splitter.onPattern("[^\\p{IsAlphabetic}]+");
	private Joiner termJoiner = Joiner.on(' ');
	private static final String UNIT_ONTOLOGY_IRI = "http://purl.obolibrary.org/obo/uo.owl";
	private static final int MAX_NUMBER_ATTRIBTUES = 100;

	@Autowired
	public SemanticSearchServiceImpl(DataService dataService, OntologyService ontologyService,
			OntologyTagService ontologyTagService, SemanticSearchServiceHelper semanticSearchServiceHelper)
	{
		this.dataService = requireNonNull(dataService);
		this.ontologyService = requireNonNull(ontologyService);
		this.ontologyTagService = requireNonNull(ontologyTagService);
		this.semanticSearchServiceHelper = requireNonNull(semanticSearchServiceHelper);
	}

	@Override
	public List<AttributeMetaData> findAttributes(AttributeMetaData targetAttribute,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData, Set<String> searchTerms)
	{
		Set<String> queryTerms = semanticSearchServiceHelper.createLexicalSearchQueryTerms(targetAttribute,
				searchTerms);

		List<OntologyTerm> ontologyTerms = getOntologyTermsForAttr(targetAttribute, targetEntityMetaData, searchTerms);

		QueryRule disMaxQueryRule = semanticSearchServiceHelper.createDisMaxQueryRuleForAttribute(queryTerms,
				ontologyTerms);

		Iterable<String> attributeIdentifiers = semanticSearchServiceHelper
				.getAttributeIdentifiers(sourceEntityMetaData);

		List<QueryRule> finalQueryRules = Lists
				.newArrayList(new QueryRule(AttributeMetaDataMetaData.IDENTIFIER, Operator.IN, attributeIdentifiers));

		if (disMaxQueryRule.getNestedRules().size() > 0)
		{
			finalQueryRules.addAll(Arrays.asList(new QueryRule(Operator.AND), disMaxQueryRule));
		}

		Stream<Entity> attributeMetaDataEntities = dataService.findAll(AttributeMetaDataMetaData.ENTITY_NAME,
				new QueryImpl(finalQueryRules).pageSize(MAX_NUMBER_ATTRIBTUES));

		List<AttributeMetaData> attributes = attributeMetaDataEntities
				.map(entity -> semanticSearchServiceHelper.entityToAttributeMetaData(entity, sourceEntityMetaData))
				.collect(Collectors.toList());

		return attributes;
	}

	@Override
	public List<OntologyTerm> getOntologyTermsForAttr(AttributeMetaData attribute, EntityMetaData entityMetadata,
			Set<String> searchTerms)
	{
		List<OntologyTerm> ontologyTerms = new ArrayList<>();

		List<String> allOntologiesIds = ontologyService.getAllOntologiesIds();
		// If the user search query is not empty, then it overrules the existing tags
		if (searchTerms != null && !searchTerms.isEmpty())
		{
			Set<String> escapedSearchTerms = searchTerms.stream().filter(StringUtils::isNotBlank)
					.map(QueryParser::escape).collect(Collectors.toSet());
			ontologyTerms
					.addAll(ontologyService.findExcatOntologyTerms(allOntologiesIds, escapedSearchTerms, MAX_NUM_TAGS));
		}
		else
		{
			Multimap<Relation, OntologyTerm> tagsForAttribute = ontologyTagService.getTagsForAttribute(entityMetadata,
					attribute);
			if (tagsForAttribute.isEmpty())
			{
				Ontology unitOntology = ontologyService.getOntology(UNIT_ONTOLOGY_IRI);
				if (unitOntology != null)
				{
					allOntologiesIds.remove(unitOntology.getId());
				}
				Hit<OntologyTerm> ontologyTermHit = findTags(attribute, allOntologiesIds);
				if (ontologyTermHit != null)
				{
					ontologyTerms.add(ontologyTermHit.getResult());
				}
			}
			else
			{
				ontologyTerms.addAll(tagsForAttribute.values());
			}
		}
		return ontologyTerms;
	}

	@Override
	public Map<AttributeMetaData, Hit<OntologyTerm>> findTags(String entity, List<String> ontologyIds)
	{
		Map<AttributeMetaData, Hit<OntologyTerm>> result = new LinkedHashMap<AttributeMetaData, Hit<OntologyTerm>>();
		EntityMetaData emd = dataService.getEntityMetaData(entity);
		for (AttributeMetaData amd : emd.getAtomicAttributes())
		{
			Hit<OntologyTerm> tag = findTags(amd, ontologyIds);
			if (tag != null)
			{
				result.put(amd, tag);
			}
		}
		return result;
	}

	@Override
	public Hit<OntologyTerm> findTags(AttributeMetaData attribute, List<String> ontologyIds)
	{
		String description = attribute.getDescription() == null ? attribute.getLabel() : attribute.getDescription();
		Set<String> searchTerms = splitIntoTerms(description);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findOntologyTerms({},{},{})", ontologyIds, searchTerms, MAX_NUM_TAGS);
		}

		List<OntologyTerm> candidates = ontologyService.findOntologyTerms(ontologyIds, searchTerms, MAX_NUM_TAGS);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", candidates);
		}

		Hit<OntologyTermHit> findBestOntologyTermCombination = findBestOntologyTermCombination(attribute, searchTerms,
				candidates);

		return findBestOntologyTermCombination == null ? null
				: Hit.<OntologyTerm> create(findBestOntologyTermCombination.getResult().getOntologyTerm(),
						findBestOntologyTermCombination.getScore());
	}

	@Override
	public Hit<OntologyTermHit> findAndFilterTags(AttributeMetaData attribute, List<String> ontologyIds,
			List<OntologyTerm> filteredOntologyTerms)
	{
		String description = attribute.getDescription() == null ? attribute.getLabel() : attribute.getDescription();
		Set<String> searchTerms = splitIntoTerms(description);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findOntologyTerms({},{},{})", ontologyIds, searchTerms, MAX_NUM_TAGS);
		}

		List<OntologyTerm> candidates = ontologyService.findAndFilterOntologyTerms(ontologyIds, searchTerms,
				MAX_NUM_TAGS, filteredOntologyTerms);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", candidates);
		}

		return findBestOntologyTermCombination(attribute, searchTerms, candidates);
	}

	private Hit<OntologyTermHit> findBestOntologyTermCombination(AttributeMetaData attribute, Set<String> searchTerms,
			List<OntologyTerm> candidates)
	{
		Stemmer stemmer = new Stemmer();
		Set<String> stemmedSearchTerms = searchTerms.stream().map(stemmer::stem).collect(Collectors.toSet());

		List<Hit<OntologyTermHit>> hits = candidates.stream()
				.filter(ontologyTerm -> filterOntologyTerm(stemmedSearchTerms, ontologyTerm)).map(ontologyTerm -> {
					Hit<String> bestMatchingSynonym = bestMatchingSynonym(ontologyTerm, searchTerms);
					OntologyTermHit candidate = OntologyTermHit.create(ontologyTerm, bestMatchingSynonym.getResult());
					return Hit.<OntologyTermHit> create(candidate, bestMatchingSynonym.getScore());
				}).sorted(new OntologyTermComparator()).collect(Collectors.toList());

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Hits: {}", hits);
		}

		Hit<OntologyTermHit> result = null;
		String bestMatchingSynonym = null;
		for (Hit<OntologyTermHit> hit : hits)
		{
			String bestMatchingSynonymForHit = hit.getResult().getMatchedSynonym();
			if (result == null)
			{
				result = hit;
				bestMatchingSynonym = bestMatchingSynonymForHit;
			}
			else
			{
				Set<String> jointTerms = Sets.union(splitIntoTerms(bestMatchingSynonym),
						splitIntoTerms(bestMatchingSynonymForHit));
				String joinedSynonyms = termJoiner.join(jointTerms);
				float joinedScore = distanceFrom(joinedSynonyms, searchTerms, stemmer);
				if (joinedScore > result.getScore())
				{
					bestMatchingSynonym = bestMatchingSynonym + " " + bestMatchingSynonymForHit;
					result = Hit.create(
							OntologyTermHit.create(OntologyTerm.and(result.getResult().getOntologyTerm(),
									hit.getResult().getOntologyTerm()), bestMatchingSynonym),
							distanceFrom(joinedSynonyms, searchTerms, stemmer));
				}
			}

			if (LOG.isDebugEnabled())
			{
				LOG.debug("result: {}", result);
			}
		}
		return result;
	}

	private boolean filterOntologyTerm(Set<String> keywordsFromAttribute, OntologyTerm ontologyTerm)
	{
		Stemmer stemmer = new Stemmer();
		Set<String> ontologyTermSynonyms = semanticSearchServiceHelper.getOtLabelAndSynonyms(ontologyTerm);
		for (String synonym : ontologyTermSynonyms)
		{
			Set<String> splitIntoTerms = splitIntoTerms(synonym).stream().map(stemmer::stem)
					.collect(Collectors.toSet());
			if (splitIntoTerms.size() != 0 && keywordsFromAttribute.containsAll(splitIntoTerms)) return true;
		}
		return false;
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
		Stemmer stemmer = new Stemmer();
		Optional<Hit<String>> bestSynonym = ontologyTerm.getSynonyms().stream()
				.map(synonym -> Hit.<String> create(synonym, distanceFrom(synonym, searchTerms, stemmer)))
				.max(Comparator.naturalOrder());
		return bestSynonym.get();
	}

	float distanceFrom(String synonym, Set<String> searchTerms, Stemmer stemmer)
	{
		String s1 = stemmer.stemAndJoin(splitIntoTerms(synonym));
		String s2 = stemmer.stemAndJoin(searchTerms);
		float distance = (float) NGramDistanceAlgorithm.stringMatching(s1, s2) / 100;
		LOG.debug("Similarity between: {} and {} is {}", s1, s2, distance);
		return distance;
	}

	private Set<String> splitIntoTerms(String description)
	{
		return FluentIterable.from(termSplitter.split(description)).transform(String::toLowerCase)
				.filter(w -> !NGramDistanceAlgorithm.STOPWORDSLIST.contains(w)).filter(StringUtils::isNotEmpty).toSet();
	}
}

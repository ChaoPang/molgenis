package org.molgenis.data.semanticsearch.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.molgenis.data.semanticsearch.semantic.Hit.create;
import static org.molgenis.data.semanticsearch.service.bean.OntologyTermHit.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.STOPWORDSLIST;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.molgenis.data.semanticsearch.string.OntologyTermComparator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.Stemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Splitter;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import static java.util.Objects.requireNonNull;

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

		List<OntologyTerm> ontologyTerms = findOntologyTermsForAttr(targetAttribute, targetEntityMetaData, searchTerms);

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
	public List<OntologyTerm> findOntologyTermsForAttr(AttributeMetaData attribute, EntityMetaData entityMetadata,
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
				Hit<OntologyTerm> ontologyTermHit = findTagForAttr(attribute, allOntologiesIds);
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
	public Map<AttributeMetaData, Hit<OntologyTerm>> findTagsForEntity(String entity, List<String> ontologyIds)
	{
		Map<AttributeMetaData, Hit<OntologyTerm>> result = new LinkedHashMap<AttributeMetaData, Hit<OntologyTerm>>();
		EntityMetaData emd = dataService.getEntityMetaData(entity);
		for (AttributeMetaData amd : emd.getAtomicAttributes())
		{
			Hit<OntologyTerm> tag = findTagForAttr(amd, ontologyIds);
			if (tag != null)
			{
				result.put(amd, tag);
			}
		}
		return result;
	}

	@Override
	public Hit<OntologyTerm> findTagForAttr(AttributeMetaData attribute, List<String> ontologyIds)
	{
		List<Hit<OntologyTermHit>> ontologyTermHits = findAllTagsForAttr(attribute, ontologyIds);
		return ontologyTermHits.stream().findFirst()
				.map(otHit -> create(otHit.getResult().getOntologyTerm(), otHit.getScore())).orElse(null);
	}

	@Override
	public Hit<OntologyTerm> findTag(String description, List<String> ontologyIds)
	{
		List<Hit<OntologyTermHit>> ontologyTermHits = findAllTags(description, ontologyIds);
		return ontologyTermHits.stream().findFirst()
				.map(otHit -> create(otHit.getResult().getOntologyTerm(), otHit.getScore())).orElse(null);
	}

	@Override
	public List<Hit<OntologyTermHit>> findAllTagsForAttr(AttributeMetaData attribute, List<String> ontologyIds)
	{
		String description = attribute.getDescription() == null ? attribute.getLabel() : attribute.getDescription();
		return findAllTags(description, ontologyIds);
	}

	@Override
	public List<Hit<OntologyTermHit>> findAllTags(String description, List<String> ontologyIds)
	{
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

		return combineOntologyTerms(searchTerms, candidates);
	}

	@Override
	public List<Hit<OntologyTermHit>> filterTagsForAttr(AttributeMetaData attribute, List<String> ontologyIds,
			List<OntologyTerm> scope)
	{
		String description = attribute.getDescription() == null ? attribute.getLabel() : attribute.getDescription();
		Set<String> searchTerms = splitIntoTerms(description);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findOntologyTerms({},{},{})", ontologyIds, searchTerms, MAX_NUM_TAGS);
		}

		List<OntologyTerm> candidates = ontologyService.findAndFilterOntologyTerms(ontologyIds, searchTerms,
				MAX_NUM_TAGS, scope);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", candidates);
		}

		return combineOntologyTerms(searchTerms, candidates);
	}

	List<Hit<OntologyTermHit>> combineOntologyTerms(Set<String> searchTerms, List<OntologyTerm> relevantOntologyTerms)
	{
		Set<String> stemmedSearchTerms = searchTerms.stream().map(Stemmer::stem).filter(StringUtils::isNotBlank)
				.collect(toSet());

		List<Hit<OntologyTermHit>> hits = relevantOntologyTerms.stream()
				.filter(ontologyTerm -> filterOntologyTerm(stemmedSearchTerms, ontologyTerm))
				.map(ontologyTerm -> createOntologyTermHit(searchTerms, ontologyTerm))
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
							splitIntoTerms(ontologyTermHit.getJoinedSynonym()));
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

		List<Hit<OntologyTermHit>> ontologyTermHits = getOntologyTerms(candidates).stream().map(
				ontologyTerm -> create(create(ontologyTerm, joinedSynonym), distanceFrom(joinedSynonym, searchTerms)))
				.collect(toList());

		if (LOG.isDebugEnabled())
		{
			LOG.debug("result: {}", ontologyTermHits);
		}

		return ontologyTermHits;
	}

	List<OntologyTerm> getOntologyTerms(Multimap<String, OntologyTerm> candidates)
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

	private List<OntologyTerm> ontologyTermUnion(Collection<OntologyTerm> listOne, Collection<OntologyTerm> listTwo)
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

	private Hit<OntologyTermHit> createOntologyTermHit(Set<String> searchTerms, OntologyTerm ontologyTerm)
	{
		Hit<String> bestMatchingSynonym = bestMatchingSynonym(ontologyTerm, searchTerms);
		OntologyTermHit candidate = create(ontologyTerm, bestMatchingSynonym.getResult());
		return Hit.<OntologyTermHit> create(candidate, bestMatchingSynonym.getScore());
	}

	private boolean filterOntologyTerm(Set<String> keywordsFromAttribute, OntologyTerm ontologyTerm)
	{
		Set<String> ontologyTermSynonyms = semanticSearchServiceHelper.getOtLabelAndSynonyms(ontologyTerm);
		for (String synonym : ontologyTermSynonyms)
		{
			Set<String> splitIntoTerms = splitIntoTerms(synonym).stream().map(Stemmer::stem).collect(toSet());
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
		Optional<Hit<String>> bestSynonym = semanticSearchServiceHelper.getOtLabelAndSynonyms(ontologyTerm).stream()
				.map(synonym -> Hit.<String> create(synonym, distanceFrom(synonym, searchTerms)))
				.max(Comparator.naturalOrder());
		return bestSynonym.get();
	}

	float distanceFrom(String synonym, Set<String> searchTerms)
	{
		String s1 = Stemmer.stemAndJoin(splitIntoTerms(synonym));
		String s2 = Stemmer.stemAndJoin(searchTerms);
		float distance = (float) stringMatching(s1, s2) / 100;
		LOG.debug("Similarity between: {} and {} is {}", s1, s2, distance);
		return distance;
	}

	private Set<String> splitIntoTerms(String description)
	{
		return stream(termSplitter.split(description).spliterator(), false).map(StringUtils::lowerCase)
				.filter(w -> !STOPWORDSLIST.contains(w)).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
	}

	float round(float score)
	{
		return Math.round(score * 100000) / 100000.0f;
	}
}

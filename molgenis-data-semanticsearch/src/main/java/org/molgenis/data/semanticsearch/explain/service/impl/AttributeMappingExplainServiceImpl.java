package org.molgenis.data.semanticsearch.explain.service.impl;

import static com.google.common.collect.Sets.union;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString.create;
import static org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceUtils.MAX_NUM_TAGS;
import static org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceUtils.UNIT_ONTOLOGY_IRI;
import static org.molgenis.ontology.core.model.OntologyTerm.create;
import static org.molgenis.ontology.utils.NGramDistanceAlgorithm.stringMatching;
import static org.molgenis.ontology.utils.Stemmer.splitAndStem;
import static org.molgenis.ontology.utils.Stemmer.stem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString;
import org.molgenis.data.semanticsearch.explain.bean.OntologyTermQueryExpansion;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceUtils;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.utils.Stemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Joiner;

import static java.util.Objects.requireNonNull;

public class AttributeMappingExplainServiceImpl implements AttributeMappingExplainService
{
	private final OntologyService ontologyService;
	private final SemanticSearchServiceUtils semanticSearchServiceUtils;
	private final Joiner termJoiner = Joiner.on(' ');
	private final static float HIGH_QUALITY_THRESHOLD = 0.85f;
	private final static OntologyTermHit EMPTY_ONTOLOGYTERM_HIT = OntologyTermHit.create(create(EMPTY, EMPTY), EMPTY);

	private static final Logger LOG = LoggerFactory.getLogger(AttributeMappingExplainServiceImpl.class);

	@Autowired
	public AttributeMappingExplainServiceImpl(OntologyService ontologyService,
			SemanticSearchServiceUtils semanticSearchServiceUtils)
	{
		this.ontologyService = requireNonNull(ontologyService);
		this.semanticSearchServiceUtils = requireNonNull(semanticSearchServiceUtils);
	}

	@Override
	public ExplainedAttributeMetaData explainAttributeMapping(AttributeMetaData targetAttribute,
			Set<String> userQueries, AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData)
	{
		ExplainedAttributeMetaData explainByAttribute = explainAttributeMapping(targetAttribute, userQueries,
				matchedSourceAttribute, targetEntityMetaData, false, false);
		if (explainByAttribute.isHighQuality()) return explainByAttribute;

		explainByAttribute = explainAttributeMapping(targetAttribute, userQueries, matchedSourceAttribute,
				targetEntityMetaData, true, false);
		if (explainByAttribute.isHighQuality()) return explainByAttribute;

		return explainAttributeMapping(targetAttribute, userQueries, matchedSourceAttribute, targetEntityMetaData, true,
				true);
	}

	@Override
	public ExplainedAttributeMetaData explainAttributeMapping(AttributeMetaData targetAttribute,
			Set<String> userQueries, AttributeMetaData matchedSourceAttribute, EntityMetaData targetEntityMetaData,
			boolean semanticSearchEnabled, boolean childExpansionEnabled)
	{
		// Collect all terms from the target attribute
		Set<String> queriesFromTargetAttribute = semanticSearchServiceUtils.getQueryTermsFromAttribute(targetAttribute,
				userQueries);

		// If semantic search is enabled, collect all the ontology terms that are associated with the target attribute,
		// which were used in query expansion for finding the relevant source attributes.
		List<OntologyTermQueryExpansion> ontologyTermQueryExpansions;
		if (semanticSearchEnabled)
		{
			List<String> ontologyTermIds = ontologyService.getOntologies().stream()
					.filter(ontology -> !ontology.getIRI().equals(UNIT_ONTOLOGY_IRI)).map(Ontology::getId)
					.collect(toList());

			ontologyTermQueryExpansions = semanticSearchServiceUtils
					.findOntologyTermsForAttr(targetAttribute, targetEntityMetaData, userQueries, ontologyTermIds)
					.stream()
					.map(hit -> new OntologyTermQueryExpansion(hit.getResult(), ontologyService, childExpansionEnabled))
					.collect(toList());
		}
		else ontologyTermQueryExpansions = Collections.emptyList();

		return explainAttributeMappingInternal(queriesFromTargetAttribute, ontologyTermQueryExpansions,
				matchedSourceAttribute);
	}

	ExplainedAttributeMetaData explainAttributeMappingInternal(Set<String> queriesFromTargetAttribute,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions, AttributeMetaData matchedSourceAttribute)
	{
		// Collect all terms from the source attribute
		Set<String> queriesFromSourceAttribute = semanticSearchServiceUtils
				.getQueryTermsFromAttribute(matchedSourceAttribute, null);

		// Compute the pairwise lexical similarities between the two sets of query terms and find the best matching
		// target query that yields the highest similarity score.
		Hit<String> targetQueryTermHit = findBestQueryTerm(queriesFromTargetAttribute, queriesFromSourceAttribute);

		// Unfortunately the ElasticSearch built-in explain-api doesn't scale up. In order to explain why the source
		// attribute was matched. Now we take the source attribute as the query and filter/find the best combination
		// of ontology terms from the relevantOntologyTerms that are associated with the target attribute. By doing
		// this, we can deduce which ontology terms were used as the expanded queries for finding that particular source
		// attribute.
		Hit<OntologyTermHit> ontologyTermHit = filterTagsForAttr(matchedSourceAttribute,
				ontologyService.getAllOntologiesIds(), ontologyTermQueryExpansions, queriesFromTargetAttribute);

		// Here we check if the source attribute is matched with the original target queries or the expanded
		// ontology term queries.
		String queryOrigin = targetQueryTermHit.getScore() >= ontologyTermHit.getScore()
				? targetQueryTermHit.getResult() : ontologyTermHit.getResult().getOntologyTerm().getLabel();

		// Here we get the best matching query depending on the origin of the query.
		String bestMatchingQuery = targetQueryTermHit.getScore() >= ontologyTermHit.getScore()
				? targetQueryTermHit.getResult() : ontologyTermHit.getResult().getJoinedSynonym();

		// We collect the final matching score.
		float score = (targetQueryTermHit.getScore() >= ontologyTermHit.getScore() ? targetQueryTermHit.getScore()
				: ontologyTermHit.getScore());

		boolean isHighQuality = score >= HIGH_QUALITY_THRESHOLD;

		// We get the matched words for the source attribute.
		List<String> matchedWords = getMatchedWords(bestMatchingQuery, queriesFromSourceAttribute);

		ExplainedQueryString explainedQueryString = create(termJoiner.join(matchedWords), bestMatchingQuery,
				queryOrigin, score);

		return ExplainedAttributeMetaData.create(matchedSourceAttribute, explainedQueryString, isHighQuality);
	}

	/**
	 * Filters all the relevant {@link OntologyTerm}s and creates the {@link OntologyTermHit}s for the attribute.
	 * {@link OntologyTermHit} contains the best combination of ontology terms that yields the highest lexical
	 * similarity score.
	 * 
	 * @param matchedSourceAttribute
	 * @param ontologyIds
	 * @param scope
	 *            defines a scope of ontology terms in which the search is performed.
	 * @return
	 */
	private Hit<OntologyTermHit> filterTagsForAttr(AttributeMetaData matchedSourceAttribute, List<String> ontologyIds,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions, Set<String> queriesFromTargetAttribute)
	{
		String sourceLabel = matchedSourceAttribute.getDescription() == null ? matchedSourceAttribute.getLabel()
				: matchedSourceAttribute.getDescription();
		Set<String> searchTerms = semanticSearchServiceUtils.splitRemoveStopWords(sourceLabel);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("findOntologyTerms({},{},{})", ontologyIds, searchTerms, MAX_NUM_TAGS);
		}

		List<OntologyTerm> ontologyTermScope = new ArrayList<>();
		ontologyTermQueryExpansions.forEach(expansion -> ontologyTermScope.addAll(expansion.getOntologyTerms()));

		List<OntologyTerm> candidates = ontologyService.fileterOntologyTerms(ontologyIds, searchTerms,
				ontologyTermScope.size(), ontologyTermScope);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Candidates: {}", candidates);
		}

		List<Hit<OntologyTermHit>> combineOntologyTerms = semanticSearchServiceUtils.combineOntologyTerms(searchTerms,
				candidates);

		if (combineOntologyTerms.size() > 0)
		{
			Hit<OntologyTermHit> hit = combineOntologyTerms.get(0);

			Optional<Hit<String>> max = stream(queriesFromTargetAttribute.spliterator(), false)
					.map(targetQueryTerm -> joinOntologyTermAndAttribute(hit, ontologyTermQueryExpansions,
							targetQueryTerm, sourceLabel))
					.max(Comparator.naturalOrder());

			if (max.isPresent())
			{
				Hit<String> joinedSynonymHit = max.get();
				Hit<OntologyTermHit> create = Hit.create(
						OntologyTermHit.create(hit.getResult().getOntologyTerm(), joinedSynonymHit.getResult()),
						joinedSynonymHit.getScore());
				return create;
			}
		}
		return Hit.create(EMPTY_ONTOLOGYTERM_HIT, 0.0f);
	}

	Hit<String> joinOntologyTermAndAttribute(Hit<OntologyTermHit> hit,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions, String targetQueryTerm,
			String sourceAttributeDescription)
	{
		Set<String> unusedOntologyTerms = getUnusedOntologyTermQueries(hit, ontologyTermQueryExpansions);

		Set<String> usedOntologyTermQueries = getUsedOntologyTermQueries(hit, targetQueryTerm);

		Set<String> joinedOntologyTermWords = Stemmer
				.splitAndStem(termJoiner.join(union(usedOntologyTermQueries, unusedOntologyTerms)));

		Set<String> additionalMatchedWords = semanticSearchServiceUtils.splitIntoTerms(targetQueryTerm).stream()
				.filter(word -> !joinedOntologyTermWords.contains(stem(word))).collect(Collectors.toSet());

		String join = termJoiner.join(joinedOntologyTermWords);

		if (additionalMatchedWords.size() > 0)
		{
			join = join + ' ' + termJoiner.join(additionalMatchedWords);
		}

		float score = (float) stringMatching(join, hit.getResult().getJoinedSynonym(), false) / 100;

		return Hit.create(join, score);
	}

	Set<String> getUnusedOntologyTermQueries(Hit<OntologyTermHit> hit,
			List<OntologyTermQueryExpansion> ontologyTermQueryExpansions)
	{
		Set<String> unusedOntologyTerms = ontologyTermQueryExpansions.stream()
				.map(expansion -> expansion.getUnusedOntologyTermQueries(hit)).sorted(new Comparator<Set<String>>()
				{
					public int compare(Set<String> o1, Set<String> o2)
					{
						return Integer.compare(o1.size(), o2.size());
					}
				}).findFirst().orElse(emptySet());

		return unusedOntologyTerms;
	}

	Set<String> getUsedOntologyTermQueries(Hit<OntologyTermHit> hit, String targetQueryTerm)
	{
		List<OntologyTerm> atomicOntologyTerms = ontologyService
				.getAtomicOntologyTerms(hit.getResult().getOntologyTerm());
		Set<String> targetQueryTermWords = splitAndStem(targetQueryTerm);
		Set<String> usedOntologyTermQueries = new LinkedHashSet<>();
		for (OntologyTerm atomicOntologyTerm : atomicOntologyTerms)
		{
			for (String ontologyTermQuery : semanticSearchServiceUtils
					.getLowerCaseTermsFromOntologyTerm(atomicOntologyTerm))
			{
				if (targetQueryTermWords.containsAll(splitAndStem(ontologyTermQuery)))
				{
					usedOntologyTermQueries.add(ontologyTermQuery);
					break;
				}
			}
		}
		return usedOntologyTermQueries;
	}

	List<String> getMatchedWords(String bestMatchingQuery, Set<String> queriesFromSourceAttribute)
	{
		List<String> matchedWords = new ArrayList<>();
		Set<String> bestMatchingQueryTokens = splitAndStem(bestMatchingQuery);
		for (String queryFromSourceAttribute : queriesFromSourceAttribute)
		{
			List<String> collect = semanticSearchServiceUtils.splitIntoTerms(queryFromSourceAttribute).stream()
					.filter(word -> bestMatchingQueryTokens.contains(Stemmer.stem(word))).collect(Collectors.toList());

			if (matchedWords.size() < collect.size())
			{
				matchedWords = collect;
			}
		}
		return matchedWords;
	}

	List<OntologyTerm> getAtomicOntologyTerms(List<OntologyTerm> ontologyTerms)
	{
		List<OntologyTerm> expandedOntologyTerms = new ArrayList<>();
		for (OntologyTerm ontologyTerm : ontologyTerms)
		{
			expandedOntologyTerms.addAll(ontologyService.getAtomicOntologyTerms(ontologyTerm));
		}
		return expandedOntologyTerms;
	}

	List<OntologyTerm> getExpandedOntologyTerms(List<OntologyTerm> ontologyTerms)
	{
		List<OntologyTerm> expandedOntologyTerms = new ArrayList<>();
		for (OntologyTerm atomicOntologyTerm : getAtomicOntologyTerms(ontologyTerms))
		{
			expandedOntologyTerms.add(atomicOntologyTerm);
			expandedOntologyTerms.addAll(ontologyService.getLevelThreeChildren(atomicOntologyTerm));
		}
		return expandedOntologyTerms;
	}

	Hit<String> findBestQueryTerm(Set<String> queriesFromTargetAttribute, Set<String> queriesFromSourceAttribute)
	{
		double highestScore = 0;
		String bestTargetQuery = null;

		for (String targetQuery : queriesFromTargetAttribute)
		{
			for (String sourceQuery : queriesFromSourceAttribute)
			{
				double score = stringMatching(targetQuery, sourceQuery);
				if (bestTargetQuery == null || score > highestScore)
				{
					bestTargetQuery = targetQuery;
					highestScore = score;
				}
			}
		}
		return Hit.<String> create(bestTargetQuery, (float) highestScore / 100);
	}
}
package org.molgenis.data.discovery.service.impl;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.molgenis.data.QueryRule.Operator.AND;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.meta.AttributeMetaDataMetaData.IDENTIFIER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.molgenis.data.QueryRule;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParam;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class OntologyBasedMatcher
{
	private static final Logger LOG = LoggerFactory.getLogger(OntologyBasedMatcher.class);
	private static final String ESCAPED_NODEPATH_SEPARATOR = "\\.";
	private static final String NODEPATH_SEPARATOR = ".";

	private static final int MAX_NUMBER_LEXICAL_MATCHES = 50;

	private final BiobankUniverseRepository biobankUniverseRepository;
	private final QueryExpansionService queryExpansionService;
	private final OntologyService ontologyService;

	private final BiobankSampleCollection biobankSampleCollection;
	private final Iterable<BiobankSampleAttribute> biobankSampleAttributes;
	private final Multimap<String, BiobankSampleAttribute> treeNodePathsRegistry;
	private final Map<OntologyTerm, List<BiobankSampleAttribute>> cachedBiobankSampleAttributes;

	public OntologyBasedMatcher(BiobankSampleCollection biobankSampleCollection,
			BiobankUniverseRepository biobankUniverseRepository, QueryExpansionService queryExpansionService,
			OntologyService ontologyService)
	{
		this.treeNodePathsRegistry = LinkedHashMultimap.create();
		this.biobankUniverseRepository = requireNonNull(biobankUniverseRepository);
		this.queryExpansionService = requireNonNull(queryExpansionService);
		this.ontologyService = requireNonNull(ontologyService);
		this.biobankSampleCollection = requireNonNull(biobankSampleCollection);
		this.biobankSampleAttributes = biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection);
		this.cachedBiobankSampleAttributes = new HashMap<>();
		construct();
	}

	public List<BiobankSampleAttribute> match(SemanticSearchParam semanticSearchParam)
	{
		Set<BiobankSampleAttribute> matchedSourceAttribtues = new LinkedHashSet<>();

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Started lexical match...");
		}

		// Lexical match
		Set<String> lexicalQueries = semanticSearchParam.getLexicalQueries();
		List<TagGroup> tagGroups = semanticSearchParam.getTagGroups();
		QueryExpansionParam queryExpansionParameter = QueryExpansionParam.create(true, false);

		QueryRule expandedQuery = queryExpansionService.expand(lexicalQueries, tagGroups, queryExpansionParameter);

		if (expandedQuery != null)
		{
			List<String> identifiers = biobankUniverseRepository
					.getBiobankSampleAttributeIdentifiers(biobankSampleCollection);

			List<QueryRule> finalQueryRules = Lists.newArrayList(new QueryRule(IDENTIFIER, IN, identifiers));

			if (expandedQuery.getNestedRules().size() > 0)
			{
				finalQueryRules.addAll(asList(new QueryRule(AND), expandedQuery));
			}

			List<BiobankSampleAttribute> lexicalMatches = biobankUniverseRepository
					.queryBiobankSampleAttribute(new QueryImpl(finalQueryRules).pageSize(MAX_NUMBER_LEXICAL_MATCHES))
					.collect(Collectors.toList());

			if (LOG.isTraceEnabled())
			{
				LOG.trace("Finished lexical match...");
				LOG.trace("Started semantic match...");
			}

			matchedSourceAttribtues.addAll(lexicalMatches);
		}

		// Semantic match
		List<BiobankSampleAttribute> semanticMatches = semanticSearchParam.getTagGroups().stream()
				.flatMap(tag -> ontologyService.getAtomicOntologyTerms(tag.getOntologyTerm()).stream()).distinct()
				.flatMap(ontologyTerm -> findBiobankSampleAttributes(ontologyTerm, semanticSearchParam).stream())
				.collect(toList());

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Finished semantic match...");
		}

		matchedSourceAttribtues.addAll(semanticMatches);

		return Lists.newArrayList(matchedSourceAttribtues);
	}

	private List<BiobankSampleAttribute> findBiobankSampleAttributes(OntologyTerm ontologyTerm,
			SemanticSearchParam semanticSearchParam)
	{
		if (cachedBiobankSampleAttributes.containsKey(ontologyTerm))
		{
			return cachedBiobankSampleAttributes.get(ontologyTerm);
		}
		else
		{
			List<BiobankSampleAttribute> candidates = new ArrayList<>();

			for (String nodePath : ontologyTerm.getNodePaths())
			{
				if (treeNodePathsRegistry.containsKey(nodePath))
				{
					candidates.addAll(treeNodePathsRegistry.get(nodePath));
				}
				else
				{
					for (String parentNodePath : getAllParents(nodePath).stream()
							.limit(semanticSearchParam.getQueryExpansionParameter().getExpansionLevel())
							.collect(toList()))
					{
						if (treeNodePathsRegistry.containsKey(parentNodePath))
						{
							candidates.addAll(treeNodePathsRegistry.get(parentNodePath));
							break;
						}
					}
				}
			}

			cachedBiobankSampleAttributes.put(ontologyTerm, candidates);
			return candidates;
		}
	}

	private void construct()
	{
		if (LOG.isTraceEnabled())
		{
			LOG.trace("Starting to construct the tree...");
		}

		for (BiobankSampleAttribute biobankSampleAttribute : biobankSampleAttributes)
		{
			biobankSampleAttribute.getTagGroups().stream().flatMap(tagGroup -> tagGroup.getOntologyTerms().stream())
					.distinct().flatMap(ot -> ot.getNodePaths().stream()).forEach(nodePath -> {

						treeNodePathsRegistry.put(nodePath, biobankSampleAttribute);

						getAllParents(nodePath).forEach(
								parentNodePath -> treeNodePathsRegistry.put(parentNodePath, biobankSampleAttribute));
					});
		}

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Finished constructing the tree...");
		}
	}

	private List<String> getAllParents(String nodePath)
	{
		String[] split = nodePath.split(ESCAPED_NODEPATH_SEPARATOR);
		List<String> parents = new ArrayList<>();
		for (int i = split.length; i > 1; i--)
		{
			String parent = Stream.of(Arrays.copyOf(split, i - 1)).collect(joining(NODEPATH_SEPARATOR));
			parents.add(parent);
		}
		return parents;
	}
}
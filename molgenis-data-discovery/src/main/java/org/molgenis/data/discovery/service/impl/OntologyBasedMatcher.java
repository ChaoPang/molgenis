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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
	private final Multimap<String, BiobankSampleAttribute> nodePathRegistry;
	private final Multimap<String, BiobankSampleAttribute> descendantNodePathsRegistry;
	private final Map<OntologyTerm, List<BiobankSampleAttribute>> cachedBiobankSampleAttributes;

	public OntologyBasedMatcher(BiobankSampleCollection biobankSampleCollection,
			BiobankUniverseRepository biobankUniverseRepository, QueryExpansionService queryExpansionService,
			OntologyService ontologyService)
	{
		this.nodePathRegistry = LinkedHashMultimap.create();
		this.descendantNodePathsRegistry = LinkedHashMultimap.create();
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
		List<BiobankSampleAttribute> candidates = new ArrayList<>();

		if (cachedBiobankSampleAttributes.containsKey(ontologyTerm))
		{
			candidates.addAll(cachedBiobankSampleAttributes.get(ontologyTerm));
		}
		else
		{
			for (String nodePath : ontologyTerm.getNodePaths())
			{
				// if a direct hit for the current nodePath is found, we want to get all the associated
				// BiobankSampleAttributes from the descendant nodePaths.
				if (descendantNodePathsRegistry.containsKey(nodePath))
				{
					candidates.addAll(descendantNodePathsRegistry.get(nodePath));
				}
				else
				{
					// if a hit for the parent nodePath is found, we only want to get associated BiobankSampleAttributes
					// from that particular parent nodePath
					List<BiobankSampleAttribute> collect = StreamSupport
							.stream(getAllParents(nodePath).spliterator(), false)
							.limit(semanticSearchParam.getQueryExpansionParameter().getExpansionLevel())
							.filter(nodePathRegistry::containsKey)
							.flatMap(parentNodePath -> nodePathRegistry.get(parentNodePath).stream()).distinct()
							.collect(Collectors.toList());

					candidates.addAll(collect);
				}
			}

			cachedBiobankSampleAttributes.put(ontologyTerm, candidates);
		}

		return candidates;

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

						// Register the direct association between nodePaths and BiobankSampleAttributes
						nodePathRegistry.put(nodePath, biobankSampleAttribute);

						// Register the direct associations plus the descendant associations between nodePaths and
						// BiobankSampleAttributes
						descendantNodePathsRegistry.put(nodePath, biobankSampleAttribute);

						for (String parentNodePath : getAllParents(nodePath))
						{
							descendantNodePathsRegistry.put(parentNodePath, biobankSampleAttribute);
						}
					});
		}

		if (LOG.isTraceEnabled())
		{
			LOG.trace("Finished constructing the tree...");
		}
	}

	String getParent(String nodePath, int traversalLevel)
	{
		String[] split = nodePath.split(ESCAPED_NODEPATH_SEPARATOR);
		int size = split.length > traversalLevel ? split.length - traversalLevel : 1;
		String parent = Stream.of(Arrays.copyOf(split, size)).collect(joining(NODEPATH_SEPARATOR));
		return parent;
	}

	Iterable<String> getAllParents(String nodePath)
	{
		return new Iterable<String>()
		{
			final String[] split = nodePath.split(ESCAPED_NODEPATH_SEPARATOR);
			private int size = split.length;

			public Iterator<String> iterator()
			{
				return new Iterator<String>()
				{
					@Override
					public boolean hasNext()
					{
						return size > 1;
					}

					@Override
					public String next()
					{
						String parent = Stream.of(Arrays.copyOf(split, --size)).collect(joining(NODEPATH_SEPARATOR));
						return parent;
					}
				};
			}
		};
	}
}
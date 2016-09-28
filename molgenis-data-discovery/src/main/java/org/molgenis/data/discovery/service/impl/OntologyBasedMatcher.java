package org.molgenis.data.discovery.service.impl;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
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
	public static final int STOP_LEVEL = 4;
	public static final int EXPANSION_LEVEL = 5;
	public static final int MAX_NUMBER_LEXICAL_MATCHES = 20;

	private static final Logger LOG = LoggerFactory.getLogger(OntologyBasedMatcher.class);
	private static final String ESCAPED_NODEPATH_SEPARATOR = "\\.";
	private static final String NODEPATH_SEPARATOR = ".";

	private final BiobankUniverseRepository biobankUniverseRepository;
	private final QueryExpansionService queryExpansionService;
	private final OntologyService ontologyService;

	private final Iterable<BiobankSampleAttribute> biobankSampleAttributes;
	private final Multimap<String, BiobankSampleAttribute> nodePathRegistry;
	private final Multimap<String, BiobankSampleAttribute> descendantNodePathsRegistry;
	private final Map<OntologyTerm, List<BiobankSampleAttribute>> cachedBiobankSampleAttributes;

	public OntologyBasedMatcher(BiobankSampleCollection biobankSampleCollection,
			BiobankUniverseRepository biobankUniverseRepository, QueryExpansionService queryExpansionService,
			OntologyService ontologyService)
	{
		this(biobankUniverseRepository.getBiobankSampleAttributes(biobankSampleCollection), biobankUniverseRepository,
				queryExpansionService, ontologyService);
	}

	public OntologyBasedMatcher(List<BiobankSampleAttribute> biobankSampleAttributes,
			BiobankUniverseRepository biobankUniverseRepository, QueryExpansionService queryExpansionService,
			OntologyService ontologyService)
	{
		this.nodePathRegistry = LinkedHashMultimap.create();
		this.descendantNodePathsRegistry = LinkedHashMultimap.create();
		this.biobankUniverseRepository = requireNonNull(biobankUniverseRepository);
		this.queryExpansionService = requireNonNull(queryExpansionService);
		this.ontologyService = requireNonNull(ontologyService);
		this.biobankSampleAttributes = requireNonNull(biobankSampleAttributes);
		this.cachedBiobankSampleAttributes = new HashMap<>();
		constructTree();
	}

	public List<BiobankSampleAttribute> match(SemanticSearchParam semanticSearchParam)
	{
		Set<BiobankSampleAttribute> matchedSourceAttribtues = new LinkedHashSet<>();

		LOG.trace("Started lexical match...");

		// Lexical match
		matchedSourceAttribtues.addAll(lexicalSearchBiobankSampleAttributes(semanticSearchParam));

		// Semantic match
		List<BiobankSampleAttribute> semanticMatches = semanticSearchParam.getTagGroups().stream()
				.flatMap(tag -> ontologyService.getAtomicOntologyTerms(tag.getOntologyTerm()).stream()).distinct()
				.flatMap(ontologyTerm -> semanticSearchBiobankSampleAttributes(ontologyTerm).stream())
				.collect(toList());

		LOG.trace("Finished semantic match...");

		matchedSourceAttribtues.addAll(semanticMatches);

		return Lists.newArrayList(matchedSourceAttribtues);
	}

	List<BiobankSampleAttribute> lexicalSearchBiobankSampleAttributes(SemanticSearchParam semanticSearchParam)
	{
		List<BiobankSampleAttribute> matches = new ArrayList<>();

		Set<String> lexicalQueries = semanticSearchParam.getLexicalQueries();

		List<TagGroup> tagGroups = semanticSearchParam.getTagGroups();

		QueryExpansionParam queryExpansionParameter = QueryExpansionParam.create(false, false);

		QueryRule expandedQuery = queryExpansionService.expand(lexicalQueries, tagGroups, queryExpansionParameter);

		if (expandedQuery != null)
		{
			List<String> identifiers = StreamSupport.stream(biobankSampleAttributes.spliterator(), false)
					.map(BiobankSampleAttribute::getIdentifier).collect(Collectors.toList());

			List<QueryRule> finalQueryRules = Lists.newArrayList(new QueryRule(IDENTIFIER, IN, identifiers));

			if (expandedQuery.getNestedRules().size() > 0)
			{
				finalQueryRules.addAll(asList(new QueryRule(AND), expandedQuery));
			}

			List<BiobankSampleAttribute> lexicalMatches = biobankUniverseRepository
					.queryBiobankSampleAttribute(new QueryImpl(finalQueryRules).pageSize(MAX_NUMBER_LEXICAL_MATCHES))
					.collect(Collectors.toList());

			LOG.trace("Finished lexical match...");
			LOG.trace("Started semantic match...");

			matches.addAll(lexicalMatches);
		}

		return matches;
	}

	List<BiobankSampleAttribute> semanticSearchBiobankSampleAttributes(OntologyTerm ontologyTerm)
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
				// if a hit for the parent nodePath is found, we only want to get associated BiobankSampleAttributes
				// from that particular parent nodePath
				List<BiobankSampleAttribute> collect = StreamSupport
						.stream(getAllParents(nodePath).spliterator(), false).limit(EXPANSION_LEVEL)
						.filter(parentNodePath -> getNodePathLevel(parentNodePath) > STOP_LEVEL)
						.filter(nodePathRegistry::containsKey)
						.flatMap(parentNodePath -> nodePathRegistry.get(parentNodePath).stream()).distinct()
						.collect(Collectors.toList());

				candidates.addAll(collect);
			}

			cachedBiobankSampleAttributes.put(ontologyTerm, candidates);
		}

		return candidates;
	}

	private void constructTree()
	{
		LOG.trace("Starting to construct the tree...");

		for (BiobankSampleAttribute biobankSampleAttribute : biobankSampleAttributes)
		{
			biobankSampleAttribute.getTagGroups().stream().flatMap(tagGroup -> tagGroup.getOntologyTerms().stream())
					.distinct().flatMap(ot -> ot.getNodePaths().stream()).forEach(nodePath -> {

						// Register the direct association between nodePaths and BiobankSampleAttributes
						nodePathRegistry.put(nodePath, biobankSampleAttribute);

						if (getNodePathLevel(nodePath) > STOP_LEVEL)
						{
							// Register the direct associations plus the descendant associations between nodePaths and
							// BiobankSampleAttributes
							descendantNodePathsRegistry.put(nodePath, biobankSampleAttribute);

							for (String parentNodePath : stream(getAllParents(nodePath).spliterator(), false)
									.limit(EXPANSION_LEVEL).collect(toList()))
							{
								if (getNodePathLevel(parentNodePath) > STOP_LEVEL)
								{
									descendantNodePathsRegistry.put(parentNodePath, biobankSampleAttribute);
								}
								else break;
							}
						}
					});
		}

		LOG.trace("Finished constructing the tree...");
	}

	int getNodePathLevel(String nodePath)
	{
		return nodePath.split(ESCAPED_NODEPATH_SEPARATOR).length;
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
package org.molgenis.ontology.core.repository;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.molgenis.data.QueryRule.Operator.AND;
import static org.molgenis.data.QueryRule.Operator.FUZZY_MATCH;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.QueryRule.Operator.OR;
import static org.molgenis.data.support.QueryImpl.EQ;
import static org.molgenis.data.support.QueryImpl.IN;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ENTITY_NAME;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ID;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_DYNAMIC_ANNOTATION;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_IRI;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_NAME;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_SEMANTIC_TYPE;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Fetch;
import org.molgenis.data.Query;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.meta.OntologyMetaData;
import org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotationMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.molgenis.ontology.core.meta.OntologyTermSynonymMetaData;
import org.molgenis.ontology.core.meta.SemanticTypeMetaData;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OntologyTermAnnotation;
import org.molgenis.ontology.core.model.OntologyTermChildrenPredicate;
import org.molgenis.ontology.core.model.SemanticType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/***
 * Maps{
 * 
 * @link OntologyTermMetaData} {@link Entity} <-> {@link OntologyTerm}
 */
public class OntologyTermRepository
{
	private static final Logger LOG = LoggerFactory.getLogger(OntologyTermRepository.class);

	private static final String ESCAPED_NODEPATH_SEPARATOR = "\\.";
	private static final String NODEPATH_SEPARATOR = ".";

	private final DataService dataService;

	@Autowired
	public OntologyTermRepository(DataService dataService)
	{
		this.dataService = requireNonNull(dataService);
	}

	/**
	 * FIXME write docs
	 *
	 * @param term
	 * @param pageSize
	 * @return
	 */
	public List<OntologyTerm> findOntologyTerms(String term, int pageSize)
	{
		Iterable<Entity> ontologyTermEntities;

		// #1 find exact match
		Query termNameQuery = new QueryImpl().eq(OntologyTermMetaData.ONTOLOGY_TERM_NAME, term).pageSize(pageSize);
		ontologyTermEntities = new Iterable<Entity>()
		{
			@Override
			public Iterator<Entity> iterator()
			{
				return dataService.findAll(ENTITY_NAME, termNameQuery).iterator();
			}
		};

		if (!ontologyTermEntities.iterator().hasNext())
		{
			Query termsQuery = new QueryImpl().search(term).pageSize(pageSize);
			ontologyTermEntities = new Iterable<Entity>()
			{
				@Override
				public Iterator<Entity> iterator()
				{
					return dataService.findAll(ENTITY_NAME, termsQuery).iterator();
				}
			};
		}
		return Lists.newArrayList(Iterables.transform(ontologyTermEntities, OntologyTermRepository::toOntologyTerm));
	}

	/**
	 * Finds {@link OntologyTerm}s within {@link Ontology}s.
	 *
	 * @param ontologyIds
	 *            IDs of the {@link Ontology}s to search in
	 * @param terms
	 *            {@link List} of search terms. the {@link OntologyTerm} must match at least one of these terms
	 * @param semanticTypeFilter
	 * @param pageSize
	 *            max number of results
	 * @return {@link List} of {@link OntologyTerm}s
	 */
	public List<OntologyTerm> findOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize)
	{
		Fetch fetch = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(attribute -> fetch.field(attribute.getName()));

		List<QueryRule> rules = new ArrayList<QueryRule>();

		rules.add(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_NAME, Operator.EQUALS, join(terms, ' ')));

		for (String term : terms)
		{
			if (rules.size() > 0)
			{
				rules.add(new QueryRule(OR));
			}
			rules.add(new QueryRule(ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, term));
		}

		rules = Arrays.asList(new QueryRule(ONTOLOGY, IN, ontologyIds), new QueryRule(Operator.AND),
				new QueryRule(rules));

		return dataService.findAll(ENTITY_NAME, new QueryImpl(rules).pageSize(pageSize).fetch(fetch))
				.map(OntologyTermRepository::toOntologyTerm).collect(toList());
	}

	public List<OntologyTerm> findAndFilterOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize,
			List<OntologyTerm> ontologyTermScope)
	{
		Fetch fetch = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(attribute -> fetch.field(attribute.getName()));

		List<QueryRule> rules = new ArrayList<QueryRule>();
		for (String term : terms)
		{
			if (rules.size() > 0)
			{
				rules.add(new QueryRule(OR));
			}
			rules.add(new QueryRule(ONTOLOGY_TERM_SYNONYM, FUZZY_MATCH, term));
		}
		rules = Arrays.asList(new QueryRule(ONTOLOGY, IN, ontologyIds), new QueryRule(AND), new QueryRule(rules));

		List<String> filteredOntologyTermIris = ontologyTermScope.stream().map(OntologyTerm::getIRI).collect(toList());

		rules = Arrays.asList(new QueryRule(ONTOLOGY_TERM_IRI, IN, filteredOntologyTermIris), new QueryRule(AND),
				new QueryRule(rules));

		return dataService.findAll(ENTITY_NAME, new QueryImpl(rules).pageSize(pageSize).fetch(fetch))
				.map(OntologyTermRepository::toOntologyTerm).collect(toList());
	}

	public List<OntologyTerm> getAllOntologyTerms(String ontologyId)
	{
		Entity ontologyEntity = dataService.findOne(OntologyMetaData.ENTITY_NAME,
				new QueryImpl().eq(OntologyMetaData.ONTOLOGY_IRI, ontologyId));

		if (ontologyEntity != null)
		{
			return dataService
					.findAll(OntologyTermMetaData.ENTITY_NAME,
							new QueryImpl().eq(OntologyTermMetaData.ONTOLOGY, ontologyEntity)
									.pageSize(Integer.MAX_VALUE))
					.map(OntologyTermRepository::toOntologyTerm).collect(toList());
		}
		return emptyList();
	}

	/**
	 * Retrieves an {@link OntologyTerm} for one or more IRIs
	 *
	 * @param iris
	 *            Array of {@link OntologyTerm} IRIs
	 * @return combined {@link OntologyTerm} for the iris.
	 */
	public OntologyTerm getOntologyTerm(String[] iris)
	{
		Fetch fetch = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(attribute -> fetch.field(attribute.getName()));

		List<OntologyTerm> ontologyTerms = Lists.newArrayList();
		for (String iri : iris)
		{
			OntologyTerm ontologyTerm = toOntologyTerm(
					dataService.findOne(ENTITY_NAME, QueryImpl.EQ(ONTOLOGY_TERM_IRI, iri).fetch(fetch)));
			if (ontologyTerm == null)
			{
				return null;
			}
			ontologyTerms.add(ontologyTerm);
		}
		return OntologyTerm.and(ontologyTerms.toArray(new OntologyTerm[0]));
	}

	/**
	 * Retrieves a list of {@link OntologyTerm}s based on the given IRIs
	 *
	 * @param iris
	 *            List of {@link OntologyTerm} IRIs
	 * @return a list of {@link OntologyTerm}
	 */
	public List<OntologyTerm> getOntologyTerms(List<String> iris)
	{
		Fetch fetch = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(attribute -> fetch.field(attribute.getName()));

		List<OntologyTerm> ontologyTerms = dataService
				.findAll(OntologyTermMetaData.ENTITY_NAME,
						QueryImpl.IN(OntologyTermMetaData.ONTOLOGY_TERM_IRI, iris).fetch(fetch))
				.map(OntologyTermRepository::toOntologyTerm).collect(Collectors.toList());

		return ontologyTerms;
	}

	/**
	 * Calculate the distance between any two ontology terms in the ontology tree structure by calculating the
	 * difference in nodePaths.
	 *
	 * @param ontologyTerm1
	 * @param ontologyTerm2
	 *
	 * @return the distance between two ontology terms
	 */
	public Integer getOntologyTermDistance(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2)
	{
		if (ontologyTerm1.getNodePaths().isEmpty() || ontologyTerm2.getNodePaths().isEmpty()) return 0;

		OptionalInt min = ontologyTerm1.getNodePaths().stream()
				.flatMap(nodePath1 -> ontologyTerm2.getNodePaths().stream()
						.map(nodePath2 -> calculateNodePathDistance(nodePath1, nodePath2)))
				.mapToInt(Integer::valueOf).min();

		return min.isPresent() ? min.getAsInt() : 0;
	}

	/**
	 * If any of the nodePaths of both of {@link OntologyTerm}s are within (less and equal) the max distance.
	 * 
	 * @param ontologyTerm1
	 * @param ontologyTerm2
	 * @param maxDistance
	 * @return
	 */
	public boolean areWithinDistance(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2, int maxDistance)
	{
		if (ontologyTerm1.getIRI().equals(ontologyTerm2.getIRI())) return true;

		if (ontologyTerm1.getNodePaths().isEmpty() || ontologyTerm2.getNodePaths().isEmpty()) return false;

		boolean anyMatch = ontologyTerm1.getNodePaths().stream().anyMatch(nodePath1 -> ontologyTerm2.getNodePaths()
				.stream().anyMatch(nodePath2 -> calculateNodePathDistance(nodePath1, nodePath2) <= maxDistance));

		return anyMatch;
	}

	/**
	 * Calculate the semantic relatedness between any two ontology terms in the ontology tree
	 *
	 * @param ontologyTerm1
	 * @param ontologyTerm2
	 *
	 * @return the distance between two ontology terms
	 */
	public double getOntologyTermSemanticRelatedness(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2)
	{
		if (ontologyTerm1.getIRI().equals(ontologyTerm2.getIRI())) return 1;

		if (ontologyTerm1.getNodePaths().isEmpty() || ontologyTerm2.getNodePaths().isEmpty()) return 0;

		OptionalDouble max = ontologyTerm1.getNodePaths().stream()
				.flatMap(nodePath1 -> ontologyTerm2.getNodePaths().stream()
						.map(nodePath2 -> calculateRelatedness(nodePath1, nodePath2)))
				.mapToDouble(Double::valueOf).max();

		return max.isPresent() ? max.getAsDouble() : 0;
	}

	/**
	 * Calculate the distance between nodePaths, e.g. 0[0].1[1].2[2], 0[0].2[1].2[2]. The distance is the non-overlap
	 * part of the strings
	 *
	 * @param nodePath1
	 * @param nodePath2
	 * @return distance
	 */
	public int calculateNodePathDistance(String nodePath1, String nodePath2)
	{
		String[] nodePathFragment1 = isBlank(nodePath1) ? new String[0] : nodePath1.split(ESCAPED_NODEPATH_SEPARATOR);
		String[] nodePathFragment2 = isBlank(nodePath2) ? new String[0] : nodePath2.split(ESCAPED_NODEPATH_SEPARATOR);

		int overlapBlock = calculateOverlapBlock(nodePathFragment1, nodePathFragment2);
		return nodePathFragment1.length + nodePathFragment2.length - overlapBlock * 2;
	}

	public double calculateRelatedness(String nodePath1, String nodePath2)
	{
		String[] nodePathFragment1 = isBlank(nodePath1) ? new String[0] : nodePath1.split(ESCAPED_NODEPATH_SEPARATOR);
		String[] nodePathFragment2 = isBlank(nodePath2) ? new String[0] : nodePath2.split(ESCAPED_NODEPATH_SEPARATOR);

		int overlapBlock = calculateOverlapBlock(nodePathFragment1, nodePathFragment2);

		if (nodePathFragment1.length == 0 || nodePathFragment2.length == 0) return 0;

		return (double) 2 * overlapBlock / (nodePathFragment1.length + nodePathFragment2.length);
	}

	public int calculateOverlapBlock(String[] nodePathFragment1, String[] nodePathFragment2)
	{
		int overlapBlock = 0;
		while (overlapBlock < nodePathFragment1.length && overlapBlock < nodePathFragment2.length
				&& nodePathFragment1[overlapBlock].equals(nodePathFragment2[overlapBlock]))
		{
			overlapBlock++;
		}
		return overlapBlock;
	}

	public Stream<OntologyTerm> getChildren(OntologyTerm ontologyTerm, OntologyTermChildrenPredicate continuePredicate)
	{
		return stream(getChildrenByPredicate(ontologyTerm, continuePredicate).spliterator(), false);
	}

	private Iterable<OntologyTerm> getChildrenByPredicate(OntologyTerm ontologyTerm,
			OntologyTermChildrenPredicate continuePredicate)
	{
		Fetch fetch = new Fetch();
		OntologyTermMetaData.INSTANCE.getAtomicAttributes().forEach(attribute -> fetch.field(attribute.getName()));

		Entity ontologyTermEntity = dataService.findOne(ENTITY_NAME,
				QueryImpl.EQ(ONTOLOGY_TERM_IRI, ontologyTerm.getIRI()).fetch(fetch));

		Iterable<OntologyTerm> iterable = null;

		if (ontologyTermEntity != null)
		{
			OntologyTerm ot = toOntologyTerm(ontologyTermEntity);
			Entity ontologyEntity = ontologyTermEntity.getEntity(OntologyTermMetaData.ONTOLOGY);

			// Since children will be the same no matter which one of nodePaths is used.
			if (ot.getNodePaths().size() > 0)
			{
				// The nodePaths that start with the same starting point have the same children in UMLS
				Multimap<String, String> uniqueSubTrees = LinkedHashMultimap.create();
				for (String nodePath : ot.getNodePaths())
				{
					String startNodePath = nodePath.split(ESCAPED_NODEPATH_SEPARATOR)[0];
					uniqueSubTrees.put(startNodePath, nodePath);
				}

				for (Entry<String, Collection<String>> entrySet : uniqueSubTrees.asMap().entrySet())
				{
					String nodePath = entrySet.getValue().iterator().next();
					Iterable<OntologyTerm> childOntologyTermStream = childOntologyTermStream(ontologyTerm,
							ontologyEntity, nodePath, continuePredicate);
					iterable = iterable == null ? childOntologyTermStream
							: Iterables.concat(iterable, childOntologyTermStream);
				}
			}
		}

		return iterable == null ? emptyList() : iterable;
	}

	// FIXME: this is a work around for getting the children of the currentNodePathEntity. The essential problem is
	// that ElasticSearch doesn't support the startsWith type of search. Therefore we first of all need to get all the
	// nodePaths that look similar to the currentNodePath of interest. Then we filter out all the nodePaths that are not
	// actually the children of the currentNodePath of interest. As the size of ontology gets bigger, it can be a very
	// expensive operation, luckily all the similar nodePaths are sorted based on the relevance, so we can stop looking
	// when we encounter the first nodePath (mismatch) that is not a child of the currentNodePath because we know the
	// rest of the nodePaths cannot be more similar than the first mismatch.
	Iterable<OntologyTerm> childOntologyTermStream(OntologyTerm ontologyTerm, Entity ontologyEntity,
			final String parentNodePath, OntologyTermChildrenPredicate continuePredicate)
	{
		Query ontologyTermNodePathQuery = new QueryImpl(new QueryRule(
				OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH, FUZZY_MATCH, "\"" + parentNodePath + "\""));

		Iterable<Entity> ontologyTermNodePathEntities = new Iterable<Entity>()
		{
			public Iterator<Entity> iterator()
			{
				Iterator<Entity> ontologyTermIterator = new Iterator<Entity>()
				{
					private Entity prevEntity = null;
					private final Iterator<Entity> ontologyTermNodePathIterator = dataService
							.findAll(OntologyTermNodePathMetaData.ENTITY_NAME, ontologyTermNodePathQuery).iterator();

					public boolean hasNext()
					{
						boolean continueIteration = true;
						if (prevEntity != null)
						{
							continueIteration = calculateNodePathDistance(
									prevEntity.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH),
									parentNodePath) <= continuePredicate.getSearchLevel();
						}
						return ontologyTermNodePathIterator.hasNext() && continueIteration;
					}

					public Entity next()
					{
						prevEntity = ontologyTermNodePathIterator.next();
						if (LOG.isTraceEnabled())
						{
							LOG.trace("Parent: " + parentNodePath + "; Child: "
									+ prevEntity.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH));
						}
						return prevEntity;
					}
				};
				return Iterators.filter(ontologyTermIterator,
						entity -> !entity.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH)
								.equals(parentNodePath)
								&& entity.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH)
										.startsWith(parentNodePath));
			}
		};

		Query ontologyTermQuery = new QueryImpl(
				new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, IN, ontologyTermNodePathEntities)).and()
						.eq(OntologyTermMetaData.ONTOLOGY, ontologyEntity);

		Iterable<OntologyTerm> ontologyTermIterable = new Iterable<OntologyTerm>()
		{
			@Override
			public Iterator<OntologyTerm> iterator()
			{
				return dataService.findAll(OntologyTermMetaData.ENTITY_NAME, ontologyTermQuery)
						.map(OntologyTermRepository::toOntologyTerm).iterator();
			}
		};

		return ontologyTermIterable;
	}

	public List<OntologyTerm> getParents(OntologyTerm ontologyTerm, OntologyTermChildrenPredicate continuePredicate)
	{
		List<OntologyTerm> parentOntologyTerms = new ArrayList<>();

		List<String> nodePaths = ontologyTerm.getNodePaths();

		for (int i = 0; i < continuePredicate.getSearchLevel(); i++)
		{
			nodePaths = nodePaths.stream().map(this::getParentNodePath).filter(StringUtils::isNotBlank)
					.collect(toList());

			if (nodePaths.size() > 0)
			{
				List<String> nodePathEntityIdentifiers = nodePaths.stream()
						.map(nodePath -> dataService.findOne(OntologyTermNodePathMetaData.ENTITY_NAME,
								EQ(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH, nodePath)))
						.filter(Objects::nonNull).map(Entity::getIdValue).map(Object::toString).collect(toList());

				if (nodePathEntityIdentifiers.size() > 0)
				{
					List<OntologyTerm> ontologyTerms = dataService
							.findAll(OntologyTermMetaData.ENTITY_NAME,
									IN(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, nodePathEntityIdentifiers))
							.map(OntologyTermRepository::toOntologyTerm).filter(Objects::nonNull).collect(toList());

					nodePaths = ontologyTerms.stream().flatMap(ot -> ot.getNodePaths().stream())
							.collect(Collectors.toList());

					parentOntologyTerms.addAll(ontologyTerms);
				}
			}
			else break;
		}

		return parentOntologyTerms;
	}

	/**
	 * Get all {@link SemanticType}s
	 *
	 * @return a list of {@link SemanticType}s
	 */
	public List<SemanticType> getAllSemanticType()
	{
		return dataService.findAll(SemanticTypeMetaData.ENTITY_NAME).map(OntologyTermRepository::entityToSemanticType)
				.collect(toList());
	}

	public List<SemanticType> getSemanticTypesByNames(List<String> semanticTypeNames)
	{
		if (semanticTypeNames.size() > 0)
		{
			return dataService
					.findAll(SemanticTypeMetaData.ENTITY_NAME,
							QueryImpl.IN(SemanticTypeMetaData.SEMANTIC_TYPE_NAME, semanticTypeNames))
					.map(OntologyTermRepository::entityToSemanticType).collect(toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Get all {@link SemanticType}s by a list of {@link SemanticType} groups
	 *
	 * @param semanticTypeGroups
	 * @return a list of {@link SemanticType}s
	 */
	public List<SemanticType> getSemanticTypesByGroups(List<String> semanticTypeGroups)
	{
		if (semanticTypeGroups.size() > 0)
		{
			return dataService
					.findAll(SemanticTypeMetaData.ENTITY_NAME,
							QueryImpl.IN(SemanticTypeMetaData.SEMANTIC_TYPE_GROUP, semanticTypeGroups))
					.map(OntologyTermRepository::entityToSemanticType).collect(toList());
		}
		return Collections.emptyList();
	}

	private String getParentNodePath(String currentNodePath)
	{
		String[] split = currentNodePath.split(ESCAPED_NODEPATH_SEPARATOR);
		if (split.length > 0)
		{
			return join(of(split).limit(split.length - 1).collect(toList()), NODEPATH_SEPARATOR);
		}
		return StringUtils.EMPTY;
	}

	public static SemanticType entityToSemanticType(Entity entity)
	{
		String id = entity.getString(SemanticTypeMetaData.ID);
		String name = entity.getString(SemanticTypeMetaData.SEMANTIC_TYPE_NAME);
		String group = entity.getString(SemanticTypeMetaData.SEMANTIC_TYPE_GROUP);
		// TODO: For test purpose, now everything is used to match source attributes
		Boolean globalKeyConcept = entity.getBoolean(SemanticTypeMetaData.SEMANTIC_TYPE_GLOBAL_KEY_CONCEPT);
		// Boolean globalKeyConcept = true;
		return SemanticType.create(id, name, group, globalKeyConcept == null ? true : globalKeyConcept);
	}

	public static OntologyTerm toOntologyTerm(Entity entity)
	{
		if (entity == null)
		{
			return null;
		}

		// Collect synonyms if there are any
		List<String> synonyms = new ArrayList<>();
		Iterable<Entity> ontologyTermSynonymEntities = entity.getEntities(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM);
		if (ontologyTermSynonymEntities != null)
		{
			synonyms.addAll(stream(ontologyTermSynonymEntities.spliterator(), false)
					.map(e -> e.getString(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM)).collect(toSet()));
		}

		// Collection nodePaths is there are any
		List<String> nodePaths = new ArrayList<>();
		Iterable<Entity> ontologyTermNodePathEntities = entity
				.getEntities(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH);
		if (ontologyTermNodePathEntities != null)
		{
			nodePaths.addAll(stream(ontologyTermNodePathEntities.spliterator(), false)
					.map(e -> e.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH)).collect(toList()));
		}

		// Collect annotations if there are any
		List<OntologyTermAnnotation> annotations = new ArrayList<>();
		Iterable<Entity> ontologyTermAnnotationEntities = entity.getEntities(ONTOLOGY_TERM_DYNAMIC_ANNOTATION);
		if (ontologyTermAnnotationEntities != null)
		{
			for (Entity annotationEntity : ontologyTermAnnotationEntities)
			{
				String annotationName = annotationEntity.getString(OntologyTermDynamicAnnotationMetaData.NAME);
				String annotationValue = annotationEntity.getString(OntologyTermDynamicAnnotationMetaData.VALUE);
				annotations.add(OntologyTermAnnotation.create(annotationName, annotationValue));
			}
		}

		// Collect semantic types if there are any
		List<SemanticType> semanticTypes = new ArrayList<>();
		Iterable<Entity> ontologyTermSemanticTypeEntities = entity.getEntities(ONTOLOGY_TERM_SEMANTIC_TYPE);
		if (ontologyTermSemanticTypeEntities != null)
		{
			List<SemanticType> collect = StreamSupport.stream(ontologyTermSemanticTypeEntities.spliterator(), false)
					.map(OntologyTermRepository::entityToSemanticType).collect(toList());
			semanticTypes.addAll(collect);
		}

		return OntologyTerm.create(entity.getString(ID), entity.getString(ONTOLOGY_TERM_IRI),
				entity.getString(ONTOLOGY_TERM_NAME), null, synonyms, nodePaths, annotations, semanticTypes);
	}

	public int getNodePathLevel(String nodePath)
	{
		return nodePath.split(ESCAPED_NODEPATH_SEPARATOR).length;
	}
}
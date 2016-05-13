package org.molgenis.ontology.core.repository;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.molgenis.data.QueryRule.Operator.AND;
import static org.molgenis.data.QueryRule.Operator.FUZZY_MATCH;
import static org.molgenis.data.QueryRule.Operator.IN;
import static org.molgenis.data.QueryRule.Operator.OR;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ENTITY_NAME;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_DYNAMIC_ANNOTATION;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_IRI;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_NAME;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.meta.OntologyMetaData;
import org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotationMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.molgenis.ontology.core.meta.OntologyTermSynonymMetaData;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OntologyTermAnnotation;
import org.molgenis.ontology.core.model.OntologyTermChildrenPredicate;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import static java.util.Objects.requireNonNull;

/**
 * Maps {@link OntologyTermMetaData} {@link Entity} <-> {@link OntologyTerm}
 */
public class OntologyTermRepository
{
	private static final String ESCAPED_NODEPATH_SEPARATOR = "\\.";
	private static final String NODEPATH_SEPARATOR = ".";

	private final DataService dataService;

	private final static int PENALIZE_EMPTY_PATH = 10;

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
	 * Finds exact {@link OntologyTerm}s within {@link Ontology}s.
	 * 
	 * @param ontologyIds
	 *            IDs of the {@link Ontology}s to search in
	 * @param terms
	 *            {@link List} of search terms. the {@link OntologyTerm} must match at least one of these terms
	 * @param pageSize
	 *            max number of results
	 * @return {@link List} of {@link OntologyTerm}s
	 */
	public List<OntologyTerm> findExcatOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize)
	{
		List<OntologyTerm> findOntologyTerms = findOntologyTerms(ontologyIds, terms, pageSize);
		return findOntologyTerms.stream().filter(ontologyTerm -> isOntologyTermExactMatch(terms, ontologyTerm))
				.collect(Collectors.toList());
	}

	private boolean isOntologyTermExactMatch(Set<String> terms, OntologyTerm ontologyTerm)
	{
		Set<String> lowerCaseSearchTerms = terms.stream().map(StringUtils::lowerCase).collect(Collectors.toSet());
		for (String synonym : ontologyTerm.getSynonyms())
		{
			if (lowerCaseSearchTerms.contains(synonym.toLowerCase()))
			{
				return true;
			}
		}
		if (lowerCaseSearchTerms.contains(ontologyTerm.getLabel().toLowerCase()))
		{
			return true;
		}
		return false;
	}

	/**
	 * Finds {@link OntologyTerm}s within {@link Ontology}s.
	 * 
	 * @param ontologyIds
	 *            IDs of the {@link Ontology}s to search in
	 * @param terms
	 *            {@link List} of search terms. the {@link OntologyTerm} must match at least one of these terms
	 * @param pageSize
	 *            max number of results
	 * @return {@link List} of {@link OntologyTerm}s
	 */
	public List<OntologyTerm> findOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize)
	{
		List<QueryRule> rules = new ArrayList<QueryRule>();
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

		final List<QueryRule> finalRules = rules;
		Iterable<Entity> termEntities = new Iterable<Entity>()
		{
			@Override
			public Iterator<Entity> iterator()
			{
				return dataService.findAll(ENTITY_NAME, new QueryImpl(finalRules).pageSize(pageSize)).iterator();
			}
		};

		return Lists.newArrayList(Iterables.transform(termEntities, OntologyTermRepository::toOntologyTerm));
	}

	public List<OntologyTerm> findAndFilterOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize,
			List<OntologyTerm> ontologyTermScope)
	{
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

		List<String> filteredOntologyTermIris = ontologyTermScope.stream().map(OntologyTerm::getIRI)
				.collect(Collectors.toList());

		rules = Arrays.asList(new QueryRule(ONTOLOGY_TERM_IRI, IN, filteredOntologyTermIris), new QueryRule(AND),
				new QueryRule(rules));

		final List<QueryRule> finalRules = rules;
		Iterable<Entity> termEntities = new Iterable<Entity>()
		{
			@Override
			public Iterator<Entity> iterator()
			{
				return dataService.findAll(ENTITY_NAME, new QueryImpl(finalRules).pageSize(pageSize)).iterator();
			}
		};

		return Lists.newArrayList(Iterables.transform(termEntities, OntologyTermRepository::toOntologyTerm));
	}

	public List<OntologyTerm> getAllOntologyTerms(String ontologyId)
	{
		Entity ontologyEntity = dataService.findOne(OntologyMetaData.ENTITY_NAME,
				new QueryImpl().eq(OntologyMetaData.ONTOLOGY_IRI, ontologyId));

		if (ontologyEntity != null)
		{
			Iterable<Entity> ontologyTermEntities = new Iterable<Entity>()
			{

				@Override
				public Iterator<Entity> iterator()
				{
					return dataService
							.findAll(OntologyTermMetaData.ENTITY_NAME, new QueryImpl()
									.eq(OntologyTermMetaData.ONTOLOGY, ontologyEntity).pageSize(Integer.MAX_VALUE))
							.iterator();
				}
			};

			return stream(ontologyTermEntities.spliterator(), false).map(OntologyTermRepository::toOntologyTerm)
					.collect(toList());
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
		List<OntologyTerm> ontologyTerms = Lists.newArrayList();
		for (String iri : iris)
		{
			OntologyTerm ontologyTerm = toOntologyTerm(
					dataService.findOne(ENTITY_NAME, QueryImpl.EQ(ONTOLOGY_TERM_IRI, iri)));
			if (ontologyTerm == null)
			{
				return null;
			}
			ontologyTerms.add(ontologyTerm);
		}
		return OntologyTerm.and(ontologyTerms.toArray(new OntologyTerm[0]));
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
		// If the list of NodePaths is empty, add an empty string to the list so that it can be calculated
		List<String> listOfNodePath1 = ontologyTerm1.getNodePaths().isEmpty() ? Arrays.asList(StringUtils.EMPTY)
				: ontologyTerm1.getNodePaths();
		List<String> listOfNodePath2 = ontologyTerm2.getNodePaths().isEmpty() ? Arrays.asList(StringUtils.EMPTY)
				: ontologyTerm2.getNodePaths();

		int shortestDistance = 0;
		for (String nodePath1 : listOfNodePath1)
		{
			for (String nodePath2 : listOfNodePath2)
			{
				int distance = calculateNodePathDistance(nodePath1, nodePath2);
				if (shortestDistance == 0 || distance < shortestDistance)
				{
					shortestDistance = distance;
				}
			}
		}
		return shortestDistance;
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
		// If the list of NodePaths is empty, add an empty string to the list so that it can be calculated
		List<String> listOfNodePath1 = ontologyTerm1.getNodePaths().isEmpty() ? asList(EMPTY)
				: ontologyTerm1.getNodePaths();
		List<String> listOfNodePath2 = ontologyTerm2.getNodePaths().isEmpty() ? asList(EMPTY)
				: ontologyTerm2.getNodePaths();

		double maxRelatedness = 0;
		for (String nodePath1 : listOfNodePath1)
		{
			for (String nodePath2 : listOfNodePath2)
			{
				double relatedness = calculateRelatedness(nodePath1, nodePath2);
				if (maxRelatedness == 0 || maxRelatedness < relatedness)
				{
					maxRelatedness = relatedness;
				}
			}
		}
		return maxRelatedness;
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
		return penalize(nodePath1) + penalize(nodePath2) + nodePathFragment1.length + nodePathFragment2.length
				- overlapBlock * 2;
	}

	public double calculateRelatedness(String nodePath1, String nodePath2)
	{
		String[] nodePathFragment1 = isBlank(nodePath1) ? new String[0] : nodePath1.split(ESCAPED_NODEPATH_SEPARATOR);
		String[] nodePathFragment2 = isBlank(nodePath2) ? new String[0] : nodePath2.split(ESCAPED_NODEPATH_SEPARATOR);

		int overlapBlock = calculateOverlapBlock(nodePathFragment1, nodePathFragment2);
		overlapBlock = overlapBlock == 0 ? 1 : overlapBlock;

		int depth1 = nodePathFragment1.length == 0 ? 1 : nodePathFragment1.length;
		int depth2 = nodePathFragment2.length == 0 ? 1 : nodePathFragment2.length;

		return (double) 2 * overlapBlock / (penalize(nodePath1) + penalize(nodePath2) + depth1 + depth2);
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
		return StreamSupport.stream(getChildrenByPredicate(ontologyTerm, continuePredicate).spliterator(), false);
	}

	private Iterable<OntologyTerm> getChildrenByPredicate(OntologyTerm ontologyTerm,
			OntologyTermChildrenPredicate continuePredicate)
	{
		Entity ontologyTermEntity = dataService.findOne(ENTITY_NAME,
				QueryImpl.EQ(ONTOLOGY_TERM_IRI, ontologyTerm.getIRI()));

		Iterable<OntologyTerm> iterable = null;

		if (ontologyTermEntity != null)
		{
			OntologyTerm ot = toOntologyTerm(ontologyTermEntity);
			Entity ontologyEntity = ontologyTermEntity.getEntity(OntologyTermMetaData.ONTOLOGY);

			Multimap<String, String> nodePathFromSameOntology = LinkedHashMultimap.create();
			for (String parentNodePath : ot.getNodePaths())
			{
				String[] split = parentNodePath.split(ESCAPED_NODEPATH_SEPARATOR);
				nodePathFromSameOntology.put(split[split.length - 1], parentNodePath);
			}

			List<Iterable<OntologyTerm>> collect = nodePathFromSameOntology.keySet().stream()
					.map(key -> Iterables.get(nodePathFromSameOntology.get(key), 0))
					.map(nodePath -> childOntologyTermStream(ontologyTerm, ontologyEntity, nodePath, continuePredicate))
					.collect(Collectors.toList());

			iterable = Iterables.concat(collect);
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
			final String parentNodePath, BiPredicate<OntologyTerm, OntologyTerm> continuePredicate)
	{
		Query query = new QueryImpl(
				new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, FUZZY_MATCH, "\"" + parentNodePath + "\""))
						.and().eq(OntologyTermMetaData.ONTOLOGY, ontologyEntity);

		Stream<Entity> ontologyTermEntityStream = dataService.findAll(OntologyTermMetaData.ENTITY_NAME, query);

		Iterable<OntologyTerm> ontologyTermIterable = new Iterable<OntologyTerm>()
		{
			public Iterator<OntologyTerm> iterator()
			{
				Iterator<OntologyTerm> ontologyTermIterator = new Iterator<OntologyTerm>()
				{
					private OntologyTerm prevOt = null;
					private final Iterator<Entity> ontologyTermIterator = ontologyTermEntityStream.iterator();

					public boolean hasNext()
					{
						boolean continueIteration = true;
						if (prevOt != null)
						{
							continueIteration = continuePredicate.test(ontologyTerm, prevOt);
						}
						return ontologyTermIterator.hasNext() && continueIteration;
					}

					public OntologyTerm next()
					{
						prevOt = toOntologyTerm(ontologyTermIterator.next());
						return prevOt;
					}
				};
				return Iterators.filter(ontologyTermIterator, ot -> !ot.getNodePaths().contains(parentNodePath)
						&& ot.getNodePaths().stream().anyMatch(nodePath -> nodePath.startsWith(parentNodePath)));
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

			List<String> parentNodePathEntityIdentifiers = nodePaths.stream()
					.map(nodePath -> dataService.findOne(OntologyTermNodePathMetaData.ENTITY_NAME,
							QueryImpl.EQ(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH, nodePath)))
					.map(entity -> entity.getString(OntologyTermNodePathMetaData.ID)).collect(Collectors.toList());

			List<OntologyTerm> ontologyTerms = dataService
					.findAll(OntologyTermMetaData.ENTITY_NAME,
							QueryImpl.IN(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, parentNodePathEntityIdentifiers))
					.map(OntologyTermRepository::toOntologyTerm).collect(toList());

			nodePaths = ontologyTerms.stream().flatMap(ot -> ot.getNodePaths().stream()).collect(Collectors.toList());

			parentOntologyTerms.addAll(ontologyTerms);
		}

		return parentOntologyTerms;
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

	private static OntologyTerm toOntologyTerm(Entity entity)
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

		return OntologyTerm.create(entity.getString(ONTOLOGY_TERM_IRI), entity.getString(ONTOLOGY_TERM_NAME), null,
				synonyms, nodePaths, annotations);
	}

	int penalize(String nodePath)
	{
		return isBlank(nodePath) ? PENALIZE_EMPTY_PATH : 0;
	}
}
package org.molgenis.ontology.core.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ENTITY_NAME;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_IRI;
import static org.molgenis.ontology.core.meta.OntologyTermMetaData.ONTOLOGY_TERM_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Sets;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Query;
import org.molgenis.data.QueryRule;
import org.molgenis.data.QueryRule.Operator;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.core.meta.OntologyMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.molgenis.ontology.core.meta.OntologyTermSynonymMetaData;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.FluentIterable;

import static java.util.Objects.requireNonNull;

/**
 * Maps {@link OntologyTermMetaData} {@link Entity} <-> {@link OntologyTerm}
 */
public class OntologyTermRepository
{
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
				rules.add(new QueryRule(Operator.OR));
			}
			rules.add(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, Operator.FUZZY_MATCH, term));
		}
		rules = Arrays.asList(new QueryRule(ONTOLOGY, Operator.IN, ontologyIds), new QueryRule(Operator.AND),
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
			List<OntologyTerm> filteredOntologyTerms)
	{
		List<QueryRule> rules = new ArrayList<QueryRule>();
		for (String term : terms)
		{
			if (rules.size() > 0)
			{
				rules.add(new QueryRule(Operator.OR));
			}
			rules.add(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM, Operator.FUZZY_MATCH, term));
		}
		rules = Arrays.asList(new QueryRule(ONTOLOGY, Operator.IN, ontologyIds), new QueryRule(Operator.AND),
				new QueryRule(rules));

		List<String> filteredOntologyTermIris = filteredOntologyTerms.stream().map(OntologyTerm::getIRI)
				.collect(Collectors.toList());

		rules = Arrays.asList(
				new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_IRI, Operator.IN, filteredOntologyTermIris),
				new QueryRule(Operator.AND), new QueryRule(rules));

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

			return Lists
					.newArrayList(Iterables.transform(ontologyTermEntities, OntologyTermRepository::toOntologyTerm));
		}

		return Collections.emptyList();
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
		List<String> listOfNodePath1 = ontologyTerm1.getNodePaths().isEmpty() ? Arrays.asList(StringUtils.EMPTY)
				: ontologyTerm1.getNodePaths();
		List<String> listOfNodePath2 = ontologyTerm2.getNodePaths().isEmpty() ? Arrays.asList(StringUtils.EMPTY)
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
		String[] nodePathFragment1 = isBlank(nodePath1) ? new String[0] : nodePath1.split("\\.");
		String[] nodePathFragment2 = isBlank(nodePath2) ? new String[0] : nodePath2.split("\\.");

		int overlapBlock = calculateOverlapBlock(nodePathFragment1, nodePathFragment2);
		return penalize(nodePath1) + penalize(nodePath2) + nodePathFragment1.length + nodePathFragment2.length
				- overlapBlock * 2;
	}

	public double calculateRelatedness(String nodePath1, String nodePath2)
	{
		String[] nodePathFragment1 = isBlank(nodePath1) ? new String[0] : nodePath1.split("\\.");
		String[] nodePathFragment2 = isBlank(nodePath2) ? new String[0] : nodePath2.split("\\.");

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

	/**
	 * Retrieve all descendant ontology terms
	 * 
	 * @param ontologyTerm
	 * @return a list of {@link OntologyTerm}
	 */
	public List<OntologyTerm> getChildren(OntologyTerm ontologyTerm)
	{
		Iterable<Entity> ontologyTermEntities = new Iterable<Entity>()
		{
			@Override
			public Iterator<Entity> iterator()
			{
				return dataService.findAll(ENTITY_NAME, QueryImpl.EQ(ONTOLOGY_TERM_IRI, ontologyTerm.getIRI()))
						.iterator();
			}
		};

		List<OntologyTerm> children = new ArrayList<OntologyTerm>();
		for (Entity ontologyTermEntity : ontologyTermEntities)
		{
			Entity ontologyEntity = ontologyTermEntity.getEntity(OntologyTermMetaData.ONTOLOGY);
			ontologyTermEntity.getEntities(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH)
					.forEach(ontologyTermNodePathEntity -> children
							.addAll(getChildOntologyTermsByNodePath(ontologyEntity, ontologyTermNodePathEntity)));
		}
		return children;
	}

	public List<OntologyTerm> getChildOntologyTermsByNodePath(Entity ontologyEntity, Entity nodePathEntity)
	{
		String nodePath = nodePathEntity.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH);

		Iterable<Entity> relatedOntologyTermEntities = new Iterable<Entity>()
		{
			@Override
			public Iterator<Entity> iterator()
			{
				return dataService.findAll(OntologyTermMetaData.ENTITY_NAME,
						new QueryImpl(new QueryRule(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH, Operator.FUZZY_MATCH,
								"\"" + nodePath + "\"")).and().eq(OntologyTermMetaData.ONTOLOGY, ontologyEntity))
						.iterator();
			}
		};
		Iterable<Entity> childOntologyTermEntities = FluentIterable.from(relatedOntologyTermEntities)
				.filter(entity -> qualifiedNodePath(nodePath, entity)).toList();

		return Lists
				.newArrayList(Iterables.transform(childOntologyTermEntities, OntologyTermRepository::toOntologyTerm));
	}

	private boolean qualifiedNodePath(String nodePath, Entity entity)
	{
		Iterable<Entity> nodePathEntities = entity.getEntities(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH);
		return Lists.newArrayList(nodePathEntities).stream().anyMatch(nodePathEntity -> {
			String childNodePath = nodePathEntity.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH);
			return !StringUtils.equals(nodePath, childNodePath) && childNodePath.startsWith(nodePath);
		});
	}

	private static OntologyTerm toOntologyTerm(Entity entity)
	{
		if (entity == null)
		{
			return null;
		}

		// Collect synonyms if there are any
		String ontologyTermName = entity.getString(ONTOLOGY_TERM_NAME).toLowerCase();
		Set<String> synonyms = Sets.newHashSet(ontologyTermName);
		Iterable<Entity> ontologyTermSynonymEntities = entity.getEntities(OntologyTermMetaData.ONTOLOGY_TERM_SYNONYM);
		if (ontologyTermSynonymEntities != null)
		{
			synonyms.addAll(StreamSupport.stream(ontologyTermSynonymEntities.spliterator(), false)
					.map(e -> e.getString(OntologyTermSynonymMetaData.ONTOLOGY_TERM_SYNONYM).toLowerCase())
					.collect(Collectors.toSet()));
		}

		// Collection nodePaths is there are any
		Set<String> nodePaths = new HashSet<>();
		Iterable<Entity> ontologyTermNodePathEntities = entity
				.getEntities(OntologyTermMetaData.ONTOLOGY_TERM_NODE_PATH);
		if (ontologyTermNodePathEntities != null)
		{
			nodePaths.addAll(StreamSupport.stream(ontologyTermNodePathEntities.spliterator(), false)
					.map(e -> e.getString(OntologyTermNodePathMetaData.ONTOLOGY_TERM_NODE_PATH))
					.collect(Collectors.toList()));
		}

		return OntologyTerm.create(entity.getString(ONTOLOGY_TERM_IRI), ontologyTermName, null,
				Lists.newArrayList(synonyms), Lists.newArrayList(nodePaths));

	}

	int penalize(String nodePath)
	{
		return isBlank(nodePath) ? PENALIZE_EMPTY_PATH : 0;
	}

}
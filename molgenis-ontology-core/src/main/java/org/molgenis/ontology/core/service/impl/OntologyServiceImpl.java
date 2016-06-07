package org.molgenis.ontology.core.service.impl;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.molgenis.ontology.utils.PredicateUtils.createRetrieveLevelThreePredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.elasticsearch.common.collect.Lists;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.ontology.core.model.ConditionalChildrenRetrieval;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.OntologyTermChildrenPredicate;
import org.molgenis.ontology.core.model.SemanticType;
import org.molgenis.ontology.core.repository.OntologyRepository;
import org.molgenis.ontology.core.repository.OntologyTermRepository;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class OntologyServiceImpl implements OntologyService
{
	private final static String ONTOLOGY_TERM_IRI_SEPARATOR = ",";
	private OntologyRepository ontologyRepository;
	private OntologyTermRepository ontologyTermRepository;

	private LoadingCache<ConditionalChildrenRetrieval, List<OntologyTerm>> cachedOntologyTermChildren = CacheBuilder
			.newBuilder().maximumSize(1000).expireAfterWrite(1, TimeUnit.HOURS)
			.build(new CacheLoader<ConditionalChildrenRetrieval, List<OntologyTerm>>()
			{
				public List<OntologyTerm> load(ConditionalChildrenRetrieval conditionalChildrenRetrieval)
				{
					return ontologyTermRepository.getChildren(conditionalChildrenRetrieval.getOntologyTerm(),
							conditionalChildrenRetrieval.getContinuePredicate()).collect(Collectors.toList());
				}
			});

	@Autowired
	public OntologyServiceImpl(OntologyRepository ontologyRepository, OntologyTermRepository ontologyTermRepository)
	{
		this.ontologyRepository = requireNonNull(ontologyRepository);
		this.ontologyTermRepository = requireNonNull(ontologyTermRepository);
	}

	@Override
	public List<Ontology> getOntologies()
	{
		return ontologyRepository.getOntologies().collect(toList());
	}

	@Override
	public List<String> getAllOntologiesIds()
	{
		final List<String> allOntologiesIds = new ArrayList<String>();
		ontologyRepository.getOntologies().forEach(e -> allOntologiesIds.add(e.getId()));
		return allOntologiesIds;
	}

	@Override
	public Ontology getOntology(String name)
	{
		return ontologyRepository.getOntology(name);
	}

	@Override
	public OntologyTerm getOntologyTerm(String iri)
	{
		return ontologyTermRepository.getOntologyTerm(iri.split(","));

	}

	@Override
	public List<OntologyTerm> getAllOntologyTerms(String ontologyIri)
	{
		return ontologyTermRepository.getAllOntologyTerms(ontologyIri);
	}

	@Override
	public List<OntologyTerm> findExcatOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize)
	{
		if (null == terms || terms.size() == 0)
		{
			return Lists.<OntologyTerm> newArrayList();
		}
		return ontologyTermRepository.findExcatOntologyTerms(ontologyIds, terms, pageSize);
	}

	@Override
	public List<OntologyTerm> findOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize)
	{
		if (null == terms || terms.size() == 0)
		{
			return Lists.<OntologyTerm> newArrayList();
		}
		return ontologyTermRepository.findOntologyTerms(ontologyIds, terms, pageSize);
	}

	@Override
	public List<OntologyTerm> fileterOntologyTerms(List<String> ontologyIds, Set<String> terms, int pageSize,
			List<OntologyTerm> filteredOntologyTerms)
	{
		if (null == terms || terms.size() == 0)
		{
			return Lists.<OntologyTerm> newArrayList();
		}
		return ontologyTermRepository.findAndFilterOntologyTerms(ontologyIds, terms, pageSize, filteredOntologyTerms);
	}

	@Override
	public List<OntologyTerm> getChildren(OntologyTerm ontologyTerm, OntologyTermChildrenPredicate continuePredicate)
	{
		try
		{
			return cachedOntologyTermChildren.get(ConditionalChildrenRetrieval.create(ontologyTerm, continuePredicate));
		}
		catch (ExecutionException e)
		{
			throw new MolgenisDataAccessException(e.getMessage());
		}
	}

	@Override
	public List<OntologyTerm> getParents(OntologyTerm ontologyTerm, OntologyTermChildrenPredicate continuePredicate)
	{
		return ontologyTermRepository.getParents(ontologyTerm, continuePredicate);
	}

	@Override
	public List<OntologyTerm> getLevelThreeChildren(OntologyTerm ontologyTerm)
	{
		OntologyTermChildrenPredicate continuePredicate = createRetrieveLevelThreePredicate(this);
		return getChildren(ontologyTerm, continuePredicate);
	}

	@Override
	public Integer getOntologyTermDistance(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2)
	{
		return ontologyTermRepository.getOntologyTermDistance(ontologyTerm1, ontologyTerm2);
	}

	@Override
	public Double getOntologyTermSemanticRelatedness(OntologyTerm ontologyTerm1, OntologyTerm ontologyTerm2)
	{
		return ontologyTermRepository.getOntologyTermSemanticRelatedness(ontologyTerm1, ontologyTerm2);
	}

	@Override
	public List<OntologyTerm> getAtomicOntologyTerms(OntologyTerm ontologyTerm)
	{
		List<OntologyTerm> ontologyTerms = new ArrayList<>();

		if (nonNull(ontologyTerm))
		{
			for (String atomicOntologyTermIri : ontologyTerm.getIRI().split(ONTOLOGY_TERM_IRI_SEPARATOR))
			{
				if (isNotBlank(atomicOntologyTermIri))
				{
					OntologyTerm atomicOntologyTerm = getOntologyTerm(atomicOntologyTermIri);
					if (atomicOntologyTerm != null)
					{
						ontologyTerms.add(atomicOntologyTerm);
					}
				}
			}
		}

		return ontologyTerms;
	}

	@Override
	public List<SemanticType> getAllSemanticTypes()
	{
		return ontologyTermRepository.getAllSemanticType();
	}

	@Override
	public List<SemanticType> getSemanticTypesByGroups(List<String> semanticTypeGroups)
	{
		return ontologyTermRepository.getSemanticTypesByGroups(semanticTypeGroups);
	}

	@Override
	public List<SemanticType> getSemanticTypes(OntologyTerm ontologyTerm)
	{
		return ontologyTermRepository.getSemanticTypes(ontologyTerm);
	}

	@Override
	public List<SemanticType> getSemanticTypesByNames(List<String> semanticTypeNames)
	{
		return ontologyTermRepository.getSemanticTypesByNames(semanticTypeNames);
	}
}
package org.molgenis.data.discovery.service.impl;

import static java.util.Collections.emptyList;

import java.util.List;

import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.DataService;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Objects.requireNonNull;

public class BiobankUniverseServiceImpl implements BiobankUniverseService
{
	private final IdGenerator idGenerator;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final DataService dataService;
	private final OntologyService ontologyService;
	private final SemanticSearchService semanticSearchService;
	private final AttributeMappingExplainService attributeMappingExplainService;

	@Autowired
	public BiobankUniverseServiceImpl(IdGenerator idGenerator, BiobankUniverseRepository biobankUniverseRepository,
			DataService dataService, OntologyService ontologyService, SemanticSearchService semanticSearchService,
			AttributeMappingExplainService attributeMappingExplainService)
	{
		this.idGenerator = requireNonNull(idGenerator);
		this.biobankUniverseRepository = biobankUniverseRepository;
		this.dataService = requireNonNull(dataService);
		this.ontologyService = requireNonNull(ontologyService);
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.attributeMappingExplainService = requireNonNull(attributeMappingExplainService);
	}

	@Override
	public List<AttributeMappingCandidate> findAttributeMappingCandidates(String queryTerm)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BiobankUniverse createBiobankUniverse(String universeName, MolgenisUser owner)
	{
		BiobankUniverse biobankUniverse = BiobankUniverse.create(idGenerator.generateId(), universeName, emptyList(),
				owner);
		biobankUniverseRepository.createBiobankUniverse(biobankUniverse);
		return biobankUniverse;
	}

	@Override
	public List<BiobankUniverse> getAllUniverses()
	{
		return biobankUniverseRepository.getAllUniverses();
	}

	@Override
	public BiobankUniverse getUniverse(String identifier)
	{
		return biobankUniverseRepository.getUniverse(identifier);
	}
}

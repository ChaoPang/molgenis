package org.molgenis.data.discovery.config;

import org.molgenis.data.DataService;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.service.impl.BiobankUniverseServiceImpl;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.user.MolgenisUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

@Configurable
public class DataDiscoveryConfig
{
	@Autowired
	private DataService dataService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private MolgenisUserService molgenisUserService;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private AttributeMappingExplainService attributeMappingExplainService;

	@Autowired
	private SemanticSearchService semanticSearchService;

	@Bean
	public BiobankUniverseRepository biobankUniverseRepository()
	{
		return new BiobankUniverseRepository(dataService, ontologyService, molgenisUserService);
	}

	@Bean
	public BiobankUniverseService biobankUniverseService()
	{
		return new BiobankUniverseServiceImpl(idGenerator, biobankUniverseRepository(), dataService, ontologyService,
				semanticSearchService, attributeMappingExplainService);
	}
}

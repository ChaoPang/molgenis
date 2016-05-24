package org.molgenis.data.discovery.config;

import org.molgenis.data.DataService;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.semanticsearch.config.SemanticSearchConfig;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.ontology.core.config.OntologyConfig;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.user.MolgenisUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(
{ OntologyConfig.class, SemanticSearchConfig.class })
public class DataDiscoveryConfig
{
	@Autowired
	DataService dataService;

	@Autowired
	OntologyService ontologyService;

	@Autowired
	MolgenisUserService molgenisUserService;

	@Autowired
	IdGenerator idGenerator;

	@Autowired
	AttributeMappingExplainService attributeMappingExplainService;

	@Autowired
	SemanticSearchService semanticSearchService;

	// @Bean
	// public BiobankUniverseRepository biobankUniverseRepository()
	// {
	// return new BiobankUniverseRepositoryImpl(dataService, ontologyService, molgenisUserService);
	// }
	//
	// @Bean
	// public BiobankUniverseService biobankUniverseService()
	// {
	// return new BiobankUniverseServiceImpl(idGenerator, biobankUniverseRepository(), dataService, ontologyService,
	// semanticSearchService, attributeMappingExplainService);
	// }
}

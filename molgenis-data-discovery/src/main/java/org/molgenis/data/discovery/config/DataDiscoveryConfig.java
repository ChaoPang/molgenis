package org.molgenis.data.discovery.config;

import org.molgenis.data.DataService;
import org.molgenis.data.EntityManager;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.repo.impl.BiobankUniverseRepositoryImpl;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.discovery.service.OntologyBasedExplainService;
import org.molgenis.data.discovery.service.impl.AttributeTermFrequencyServiceImpl;
import org.molgenis.data.discovery.service.impl.BiobankUniverseServiceImpl;
import org.molgenis.data.discovery.service.impl.OntologyBasedExplainServiceImpl;
import org.molgenis.data.semanticsearch.config.SemanticSearchConfig;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.ontology.core.config.OntologyConfig;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.user.MolgenisUserService;
import org.molgenis.security.user.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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
	EntityManager entityManager;

	@Autowired
	OntologyService ontologyService;

	@Autowired
	MolgenisUserService molgenisUserService;

	@Autowired
	IdGenerator idGenerator;

	@Autowired
	TagGroupGenerator tagGroupGenerator;

	@Autowired
	QueryExpansionService queryExpansionService;

	@Autowired
	ExplainMappingService explainMappingService;

	@Autowired
	UserAccountService userAccountService;

	@Bean
	public BiobankUniverseRepository biobankUniverseRepository()
	{
		return new BiobankUniverseRepositoryImpl(dataService, molgenisUserService, userAccountService, entityManager);
	}

	@Bean
	public BiobankUniverseService biobankUniverseService()
	{
		return new BiobankUniverseServiceImpl(idGenerator, biobankUniverseRepository(), ontologyService,
				tagGroupGenerator, explainMappingService, ontologyBasedExplainService(),
				new AttributeTermFrequencyServiceImpl(dataService));
	}

	@Bean
	public OntologyBasedExplainService ontologyBasedExplainService()
	{
		return new OntologyBasedExplainServiceImpl(idGenerator, ontologyService);
	}
}

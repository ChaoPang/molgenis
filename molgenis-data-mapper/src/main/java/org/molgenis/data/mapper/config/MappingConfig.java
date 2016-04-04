package org.molgenis.data.mapper.config;

import org.molgenis.data.DataService;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.mapper.algorithmgenerator.service.AlgorithmGeneratorService;
import org.molgenis.data.mapper.algorithmgenerator.service.impl.AlgorithmGeneratorServiceImpl;
import org.molgenis.data.mapper.repository.impl.AttributeMappingRepositoryImpl;
import org.molgenis.data.mapper.repository.impl.EntityMappingRepositoryImpl;
import org.molgenis.data.mapper.repository.impl.MappingProjectRepositoryImpl;
import org.molgenis.data.mapper.repository.impl.MappingTargetRepositoryImpl;
import org.molgenis.data.mapper.service.AlgorithmService;
import org.molgenis.data.mapper.service.MappingService;
import org.molgenis.data.mapper.service.UnitResolver;
import org.molgenis.data.mapper.service.impl.AlgorithmServiceImpl;
import org.molgenis.data.mapper.service.impl.AlgorithmTemplateService;
import org.molgenis.data.mapper.service.impl.AlgorithmTemplateServiceImpl;
import org.molgenis.data.mapper.service.impl.MappingServiceImpl;
import org.molgenis.data.mapper.service.impl.UnitResolverImpl;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.ontology.core.config.OntologyConfig;
import org.molgenis.ontology.core.repository.OntologyTermRepository;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.permission.PermissionSystemService;
import org.molgenis.security.user.MolgenisUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(OntologyConfig.class)
public class MappingConfig
{
	@Autowired
	DataService dataService;

	@Autowired
	MolgenisUserService userService;

	@Autowired
	OntologyTagService ontologyTagService;

	@Autowired
	SemanticSearchService semanticSearchService;

	@Autowired
	AttributeMappingExplainService attributeMappingExplainService;

	@Autowired
	OntologyService ontologyService;

	@Autowired
	IdGenerator idGenerator;

	@Autowired
	PermissionSystemService permissionSystemService;

	@Autowired
	OntologyTermRepository ontologyTermRepository;

	@Autowired
	MolgenisUserService molgenisUserService;

	@Bean
	public MappingService mappingService()
	{
		return new MappingServiceImpl(dataService, algorithmServiceImpl(), idGenerator, mappingProjectRepository(),
				mappingTargetRepository(), entityMappingRepository(), attributeMappingRepository(),
				permissionSystemService);
	}

	@Bean
	public AlgorithmGeneratorService algorithmGeneratorService()
	{
		return new AlgorithmGeneratorServiceImpl(dataService, attributeMappingExplainService, unitResolver(),
				algorithmTemplateServiceImpl());
	}

	@Bean
	public AlgorithmService algorithmServiceImpl()
	{
		return new AlgorithmServiceImpl(dataService, semanticSearchService, algorithmGeneratorService());
	}

	@Bean
	public AlgorithmTemplateService algorithmTemplateServiceImpl()
	{
		return new AlgorithmTemplateServiceImpl(dataService, ontologyService, semanticSearchService);
	}

	@Bean
	public MappingProjectRepositoryImpl mappingProjectRepository()
	{
		return new MappingProjectRepositoryImpl(mappingTargetRepository(), dataService, idGenerator,
				molgenisUserService);
	}

	@Bean
	public MappingTargetRepositoryImpl mappingTargetRepository()
	{
		return new MappingTargetRepositoryImpl(entityMappingRepository(), dataService, idGenerator);
	}

	@Bean
	public EntityMappingRepositoryImpl entityMappingRepository()
	{
		return new EntityMappingRepositoryImpl(attributeMappingRepository(), dataService, idGenerator);
	}

	@Bean
	public AttributeMappingRepositoryImpl attributeMappingRepository()
	{
		return new AttributeMappingRepositoryImpl(dataService, idGenerator);
	}

	@Bean
	public UnitResolver unitResolver()
	{
		return new UnitResolverImpl(ontologyService);
	}
}

package org.molgenis.data.semanticsearch.config;

import org.molgenis.data.DataService;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.Repository;
import org.molgenis.data.elasticsearch.factory.EmbeddedElasticSearchServiceFactory;
import org.molgenis.data.meta.TagMetaData;
import org.molgenis.data.semantic.LabeledResource;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.explain.service.impl.AttributeMappingExplainServiceImpl;
import org.molgenis.data.semanticsearch.repository.TagRepository;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.TagService;
import org.molgenis.data.semanticsearch.service.impl.OntologyTagServiceImpl;
import org.molgenis.data.semanticsearch.service.impl.QueryExpansionServiceImpl;
import org.molgenis.data.semanticsearch.service.impl.SemanticSearchServiceImpl;
import org.molgenis.data.semanticsearch.service.impl.TagGroupGeneratorImpl;
import org.molgenis.data.semanticsearch.service.impl.UntypedTagService;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SemanticSearchConfig
{
	@Autowired
	DataService dataService;

	@Autowired
	OntologyService ontologyService;

	@Autowired
	IdGenerator idGenerator;

	@Autowired
	TermFrequencyService termFrequencyService;

	@Autowired
	EmbeddedElasticSearchServiceFactory embeddedElasticSearchServiceFactory;

	// @Bean
	// public SemanticSearchServiceUtils semanticSearchServiceUtils()
	// {
	// return new SemanticSearchServiceUtils(dataService, ontologyService, ontologyTagService(), termFrequencyService);
	// }

	@Bean
	public OntologyTagService ontologyTagService()
	{
		return new OntologyTagServiceImpl(dataService, ontologyService, tagRepository(), idGenerator);
	}

	@Bean
	public SemanticSearchService semanticSearchService()
	{
		return new SemanticSearchServiceImpl(dataService, ontologyService, tagGroupGenerator(), queryExpansionService(),
				attributeMappingExplainService());
	}

	@Bean
	public TagGroupGenerator tagGroupGenerator()
	{
		return new TagGroupGeneratorImpl(ontologyService, ontologyTagService());
	}

	@Bean
	public QueryExpansionService queryExpansionService()
	{
		return new QueryExpansionServiceImpl(ontologyService, termFrequencyService);
	}

	@Bean
	public TagService<LabeledResource, LabeledResource> tagService()
	{
		return new UntypedTagService(dataService, tagRepository());
	}

	@Bean
	TagRepository tagRepository()
	{
		Repository repo = dataService.getRepository(TagMetaData.ENTITY_NAME);
		return new TagRepository(repo, idGenerator);
	}

	@Bean
	AttributeMappingExplainService attributeMappingExplainService()
	{
		return new AttributeMappingExplainServiceImpl(ontologyService, tagGroupGenerator());
	}
}

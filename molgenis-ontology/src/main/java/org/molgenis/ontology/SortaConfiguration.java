package org.molgenis.ontology;

import org.molgenis.data.DataService;
import org.molgenis.ontology.core.meta.OntologyMetaData;
import org.molgenis.ontology.core.meta.OntologyTermDynamicAnnotationMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.molgenis.ontology.core.meta.OntologyTermSynonymMetaData;
import org.molgenis.ontology.ic.OntologyTermFrequencyServiceImpl;
import org.molgenis.ontology.ic.TermFrequencyEntityMetaData;
import org.molgenis.ontology.ic.TermFrequencyService;
import org.molgenis.ontology.roc.InformationContentService;
import org.molgenis.ontology.roc.MatchQualityRocService;
import org.molgenis.ontology.sorta.meta.MatchingTaskContentEntityMetaData;
import org.molgenis.ontology.sorta.meta.MatchingTaskEntityMetaData;
import org.molgenis.ontology.sorta.service.SortaService;
import org.molgenis.ontology.sorta.service.impl.SortaServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SortaConfiguration
{
	@Autowired
	private DataService dataService;

	// Declaring these EntityMetaData beans makes sure their repositories are created in the default backend.
	@Bean
	public OntologyMetaData ontologyMetaData()
	{
		return OntologyMetaData.INSTANCE;
	}

	@Bean
	public OntologyTermSynonymMetaData ontologyTermSynonymMetaData()
	{
		return OntologyTermSynonymMetaData.INSTANCE;
	}

	@Bean
	public OntologyTermDynamicAnnotationMetaData ontologyTermDynamicAnnotationMetaData()
	{
		return OntologyTermDynamicAnnotationMetaData.INSTANCE;
	}

	@Bean
	public OntologyTermNodePathMetaData ontologyTermNodePathMetaData()
	{
		return OntologyTermNodePathMetaData.INSTANCE;
	}

	@Bean
	public OntologyTermMetaData ontologyTermMetaData()
	{
		return OntologyTermMetaData.INSTANCE;
	}

	@Bean
	public MatchingTaskEntityMetaData matchingTaskEntityMetaData()
	{
		return MatchingTaskEntityMetaData.INSTANCE;
	}

	@Bean
	public MatchingTaskContentEntityMetaData matchingTaskContentEntityMetaData()
	{
		return MatchingTaskContentEntityMetaData.INSTANCE;
	}

	@Bean
	public TermFrequencyEntityMetaData termFrequencyEntityMetaData()
	{
		return TermFrequencyEntityMetaData.INSTANCE;
	}

	@Bean
	public TermFrequencyService termFrequencyService()
	{
		return new OntologyTermFrequencyServiceImpl(dataService);
	}

	@Bean
	public SortaService sortaService()
	{
		return new SortaServiceImpl(dataService, informationContentService());
	}

	@Bean
	public MatchQualityRocService matchQualityRocService()
	{
		return new MatchQualityRocService(dataService, sortaService());
	}

	@Bean
	public InformationContentService informationContentService()
	{
		return new InformationContentService(dataService);
	}

	public SortaConfiguration()
	{
		System.setProperty("jdk.xml.entityExpansionLimit", "1280000");
	}
}

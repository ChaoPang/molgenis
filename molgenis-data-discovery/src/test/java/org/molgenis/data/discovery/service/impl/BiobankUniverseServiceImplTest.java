package org.molgenis.data.discovery.service.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.mockito.ArgumentCaptor;
import org.molgenis.auth.MolgenisUser;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;
import org.molgenis.data.discovery.model.biobank.BiobankUniverse;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.discovery.service.BiobankUniverseService;
import org.molgenis.data.semanticsearch.explain.service.ExplainMappingService;
import org.molgenis.data.semanticsearch.service.QueryExpansionService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(classes = BiobankUniverseServiceImplTest.Config.class)
public class BiobankUniverseServiceImplTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	IdGenerator idGenerator;

	@Autowired
	BiobankUniverseRepository biobankUniverseRepository;

	@Autowired
	BiobankUniverseService biobankUniverseService;

	@Autowired
	OntologyService ontologyService;

	MolgenisUser molgenisUser;

	BiobankUniverse biobankUniverse;

	BiobankSampleCollection sampleCollection1;

	BiobankSampleCollection sampleCollection2;

	@BeforeMethod
	public void setup()
	{
		// Set up the generic data
		molgenisUser = new MolgenisUser();
		biobankUniverse = BiobankUniverse.create("1", "universe", emptyList(), molgenisUser, emptyList());
		sampleCollection1 = BiobankSampleCollection.create("sampleCollection1");
		sampleCollection2 = BiobankSampleCollection.create("sampleCollection2");

		// Set up the generic method invocations
		when(ontologyService.getSemanticTypesByGroups(any())).thenReturn(emptyList());
		when(biobankUniverseRepository.getUniverse(biobankUniverse.getIdentifier())).thenReturn(biobankUniverse);
	}

	@Test
	public void addBiobankUniverse()
	{
		when(idGenerator.generateId()).thenReturn(biobankUniverse.getIdentifier());

		assertEquals(biobankUniverseService.addBiobankUniverse(biobankUniverse.getName(), emptyList(), molgenisUser),
				biobankUniverse);

		ArgumentCaptor<BiobankUniverse> biobankUniverseCaptor = forClass(BiobankUniverse.class);

		verify(biobankUniverseRepository).addBiobankUniverse(biobankUniverseCaptor.capture());
	}

	@Test
	public void addBiobankUniverseMember()
	{
		biobankUniverseService.addBiobankUniverseMember(biobankUniverse, asList(sampleCollection1, sampleCollection2));

		ArgumentCaptor<BiobankUniverse> biobankUniverseCaptor = ArgumentCaptor.forClass(BiobankUniverse.class);
		ArgumentCaptor<List> biobankSampleCollectionListCaptor = ArgumentCaptor.forClass(List.class);

		verify(biobankUniverseRepository).addUniverseMembers(biobankUniverseCaptor.capture(),
				biobankSampleCollectionListCaptor.capture());
	}

	@Test
	public void addKeyConcepts()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void applyKeyConceptFilter()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void deleteBiobankUniverse()
	{
		biobankUniverseService.deleteBiobankUniverse(biobankUniverse.getIdentifier());

		ArgumentCaptor<String> identifierCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<BiobankUniverse> biobankUniverseCaptor = ArgumentCaptor.forClass(BiobankUniverse.class);

		verify(biobankUniverseRepository).getUniverse(identifierCaptor.capture());
		verify(biobankUniverseRepository).removeBiobankUniverse(biobankUniverseCaptor.capture());
	}

	@Test
	public void explainAttributeMatchLazy()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void findCandidateMappings()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void findTagGroupsForAttributes()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllBiobankSampleCollections()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllSemanticType()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getBiobankSampleCollection()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getBiobankSampleCollections()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getBiobankUniverse()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getBiobankUniverses()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void isMatchHighQuality()
	{
		throw new RuntimeException("Test not implemented");
	}

	@Configuration
	public static class Config
	{
		@Bean
		public IdGenerator idGenerator()
		{
			return mock(IdGenerator.class);
		}

		@Bean
		public BiobankUniverseRepository biobankUniverseRepository()
		{
			return mock(BiobankUniverseRepository.class);
		}

		@Bean
		public OntologyService ontologyService()
		{
			return mock(OntologyService.class);
		}

		@Bean
		public TagGroupGenerator tagGroupGenerator()
		{
			return mock(TagGroupGenerator.class);
		}

		@Bean
		public QueryExpansionService queryExpansionService()
		{
			return mock(QueryExpansionService.class);
		}

		@Bean
		public ExplainMappingService explainMappingService()
		{
			return mock(ExplainMappingService.class);
		}

		@Bean
		public BiobankUniverseService biobankUniverseService()
		{
			return new BiobankUniverseServiceImpl(idGenerator(), biobankUniverseRepository(), ontologyService(),
					tagGroupGenerator(), queryExpansionService(), explainMappingService());
		}
	}
}

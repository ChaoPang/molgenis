package org.molgenis.data.mapper.job;

import org.mockito.Mock;
import org.molgenis.data.DataService;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.mapper.mapping.model.EntityMapping;
import org.molgenis.data.mapper.mapping.model.MappingProject;
import org.molgenis.data.mapper.mapping.model.MappingTarget;
import org.molgenis.data.mapper.service.MappingService;
import org.molgenis.data.meta.model.EntityType;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.molgenis.data.mapper.service.impl.MappingServiceImpl.MAPPING_BATCH_SIZE;

public class MappingJobTest
{
	@Mock
	private Progress progress;
	@Mock
	private PlatformTransactionManager transactionManager;
	@Mock
	private MappingService mappingService;
	@Mock
	private DataService dataService;
	@Mock
	private Authentication authentication;
	@Mock
	private MappingTarget mappingTarget;

	@BeforeMethod
	public void beforeMethod()
	{
		initMocks(this);

		MappingProject mappingProject = mock(MappingProject.class);
		when(mappingService.getMappingProject("test")).thenReturn(mappingProject);
		when(mappingProject.getMappingTargets()).thenReturn(newArrayList(mappingTarget));
	}

	@Test
	public void testMappingJobWithOneSourceOneBatch()
	{
		EntityMapping entityMapping = getMockEntityMapping("a", MAPPING_BATCH_SIZE - 1);
		List<EntityMapping> mappings = singletonList(entityMapping);
		when(mappingTarget.getEntityMappings()).thenReturn(mappings);

		getDummyMappingJob().call();

		verify(progress).start();
		verify(progress).setProgressMax(1);
		verify(mappingService).applyMappings(mappingTarget, "test", true, "packageId", "label", progress);
		verify(progress).success();
	}

	@Test
	public void testMappingJobWithOneSourceMultipleBatches()
	{
		EntityMapping entityMapping = getMockEntityMapping("a", (3 * MAPPING_BATCH_SIZE) + 1);
		List<EntityMapping> mappings = singletonList(entityMapping);
		when(mappingTarget.getEntityMappings()).thenReturn(mappings);

		getDummyMappingJob().call();

		verify(progress).start();
		verify(progress).setProgressMax(4);
		verify(mappingService).applyMappings(mappingTarget, "test", true, "packageId", "label", progress);
		verify(progress).success();
	}

	@Test
	public void testMappingJobWithOneSourceMultipleBatchesSelfReferencing()
	{
		EntityMapping entityMapping = getMockEntityMapping("a", (3 * MAPPING_BATCH_SIZE) + 1);
		when(mappingTarget.hasSelfReferences()).thenReturn(true);
		List<EntityMapping> mappings = singletonList(entityMapping);
		when(mappingTarget.getEntityMappings()).thenReturn(mappings);

		getDummyMappingJob().call();

		verify(progress).start();
		verify(progress).setProgressMax(8);
		verify(mappingService).applyMappings(mappingTarget, "test", true, "packageId", "label", progress);
		verify(progress).success();
	}

	@Test
	public void testMappingJobWithMultipleSourcesSelfReferencing()
	{
		EntityMapping mapping1 = getMockEntityMapping("a", MAPPING_BATCH_SIZE);
		EntityMapping mapping2 = getMockEntityMapping("b", MAPPING_BATCH_SIZE + 1);
		List<EntityMapping> mappings = newArrayList(mapping1, mapping2);
		when(mappingTarget.hasSelfReferences()).thenReturn(true);
		when(mappingTarget.getEntityMappings()).thenReturn(mappings);

		getDummyMappingJob().call();

		verify(progress).start();
		verify(progress).setProgressMax(6);
		verify(mappingService).applyMappings(mappingTarget, "test", true, "packageId", "label", progress);
		verify(progress).success();
	}

	@Test
	public void testMappingJobWithMultipleSources()
	{
		EntityMapping mapping1 = getMockEntityMapping("a", MAPPING_BATCH_SIZE);
		EntityMapping mapping2 = getMockEntityMapping("b", MAPPING_BATCH_SIZE + 1);
		List<EntityMapping> mappings = newArrayList(mapping1, mapping2);
		when(mappingTarget.getEntityMappings()).thenReturn(mappings);

		getDummyMappingJob().call();

		verify(progress).start();
		verify(progress).setProgressMax(3);
		verify(mappingService).applyMappings(mappingTarget, "test", true, "packageId", "label", progress);
		verify(progress).success();
	}

	private EntityMapping getMockEntityMapping(String id, long sourceRows)
	{
		EntityMapping entityMapping = mock(EntityMapping.class);
		EntityType sourceEntityType = mock(EntityType.class);
		when(entityMapping.getSourceEntityType()).thenReturn(sourceEntityType);
		when(sourceEntityType.getId()).thenReturn(id);
		when(dataService.count(id)).thenReturn(sourceRows);
		return entityMapping;
	}

	private MappingJob getDummyMappingJob()
	{
		return new MappingJob("test", "test", true, "packageId", "label", progress, authentication,
				new TransactionTemplate(transactionManager), mappingService, dataService);
	}
}
package org.molgenis.test.data;

import org.molgenis.data.meta.SystemEntityMetaData;
import org.molgenis.data.meta.model.AttributeMetaDataMetaData;
import org.molgenis.data.meta.model.EntityMetaDataMetaData;
import org.molgenis.util.GenericDependencyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

import java.util.Map;

@ContextConfiguration(classes = { AbstractMolgenisSpringTest.Config.class })
public abstract class AbstractMolgenisSpringTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	protected ApplicationContext applicationContext;

	@BeforeClass
	public void bootstrap()
	{
		// bootstrap meta data
		EntityMetaDataMetaData entityMetaMeta = applicationContext.getBean(EntityMetaDataMetaData.class);
		applicationContext.getBean(AttributeMetaDataMetaData.class).bootstrap(entityMetaMeta);
		Map<String, SystemEntityMetaData> systemEntityMetaDataMap = applicationContext
				.getBeansOfType(SystemEntityMetaData.class);
		new GenericDependencyResolver().resolve(systemEntityMetaDataMap.values(), SystemEntityMetaData::getDependencies)
				.stream().forEach(systemEntityMetaData -> systemEntityMetaData.bootstrap(entityMetaMeta));

	}

	@Configuration
	@ComponentScan({ "org.molgenis.data.meta.model", "org.molgenis.data.system.model" })
	public static class Config
	{
	}
}

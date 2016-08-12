package org.molgenis.annotation.test.cmd;

import org.molgenis.annotation.cmd.CommandLineAnnotatorConfig;
import org.molgenis.annotation.cmd.VcfValidator;
import org.molgenis.data.Entity;
import org.molgenis.data.annotation.core.RepositoryAnnotator;
import org.molgenis.data.annotation.core.entity.AnnotatorConfig;
import org.molgenis.data.annotation.core.entity.impl.GoNLAnnotator;
import org.molgenis.data.annotation.core.entity.impl.ThousandGenomesAnnotator;
import org.molgenis.data.annotation.core.utils.AnnotatorUtils;
import org.molgenis.data.meta.model.AttributeMetaDataFactory;
import org.molgenis.data.meta.model.EntityMetaData;
import org.molgenis.data.meta.model.EntityMetaDataFactory;
import org.molgenis.data.vcf.VcfRepository;
import org.molgenis.data.vcf.model.VcfAttributes;
import org.molgenis.data.vcf.utils.VcfUtils;
import org.molgenis.test.data.AbstractMolgenisSpringTest;
import org.molgenis.util.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@ContextConfiguration(classes = { CommandLineAnnotatorConfig.class })
public class AnnotatorChainingIT extends AbstractMolgenisSpringTest
{
	@Autowired
	CommandLineAnnotatorConfig commandLineAnnotatorConfig;

	@Autowired
	VcfValidator vcfValidator;

	@Autowired
	VcfAttributes vcfAttributes;

	@Autowired
	VcfUtils vcfUtils;

	@Autowired
	EntityMetaDataFactory entityMetaDataFactory;

	@Autowired
	AttributeMetaDataFactory attributeMetaDataFactory;

	@Test
	public void chain() throws IOException
	{
		File vcf = ResourceUtils.getFile(getClass(), "/gonl/test_gonl_and_1000g.vcf");

		try (VcfRepository repo = new VcfRepository(vcf, "vcf", vcfAttributes, entityMetaDataFactory,
				attributeMetaDataFactory))
		{
			try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
					"org.molgenis.data.annotation.core", "org.molgenis.annotation.cmd"))
			{
				ctx.register(CommandLineAnnotatorConfig.class);
				Map<String, AnnotatorConfig> annotatorMap = ctx.getBeansOfType(AnnotatorConfig.class);
				annotatorMap.values().forEach(AnnotatorConfig::init);
				Map<String, RepositoryAnnotator> annotators = ctx.getBeansOfType(RepositoryAnnotator.class);

				RepositoryAnnotator gonlAnnotator = annotators.get("gonl");
				gonlAnnotator.getCmdLineAnnotatorSettingsConfigurer()
						.addSettings(ResourceUtils.getFile(getClass(), "/gonl").getPath());

				RepositoryAnnotator tgAnnotator = annotators.get("thousandGenomes");
				tgAnnotator.getCmdLineAnnotatorSettingsConfigurer()
						.addSettings(ResourceUtils.getFile(getClass(), "/1000g").getPath());

				AnnotatorUtils.addAnnotatorMetadataToRepositories(repo.getEntityMetaData(),
						gonlAnnotator.getOutputAttributes());

				Iterator<Entity> it = gonlAnnotator.annotate(repo);
				assertNotNull(it);
				assertTrue(it.hasNext());

				AnnotatorUtils.addAnnotatorMetadataToRepositories(repo.getEntityMetaData(),
						tgAnnotator.getOutputAttributes());
				it = tgAnnotator.annotate(it);
				assertNotNull(it);
				assertTrue(it.hasNext());

				Entity entity = it.next();
				assertNotNull(entity.get(GoNLAnnotator.GONL_GENOME_AF));
				assertNotNull(entity.get(GoNLAnnotator.GONL_GENOME_GTC));
				assertNotNull(entity.get(ThousandGenomesAnnotator.THOUSAND_GENOME_AF));

				EntityMetaData meta = entity.getEntityMetaData();
				assertNotNull(meta);
				assertNotNull(meta.getAttribute(GoNLAnnotator.GONL_GENOME_AF));
				assertNotNull(meta.getAttribute(GoNLAnnotator.GONL_GENOME_GTC));
				assertNotNull(meta.getAttribute(ThousandGenomesAnnotator.THOUSAND_GENOME_AF));
			}
		}
	}
}

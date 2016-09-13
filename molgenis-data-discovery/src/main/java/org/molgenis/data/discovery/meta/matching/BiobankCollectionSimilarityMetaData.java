package org.molgenis.data.discovery.meta.matching;

import static org.molgenis.MolgenisFieldTypes.DECIMAL;
import static org.molgenis.MolgenisFieldTypes.INT;
import static org.molgenis.MolgenisFieldTypes.XREF;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

import org.molgenis.data.discovery.meta.biobank.BiobankSampleCollectionMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankUniverseMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class BiobankCollectionSimilarityMetaData extends DefaultEntityMetaData
{
	public static String ENTITY_NAME = "BiobankCollectionSimilarity";
	public static final String IDENTIFIER = "identifier";
	public static final String TARGET = "target";
	public static final String SOURCE = "source";
	public static final String SIMILARITY = "similarity";
	public static final String COVERAGE = "coverage";
	public static final String UNIVERSE = "universe";
	public static final BiobankCollectionSimilarityMetaData INSTANCE = new BiobankCollectionSimilarityMetaData();

	public BiobankCollectionSimilarityMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(TARGET).setDataType(XREF).setRefEntity(BiobankSampleCollectionMetaData.INSTANCE);
		addAttribute(SOURCE).setDataType(XREF).setRefEntity(BiobankSampleCollectionMetaData.INSTANCE);
		addAttribute(SIMILARITY).setDataType(DECIMAL);
		addAttribute(COVERAGE).setDataType(INT);
		addAttribute(UNIVERSE).setDataType(XREF).setRefEntity(BiobankUniverseMetaData.INSTANCE);
	}
}

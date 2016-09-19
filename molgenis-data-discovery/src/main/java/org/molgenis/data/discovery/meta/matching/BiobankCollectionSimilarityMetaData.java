package org.molgenis.data.discovery.meta.matching;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.molgenis.MolgenisFieldTypes.DECIMAL;
import static org.molgenis.MolgenisFieldTypes.ENUM;
import static org.molgenis.MolgenisFieldTypes.INT;
import static org.molgenis.MolgenisFieldTypes.XREF;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

import java.util.Objects;

import org.molgenis.data.discovery.meta.biobank.BiobankSampleCollectionMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankUniverseMetaData;
import org.molgenis.data.discovery.model.matching.BiobankCollectionSimilarity.SimilarityOption;
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
	public static final String SIMILARITY_OPTION = "similarityOption";
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
		addAttribute(SIMILARITY_OPTION).setDataType(ENUM)
				.setEnumOptions(of(SimilarityOption.values()).map(Objects::toString).collect(toList()));
	}
}

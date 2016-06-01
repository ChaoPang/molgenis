package org.molgenis.data.discovery.meta.matching;

import static org.molgenis.MolgenisFieldTypes.MREF;
import static org.molgenis.MolgenisFieldTypes.XREF;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

import org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData;
import org.molgenis.data.discovery.meta.biobank.BiobankUniverseMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class AttributeMappingCandidateMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "AttributeMappingCandidate";
	public static final String IDENTIFIER = "identifier";
	public static final String BIOBANK_UNIVERSE = "biobankUniverse";
	public static final String TARGET = "target";
	public static final String SOURCE = "source";
	public static final String EXPLANATION = "explanation";
	public static final String DECISIONS = "decisions";

	public static final AttributeMappingCandidateMetaData INSTANCE = new AttributeMappingCandidateMetaData();

	public AttributeMappingCandidateMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(BIOBANK_UNIVERSE).setDataType(XREF).setRefEntity(BiobankUniverseMetaData.INSTANCE);
		addAttribute(TARGET).setDataType(XREF).setRefEntity(BiobankSampleAttributeMetaData.INSTANCE);
		addAttribute(SOURCE).setDataType(XREF).setRefEntity(BiobankSampleAttributeMetaData.INSTANCE);
		addAttribute(EXPLANATION).setDataType(XREF).setRefEntity(MatchingExplanationMetaData.INSTANCE);
		addAttribute(DECISIONS).setDataType(MREF).setRefEntity(AttributeMappingDecisionMetaData.INSTANCE)
				.setNillable(true);
	}
}

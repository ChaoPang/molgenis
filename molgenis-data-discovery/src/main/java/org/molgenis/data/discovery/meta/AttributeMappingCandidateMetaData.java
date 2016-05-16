package org.molgenis.data.discovery.meta;

import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class AttributeMappingCandidateMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "AttributeMappingCandidate";
	public static final String IDENTIFIER = "identifier";
	public static final String TARGET = "target";
	public static final String SOURCE = "source";
	public static final String TARGET_ENTITY = "targetEntity";
	public static final String SOURCE_ENTITY = "sourceEntity";
	public static final String EXPLANATION = "explanation";
	public static final String DECISIONS = "decisions";

	public AttributeMappingCandidateMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(TARGET);
		addAttribute(SOURCE);
		addAttribute(TARGET_ENTITY).setDataType(MolgenisFieldTypes.XREF).setRefEntity(EntityMetaDataMetaData.INSTANCE);
		addAttribute(SOURCE_ENTITY).setDataType(MolgenisFieldTypes.XREF).setRefEntity(EntityMetaDataMetaData.INSTANCE);
		addAttribute(EXPLANATION).setDataType(MolgenisFieldTypes.XREF)
				.setRefEntity(MappingExplanationMetaData.INSTANCE);
		addAttribute(DECISIONS).setDataType(MolgenisFieldTypes.MREF)
				.setRefEntity(AttributeMappingDecisionMetaData.INSTANCE);
	}
}

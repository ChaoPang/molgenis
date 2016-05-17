package org.molgenis.data.discovery.meta;

import static org.molgenis.MolgenisFieldTypes.MREF;
import static org.molgenis.MolgenisFieldTypes.XREF;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class TaggedAttributeMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "TaggedAttribute";
	public static final String IDENTIFIER = "identifier";
	public static final String ATTRIBUTE = "attribute";
	public static final String ENTITY = "entity";
	public static final String TAG_GROUPS = "tagGroups";
	public static final TaggedAttributeMetaData INSTANCE = new TaggedAttributeMetaData();

	public TaggedAttributeMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(ATTRIBUTE);
		addAttribute(ENTITY).setDataType(XREF).setRefEntity(EntityMetaDataMetaData.INSTANCE);
		addAttribute(TAG_GROUPS).setDataType(MREF).setRefEntity(MappingExplanationMetaData.INSTANCE);
	}
}

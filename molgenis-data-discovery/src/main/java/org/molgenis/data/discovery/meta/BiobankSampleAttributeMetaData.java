package org.molgenis.data.discovery.meta;

import static org.molgenis.MolgenisFieldTypes.MREF;
import static org.molgenis.MolgenisFieldTypes.TEXT;
import static org.molgenis.MolgenisFieldTypes.XREF;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_LABEL;

import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class BiobankSampleAttributeMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "BiobankSampleAttribute";
	public static final String IDENTIFIER = "identifier";
	public static final String NAME = "name";
	public static final String LABEL = "label";
	public static final String DESCRIPTION = "description";
	public static final String COLLECTION = "collection";
	public static final String TAG_GROUPS = "tagGroups";

	public static final BiobankSampleAttributeMetaData INSTANCE = new BiobankSampleAttributeMetaData();

	public BiobankSampleAttributeMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(NAME, ROLE_LABEL);
		addAttribute(LABEL);
		addAttribute(DESCRIPTION).setDataType(TEXT).setNillable(true);
		addAttribute(COLLECTION).setDataType(XREF).setRefEntity(BiobankSampleCollectionMetaData.INSTANCE);
		addAttribute(TAG_GROUPS).setDataType(MREF).setRefEntity(TagGroupMetaData.INSTANCE).setNillable(true);
	}
}

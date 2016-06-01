package org.molgenis.data.discovery.meta.biobank;

import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_LABEL;

import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class BiobankSampleCollectionMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "BiobankSampleCollection";
	public static final String NAME = "name";
	public static final BiobankSampleCollectionMetaData INSTANCE = new BiobankSampleCollectionMetaData();

	public BiobankSampleCollectionMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(NAME, ROLE_ID, ROLE_LABEL).setUnique(true);
	}
}

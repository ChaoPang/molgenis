package org.molgenis.data.discovery.meta;

import static org.molgenis.MolgenisFieldTypes.XREF;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_LABEL;

import org.molgenis.auth.MolgenisUserMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class BiobankUniverseMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "BiobankUniverseMember";
	public static final String IDENTIFIER = "identifier";
	public static final String NAME = "name";
	public static final String MEMBERS = "members";
	public static final String OWNER = "owner";
	public static final BiobankUniverseMetaData INSTANCE = new BiobankUniverseMetaData();

	public BiobankUniverseMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(NAME, ROLE_LABEL);
		addAttribute(MEMBERS).setDataType(XREF).setRefEntity(EntityMetaDataMetaData.INSTANCE);
		addAttribute(OWNER).setDataType(XREF).setRefEntity(new MolgenisUserMetaData());
	}
}

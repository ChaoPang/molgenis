package org.molgenis.data.discovery.meta;

import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_LABEL;

import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class SemanticTypeMetaData extends DefaultEntityMetaData
{
	public final static String ID = "id";
	public final static String SEMANTIC_TYPE_NAME = "semanticTypeName";
	public final static String SEMANTIC_TYPE_GROUP = "semanticTypeGroup";
	public final static String ENTITY_NAME = "SemanticType";

	public final static SemanticTypeMetaData INSTANCE = new SemanticTypeMetaData();

	public SemanticTypeMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(ID, ROLE_ID);
		addAttribute(SEMANTIC_TYPE_NAME, ROLE_LABEL);
		addAttribute(SEMANTIC_TYPE_GROUP);
	}
}

package org.molgenis.ontology.core.meta;

import static org.molgenis.MolgenisFieldTypes.BOOL;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_LABEL;

import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.ontology.core.model.OntologyPackage;
import org.springframework.stereotype.Component;

@Component
public class SemanticTypeMetaData extends DefaultEntityMetaData
{
	public final static String ID = "id";
	public final static String SEMANTIC_TYPE_NAME = "semanticTypeName";
	public final static String SEMANTIC_TYPE_GROUP = "semanticTypeGroup";
	public final static String SEMANTIC_TYPE_GLOBAL_KEY_CONCEPT = "globalKeyConcept";
	public final static String SIMPLE_NAME = "SemanticType";
	public final static String ENTITY_NAME = OntologyPackage.PACKAGE_NAME + "_" + SIMPLE_NAME;

	public final static SemanticTypeMetaData INSTANCE = new SemanticTypeMetaData();

	public SemanticTypeMetaData()
	{
		super(SIMPLE_NAME, OntologyPackage.getPackageInstance());
		addAttribute(ID, ROLE_ID);
		addAttribute(SEMANTIC_TYPE_NAME, ROLE_LABEL);
		addAttribute(SEMANTIC_TYPE_GROUP);
		addAttribute(SEMANTIC_TYPE_GLOBAL_KEY_CONCEPT).setDataType(BOOL);
	}
}
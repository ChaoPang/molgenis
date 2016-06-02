 package org.molgenis.ontology.core.meta;

 import static org.molgenis.MolgenisFieldTypes.MREF;
 import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

 import org.molgenis.data.support.DefaultEntityMetaData;
 import org.springframework.stereotype.Component;

 @Component
 public class OntologyTermSemanticTypeMetaData extends DefaultEntityMetaData
 {
 public final static String ID = "id";
 public final static String ONTOLOGY_TERM = "ontologyTerm";
 public final static String SEMANTIC_TYPE = "semanticType";
 public final static String ENTITY_NAME = "OntologyTermSemanticType";

 public final static OntologyTermSemanticTypeMetaData INSTANCE = new OntologyTermSemanticTypeMetaData();

 public OntologyTermSemanticTypeMetaData()
 {
 super(ENTITY_NAME);
 addAttribute(ID, ROLE_ID);
 addAttribute(ONTOLOGY_TERM);
 addAttribute(SEMANTIC_TYPE).setDataType(MREF).setRefEntity(SemanticTypeMetaData.INSTANCE);
 }
 }

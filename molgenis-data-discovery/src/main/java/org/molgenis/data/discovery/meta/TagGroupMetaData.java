package org.molgenis.data.discovery.meta;

import static org.molgenis.MolgenisFieldTypes.DECIMAL;
import static org.molgenis.MolgenisFieldTypes.MREF;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.springframework.stereotype.Component;

@Component
public class TagGroupMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "TagGroup";
	public static final String IDENTIFIER = "identifier";
	public static final String MATCHED_WORDS = "matchedWords";
	public static final String ONTOLOGY_TERMS = "ontologyTerms";
	public static final String NGRAM_SCORE = "ngramScore";
	public static final TagGroupMetaData INSTANCE = new TagGroupMetaData();

	public TagGroupMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(ONTOLOGY_TERMS).setDataType(MREF).setRefEntity(OntologyTermMetaData.INSTANCE);
		addAttribute(MATCHED_WORDS);
		addAttribute(NGRAM_SCORE).setDataType(DECIMAL);
	}
}

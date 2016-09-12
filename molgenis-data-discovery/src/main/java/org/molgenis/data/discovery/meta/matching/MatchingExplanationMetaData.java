package org.molgenis.data.discovery.meta.matching;

import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.springframework.stereotype.Component;

@Component
public class MatchingExplanationMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "MatchingExplanation";
	public static final String IDENTIFIER = "identifier";
	public static final String ONTOLOGY_TERMS = "ontologyTerms";
	public static final String MATCHED_QUERY_STRING = "matchedQueryString";
	public static final String MATCHED_WORDS = "matchedWords";
	public static final String N_GRAM_SCORE = "ngramScore";
	public static final MatchingExplanationMetaData INSTANCE = new MatchingExplanationMetaData();

	public MatchingExplanationMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(ONTOLOGY_TERMS).setDataType(MolgenisFieldTypes.MREF).setRefEntity(OntologyTermMetaData.INSTANCE);
		addAttribute(MATCHED_QUERY_STRING);
		addAttribute(MATCHED_WORDS);
		addAttribute(N_GRAM_SCORE).setDataType(MolgenisFieldTypes.DECIMAL);
	}
}
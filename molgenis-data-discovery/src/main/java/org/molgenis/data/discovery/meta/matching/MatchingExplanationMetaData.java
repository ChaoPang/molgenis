package org.molgenis.data.discovery.meta.matching;

import org.molgenis.data.discovery.meta.BiobankUniversePackage;
import org.molgenis.data.meta.SystemEntityMetaData;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.molgenis.MolgenisFieldTypes.AttributeType.DECIMAL;
import static org.molgenis.MolgenisFieldTypes.AttributeType.MREF;
import static org.molgenis.data.discovery.meta.BiobankUniversePackage.PACKAGE_UNIVERSE;
import static org.molgenis.data.meta.model.EntityMetaData.AttributeRole.ROLE_ID;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;

@Component
public class MatchingExplanationMetaData extends SystemEntityMetaData
{
	public static final String SIMPLE_NAME = "MatchingExplanation";
	public static final String MATCHING_EXPLANATION = PACKAGE_UNIVERSE + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String IDENTIFIER = "identifier";
	public static final String TARGET_ONTOLOGY_TERMS = "targetOntologyTerms";
	public static final String SOURCE_ONTOLOGY_TERMS = "sourceOntologyTerms";
	public static final String MATCHED_QUERY_STRING = "matchedQueryString";
	public static final String MATCHED_TARGET_WORDS = "matchedTargetWords";
	public static final String MATCHED_SOURCE_WORDS = "matchedSourceWords";
	public static final String VSM_SCORE = "vsmScore";
	public static final String N_GRAM_SCORE = "ngramScore";

	private final BiobankUniversePackage biobankUniversePackage;
	private final OntologyTermMetaData ontologyTermMetaData;

	@Autowired
	public MatchingExplanationMetaData(BiobankUniversePackage biobankUniversePackage,
			OntologyTermMetaData ontologyTermMetaData)
	{
		super(SIMPLE_NAME, PACKAGE_UNIVERSE);
		this.biobankUniversePackage = requireNonNull(biobankUniversePackage);
		this.ontologyTermMetaData = requireNonNull(ontologyTermMetaData);
	}

	@Override
	protected void init()
	{
		setLabel("Matching explanation");
		setPackage(biobankUniversePackage);

		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(TARGET_ONTOLOGY_TERMS).setDataType(MREF).setRefEntity(ontologyTermMetaData);
		addAttribute(SOURCE_ONTOLOGY_TERMS).setDataType(MREF).setRefEntity(ontologyTermMetaData);
		addAttribute(MATCHED_QUERY_STRING);
		addAttribute(MATCHED_TARGET_WORDS);
		addAttribute(MATCHED_SOURCE_WORDS);
		addAttribute(VSM_SCORE).setDataType(DECIMAL);
		addAttribute(N_GRAM_SCORE).setDataType(DECIMAL);

	}
}

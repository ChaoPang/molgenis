package org.molgenis.data.discovery.meta.matching;

import org.molgenis.data.discovery.meta.BiobankUniversePackage;
import org.molgenis.data.meta.AttributeType;
import org.molgenis.data.meta.SystemEntityType;
import org.molgenis.ontology.core.meta.OntologyTermMetaData;
import org.molgenis.ontology.core.meta.SemanticTypeMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.discovery.meta.BiobankUniversePackage.PACKAGE_UNIVERSE;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;

@Component
public class TagGroupMetaData extends SystemEntityType
{
	public static final String SIMPLE_NAME = "TagGroup";
	public static final String TAG_GROUP = PACKAGE_UNIVERSE + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String IDENTIFIER = "identifier";
	public static final String MATCHED_WORDS = "matchedWords";
	public static final String ONTOLOGY_TERMS = "ontologyTerms";
	public static final String SEMANTIC_TYPES = "semanticTypes";
	public static final String NGRAM_SCORE = "ngramScore";

	private final BiobankUniversePackage biobankUniversePackage;
	private final OntologyTermMetaData ontologyTermMetaData;
	private final SemanticTypeMetaData semanticTypeMetaData;

	@Autowired
	public TagGroupMetaData(BiobankUniversePackage biobankUniversePackage, OntologyTermMetaData ontologyTermMetaData,
			SemanticTypeMetaData semanticTypeMetaData)
	{
		super(SIMPLE_NAME, PACKAGE_UNIVERSE);
		this.biobankUniversePackage = requireNonNull(biobankUniversePackage);
		this.ontologyTermMetaData = requireNonNull(ontologyTermMetaData);
		this.semanticTypeMetaData = requireNonNull(semanticTypeMetaData);
	}

	@Override
	protected void init()
	{
		setLabel("Tag group");
		setPackage(biobankUniversePackage);

		addAttribute(IDENTIFIER, AttributeRole.ROLE_ID);
		addAttribute(ONTOLOGY_TERMS).setDataType(AttributeType.MREF).setRefEntity(ontologyTermMetaData);
		addAttribute(SEMANTIC_TYPES).setDataType(AttributeType.MREF).setRefEntity(semanticTypeMetaData);
		addAttribute(MATCHED_WORDS);
		addAttribute(NGRAM_SCORE).setDataType(AttributeType.DECIMAL);
	}
}

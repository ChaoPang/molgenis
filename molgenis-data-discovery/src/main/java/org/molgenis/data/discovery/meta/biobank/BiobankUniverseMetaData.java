package org.molgenis.data.discovery.meta.biobank;

import org.molgenis.auth.UserMetaData;
import org.molgenis.data.discovery.meta.BiobankUniversePackage;
import org.molgenis.data.meta.AttributeType;
import org.molgenis.data.meta.SystemEntityType;
import org.molgenis.ontology.core.meta.SemanticTypeMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.discovery.meta.BiobankUniversePackage.PACKAGE_UNIVERSE;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;

@Component
public class BiobankUniverseMetaData extends SystemEntityType
{
	public static final String SIMPLE_NAME = "BiobankUniverse";
	public static final String BIOBANK_UNIVERSE = PACKAGE_UNIVERSE + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String IDENTIFIER = "identifier";
	public static final String NAME = "name";
	public static final String MEMBERS = "members";
	public static final String OWNER = "owner";
	public static final String KEY_CONCEPTS = "keyConcepts";

	private final BiobankUniversePackage biobankUniversePackage;
	private final BiobankSampleCollectionMetaData biobankSampleCollectionMetaData;
	private final UserMetaData userMetaData;
	private final SemanticTypeMetaData semanticTypeMetaData;

	@Autowired
	public BiobankUniverseMetaData(BiobankUniversePackage biobankUniversePackage,
			BiobankSampleCollectionMetaData biobankSampleCollectionMetaData, UserMetaData molgenisUserMetaData,
			SemanticTypeMetaData semanticTypeMetaData)
	{
		super(SIMPLE_NAME, PACKAGE_UNIVERSE);

		this.biobankUniversePackage = requireNonNull(biobankUniversePackage);
		this.biobankSampleCollectionMetaData = requireNonNull(biobankSampleCollectionMetaData);
		this.userMetaData = requireNonNull(molgenisUserMetaData);
		this.semanticTypeMetaData = requireNonNull(semanticTypeMetaData);
	}

	@Override
	protected void init()
	{
		setLabel("Biobank universe");
		setPackage(biobankUniversePackage);

		addAttribute(IDENTIFIER, AttributeRole.ROLE_ID);
		addAttribute(NAME, AttributeRole.ROLE_LABEL);
		addAttribute(MEMBERS).setDataType(AttributeType.MREF).setRefEntity(biobankSampleCollectionMetaData);
		addAttribute(OWNER).setDataType(AttributeType.XREF).setRefEntity(userMetaData);
		addAttribute(KEY_CONCEPTS).setDataType(AttributeType.MREF).setRefEntity(semanticTypeMetaData);
	}
}

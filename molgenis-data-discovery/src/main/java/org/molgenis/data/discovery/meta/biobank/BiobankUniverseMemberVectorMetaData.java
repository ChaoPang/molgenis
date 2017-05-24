package org.molgenis.data.discovery.meta.biobank;

import org.molgenis.data.discovery.meta.BiobankUniversePackage;
import org.molgenis.data.meta.AttributeType;
import org.molgenis.data.meta.SystemEntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.discovery.meta.BiobankUniversePackage.PACKAGE_UNIVERSE;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;

@Component
public class BiobankUniverseMemberVectorMetaData extends SystemEntityType
{
	public static final String SIMPLE_NAME = "BiobankUniverseMemberVector";
	public static final String BIOBANK_UNIVERSE_MEMBER_VECTOR = PACKAGE_UNIVERSE + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String IDENTIFIER = "identifier";
	public static final String MEMBER = "member";
	public static final String BIOBANK_UNIVERSE = "biobankUniverse";
	public static final String VECTOR = "vecotr";

	private final BiobankUniversePackage biobankUniversePackage;
	private final BiobankSampleCollectionMetaData biobankSampleCollectionMetaData;
	private final BiobankUniverseMetaData biobankUniverseMetaData;

	@Autowired
	public BiobankUniverseMemberVectorMetaData(BiobankUniversePackage biobankUniversePackage,
			BiobankUniverseMetaData biobankUniverseMetaData,
			BiobankSampleCollectionMetaData biobankSampleCollectionMetaData)
	{
		super(SIMPLE_NAME, PACKAGE_UNIVERSE);

		this.biobankUniversePackage = requireNonNull(biobankUniversePackage);
		this.biobankSampleCollectionMetaData = requireNonNull(biobankSampleCollectionMetaData);
		this.biobankUniverseMetaData = requireNonNull(biobankUniverseMetaData);
	}

	@Override
	protected void init()
	{
		setLabel("Biobank universe member vector");
		setPackage(biobankUniversePackage);
		addAttribute(IDENTIFIER, AttributeRole.ROLE_ID);
		addAttribute(MEMBER).setDataType(AttributeType.XREF).setRefEntity(biobankSampleCollectionMetaData);
		addAttribute(BIOBANK_UNIVERSE).setDataType(AttributeType.XREF).setRefEntity(biobankUniverseMetaData);
		addAttribute(VECTOR).setDataType(AttributeType.SCRIPT);
	}
}

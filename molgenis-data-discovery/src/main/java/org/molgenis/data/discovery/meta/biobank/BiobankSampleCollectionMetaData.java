package org.molgenis.data.discovery.meta.biobank;

import org.molgenis.data.discovery.meta.BiobankUniversePackage;
import org.molgenis.data.meta.SystemEntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.discovery.meta.BiobankUniversePackage.PACKAGE_UNIVERSE;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;

@Component
public class BiobankSampleCollectionMetaData extends SystemEntityType
{
	public static final String SIMPLE_NAME = "BiobankSampleCollection";
	public static final String BIOBANK_SAMPLE_COLLECTION = PACKAGE_UNIVERSE + PACKAGE_SEPARATOR + SIMPLE_NAME;
	public static final String NAME = "name";

	private final BiobankUniversePackage biobankUniversePackage;

	@Autowired
	public BiobankSampleCollectionMetaData(BiobankUniversePackage biobankUniversePackage)
	{
		super(SIMPLE_NAME, PACKAGE_UNIVERSE);
		this.biobankUniversePackage = requireNonNull(biobankUniversePackage);
	}

	@Override
	protected void init()
	{
		setLabel("Biobank sample collection");
		setPackage(biobankUniversePackage);
		addAttribute(NAME, AttributeRole.ROLE_ID, AttributeRole.ROLE_LABEL).setUnique(true);
	}
}

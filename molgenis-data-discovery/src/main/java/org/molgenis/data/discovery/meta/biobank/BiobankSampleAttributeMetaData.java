package org.molgenis.data.discovery.meta.biobank;

import com.google.common.collect.Lists;
import org.molgenis.data.discovery.meta.BiobankUniversePackage;
import org.molgenis.data.discovery.meta.matching.TagGroupMetaData;
import org.molgenis.data.meta.AttributeType;
import org.molgenis.data.meta.SystemEntityType;
import org.molgenis.ontology.core.meta.OntologyTermNodePathMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static java.util.Objects.requireNonNull;
import static org.molgenis.data.discovery.meta.BiobankUniversePackage.PACKAGE_UNIVERSE;
import static org.molgenis.data.discovery.meta.biobank.BiobankSampleAttributeMetaData.BiobankAttributeDataType.getEnumValues;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;

@Component
public class BiobankSampleAttributeMetaData extends SystemEntityType
{
	private static final String SIMPLE_NAME = "BiobankSampleAttribute";
	public static final String BIOBANK_SAMPLE_ATTRIBUTE = PACKAGE_UNIVERSE + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String IDENTIFIER = "identifier";
	public static final String NAME = "name";
	public static final String LABEL = "label";
	public static final String DESCRIPTION = "description";
	public static final String DATA_TYPE = "dataType";
	public static final String COLLECTION = "collection";
	public static final String TAG_GROUPS = "tagGroups";
	public static final String ONTOLOGY_TERM_NODE_PATH = "nodePaths";

	public enum BiobankAttributeDataType
	{
		STRING("string"), DATE("date"), INT("int"), DECIMAL("decimal"), CATEGORICAL("categorical");

		private static final Map<String, BiobankAttributeDataType> strValMap;

		static
		{
			BiobankAttributeDataType[] biobankAttributeDataTypes = BiobankAttributeDataType.values();
			strValMap = newHashMapWithExpectedSize(biobankAttributeDataTypes.length);
			for (BiobankAttributeDataType biobankAttributeDataType : biobankAttributeDataTypes)
			{
				strValMap.put(biobankAttributeDataType.toString(), biobankAttributeDataType);
			}
		}

		private String label;

		BiobankAttributeDataType(String label)
		{
			this.label = label;
		}

		/**
		 * Get the String label of the Operator.
		 */
		@Override
		public String toString()
		{
			return label;
		}

		public static List<String> getEnumValues()
		{
			return Lists.newArrayList(strValMap.keySet());
		}

		public static BiobankAttributeDataType toEnum(String valueString)
		{
			String lowerCase = valueString.toLowerCase();
			return strValMap.containsKey(lowerCase) ? strValMap.get(lowerCase) : STRING;
		}
	}

	private final BiobankUniversePackage biobankUniversePackage;
	private final BiobankSampleCollectionMetaData biobankSampleCollectionMetaData;
	private final TagGroupMetaData tagGroupMetaData;
	private final OntologyTermNodePathMetaData ontologyTermNodePathMetaData;

	@Autowired
	public BiobankSampleAttributeMetaData(BiobankUniversePackage biobankUniversePackage,
			BiobankSampleCollectionMetaData biobankSampleCollectionMetaData, TagGroupMetaData tagGroupMetaData,
			OntologyTermNodePathMetaData ontologyTermNodePathMetaData)
	{
		super(SIMPLE_NAME, PACKAGE_UNIVERSE);
		this.biobankUniversePackage = requireNonNull(biobankUniversePackage);
		this.biobankSampleCollectionMetaData = requireNonNull(biobankSampleCollectionMetaData);
		this.tagGroupMetaData = requireNonNull(tagGroupMetaData);
		this.ontologyTermNodePathMetaData = requireNonNull(ontologyTermNodePathMetaData);
	}

	@Override
	protected void init()
	{
		setLabel("Biobank sample attribute");
		setPackage(biobankUniversePackage);

		addAttribute(IDENTIFIER, AttributeRole.ROLE_ID);
		addAttribute(NAME, AttributeRole.ROLE_LABEL);
		addAttribute(LABEL);
		addAttribute(DESCRIPTION).setDataType(AttributeType.TEXT).setNillable(true);
		addAttribute(DATA_TYPE).setDataType(AttributeType.ENUM).setNillable(false).setEnumOptions(getEnumValues());
		addAttribute(COLLECTION).setDataType(AttributeType.XREF).setRefEntity(biobankSampleCollectionMetaData);
		addAttribute(TAG_GROUPS).setDataType(AttributeType.MREF).setRefEntity(tagGroupMetaData).setNillable(true);
		addAttribute(ONTOLOGY_TERM_NODE_PATH).setDataType(AttributeType.MREF)
				.setRefEntity(ontologyTermNodePathMetaData);
	}
}

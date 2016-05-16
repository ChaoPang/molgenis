package org.molgenis.data.discovery.meta;

import static java.util.stream.Collectors.toList;
import static org.molgenis.data.EntityMetaData.AttributeRole.ROLE_ID;

import java.util.stream.Stream;

import org.molgenis.MolgenisFieldTypes;
import org.molgenis.auth.MolgenisUserMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;
import org.springframework.stereotype.Component;

@Component
public class AttributeMappingDecisionMetaData extends DefaultEntityMetaData
{
	public static final String ENTITY_NAME = "AttributeMappingDecision";
	public static final String IDENTIFIER = "identifier";
	public static final String DECISION = "decision";
	public static final String COMMENT = "comment";
	public static final String OWNER = "owner";
	public static final AttributeMappingDecisionMetaData INSTANCE = new AttributeMappingDecisionMetaData();

	public static enum DecisionOptions
	{
		YES("Yes"), NO("No"), UNDECIDED("Undecided");

		private String label;

		DecisionOptions(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	public AttributeMappingDecisionMetaData()
	{
		super(ENTITY_NAME);
		addAttribute(IDENTIFIER, ROLE_ID);
		addAttribute(DECISION).setDataType(MolgenisFieldTypes.ENUM)
				.setEnumOptions(Stream.of(DecisionOptions.values()).map(DecisionOptions::toString).collect(toList()));
		addAttribute(COMMENT).setDataType(MolgenisFieldTypes.TEXT);
		addAttribute(OWNER).setDataType(MolgenisFieldTypes.XREF).setRefEntity(new MolgenisUserMetaData());
	}
}

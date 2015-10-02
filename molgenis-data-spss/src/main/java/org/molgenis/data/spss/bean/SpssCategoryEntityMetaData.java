package org.molgenis.data.spss.bean;

import org.molgenis.data.support.DefaultAttributeMetaData;
import org.molgenis.data.support.DefaultEntityMetaData;

public class SpssCategoryEntityMetaData extends DefaultEntityMetaData
{
	public final static String Label = "label";
	public final static String CODE = "code";

	public SpssCategoryEntityMetaData(String entityName)
	{
		super(entityName);
		addAttributeMetaData(new DefaultAttributeMetaData(CODE).setIdAttribute(true).setNillable(false)
				.setLookupAttribute(true));
		addAttributeMetaData(new DefaultAttributeMetaData(Label).setLabelAttribute(true).setNillable(false)
				.setLookupAttribute(true));
	}
}

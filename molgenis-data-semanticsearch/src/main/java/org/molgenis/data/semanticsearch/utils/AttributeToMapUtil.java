package org.molgenis.data.semanticsearch.utils;

import java.util.HashMap;
import java.util.Map;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;

public class AttributeToMapUtil
{
	public static Map<String, Object> attributeToMap(AttributeMetaData attributeMetaData)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(AttributeMetaDataMetaData.NAME, attributeMetaData.getName());
		map.put(AttributeMetaDataMetaData.LABEL, attributeMetaData.getLabel());
		map.put(AttributeMetaDataMetaData.DESCRIPTION, attributeMetaData.getDescription());
		map.put(AttributeMetaDataMetaData.DATA_TYPE, attributeMetaData.getDataType().toString());
		map.put(AttributeMetaDataMetaData.NILLABLE, attributeMetaData.isNillable());
		map.put(AttributeMetaDataMetaData.UNIQUE, attributeMetaData.isUnique());
		if (attributeMetaData.getRefEntity() != null)
		{
			map.put(AttributeMetaDataMetaData.REF_ENTITY, attributeMetaData.getRefEntity().getName());
		}
		return map;
	}

	public static AttributeMetaData explainedAttrToAttributeMetaData(ExplainedAttributeMetaData explainedAttribute,
			EntityMetaData sourceEntityMetaData)
	{
		String attributeName = explainedAttribute.getAttributeMetaData().get(AttributeMetaDataMetaData.NAME).toString();
		return sourceEntityMetaData.getAttribute(attributeName);
	}
}

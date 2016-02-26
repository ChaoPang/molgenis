package org.molgenis.data.semanticsearch.string;

import java.util.HashMap;
import java.util.Map;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.meta.AttributeMetaDataMetaData;

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
}

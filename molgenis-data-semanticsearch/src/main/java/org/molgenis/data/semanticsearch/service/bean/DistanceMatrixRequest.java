package org.molgenis.data.semanticsearch.service.bean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_DistanceMatrixRequest.class)
public abstract class DistanceMatrixRequest
{
	public static DistanceMatrixRequest create(String entityName1, String entityName2)
	{
		List<String> list = Arrays.asList(entityName1, entityName2);
		Collections.sort(list);

		return new AutoValue_DistanceMatrixRequest(list.get(0), list.get(1));
	}

	public abstract String getEntityName1();

	public abstract String getEntityName2();
}

package org.molgenis.data.discovery.model.biobank;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.molgenis.gson.AutoGson;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_BiobankUniverseMemberVector.class)
public abstract class BiobankUniverseMemberVector implements Clusterable
{
	public abstract BiobankSampleCollection getBiobankSampleCollection();

	public abstract double[] getPoint();

	public static BiobankUniverseMemberVector create(BiobankSampleCollection biobankSampleCollection, double[] point)
	{
		return new AutoValue_BiobankUniverseMemberVector(biobankSampleCollection, point);
	}
}

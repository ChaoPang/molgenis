package org.molgenis.data.discovery.scoring.collections;

import static java.util.Objects.requireNonNull;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;

public class BiobankSampleCollectionVector implements Clusterable
{
	private final BiobankSampleCollection biobankSampleCollection;
	private final int coverage;
	private final double[] point;

	public BiobankSampleCollectionVector(BiobankSampleCollection biobankSampleCollection, double[] point, int coverage)
	{
		this.biobankSampleCollection = requireNonNull(biobankSampleCollection);
		this.point = point;
		this.coverage = coverage;
	}

	@Override
	public double[] getPoint()
	{
		return point;
	}

	public BiobankSampleCollection getBiobankSampleCollection()
	{
		return biobankSampleCollection;
	}

	public int getCoverage()
	{
		return coverage;
	}
}

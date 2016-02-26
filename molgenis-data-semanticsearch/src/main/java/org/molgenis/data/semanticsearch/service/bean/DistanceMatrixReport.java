package org.molgenis.data.semanticsearch.service.bean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DistanceMatrixReport
{
	private boolean finished;

	private double progress;

	private Map<String, List<DistanceMetric>> distanceMetrics;

	public DistanceMatrixReport()
	{
		this.distanceMetrics = new LinkedHashMap<>();
		this.finished = false;
	}

	public double getProgress()
	{
		return progress;
	}

	public boolean isFinished()
	{
		return finished;
	}

	public void setFinished(boolean finished)
	{
		this.finished = finished;
	}

	public void setProgress(double progress)
	{
		this.progress = progress;
	}

	public Map<String, List<DistanceMetric>> getDistanceMetrics()
	{
		return distanceMetrics;
	}

	public void setDistanceMetrics(String attrOneLabel, DistanceMetric distanceMetric)
	{
		if (!distanceMetrics.containsKey(attrOneLabel))
		{
			distanceMetrics.put(attrOneLabel, new ArrayList<>());
		}
		distanceMetrics.get(attrOneLabel).add(distanceMetric);
	}
}

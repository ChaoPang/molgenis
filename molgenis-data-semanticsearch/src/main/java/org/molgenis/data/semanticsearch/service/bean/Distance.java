package org.molgenis.data.semanticsearch.service.bean;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Distance<T> implements Comparable<Distance<T>>
{
	public static <T> Distance<T> create(T termOne, T TermTwo, boolean valid, double logDistance)
	{
		return new AutoValue_Distance<T>(termOne, TermTwo, valid, logDistance);
	}

	public abstract T getTermOne();

	public abstract T getTermTwo();

	public abstract boolean isValid();

	public abstract double getLogDistance();

	public int compareTo(Distance<T> o)
	{
		return Double.compare(getLogDistance(), o.getLogDistance());
	}
}

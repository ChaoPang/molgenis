package org.molgenis.data.semanticsearch.service.bean;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermHit.class)
public abstract class OntologyTermHit implements Comparable<OntologyTermHit>
{
	public static OntologyTermHit create(OntologyTerm ontologyTerm, String joinedSynonym, String matchedWords,
			float score)
	{
		return new AutoValue_OntologyTermHit(ontologyTerm, joinedSynonym, matchedWords, Math.round(score * 100000));
	}

	public abstract OntologyTerm getOntologyTerm();

	public abstract String getJoinedSynonym();

	public abstract String getMatchedWords();

	public abstract int getScoreInt();

	public float getScore()
	{
		return getScoreInt() / 100000.0f;
	}

	@Override
	public int compareTo(OntologyTermHit o)
	{
		return Float.compare(getScore(), o.getScore());
	}
}

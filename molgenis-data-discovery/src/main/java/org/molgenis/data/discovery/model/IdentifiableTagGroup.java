package org.molgenis.data.discovery.model;

import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

import com.google.auto.value.AutoValue;

/**
 * {@link OntologyTerm}s that got matched to an attribute.
 */
@AutoValue
@AutoGson(autoValueClass = AutoValue_IdentifiableTagGroup.class)
public abstract class IdentifiableTagGroup implements Comparable<IdentifiableTagGroup>
{
	public static IdentifiableTagGroup create(String identifier, TagGroup tagGroup)
	{
		return new AutoValue_IdentifiableTagGroup(identifier, tagGroup.getOntologyTerm(), tagGroup.getMatchedWords(),
				Math.round(tagGroup.getScore() * 100000));
	}

	public static IdentifiableTagGroup create(String identifier, OntologyTerm ontologyTerm, String matchedWords,
			float score)
	{
		return new AutoValue_IdentifiableTagGroup(identifier, ontologyTerm, matchedWords, Math.round(score * 100000));
	}

	public abstract String getIdentifier();

	/**
	 * The ontology terms that got matched to the attribute, combined into one {@link OntologyTerm}
	 */
	public abstract OntologyTerm getOntologyTerm();

	/**
	 * A long string containing all words in the {@link getJoinedSynonym()} that got matched to the attribute.
	 */
	public abstract String getMatchedWords();

	public abstract int getScoreInt();

	public double getScore()
	{
		return getScoreInt() / 100000.0d;
	}

	@Override
	public int compareTo(IdentifiableTagGroup o)
	{
		return Double.compare(getScore(), o.getScore());
	}
}

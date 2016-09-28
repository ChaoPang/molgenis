package org.molgenis.data.discovery.model.matching;

import java.util.List;

import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.model.SemanticType;

import com.google.auto.value.AutoValue;

/**
 * {@link OntologyTerm}s that got matched to an attribute.
 */
@AutoValue
@AutoGson(autoValueClass = AutoValue_IdentifiableTagGroup.class)
public abstract class IdentifiableTagGroup implements Comparable<IdentifiableTagGroup>
{
	public static IdentifiableTagGroup create(String identifier, List<OntologyTerm> ontologyTerms,
			List<SemanticType> semanticTypes, String matchedWords, float score)
	{
		return new AutoValue_IdentifiableTagGroup(identifier, ontologyTerms, semanticTypes, matchedWords,
				Math.round(score * 100000));
	}

	public abstract String getIdentifier();

	/**
	 * The ontology terms that got matched to the attribute, combined into one {@link OntologyTerm}
	 */
	public abstract List<OntologyTerm> getOntologyTerms();

	public abstract List<SemanticType> getSemanticTypes();

	/**
	 * A long string containing all words in the {@link getJoinedSynonym()} that got matched to the attribute.
	 */
	public abstract String getMatchedWords();

	public abstract int getScoreInt();

	public float getScore()
	{
		return getScoreInt() / 100000.0f;
	}

	@Override
	public int compareTo(IdentifiableTagGroup o)
	{
		return Double.compare(getScore(), o.getScore());
	}
}

package org.molgenis.data.nlp.relations;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;
import org.molgenis.data.nlp.beans.core.Word;

@AutoValue
@AutoGson(autoValueClass = AutoValue_WordRelation.class)
public abstract class WordRelation
{
	public abstract Word getGovernorWord();

	public abstract Relation getRelation();

	public abstract Word getDependentWord();

	public static WordRelation create(Word goverWord, Relation relation, Word dependentWord)
	{
		return new AutoValue_WordRelation(goverWord, relation, dependentWord);
	}
}

package org.molgenis.data.nlp.beans.relations;

import com.google.auto.value.AutoValue;
import edu.stanford.nlp.trees.GrammaticalRelation;
import org.molgenis.gson.AutoGson;

import javax.annotation.Nullable;

@AutoValue
@AutoGson(autoValueClass = AutoValue_Relation.class)
public abstract class Relation
{
	public abstract String getShortName();

	@Nullable
	public abstract String getSpecific();

	public abstract String getLongName();

	public static Relation create(GrammaticalRelation grammaticalRelation)
	{
		return new AutoValue_Relation(grammaticalRelation.getShortName(), grammaticalRelation.getSpecific(),
				grammaticalRelation.getLongName());
	}

	public boolean isComplementaryClausal()
	{
		return getShortName().startsWith("ccomp");
	}

	public boolean isAuxiliary()
	{
		return getShortName().startsWith("aux");
	}

	public boolean isSubject()
	{
		return getShortName().startsWith("nsubj");
	}

	public boolean isObject()
	{
		return getShortName().startsWith("dobj");
	}
}

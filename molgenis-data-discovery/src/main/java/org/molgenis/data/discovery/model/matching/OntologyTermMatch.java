package org.molgenis.data.discovery.model.matching;

import com.google.auto.value.AutoValue;
import org.molgenis.gson.AutoGson;
import org.molgenis.ontology.core.model.OntologyTerm;

@AutoValue
@AutoGson(autoValueClass = AutoValue_OntologyTermMatch.class)
public abstract class OntologyTermMatch
{
	public abstract OntologyTerm getTarget();

	public abstract OntologyTerm getSource();

	public abstract int getStopLevel();

	public static OntologyTermMatch create(OntologyTerm target, OntologyTerm source, int stopLevel)
	{
		return new AutoValue_OntologyTermMatch(target, source, stopLevel);
	}
}
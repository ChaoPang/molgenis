package org.molgenis.ontology.utils;

import org.molgenis.ontology.core.model.OntologyTermChildrenPredicate;
import org.molgenis.ontology.core.service.OntologyService;

public class PredicateUtils
{
	public static OntologyTermChildrenPredicate createRetrieveAllLevelPredicate(OntologyService ontologyService)
	{
		return new OntologyTermChildrenPredicate(-1, true, ontologyService);
	}

	public static OntologyTermChildrenPredicate createRetrieveLevelThreePredicate(OntologyService ontologyService)
	{
		return new OntologyTermChildrenPredicate(3, false, ontologyService);
	}

	public static OntologyTermChildrenPredicate createRetrieveLevelTwoPredicate(OntologyService ontologyService)
	{
		return new OntologyTermChildrenPredicate(2, false, ontologyService);
	}

	public static OntologyTermChildrenPredicate createRetrieveLevelOnePredicate(OntologyService ontologyService)
	{
		return new OntologyTermChildrenPredicate(1, false, ontologyService);
	}
}

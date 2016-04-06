package org.molgenis.data.semanticsearch.service;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.bean.Distance;
import org.molgenis.ontology.core.model.OntologyTerm;

public interface OntologyTermSemanticSearch
{
	public abstract Distance<String> getDistance(String queryOne, String queryTwo) throws ExecutionException;

	public abstract Distance<AttributeMetaData> getDistance(AttributeMetaData attr1, AttributeMetaData attr2,
			EntityMetaData entityMetaData1, EntityMetaData entityMetaData2) throws ExecutionException;

	public abstract double calculateAverageDistance(List<Hit<OntologyTerm>> ontologyTermsForAttr1,
			List<Hit<OntologyTerm>> ontologyTermsForAttr2) throws ExecutionException;
}

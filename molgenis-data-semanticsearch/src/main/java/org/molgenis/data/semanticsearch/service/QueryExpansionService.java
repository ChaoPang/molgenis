package org.molgenis.data.semanticsearch.service;

import java.util.List;
import java.util.Set;

import org.molgenis.data.QueryRule;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;

public interface QueryExpansionService
{
	public abstract QueryRule expand(Set<String> lexicalQueries, List<TagGroup> ontologyTermHits,
			QueryExpansionParameter ontologyExpansionParameters);
}

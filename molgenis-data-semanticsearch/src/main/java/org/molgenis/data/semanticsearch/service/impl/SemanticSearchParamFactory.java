package org.molgenis.data.semanticsearch.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.semantic.Relation;
import org.molgenis.data.semanticsearch.service.OntologyTagService;
import org.molgenis.data.semanticsearch.service.TagGroupGenerator;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParam;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParam;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.data.semanticsearch.utils.SemanticSearchServiceUtils;
import org.molgenis.ontology.core.model.Ontology;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Multimap;

import static java.util.Objects.requireNonNull;

public class SemanticSearchParamFactory
{
	public static final String UNIT_ONTOLOGY_IRI = "http://purl.obolibrary.org/obo/uo.owl";

	private final OntologyService ontologyService;
	private final OntologyTagService ontologyTagService;
	private final TagGroupGenerator tagGroupGenerator;

	@Autowired
	public SemanticSearchParamFactory(OntologyService ontologyService, OntologyTagService ontologyTagService,
			TagGroupGenerator tagGroupGenerator)
	{
		this.ontologyService = requireNonNull(ontologyService);
		this.ontologyTagService = requireNonNull(ontologyTagService);
		this.tagGroupGenerator = requireNonNull(tagGroupGenerator);
	}

	public SemanticSearchParam create(SemanticSearchParam semanticSearchParam, boolean semanticSearchEnabled,
			boolean childOntologyTermExpansionEnabled)
	{
		return SemanticSearchParam.create(semanticSearchParam.getLexicalQueries(), semanticSearchParam.getTagGroups(),
				QueryExpansionParam.create(semanticSearchEnabled, childOntologyTermExpansionEnabled));
	}

	public SemanticSearchParam create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData)
	{
		return create(targetAttribute, userQueries, targetEntityMetaData, false, false);
	}

	public SemanticSearchParam create(AttributeMetaData targetAttribute, Set<String> userQueries,
			EntityMetaData targetEntityMetaData, boolean semanticSearchEnabled,
			boolean childOntologyTermExpansionEnabled)
	{
		Set<String> lexicalQueries = SemanticSearchServiceUtils.getQueryTermsFromAttribute(targetAttribute,
				userQueries);

		List<TagGroup> tagGroups;

		if (semanticSearchEnabled)
		{
			List<String> ontologyIds = ontologyService.getOntologies().stream()
					.filter(ontology -> !ontology.getIRI().equals(UNIT_ONTOLOGY_IRI)).map(Ontology::getId)
					.collect(Collectors.toList());

			Multimap<Relation, OntologyTerm> tagsForAttribute = ontologyTagService
					.getTagsForAttribute(targetEntityMetaData, targetAttribute);

			if (!tagsForAttribute.isEmpty())
			{
				tagGroups = tagsForAttribute.values().stream().map(ot -> TagGroup.create(ot, ot.getLabel(), 1.0f))
						.collect(toList());
			}
			else
			{
				tagGroups = tagGroupGenerator.findTagGroups(StringUtils.join(lexicalQueries, ' '), ontologyIds);
			}

		}
		else tagGroups = emptyList();

		return SemanticSearchParam.create(lexicalQueries, tagGroups,
				QueryExpansionParam.create(semanticSearchEnabled, childOntologyTermExpansionEnabled));
	}
}

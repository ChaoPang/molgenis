package org.molgenis.data.discovery.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.discovery.controller.BiobankUniverseController;
import org.molgenis.data.discovery.model.AttributeMappingCandidate;
import org.molgenis.data.discovery.model.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.BiobankSampleCollection;
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.model.MatchingExplanation;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.semanticsearch.explain.bean.AttributeMatchExplanation;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameter;
import org.molgenis.data.semanticsearch.service.bean.TagGroup;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystemProxy;
import org.molgenis.ui.menu.MenuReaderService;

import static java.util.Objects.requireNonNull;

public class BiobankUniverseJobProcessor
{
	private static final int PROGRESS_UPDATE_BATCH_SIZE = 50;

	private final BiobankUniverse biobankUniverse;
	private final List<BiobankSampleCollection> members;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final SemanticSearchService semanticSearchService;
	private final AttributeMappingExplainService attributeMappingExplainService;
	private final OntologyService ontologyService;
	private final IdGenerator idGenerator;
	private final AtomicInteger counter;

	private final Progress progress;
	private final MenuReaderService menuReaderService;

	public BiobankUniverseJobProcessor(BiobankUniverse biobankUniverse, List<BiobankSampleCollection> members,
			BiobankUniverseRepository biobankUniverseRepository, SemanticSearchService semanticSearchService,
			AttributeMappingExplainService attributeMappingExplainService, OntologyService ontologyService,
			IdGenerator idGenerator, Progress progress, MenuReaderService menuReaderService)
	{
		this.biobankUniverse = requireNonNull(biobankUniverse);
		this.members = requireNonNull(members);
		this.biobankUniverseRepository = requireNonNull(biobankUniverseRepository);
		this.semanticSearchService = requireNonNull(semanticSearchService);
		this.attributeMappingExplainService = requireNonNull(attributeMappingExplainService);
		this.ontologyService = requireNonNull(ontologyService);
		this.idGenerator = requireNonNull(idGenerator);
		this.progress = requireNonNull(progress);
		this.counter = new AtomicInteger(0);
		this.menuReaderService = requireNonNull(menuReaderService);
	}

	public void process()
	{
		RunAsSystemProxy.runAsSystem(() -> {

			List<BiobankSampleCollection> existingMembers = biobankUniverse.getMembers();

			List<BiobankSampleCollection> newMembers = members.stream()
					.filter(member -> !existingMembers.contains(member)).collect(Collectors.toList());

			int totalNumberOfAttributes = newMembers.stream()
					.map(member -> biobankUniverseRepository.getAttributesFromCollection(member).size())
					.mapToInt(Integer::intValue).sum();

			progress.setProgressMax(totalNumberOfAttributes);

			for (BiobankSampleCollection biobankSampleCollection : newMembers)
			{
				// Add the member to the universe
				biobankUniverseRepository.addUniverseMembers(biobankUniverse, Arrays.asList(biobankSampleCollection));

				// Start tagging attributes for each entity
				List<TaggedAttribute> taggedAttributes = new ArrayList<>();

				List<AttributeMappingCandidate> candidates = new ArrayList<>();

				for (BiobankSampleAttribute biobankSampleAttribute : biobankUniverseRepository
						.getAttributesFromCollection(biobankSampleCollection))
				{
					// tag the target attribute with ontology terms
					List<TagGroup> ontologyTermHits = semanticSearchService
							.findAllTagsForAttr(targetAttribute, ontologyService.getAllOntologiesIds()).stream()
							.collect(Collectors.toList());

					taggedAttributes.add(TaggedAttribute.create(idGenerator.generateId(), targetAttribute,
							createExplanations(ontologyTermHits)));

					// SemanticSearch finding all the relevant attributes from existing entities
					SemanticSearchParameter semanticSearchParameter = SemanticSearchParameter.create(targetAttribute,
							null, biobankSampleCollection, existingMembers, QueryExpansionParameter.create(true, true));

					Map<EntityMetaData, List<AttributeMetaData>> candidateAttributes = semanticSearchService
							.findMultiEntityAttributes(semanticSearchParameter);

					for (Entry<EntityMetaData, List<AttributeMetaData>> entrySet : candidateAttributes.entrySet())
					{
						EntityMetaData sourceEntityMetaData = entrySet.getKey();

						List<ExplainedAttributeMetaData> explainedAttributes = entrySet.getValue().stream().limit(10)
								.map(matchedSourceAttribute -> attributeMappingExplainService
										.explainAttributeMapping(semanticSearchParameter, matchedSourceAttribute))
								.sorted().collect(toList());

						candidates.addAll(convertExplainedAttributeToCandidate(targetAttribute.getName(),
								biobankSampleCollection, sourceEntityMetaData, explainedAttributes));
					}

					// Update the progress only when the progress proceeds the threshold
					if (counter.incrementAndGet() % PROGRESS_UPDATE_BATCH_SIZE == 0)
					{
						progress.progress(counter.get(), "Processed " + counter + " input terms.");
					}

				}

				biobankUniverseRepository.addTaggedAttributes(biobankSampleCollection, taggedAttributes);

				biobankUniverseRepository.addAttributeMappingCandidates(candidates);
			}

			progress.setResultUrl(menuReaderService.getMenu().findMenuItemPath(BiobankUniverseController.ID)
					+ "/universe/" + biobankUniverse.getIdentifier());
		});
	}

	private List<AttributeMappingCandidate> convertExplainedAttributeToCandidate(String target,
			EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData,
			List<ExplainedAttributeMetaData> explainedAttributes)
	{
		List<AttributeMappingCandidate> candidates = new ArrayList<>();

		for (ExplainedAttributeMetaData explainedAttributeMetaData : explainedAttributes)
		{
			String source = explainedAttributeMetaData.getAttributeMetaData().get(AttributeMetaDataMetaData.NAME)
					.toString();

			MatchingExplanation mappingExplanation = explainedQueryStringToMappingExplanation(
					explainedAttributeMetaData.getExplainedQueryString());

			candidates.add(AttributeMappingCandidate.create(idGenerator.generateId(), target, source,
					targetEntityMetaData, sourceEntityMetaData, mappingExplanation, Collections.emptyList()));
		}
		return candidates;
	}

	private MatchingExplanation explainedQueryStringToMappingExplanation(AttributeMatchExplanation explainedQueryString)
	{
		String matchedWords = explainedQueryString.getMatchedWords();
		String queryString = explainedQueryString.getQueryString();
		float ngramScore = explainedQueryString.getScore();
		List<OntologyTerm> ontologyTerms = ontologyService
				.getAtomicOntologyTerms(explainedQueryString.getOntologyTerm());
		return MatchingExplanation.create(idGenerator.generateId(), ontologyTerms, queryString, matchedWords,
				ngramScore);
	}

	private List<MatchingExplanation> createExplanations(List<TagGroup> ontologyTermHits)
	{
		List<MatchingExplanation> explanations = new ArrayList<>();
		for (TagGroup hit : ontologyTermHits)
		{
			OntologyTerm ontologyTerm = hit.getOntologyTerm();
			List<OntologyTerm> atomicOntologyTerms = ontologyService.getAtomicOntologyTerms(ontologyTerm);
			MatchingExplanation mappingExplanation = MatchingExplanation.create(idGenerator.generateId(),
					atomicOntologyTerms, hit.getJoinedSynonym(), hit.getMatchedWords(), hit.getScore());
			explanations.add(mappingExplanation);
		}
		return explanations;
	}
}
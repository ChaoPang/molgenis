package org.molgenis.data.discovery.job;

import static com.google.common.collect.Iterables.size;
import static java.util.stream.Collectors.toList;

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
import org.molgenis.data.discovery.model.BiobankUniverse;
import org.molgenis.data.discovery.model.MappingExplanation;
import org.molgenis.data.discovery.model.TaggedAttribute;
import org.molgenis.data.discovery.repo.BiobankUniverseRepository;
import org.molgenis.data.jobs.Progress;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedAttributeMetaData;
import org.molgenis.data.semanticsearch.explain.bean.ExplainedQueryString;
import org.molgenis.data.semanticsearch.explain.service.AttributeMappingExplainService;
import org.molgenis.data.semanticsearch.semantic.Hit;
import org.molgenis.data.semanticsearch.service.SemanticSearchService;
import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
import org.molgenis.data.semanticsearch.service.bean.QueryExpansionParameter;
import org.molgenis.data.semanticsearch.service.bean.SemanticSearchParameter;
import org.molgenis.ontology.core.model.OntologyTerm;
import org.molgenis.ontology.core.service.OntologyService;
import org.molgenis.security.core.runas.RunAsSystemProxy;
import org.molgenis.ui.menu.MenuReaderService;

import static java.util.Objects.requireNonNull;

public class BiobankUniverseJobProcessor
{
	private static final int PROGRESS_UPDATE_BATCH_SIZE = 50;

	private final BiobankUniverse biobankUniverse;
	private final List<EntityMetaData> members;
	private final BiobankUniverseRepository biobankUniverseRepository;
	private final SemanticSearchService semanticSearchService;
	private final AttributeMappingExplainService attributeMappingExplainService;
	private final OntologyService ontologyService;
	private final IdGenerator idGenerator;
	private final AtomicInteger counter;

	private final Progress progress;
	private final MenuReaderService menuReaderService;

	public BiobankUniverseJobProcessor(BiobankUniverse biobankUniverse, List<EntityMetaData> members,
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

			List<EntityMetaData> existingMembers = biobankUniverse.getMembers();

			List<EntityMetaData> newMembers = members.stream().filter(member -> !existingMembers.contains(member))
					.collect(Collectors.toList());

			int totalNumberOfAttributes = newMembers.stream().map(member -> size(member.getAtomicAttributes()))
					.mapToInt(Integer::intValue).sum();

			progress.setProgressMax(totalNumberOfAttributes);

			for (EntityMetaData entityMetaData : newMembers)
			{
				// Add the member to the universe
				biobankUniverseRepository.addUniverseMembers(biobankUniverse, Arrays.asList(entityMetaData));

				// Start tagging attributes for each entity
				List<TaggedAttribute> taggedAttributes = new ArrayList<>();

				List<AttributeMappingCandidate> candidates = new ArrayList<>();

				for (AttributeMetaData targetAttribute : entityMetaData.getAtomicAttributes())
				{
					// tag the target attribute with ontology terms
					List<Hit<OntologyTermHit>> ontologyTermHits = semanticSearchService
							.findAllTagsForAttr(targetAttribute, ontologyService.getAllOntologiesIds()).stream()
							.collect(Collectors.toList());

					taggedAttributes.add(TaggedAttribute.create(idGenerator.generateId(), targetAttribute,
							createExplanations(ontologyTermHits)));

					// SemanticSearch finding all the relevant attributes from existing entities
					SemanticSearchParameter semanticSearchParameter = SemanticSearchParameter.create(targetAttribute,
							null, entityMetaData, existingMembers, true, QueryExpansionParameter.create(true, true));

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
								entityMetaData, sourceEntityMetaData, explainedAttributes));
					}

					// Update the progress only when the progress proceeds the threshold
					if (counter.incrementAndGet() % PROGRESS_UPDATE_BATCH_SIZE == 0)
					{
						progress.progress(counter.get(), "Processed " + counter + " input terms.");
					}

				}

				biobankUniverseRepository.addTaggedAttributes(entityMetaData, taggedAttributes);

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

			MappingExplanation mappingExplanation = explainedQueryStringToMappingExplanation(
					explainedAttributeMetaData.getExplainedQueryString());

			candidates.add(AttributeMappingCandidate.create(idGenerator.generateId(), target, source,
					targetEntityMetaData, sourceEntityMetaData, mappingExplanation, Collections.emptyList()));
		}
		return candidates;
	}

	private MappingExplanation explainedQueryStringToMappingExplanation(ExplainedQueryString explainedQueryString)
	{
		String matchedWords = explainedQueryString.getMatchedWords();
		String queryString = explainedQueryString.getQueryString();
		float ngramScore = explainedQueryString.getScore();
		List<OntologyTerm> ontologyTerms = ontologyService
				.getAtomicOntologyTerms(explainedQueryString.getOntologyTerm());
		return MappingExplanation.create(idGenerator.generateId(), ontologyTerms, queryString, matchedWords,
				ngramScore);
	}

	private List<MappingExplanation> createExplanations(List<Hit<OntologyTermHit>> ontologyTermHits)
	{
		List<MappingExplanation> explanations = new ArrayList<>();
		for (Hit<OntologyTermHit> hit : ontologyTermHits)
		{
			OntologyTerm ontologyTerm = hit.getResult().getOntologyTerm();
			List<OntologyTerm> atomicOntologyTerms = ontologyService.getAtomicOntologyTerms(ontologyTerm);
			MappingExplanation mappingExplanation = MappingExplanation.create(idGenerator.generateId(),
					atomicOntologyTerms, hit.getResult().getJoinedSynonym(), hit.getResult().getMatchedWords(),
					hit.getScore());
			explanations.add(mappingExplanation);
		}
		return explanations;
	}
}

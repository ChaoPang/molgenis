package org.molgenis.data.spss;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.molgenis.data.EntityMetaData;
import org.molgenis.data.MolgenisDataAccessException;
import org.molgenis.data.Repository;
import org.molgenis.data.support.FileRepositoryCollection;
import org.molgenis.data.support.GenericImporterExtensions;

import com.google.common.collect.Lists;

public class SpssRepositoryCollection extends FileRepositoryCollection
{
	private final SpssRepository spssRepository;

	public SpssRepositoryCollection(File file)
	{
		super(GenericImporterExtensions.getSpss());
		if (file == null) throw new IllegalArgumentException("file is null");

		String name = file.getName();
		if (name.endsWith(GenericImporterExtensions.SPSS.toString()))
		{
			name = name.substring(0, name.lastIndexOf('.' + GenericImporterExtensions.SPSS.toString())).replace('.',
					'_');
		}
		else
		{
			throw new IllegalArgumentException("Not a obo.zip or owl.zip file [" + file.getName() + "]");
		}
		this.spssRepository = new SpssRepository(file, name);
	}

	@Override
	public String getName()
	{
		return spssRepository.getName();
	}

	@Override
	public Repository addEntityMeta(EntityMetaData entityMeta)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasRepository(String name)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Repository> iterator()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<String> getEntityNames()
	{
		List<String> orderedEntityNames = Lists.newArrayList(spssRepository.getReferenceRepositories().keySet()
				.stream().collect(Collectors.toSet()));
		orderedEntityNames.add(spssRepository.getEntityMetaData().getName());
		return orderedEntityNames;
	}

	@Override
	public Repository getRepository(String name)
	{
		if (spssRepository.getReferenceRepositories().containsKey(name))
		{
			return spssRepository.getReferenceRepositories().get(name);
		}

		if (spssRepository.getEntityMetaData().getName().equalsIgnoreCase(name))
		{
			return spssRepository;
		}

		throw new MolgenisDataAccessException("Could not find the Repository (" + name + ")");
	}
}

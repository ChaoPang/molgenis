package org.molgenis.data.csv;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.Entity;
import org.molgenis.data.processor.CellProcessor;
import org.springframework.util.FileCopyUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CsvRepositoryTest
{

	private static File test;
	private static File testdata;
	private static File novalues;
	private static File emptyvalues;
	private static File testtsv;
	private static File emptylines;
	private static File emptylinessinglecol;

	@BeforeClass
	public static void beforeClass() throws FileNotFoundException, IOException
	{
		InputStream in = CsvRepositoryTest.class.getResourceAsStream("/test.csv");
		test = new File(FileUtils.getTempDirectory(), "test.csv");
		FileCopyUtils.copy(in, new FileOutputStream(test));

		in = CsvRepositoryTest.class.getResourceAsStream("/testdata.csv");
		testdata = new File(FileUtils.getTempDirectory(), "testdata.csv");
		FileCopyUtils.copy(in, new FileOutputStream(testdata));

		in = CsvRepositoryTest.class.getResourceAsStream("/novalues.csv");
		novalues = new File(FileUtils.getTempDirectory(), "novalues.csv");
		FileCopyUtils.copy(in, new FileOutputStream(novalues));

		in = CsvRepositoryTest.class.getResourceAsStream("/emptyvalues.csv");
		emptyvalues = new File(FileUtils.getTempDirectory(), "emptyvalues.csv");
		FileCopyUtils.copy(in, new FileOutputStream(emptyvalues));

		in = CsvRepositoryTest.class.getResourceAsStream("/test.tsv");
		testtsv = new File(FileUtils.getTempDirectory(), "test.tsv");
		FileCopyUtils.copy(in, new FileOutputStream(testtsv));

		in = CsvRepositoryTest.class.getResourceAsStream("/emptylines.csv");
		emptylines = new File(FileUtils.getTempDirectory(), "emptylines.csv");
		FileCopyUtils.copy(in, new FileOutputStream(emptylines));

		in = CsvRepositoryTest.class.getResourceAsStream("/emptylinessinglecol.csv");
		emptylinessinglecol = new File(FileUtils.getTempDirectory(), "emptylinessinglecol.csv");
		FileCopyUtils.copy(in, new FileOutputStream(emptylinessinglecol));
	}

	@Test
	public void addCellProcessor_header() throws IOException
	{
		CellProcessor processor = when(mock(CellProcessor.class).processHeader()).thenReturn(true).getMock();
		when(processor.process("col1")).thenReturn("col1");
		when(processor.process("col2")).thenReturn("col2");

		CsvRepository csvRepository = new CsvRepository(test, null);
		try
		{
			csvRepository.addCellProcessor(processor);
			for (@SuppressWarnings("unused")
			Entity entity : csvRepository)
			{
			}
			verify(processor).process("col1");
			verify(processor).process("col2");
		}
		finally
		{
			csvRepository.close();
		}
	}

	@Test
	public void addCellProcessor_data() throws IOException
	{
		CellProcessor processor = when(mock(CellProcessor.class).processData()).thenReturn(true).getMock();
		CsvRepository csvRepository = new CsvRepository(test, null);
		try
		{
			csvRepository.addCellProcessor(processor);
			for (@SuppressWarnings("unused")
			Entity entity : csvRepository)
			{
			}
			verify(processor).process("val1");
			verify(processor).process("val2");
		}
		finally
		{
			csvRepository.close();
		}
	}

	@Test
	public void metaData() throws IOException
	{
		CsvRepository csvRepository = null;
		try
		{
			csvRepository = new CsvRepository(testdata, null);
			assertEquals(csvRepository.getName(), "testdata");
			Iterator<AttributeMetaData> it = csvRepository.getEntityMetaData().getAttributes().iterator();
			assertTrue(it.hasNext());
			assertEquals(it.next().getName(), "col1");
			assertTrue(it.hasNext());
			assertEquals(it.next().getName(), "col2");
			assertFalse(it.hasNext());
		}
		finally
		{
			IOUtils.closeQuietly(csvRepository);
		}
	}

	/**
	 * Test based on au.com.bytecode.opencsv.CSVReaderTest
	 * 
	 * @throws IOException
	 */
	@Test
	public void iterator() throws IOException
	{
		CsvRepository csvRepository = null;
		try
		{
			csvRepository = new CsvRepository(testdata, null);
			Iterator<Entity> it = csvRepository.iterator();

			assertTrue(it.hasNext());
			Entity entity = it.next();
			assertEquals(entity.get("col1"), "val1");
			assertEquals(entity.get("col2"), "val2");

			assertTrue(it.hasNext());
			entity = it.next();
			assertEquals(entity.get("col1"), "a,a");
			assertEquals(entity.get("col2"), "b");
			assertTrue(it.hasNext());

			assertTrue(it.hasNext());
			entity = it.next();
			assertNull(entity.get("col1"));
			assertEquals(entity.get("col2"), "a");

			assertTrue(it.hasNext());
			entity = it.next();
			assertEquals(entity.get("col1"), "\"");
			assertEquals(entity.get("col2"), "\"\"");

			assertTrue(it.hasNext());
			entity = it.next();
			assertEquals(entity.get("col1"), ",");
			assertEquals(entity.get("col2"), ",,");

			assertFalse(it.hasNext());
		}
		finally
		{
			IOUtils.closeQuietly(csvRepository);
		}
	}

	@Test
	public void iterator_noValues() throws IOException
	{
		CsvRepository csvRepository = new CsvRepository(novalues, null);
		try
		{
			Iterator<Entity> it = csvRepository.iterator();
			assertFalse(it.hasNext());
		}
		finally
		{
			csvRepository.close();
		}
	}

	@Test
	public void iterator_emptyValues() throws IOException
	{
		CsvRepository csvRepository = new CsvRepository(emptyvalues, null);
		try
		{
			Iterator<Entity> it = csvRepository.iterator();
			assertTrue(it.hasNext());
			assertNull(it.next().get("col1"));
		}
		finally
		{
			csvRepository.close();
		}
	}

	@Test
	public void iterator_tsv() throws IOException
	{
		CsvRepository tsvRepository = new CsvRepository(testtsv, null);
		try
		{
			Iterator<Entity> it = tsvRepository.iterator();
			Entity entity = it.next();
			assertEquals(entity.get("col1"), "val1");
			assertEquals(entity.get("col2"), "val2");
			assertFalse(it.hasNext());
		}
		finally
		{
			tsvRepository.close();
		}
	}

	@Test
	public void iterator_emptylines() throws IOException
	{
		CsvRepository csvRepository = new CsvRepository(emptylines, null);
		try
		{
			Iterator<Entity> it = csvRepository.iterator();
			Entity entity = it.next();
			assertEquals(entity.get("col1"), "val1");
			assertEquals(entity.get("col2"), "val2");
			assertFalse(it.hasNext());
		}
		finally
		{
			csvRepository.close();
		}
	}

	@Test
	public void iterator_emptylines_singlecol() throws IOException
	{
		CsvRepository csvRepository = new CsvRepository(emptylinessinglecol, null);
		try
		{
			Iterator<Entity> it = csvRepository.iterator();
			Entity entity = it.next();
			assertEquals(entity.get("col1"), "val1");

			assertTrue(it.hasNext());
			entity = it.next();
			assertNull(entity.get("col1"));

			assertFalse(it.hasNext());
		}
		finally
		{
			csvRepository.close();
		}
	}
}

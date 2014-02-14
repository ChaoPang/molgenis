package org.molgenis.omx.biobankconnect.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.molgenis.io.excel.ExcelReader;
import org.molgenis.io.excel.ExcelSheetReader;
import org.molgenis.io.excel.ExcelSheetWriter;
import org.molgenis.io.excel.ExcelWriter;
import org.molgenis.io.excel.ExcelWriter.FileFormat;
import org.molgenis.util.tuple.KeyValueTuple;
import org.molgenis.util.tuple.Tuple;

public class EvaluationMatrixHelper
{
	public static void main(String args[]) throws IOException
	{
		String rootDirectoryPath = "/Users/chaopang/Desktop/Variables_result/Evaluation/Retrieved-Documents/matrix/";
		ExcelReader excelReader = null;
		try
		{
			excelReader = new ExcelReader(new File(rootDirectoryPath + "observableFeature.xlsx"));
			ExcelSheetReader excelSheetReader = excelReader.getSheet(0);

			Iterator<Tuple> iterator = excelSheetReader.iterator();

			List<KeyValueTuple> listOfTuples = new ArrayList<KeyValueTuple>();
			List<String> headers = new ArrayList<String>();
			headers.add("Identifier");
			headers.add("Name");
			while (iterator.hasNext())
			{
				Tuple row = iterator.next();
				StringBuilder dataElement = new StringBuilder();
				dataElement.append(row.getString("Name")).append(":").append(row.getString("description"));
				String identifier = row.getString("Identifier");
				if (identifier.matches("HOP-minimal-feature.+"))
				{
					headers.add(dataElement.toString());
				}
				else
				{
					KeyValueTuple tuple = new KeyValueTuple();
					tuple.set("Identifier", row.getString("Identifier"));
					tuple.set("Name", dataElement.toString());
					listOfTuples.add(tuple);
				}
			}
			createExcel(rootDirectoryPath + "complete.xlsx", headers, listOfTuples);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (excelReader != null) excelReader.close();
		}

	}

	private static void createExcel(String outputFilePath, List<String> headers, List<KeyValueTuple> tuples)
			throws IOException
	{
		ExcelWriter excelWriter = null;
		try
		{
			excelWriter = new ExcelWriter(new File(outputFilePath), FileFormat.XLSX);
			ExcelSheetWriter tupleWriter = (ExcelSheetWriter) excelWriter.createTupleWriter("evaluation");
			tupleWriter.writeColNames(headers);
			for (KeyValueTuple tuple : tuples)
			{
				tupleWriter.write(tuple);
			}
			tupleWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (excelWriter != null) excelWriter.close();
		}
	}
}

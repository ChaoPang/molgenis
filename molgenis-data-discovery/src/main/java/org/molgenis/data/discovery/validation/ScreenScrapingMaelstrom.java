package org.molgenis.data.discovery.validation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.molgenis.data.discovery.model.biobank.BiobankSampleAttribute;
import org.molgenis.data.discovery.model.biobank.BiobankSampleCollection;

public class ScreenScrapingMaelstrom
{
	private static final String URL_PREFIX = "https://www.maelstrom-research.org/mica/repository/variables/_rql/network(exists(Mica_network.id)),variable(in(Mica_variable.variableType,(Study)),limit(";
	private static final String URL_SUFFIX = "),sort(name)),study(exists(Mica_study.id)),locale(en)/ws";
	private static final int SIZE = 5000;

	public static void main(String args[]) throws IOException, InterruptedException
	{
		File file = new File("/Users/chaopang/Desktop/MaelstromVariables.csv");
		FileOutputStream openOutputStream = FileUtils.openOutputStream(file);
		for (int i = 0; i < 95; i++)
		{

			String url = URL_PREFIX + i * SIZE + "," + SIZE + URL_SUFFIX;

			InputStream is = new URL(url).openStream();
			try
			{
				BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
				String jsonText = readAll(rd);
				List<BiobankSampleAttribute> extractJson = extractJson(jsonText);

				for (BiobankSampleAttribute biobankSampleAttribute : extractJson)
				{
					String label = biobankSampleAttribute.getLabel() + "\n";
					openOutputStream.write(label.getBytes());
				}
			}
			finally
			{
				is.close();
			}

			Thread.sleep(1000);
		}
		openOutputStream.close();
	}

	private static List<BiobankSampleAttribute> extractJson(String jsonText)
	{
		JSONObject jsonObject = new JSONObject(jsonText);
		JSONObject variableResultDto = jsonObject.getJSONObject("variableResultDto");
		JSONObject result = variableResultDto.getJSONObject("obiba.mica.DatasetVariableResultDto.result");
		JSONArray jsonArray = result.getJSONArray("summaries");

		List<BiobankSampleAttribute> attributes = new ArrayList<>();

		for (Object object : jsonArray)
		{
			JSONObject varaibleJson = new JSONObject(object.toString());
			String dataSetId = varaibleJson.getString("datasetId");
			String variableId = varaibleJson.getString("id");
			String variableName = varaibleJson.getString("name");
			JSONArray labels = varaibleJson.getJSONArray("variableLabel");
			String variableLabel = "";
			for (Object label : labels)
			{
				JSONObject labelJSONObject = new JSONObject(label.toString());
				String lang = labelJSONObject.getString("lang");
				if (lang.equals("en"))
				{
					variableLabel = labelJSONObject.getString("value");
					break;
				}
			}

			attributes.add(BiobankSampleAttribute.create(variableId, variableName, variableLabel, StringUtils.EMPTY,
					BiobankSampleCollection.create(dataSetId), Collections.emptyList()));
		}

		return attributes;
	}

	private static String readAll(Reader rd) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1)
		{
			sb.append((char) cp);
		}
		return sb.toString();
	}
}

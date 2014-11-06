package org.molgenis.js;

import static org.molgenis.js.ScriptableValue.MISSING_VALUE;

public class ScriptHelper
{
	private final static String INTEGER_IN_DECIMAL_FORMAT = "\\d+\\.0*";

	public static Boolean isNull(Object object)
	{
		return object == null || object.toString().equals(MISSING_VALUE);
	}

	public static String isValid(Object object)
	{
		StringBuilder stringBuilder = new StringBuilder();

		if (object.toString().matches(INTEGER_IN_DECIMAL_FORMAT))
		{
			stringBuilder.append((int) Double.parseDouble(object.toString()));
		}
		else
		{
			stringBuilder.append(object);
		}
		return stringBuilder.toString();
	}
}

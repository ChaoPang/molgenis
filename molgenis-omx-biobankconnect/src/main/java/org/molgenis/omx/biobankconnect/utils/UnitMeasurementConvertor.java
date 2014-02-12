//package org.molgenis.omx.biobankconnect.utils;
//
//import java.lang.reflect.Field;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import javax.measure.unit.NonSI;
//import javax.measure.unit.SI;
//import javax.measure.unit.Unit;
//
//import org.jscience.physics.amount.Amount;
//
//public class UnitMeasurementConvertor
//{
//
//	private static Map<String, Unit<?>> UnitMap = new HashMap<String, Unit<?>>();
//
//	/**
//	 * @param args
//	 * @throws SecurityException
//	 * @throws NoSuchFieldException
//	 * @throws IllegalAccessException
//	 * @throws IllegalArgumentException
//	 */
//	public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException,
//			NoSuchFieldException, SecurityException
//	{
//		initializeUnitMap();
//		// input standard unit
//		String standardUnit1 = "kilogram/m^[2]";
//		List<String> list1 = Arrays.asList(standardUnit1.split("/"));
//		// custom units to convert
//		List<String> list2 = Arrays.asList("gram", "cm");
//
//		for (String unitName1 : list1)
//		{
//			for (String unitName2 : list2)
//			{
//				String unitConversionScript = compare(getUnit(unitName1), getUnit(unitName2));
//				if (unitConversionScript.isEmpty())
//				{
//					unitConversionScript = compareAdvanced(unitName1, unitName2);
//				}
//				if (!unitConversionScript.isEmpty())
//				{
//					System.out.println("Unit conversion from : " + unitName2 + " to : " + unitName1 + " -------> "
//							+ unitConversionScript);
//					break;
//				}
//			}
//		}
//	}
//
//	private static String compareAdvanced(String unitName1, String unitName2)
//	{
//		boolean foundGroup = false;
//		StringBuilder algorithmScript = new StringBuilder();
//		Pattern pattern = Pattern.compile("(\\w*)\\^\\[(\\d*)\\]");
//		Matcher matcher = pattern.matcher(unitName1);
//		if (matcher.find())
//		{
//			foundGroup = true;
//			unitName1 = matcher.group(1);
//			algorithmScript.append(".pow(").append(matcher.group(2)).append(")");
//		}
//		matcher = pattern.matcher(unitName2);
//		if (matcher.find())
//		{
//			foundGroup = true;
//			unitName2 = matcher.group(1);
//			algorithmScript.append(".root(").append(matcher.group(2)).append(")");
//		}
//		if (getUnit(unitName1) != null && getUnit(unitName2) != null && foundGroup)
//		{
//			String compare = compare(getUnit(unitName1), getUnit(unitName2));
//			if (!unitName1.equals(unitName2) && compare.isEmpty()) algorithmScript.delete(0, algorithmScript.length());
//			else algorithmScript.insert(0, compare);
//		}
//
//		return algorithmScript.toString();
//	}
//
//	private static String compare(Unit<?> unit1, Unit<?> unit2)
//	{
//		StringBuilder conversionScript = new StringBuilder();
//		Amount<?> value2 = Amount.valueOf(1, unit2);
//		if (unit1 == null || unit2 == null || unit1.equals(unit2)) return conversionScript.toString();
//		if (unit1.isCompatible(unit2))
//		{
//			Amount<?> value1 = value2.to(unit1);
//			double estimatedValue1 = value1.getEstimatedValue();
//			double estimatedValue2 = value2.getEstimatedValue();
//
//			if (estimatedValue1 > estimatedValue2)
//			{
//				conversionScript.append(".times(").append(value1.divide(value2).getEstimatedValue()).append(")");
//			}
//			else
//			{
//				conversionScript.append(".div(").append(value2.divide(value1).getEstimatedValue()).append(")");
//			}
//		}
//
//		return conversionScript.toString();
//	}
//
//	private static Unit<?> getUnit(String unitName)
//	{
//		return UnitMap.get(superscript(unitName.toLowerCase()));
//	}
//
//	private static void initializeUnitMap() throws IllegalArgumentException, IllegalAccessException
//	{
//		// initializeUnitMap();
//		for (Field field : SI.class.getFields())
//		{
//			Unit<?> unit = (Unit<?>) field.get(null);
//			UnitMap.put(unit.toString().toLowerCase(), unit);
//			UnitMap.put(field.getName().toLowerCase(), unit);
//		}
//		for (Field field : NonSI.class.getFields())
//		{
//			Unit<?> unit = (Unit<?>) field.get(null);
//			UnitMap.put(unit.toString().toLowerCase(), unit);
//			UnitMap.put(field.getName().toLowerCase(), unit);
//		}
//	}
//
//	// private static void initializeUnitMap()
//	// {
//	// // Standard units
//	// UnitMap.put(SI.AMPERE.toString(), SI.AMPERE);
//	// UnitMap.put(SI.BECQUEREL.toString(), SI.BECQUEREL);
//	// UnitMap.put(SI.BIT.toString(), SI.BIT);
//	// UnitMap.put(SI.CANDELA.toString(), SI.CANDELA);
//	// UnitMap.put(SI.CELSIUS.toString(), SI.CELSIUS);
//	// UnitMap.put(SI.CENTIMETER.toString(), SI.CENTIMETER);
//	// UnitMap.put(SI.CENTIMETRE.toString(), SI.CENTIMETRE);
//	// UnitMap.put(SI.COULOMB.toString(), SI.COULOMB);
//	// UnitMap.put(SI.CUBIC_METRE.toString(), SI.CUBIC_METRE);
//	// UnitMap.put(SI.FARAD.toString(), SI.FARAD);
//	// UnitMap.put(SI.GRAM.toString(), SI.GRAM);
//	// UnitMap.put(SI.GRAY.toString(), SI.GRAY);
//	// UnitMap.put(SI.HENRY.toString(), SI.HENRY);
//	// UnitMap.put(SI.HERTZ.toString(), SI.HERTZ);
//	// UnitMap.put(SI.JOULE.toString(), SI.JOULE);
//	// UnitMap.put(SI.KATAL.toString(), SI.KATAL);
//	// UnitMap.put(SI.KELVIN.toString(), SI.KELVIN);
//	// UnitMap.put(SI.KILOGRAM.toString(), SI.KILOGRAM);
//	// UnitMap.put(SI.KILOMETRE.toString(), SI.KILOMETRE);
//	// UnitMap.put(SI.KILOMETER.toString(), SI.KILOMETER);
//	// UnitMap.put(SI.LUMEN.toString(), SI.LUMEN);
//	// UnitMap.put(SI.LUX.toString(), SI.LUX);
//	// UnitMap.put(SI.METER.toString(), SI.METER);
//	// UnitMap.put(SI.METRE.toString(), SI.METRE);
//	// UnitMap.put(SI.MOLE.toString(), SI.MOLE);
//	// UnitMap.put(SI.MILLIMETER.toString(), SI.MILLIMETER);
//	// UnitMap.put(SI.MILLIMETRE.toString(), SI.MILLIMETRE);
//	// UnitMap.put(SI.METERS_PER_SECOND.toString(), SI.METERS_PER_SECOND);
//	// UnitMap.put(SI.METERS_PER_SQUARE_SECOND.toString(),
//	// SI.METERS_PER_SQUARE_SECOND);
//	// UnitMap.put(SI.METRES_PER_SECOND.toString(), SI.METRES_PER_SECOND);
//	// UnitMap.put(SI.METRES_PER_SQUARE_SECOND.toString(),
//	// SI.METRES_PER_SQUARE_SECOND);
//	// UnitMap.put(SI.NEWTON.toString(), SI.NEWTON);
//	// UnitMap.put(SI.OHM.toString(), SI.OHM);
//	// UnitMap.put(SI.PASCAL.toString(), SI.PASCAL);
//	// UnitMap.put(SI.RADIAN.toString(), SI.RADIAN);
//	// UnitMap.put(SI.SQUARE_METRE.toString(), SI.SQUARE_METRE);
//	// UnitMap.put(SI.SECOND.toString(), SI.SECOND);
//	// UnitMap.put(SI.SIEMENS.toString(), SI.SIEMENS);
//	// UnitMap.put(SI.SIEVERT.toString(), SI.SIEVERT);
//	// UnitMap.put(SI.STERADIAN.toString(), SI.STERADIAN);
//	// UnitMap.put(SI.TESLA.toString(), SI.TESLA);
//	// UnitMap.put(SI.OHM.toString(), SI.OHM);
//	// UnitMap.put(SI.VOLT.toString(), SI.VOLT);
//	// UnitMap.put(SI.WATT.toString(), SI.WATT);
//	// UnitMap.put(SI.WEBER.toString(), SI.WEBER);
//	//
//	// // Non-standard units
//	// UnitMap.put(NonSI.ANGSTROM.toString(), NonSI.ANGSTROM);
//	// UnitMap.put(NonSI.ARE.toString(), NonSI.ARE);
//	// UnitMap.put(NonSI.ASTRONOMICAL_UNIT.toString(), NonSI.ASTRONOMICAL_UNIT);
//	// UnitMap.put(NonSI.ATMOSPHERE.toString(), NonSI.ATMOSPHERE);
//	// UnitMap.put(NonSI.ATOM.toString(), NonSI.ATOM);
//	// UnitMap.put(NonSI.ATOMIC_MASS.toString(), NonSI.ATOMIC_MASS);
//	// UnitMap.put(NonSI.BAR.toString(), NonSI.BAR);
//	// UnitMap.put(NonSI.BYTE.toString(), NonSI.BYTE);
//	// UnitMap.put(NonSI.C.toString(), NonSI.C);
//	// UnitMap.put(NonSI.CENTIRADIAN.toString(), NonSI.CENTIRADIAN);
//	// UnitMap.put(NonSI.COMPUTER_POINT.toString(), NonSI.COMPUTER_POINT);
//	// UnitMap.put(NonSI.CUBIC_INCH.toString(), NonSI.CUBIC_INCH);
//	// UnitMap.put(NonSI.CURIE.toString(), NonSI.CURIE);
//	// UnitMap.put(NonSI.DAY.toString(), NonSI.DAY);
//	// UnitMap.put(NonSI.DAY_SIDEREAL.toString(), NonSI.DAY_SIDEREAL);
//	// UnitMap.put(NonSI.DECIBEL.toString(), NonSI.DECIBEL);
//	// UnitMap.put(NonSI.DEGREE_ANGLE.toString(), NonSI.DEGREE_ANGLE);
//	// UnitMap.put(NonSI.DYNE.toString(), NonSI.DYNE);
//	// UnitMap.put(NonSI.E.toString(), NonSI.E);
//	// UnitMap.put(NonSI.ELECTRON_MASS.toString(), NonSI.ELECTRON_MASS);
//	// UnitMap.put(NonSI.ELECTRON_VOLT.toString(), NonSI.ELECTRON_VOLT);
//	// UnitMap.put(NonSI.ERG.toString(), NonSI.ERG);
//	// UnitMap.put(NonSI.FAHRENHEIT.toString(), NonSI.FAHRENHEIT);
//	// UnitMap.put(NonSI.FARADAY.toString(), NonSI.FARADAY);
//	// UnitMap.put(NonSI.FOOT.toString(), NonSI.FOOT);
//	// UnitMap.put(NonSI.FOOT_SURVEY_US.toString(), NonSI.FOOT_SURVEY_US);
//	// UnitMap.put(NonSI.FRANKLIN.toString(), NonSI.FRANKLIN);
//	// UnitMap.put(NonSI.G.toString(), NonSI.G);
//	// UnitMap.put(NonSI.GALLON_DRY_US.toString(), NonSI.GALLON_DRY_US);
//	// UnitMap.put(NonSI.GALLON_LIQUID_US.toString(), NonSI.GALLON_LIQUID_US);
//	// UnitMap.put(NonSI.GALLON_UK.toString(), NonSI.GALLON_UK);
//	// UnitMap.put(NonSI.GAUSS.toString(), NonSI.GAUSS);
//	// UnitMap.put(NonSI.GILBERT.toString(), NonSI.GILBERT);
//	// UnitMap.put(NonSI.GRADE.toString(), NonSI.GRADE);
//	// UnitMap.put(NonSI.HECTARE.toString(), NonSI.HECTARE);
//	// UnitMap.put(NonSI.HORSEPOWER.toString(), NonSI.HORSEPOWER);
//	// UnitMap.put(NonSI.HOUR.toString(), NonSI.HOUR);
//	// UnitMap.put(NonSI.INCH.toString(), NonSI.INCH);
//	// UnitMap.put(NonSI.INCH_OF_MERCURY.toString(), NonSI.INCH_OF_MERCURY);
//	// UnitMap.put(NonSI.KILOGRAM_FORCE.toString(), NonSI.KILOGRAM_FORCE);
//	// UnitMap.put(NonSI.KILOMETERS_PER_HOUR.toString(),
//	// NonSI.KILOMETERS_PER_HOUR);
//	// UnitMap.put(NonSI.KILOMETRES_PER_HOUR.toString(),
//	// NonSI.KILOMETRES_PER_HOUR);
//	// UnitMap.put(NonSI.KNOT.toString(), NonSI.KNOT);
//	// UnitMap.put(NonSI.LAMBERT.toString(), NonSI.LAMBERT);
//	// UnitMap.put(NonSI.LIGHT_YEAR.toString(), NonSI.LIGHT_YEAR);
//	// UnitMap.put(NonSI.LITER.toString(), NonSI.LITER);
//	// UnitMap.put(NonSI.LITRE.toString(), NonSI.LITRE);
//	// UnitMap.put(NonSI.MACH.toString(), NonSI.MACH);
//	// UnitMap.put(NonSI.MAXWELL.toString(), NonSI.MAXWELL);
//	// UnitMap.put(NonSI.METRIC_TON.toString(), NonSI.METRIC_TON);
//	// UnitMap.put(NonSI.MILE.toString(), NonSI.MILE);
//	// UnitMap.put(NonSI.MILES_PER_HOUR.toString(), NonSI.MILES_PER_HOUR);
//	// UnitMap.put(NonSI.MILLIMETER_OF_MERCURY.toString(),
//	// NonSI.MILLIMETER_OF_MERCURY);
//	// UnitMap.put(NonSI.MINUTE.toString(), NonSI.MINUTE);
//	// UnitMap.put(NonSI.MINUTE_ANGLE.toString(), NonSI.MINUTE_ANGLE);
//	// UnitMap.put(NonSI.MONTH.toString(), NonSI.MONTH);
//	// UnitMap.put(NonSI.NAUTICAL_MILE.toString(), NonSI.NAUTICAL_MILE);
//	// UnitMap.put(NonSI.OCTET.toString(), NonSI.OCTET);
//	// UnitMap.put(NonSI.OUNCE.toString(), NonSI.OUNCE);
//	// UnitMap.put(NonSI.OUNCE_LIQUID_UK.toString(), NonSI.OUNCE_LIQUID_UK);
//	// UnitMap.put(NonSI.OUNCE_LIQUID_US.toString(), NonSI.OUNCE_LIQUID_US);
//	// UnitMap.put(NonSI.PARSEC.toString(), NonSI.PARSEC);
//	// UnitMap.put(NonSI.PERCENT.toString(), NonSI.PERCENT);
//	// UnitMap.put(NonSI.PIXEL.toString(), NonSI.PIXEL);
//	// UnitMap.put(NonSI.POINT.toString(), NonSI.POINT);
//	// UnitMap.put(NonSI.POISE.toString(), NonSI.POISE);
//	// UnitMap.put(NonSI.POUND.toString(), NonSI.POUND);
//	// UnitMap.put(NonSI.POUND_FORCE.toString(), NonSI.POUND_FORCE);
//	// UnitMap.put(NonSI.RAD.toString(), NonSI.RAD);
//	// UnitMap.put(NonSI.RANKINE.toString(), NonSI.RANKINE);
//	// UnitMap.put(NonSI.REM.toString(), NonSI.REM);
//	// UnitMap.put(NonSI.REVOLUTION.toString(), NonSI.REVOLUTION);
//	// UnitMap.put(NonSI.ROENTGEN.toString(), NonSI.ROENTGEN);
//	// UnitMap.put(NonSI.RUTHERFORD.toString(), NonSI.RUTHERFORD);
//	// UnitMap.put(NonSI.SECOND_ANGLE.toString(), NonSI.SECOND_ANGLE);
//	// UnitMap.put(NonSI.SPHERE.toString(), NonSI.SPHERE);
//	// UnitMap.put(NonSI.STOKE.toString(), NonSI.STOKE);
//	// UnitMap.put(NonSI.TON_UK.toString(), NonSI.TON_UK);
//	// UnitMap.put(NonSI.TON_US.toString(), NonSI.TON_US);
//	// UnitMap.put(NonSI.WEEK.toString(), NonSI.WEEK);
//	// UnitMap.put(NonSI.YARD.toString(), NonSI.YARD);
//	// UnitMap.put(NonSI.YEAR.toString(), NonSI.YEAR);
//	// UnitMap.put(NonSI.YEAR_CALENDAR.toString(), NonSI.YEAR_CALENDAR);
//	// UnitMap.put(NonSI.YEAR_SIDEREAL.toString(), NonSI.YEAR_SIDEREAL);
//	// }
//
//	private static String superscript(String str)
//	{
//		str = str.replaceAll("0", "⁰");
//		str = str.replaceAll("1", "¹");
//		str = str.replaceAll("2", "²");
//		str = str.replaceAll("3", "³");
//		str = str.replaceAll("4", "⁴");
//		str = str.replaceAll("5", "⁵");
//		str = str.replaceAll("6", "⁶");
//		str = str.replaceAll("7", "⁷");
//		str = str.replaceAll("8", "⁸");
//		str = str.replaceAll("9", "⁹");
//		return str;
//	}
//
//	// private static String subscript(String str)
//	// {
//	// str = str.replaceAll("0", "₀");
//	// str = str.replaceAll("1", "₁");
//	// str = str.replaceAll("2", "₂");
//	// str = str.replaceAll("3", "₃");
//	// str = str.replaceAll("4", "₄");
//	// str = str.replaceAll("5", "₅");
//	// str = str.replaceAll("6", "₆");
//	// str = str.replaceAll("7", "₇");
//	// str = str.replaceAll("8", "₈");
//	// str = str.replaceAll("9", "₉");
//	// return str;
//	// }
// }

// package org.molgenis.data.mapper.algorithmgenerator.generator;
//
// import java.util.Arrays;
// import java.util.List;
//
// import org.molgenis.data.AttributeMetaData;
// import org.molgenis.data.EntityMetaData;
//
// public class MixedTypeAlgorithmGenerator implements AlgorithmGenerator
// {
// @Override
// public String generate(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes,
// EntityMetaData targetEntityMetaData, EntityMetaData sourceEntityMetaData)
// {
//
// StringBuilder stringBuilder = new StringBuilder();
//
// if (sourceAttributes.size() == 1)
// {
// stringBuilder.append(String.format("$('%s').value();", sourceAttributes.get(0).getName()));
// }
// else if (sourceAttributes.size() > 1)
// {
// for (AttributeMetaData sourceAttribute : sourceAttributes)
// {
// stringBuilder.append(generate(targetAttribute, Arrays.asList(sourceAttribute), targetEntityMetaData,
// sourceEntityMetaData));
// }
// }
//
// return stringBuilder.toString();
//
// return null;
// }
//
// @Override
// public boolean isSuitable(AttributeMetaData targetAttribute, List<AttributeMetaData> sourceAttributes)
// {
// return true;
// }
// }

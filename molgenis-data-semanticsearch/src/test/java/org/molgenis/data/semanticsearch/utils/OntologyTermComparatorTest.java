package org.molgenis.data.semanticsearch.utils;
// package org.molgenis.data.semanticsearch.string;
//
// import static org.testng.Assert.assertEquals;
// import static org.testng.Assert.assertFalse;
// import static org.testng.Assert.assertTrue;
//
// import java.util.Arrays;
// import java.util.List;
//
// import org.molgenis.data.semanticsearch.semantic.Hit;
// import org.molgenis.data.semanticsearch.service.bean.OntologyTermHit;
// import org.molgenis.ontology.core.model.OntologyTerm;
// import org.testng.annotations.BeforeMethod;
// import org.testng.annotations.Test;
//
// public class OntologyTermComparatorTest
// {
// private OntologyTermComparator ontologyTermComparator = new OntologyTermComparator();
//
// private Hit<OntologyTermHit> hit1;
//
// private Hit<OntologyTermHit> hit2;
//
// private Hit<OntologyTermHit> hit3;
//
// private Hit<OntologyTermHit> hit4;
//
// @BeforeMethod
// public void setup()
// {
// OntologyTerm ontologyTerm1 = OntologyTerm.create("iri1", "centimeter", null,
// Arrays.asList("centimeter", "centimeter", "centimeter", "centimeter", "Centimeter", "Centimeter",
// "Centimeter", "Centimeter", "centimetre", "centimeters", "cm", "cm", "cm", "cm",
// "centimeter (cm)"));
// OntologyTerm ontologyTerm2 = OntologyTerm.create("iri2", "centers for medicare and medicaid services", null,
// Arrays.asList("cms", "Centers for Medicare and Medicaid Services",
// "Centers for Medicare and Medicaid Services", "Centers for Medicare and Medicaid Services",
// "center medicaid medicare services", "centers medicaid medicare services",
// "Health Care Financing Administration", "Health Care Financing Administration (U.S.)",
// "United States Health Care Financing Administration", "CMS",
// "Centers for Medicare and Medicaid Services (U.S.)",
// "United States Centers for Medicare and Medicaid Services",
// "united states centers for medicare and medicaid services",
// "UNITED STATES HEALTH CARE FINANCING ADMIN", "HCFA", "HEALTH CARE FINANCING ADMIN",
// "CENTERS FOR MEDICARE MEDICAID SERV", "UNITED STATES CENTERS FOR MEDICARE MEDICAID SERV"));
//
// OntologyTerm ontologyTerm3 = OntologyTerm.create("iri3", "length of trial", null,
// Arrays.asList("trial length", "length", "length of trial"));
//
// OntologyTerm ontologyTerm4 = OntologyTerm.create("iri4", "Length", null,
// Arrays.asList("length", "lengths", "len"));
//
// hit1 = Hit.<OntologyTermHit> create(OntologyTermHit.create(ontologyTerm1, "cm"), (float) 0.45);
// hit2 = Hit.<OntologyTermHit> create(OntologyTermHit.create(ontologyTerm2, "cms"), (float) 0.45);
// hit3 = Hit.<OntologyTermHit> create(OntologyTermHit.create(ontologyTerm3, "length"), (float) 0.55);
// hit4 = Hit.<OntologyTermHit> create(OntologyTermHit.create(ontologyTerm4, "length"), (float) 0.55);
// }
//
// @Test
// public void integrationTest()
// {
// List<Hit<OntologyTermHit>> hits = Arrays.asList(hit1, hit2, hit3, hit4);
// hits.sort(new OntologyTermComparator());
// assertEquals(hits, Arrays.asList(hit4, hit3, hit1, hit2));
// }
//
// @Test
// public void calculateInformationContent()
// {
// float calculateInformationContent1 = ontologyTermComparator.calculateInformationContent("cm",
// Arrays.asList("centimeter", "centimeter", "centimeter", "centimeter", "Centimeter", "Centimeter",
// "Centimeter", "Centimeter", "centimetre", "centimeters", "cm", "cm", "cm", "cm",
// "centimeter (cm)"));
//
// assertEquals(calculateInformationContent1, (float) 0.08064516);
//
// float calculateInformationContent2 = ontologyTermComparator.calculateInformationContent("cm",
// Arrays.asList("Centers for Medicare and Medicaid Services",
// "Centers for Medicare and Medicaid Services", "Centers for Medicare and Medicaid Services",
// "center medicaid medicare services", "centers medicaid medicare services",
// "Health Care Financing Administration", "Health Care Financing Administration (U.S.)",
// "United States Health Care Financing Administration", "CMS",
// "Centers for Medicare and Medicaid Services (U.S.)",
// "United States Centers for Medicare and Medicaid Services",
// "united states centers for medicare and medicaid services",
// "UNITED STATES HEALTH CARE FINANCING ADMIN", "HCFA", "HEALTH CARE FINANCING ADMIN",
// "CENTERS FOR MEDICARE MEDICAID SERV", "UNITED STATES CENTERS FOR MEDICARE MEDICAID SERV"));
//
// assertEquals(calculateInformationContent2, (float) 0.003125);
// }
//
// @Test
// public void compare()
// {
// assertTrue(ontologyTermComparator.compare(hit1, hit2) < 0);
// assertTrue(ontologyTermComparator.compare(hit4, hit3) < 0);
// }
//
// @Test
// public void isOntologyTermNameMatched()
// {
// assertFalse(ontologyTermComparator.isOntologyTermNameMatched(hit1));
// assertFalse(ontologyTermComparator.isOntologyTermNameMatched(hit2));
// assertFalse(ontologyTermComparator.isOntologyTermNameMatched(hit3));
// assertTrue(ontologyTermComparator.isOntologyTermNameMatched(hit4));
// }
//
// @Test
// public void synonymEquals()
// {
// assertTrue(ontologyTermComparator.synonymEquals("cm", "cms"));
// assertTrue(ontologyTermComparator.synonymEquals("smoke", "smoking"));
// assertFalse(ontologyTermComparator.synonymEquals("hypertension", "smoking"));
// }
// }

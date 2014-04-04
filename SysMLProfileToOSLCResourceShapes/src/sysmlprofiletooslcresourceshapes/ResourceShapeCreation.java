/*
 * The MIT License (MIT)

Copyright (c) 2013 Axel Reichwein

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * 
 * 
 * */

package sysmlprofiletooslcresourceshapes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFWriter;

public class ResourceShapeCreation {

	static String omgSysMLNamespaceURI = "http://omg.org/sysml/1.3/";
	static String omgSysMLNamespacePrefix = "sysml";
	static String sysmlEcoreFileLocation = "Original XMI/sysMl.ecore";
	static String umlEcoreFileLocation = "Original XMI/uml.ecore";
	static String reusedUMLConceptsFileLocation = "Reused UML Concepts/UML4SysML.txt";
	static EPackage sysmlPackage;
	static EPackage umlPackage;
	static EPackage primitiveSysMLValueTypesPackage;
	static Set<String> metaClasses = new HashSet<String>();
	static Set<String> metaPropertyURIs = new HashSet<String>();
	static StringBuffer rdfVocabularyBuffer = new StringBuffer();

	public static void main(String[] args) {
		loadSysMLProfileAndUMLMetaModel();		
		prepareRDFVocabularyFile();
		convertSysMLMetamodelIntoRDFandOSLCResources();
		closeRDFVocabularyFile();
		
		System.out.println("Created " + metaClasses.size() + " OSLC Resource Shapes");
	}

	private static void closeRDFVocabularyFile() {
		rdfVocabularyBuffer.append("</rdf:RDF>");
		FileWriter rdfsClassFileWriter;
		try {
			rdfsClassFileWriter = new FileWriter("RDF Vocabulary/"
					+ omgSysMLNamespacePrefix + "RDFVocabulary.rdf");
			rdfsClassFileWriter.append(rdfVocabularyBuffer);
			rdfsClassFileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	private static void convertSysMLMetamodelIntoRDFandOSLCResources() {
		// Convert SysML metaclasses and stereotypes into RDF vocabulary and
		// OSLC resource shapes
		ArrayList<EClassifier> sysmlConcepts = getSysMLConceptsToMap();
		mapConceptsToOSLCResourceShapes("SysML", sysmlConcepts);
		ArrayList<EClassifier> umlConcepts = getReusedUMLConceptsToMap();
		mapConceptsToOSLCResourceShapes("UML", umlConcepts);

	}

	private static void prepareRDFVocabularyFile() {
		// Create RDF Vocabulary
		rdfVocabularyBuffer
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		rdfVocabularyBuffer.append("\r\n");
		rdfVocabularyBuffer.append("<rdf:RDF");
		rdfVocabularyBuffer.append("\r\n");
		rdfVocabularyBuffer
				.append("\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
		rdfVocabularyBuffer.append("\r\n");
		rdfVocabularyBuffer
				.append("\txmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
		rdfVocabularyBuffer.append("\r\n");
		rdfVocabularyBuffer
				.append("\txmlns:dcterms=\"http://purl.org/dc/terms/\"");
		rdfVocabularyBuffer.append("\r\n");
		rdfVocabularyBuffer.append("\txmlns:" + omgSysMLNamespacePrefix + "=\""
				+ omgSysMLNamespaceURI + "\">");
		rdfVocabularyBuffer.append("\r\n");

	}

	private static void mapConceptsToOSLCResourceShapes(String prefix,
			ArrayList<EClassifier> eClassifiers) {
		for (EClassifier eClassifier : eClassifiers) {
			System.out.println("\t\t" + eClassifier.getName());

			if (metaClasses.contains(eClassifier.getName())) {
				System.err.println(eClassifier.getName() + " ALREADY DEFINED!");
				continue;
			} else {
				metaClasses.add(eClassifier.getName());
			}

			if (eClassifier instanceof EClass) {
				EClass sysmlClass = (EClass) eClassifier;

				// Create RDFS Class
				rdfVocabularyBuffer.append("\t<rdfs:Class");								
				rdfVocabularyBuffer.append(" rdf:about=\""
						+ omgSysMLNamespacePrefix + ":" + eClassifier.getName()
						+ "\">");
				rdfVocabularyBuffer.append("\r\n");
				
				// rdfs:label
				rdfVocabularyBuffer
						.append("\t\t<rdfs:label xml:lang=\"en-GB\">"
								+ eClassifier.getName() + "</rdfs:label>");
				rdfVocabularyBuffer.append("\r\n");
				
				// dcterms:description
				if(sysmlClass.getEAnnotations().size() > 0){
					for (EAnnotation eAnnotation : sysmlClass.getEAnnotations()) {
						if(eAnnotation.getSource().equals("http://www.eclipse.org/emf/2002/GenModel")){
							String documentation = eAnnotation.getDetails().get("documentation");
							// convert string into UTF8 encoding
							try {
								byte[] bytes = documentation.getBytes("ISO-8859-1");
								String documentationUTF8 = new String(bytes, "UTF-8"); 
								if(documentationUTF8 != null){
									rdfVocabularyBuffer
									.append("\t\t<dcterms:description xml:lang=\"en-GB\">"
											+ documentationUTF8 + "</dcterms:description>");
									rdfVocabularyBuffer.append("\r\n");	
								}	
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}												
							break;
						}
					}					
				}
				
				// rdfs:isDefinedBy
				rdfVocabularyBuffer
						.append("\t\t<rdfs:isDefinedBy rdf:resource=\""
								+ omgSysMLNamespaceURI + "\"/>");
				rdfVocabularyBuffer.append("\r\n");
				
				// dcterms:issued
				rdfVocabularyBuffer
						.append("\t\t<dcterms:issued>2014-01-05</dcterms:issued>");
				rdfVocabularyBuffer.append("\r\n");				
				
				// rdfs:subClassOf
				if(sysmlClass.getEGenericSuperTypes().size() > 0){
					for (EGenericType genericType : sysmlClass.getEGenericSuperTypes()) {
						rdfVocabularyBuffer
						.append("\t\t<rdfs:subClassOf rdf:resource=\""
								+ omgSysMLNamespacePrefix + ":" + genericType.getEClassifier().getName() + "\"/>");
						rdfVocabularyBuffer.append("\r\n");
					}					
				}				
				rdfVocabularyBuffer.append("\t</rdfs:Class>");
				rdfVocabularyBuffer.append("\r\n");

				
				
				// Create OSLC Resource Shape
				StringBuffer resourceShapeBuffer = new StringBuffer();
				resourceShapeBuffer
						.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer.append("<rdf:RDF");
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer
						.append("\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer
						.append("\txmlns:oslc=\"http://open-services.net/ns/core#\"");
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer
						.append("\txmlns:dcterms=\"http://purl.org/dc/terms/\"");
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer
						.append("\txmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer.append("\txmlns:" + omgSysMLNamespacePrefix
						+ "=\"" + omgSysMLNamespaceURI + "\">");

				// oslc:ResourceShape
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer
						.append("\t<oslc:ResourceShape rdf:about=\"http://myOSLCServiceProvider.com/sysml/"
								+ sysmlClass.getName() + "ResourceShape\">");

				// oslc:describes
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer
						.append("\t\t<oslc:describes rdf:resource=\""
								+ omgSysMLNamespacePrefix + ":"
								+ sysmlClass.getName() + "\"/>");

				// dcterms:title
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer
						.append("\t\t<dcterms:title rdf:datatype=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral\">"
								+ sysmlClass.getName()
								+ " Resource Shape</dcterms:title>");

				Set<EStructuralFeature> eStructuralFeatures = getAllEStructuralFeatures(
						sysmlClass, new LinkedHashSet<EStructuralFeature>());
				// Set<EEnum> enumerations = getAllEEnumerations(sysmlClass, new
				// LinkedHashSet<EEnum>());
				Set<EEnum> enumerations = new LinkedHashSet<EEnum>();
				for (EStructuralFeature eStructuralFeature : eStructuralFeatures) {					
					String propertyID = eStructuralFeature
							.getEContainingClass().getName()
							+ "_"
							+ eStructuralFeature.getName();					
					if (metaPropertyURIs.contains(propertyID)) {
						System.err.println(propertyID + " ALREADY DEFINED!");
						continue;
					} else {
						metaPropertyURIs.add(propertyID);
					}

					// Create RDF Property
					rdfVocabularyBuffer.append("\t<rdf:Property");
					rdfVocabularyBuffer.append(" rdf:about=\""
							+ omgSysMLNamespacePrefix + ":" + propertyID
							+ "\">");
					rdfVocabularyBuffer.append("\r\n");
					
					// rdfs:label
					rdfVocabularyBuffer
							.append("\t\t<rdfs:label xml:lang=\"en-GB\">"
									+ eStructuralFeature.getName()
									+ "</rdfs:label>");
					rdfVocabularyBuffer.append("\r\n");
					
					// dcterms:description
					if(eStructuralFeature.getEAnnotations().size() > 0){
						for (EAnnotation eAnnotation : eStructuralFeature.getEAnnotations()) {
							if(eAnnotation.getSource().equals("http://www.eclipse.org/emf/2002/GenModel")){
								String documentation = eAnnotation.getDetails().get("documentation");
								// convert string into UTF8 encoding
								try {
									byte[] bytes = documentation.getBytes("ISO-8859-1");
									String documentationUTF8 = new String(bytes, "UTF-8"); 
									if(documentationUTF8 != null){
										rdfVocabularyBuffer
										.append("\t\t<dcterms:description xml:lang=\"en-GB\">"
												+ documentationUTF8 + "</dcterms:description>");
										rdfVocabularyBuffer.append("\r\n");	
									}	
								} catch (UnsupportedEncodingException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}												
								break;
							}
						}					
					}
					
					// rdfs:isDefinedBy
					rdfVocabularyBuffer
							.append("\t\t<rdfs:isDefinedBy rdf:resource=\""
									+ omgSysMLNamespaceURI + "\"/>");
					rdfVocabularyBuffer.append("\r\n");
					
					// rdfs:subPropertyOf
					if(eStructuralFeature.getEAnnotations().size() > 0){
						for (EAnnotation eAnnotation : eStructuralFeature.getEAnnotations()) {
							if(eAnnotation.getSource().equals("subsets")){
								if(eAnnotation.getReferences().size() > 0){
									for (EObject eObject : eAnnotation.getReferences()) {
										if(eObject instanceof EStructuralFeature){
											EStructuralFeature eStructuralFeature2 = (EStructuralFeature) eObject;
											String propertyID2 = eStructuralFeature2
													.getEContainingClass().getName()
													+ "_"
													+ eStructuralFeature2.getName();
											rdfVocabularyBuffer
											.append("\t\t<rdfs:subPropertyOf rdf:resource=\""
													+ omgSysMLNamespacePrefix + ":" + propertyID2 + "\"/>");
											rdfVocabularyBuffer.append("\r\n");
											break;
										}
									}
								}									
								break;
							}
						}					
					}
					
					// dcterms:issued
					rdfVocabularyBuffer
							.append("\t\t<dcterms:issued>2014-01-05</dcterms:issued>");
					rdfVocabularyBuffer.append("\r\n");
					rdfVocabularyBuffer.append("\t</rdf:Property>");
					rdfVocabularyBuffer.append("\r\n");

				}
				
				for (EStructuralFeature eStructuralFeature : eStructuralFeatures) {				
					String propertyID = eStructuralFeature
							.getEContainingClass().getName()
							+ "_"
							+ eStructuralFeature.getName();
					if (eStructuralFeature.isDerived()) {
						continue;
					}
					
					// if (metaPropertyURIs.contains(propertyID)) {
					// // System.err.println(propertyID + " ALREADY DEFINED!");
					// continue;
					// }else{
					// metaPropertyURIs.add(propertyID);
					// }

					// oslc:property
					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer.append("\t\t<oslc:property>");

					// oslc:Property
					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer.append("\t\t\t<oslc:Property>");

					// oslc:name
					System.out.println("\t\t\t" + eStructuralFeature.getName());
					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer.append("\t\t\t\t<oslc:name>"
							+ eStructuralFeature.getName() + "</oslc:name>");

					// oslc:propertyDefinition
					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer
							.append("\t\t\t\t<oslc:propertyDefinition rdf:resource=\""
									+ omgSysMLNamespacePrefix
									+ ":"
									+ propertyID + "\"/>");

					// oslc:valueType or oslc:range
					System.out.println("\t\t\t\t"
							+ eStructuralFeature.getEType().getName());
					if (eStructuralFeature instanceof EAttribute) {
						resourceShapeBuffer.append("\r\n");
						if (eStructuralFeature.getEType().getName()
								.equals("String")) {
							resourceShapeBuffer
									.append("\t\t\t\t<oslc:valueType rdf:resource=\"http://www.w3.org/2001/XMLSchema#string\"/>");
						} else if (eStructuralFeature.getEType().getName()
								.equals("Integer")) {
							resourceShapeBuffer
									.append("\t\t\t\t<oslc:valueType rdf:resource=\"http://www.w3.org/2001/XMLSchema#integer\"/>");
						} else if (eStructuralFeature.getEType().getName()
								.equals("Boolean")) {
							resourceShapeBuffer
									.append("\t\t\t\t<oslc:valueType rdf:resource=\"http://www.w3.org/2001/XMLSchema#boolean\"/>");
						}

						if (eStructuralFeature.getEType() instanceof EEnum) {
							EEnum eEnum = (EEnum) eStructuralFeature.getEType();
							enumerations.add(eEnum);

							// oslc:allowedValues
							resourceShapeBuffer
									.append("\t\t\t\t<oslc:allowedValues rdf:resource=\""
											+ omgSysMLNamespacePrefix
											+ ":"
											+ eEnum.getName() + "\"/>");
						}
					} else if (eStructuralFeature instanceof EReference) {
						// oslc:range
						resourceShapeBuffer.append("\r\n");
						resourceShapeBuffer
								.append("\t\t\t\t<oslc:range rdf:resource=\""
										+ omgSysMLNamespacePrefix
										+ ":"
										+ eStructuralFeature.getEType()
												.getName() + "\"/>");

						// oslc:valueType
						resourceShapeBuffer.append("\r\n");
						resourceShapeBuffer
								.append("\t\t\t\t<oslc:valueType rdf:resource=\"http://open-services.net/ns/core#Resource\"/>");
					}

					// oslc:occurs
					System.out.println("\t\t\t\t"
							+ eStructuralFeature.getLowerBound());
					System.out.println("\t\t\t\t"
							+ eStructuralFeature.getUpperBound());
					resourceShapeBuffer.append("\r\n");
					int lowerBound = eStructuralFeature.getLowerBound();
					int upperBound = eStructuralFeature.getUpperBound();
					// occurs EXACTLY ONE
					if (lowerBound == 1 & upperBound == 1) {
						resourceShapeBuffer
								.append("\t\t\t\t<oslc:occurs rdf:resource=\"http://open-service.net/ns/core#Exactly-one\"/>");
					}
					// zero-or-one
					else if (lowerBound == 0 & upperBound == 1) {
						resourceShapeBuffer
								.append("\t\t\t\t<oslc:occurs rdf:resource=\"http://open-services.net/ns/core#Zero-or-one\"/>");
					}
					// one or many
					else if (lowerBound == 1 & upperBound == -1) {
						resourceShapeBuffer
								.append("\t\t\t\t<oslc:occurs rdf:resource=\"http://open-services.net/ns/core#One-or-many\"/>");
					}
					// Zero-or-many
					else if (lowerBound == 0 & upperBound == -1) {
						resourceShapeBuffer
								.append("\t\t\t\t<oslc:occurs rdf:resource=\"http://open-service.net/ns/core#Zero-or-many\"/>");
					} else {
						resourceShapeBuffer
								.append("\t\t\t\t<oslc:occurs rdf:resource=\"http://open-service.net/ns/core#Zero-or-many\"/>");
					}

					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer.append("\t\t\t</oslc:Property>");

					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer.append("\t\t</oslc:property>");
				}
				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer.append("\t</oslc:ResourceShape>");

				// enumerations
				for (EEnum enumration : enumerations) {
					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer
							.append("\t<oslc:AllowedValues rdf:about=\""
									+ omgSysMLNamespacePrefix + ":"
									+ enumration.getName() + "\">");

					// dcterms:title
					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer
							.append("\t\t<dcterms:title rdf:parseType=\"Literal\">"
									+ enumration.getName() + "</dcterms:title>");

					for (EEnumLiteral eEnumLiteral : enumration.getELiterals()) {
						resourceShapeBuffer.append("\r\n");
						resourceShapeBuffer.append("\t\t<oslc:allowedValue>"
								+ eEnumLiteral.getLiteral()
								+ "</oslc:allowedValue>");
					}

					resourceShapeBuffer.append("\r\n");
					resourceShapeBuffer.append("\t</oslc:AllowedValues>");

				}

				resourceShapeBuffer.append("\r\n");
				resourceShapeBuffer.append("</rdf:RDF>");

				FileWriter resourceShapeFileWriter;
				try {
					resourceShapeFileWriter = new FileWriter("Resource Shapes/"
							+ prefix + sysmlClass.getName() + ".rdf");
					resourceShapeFileWriter.append(resourceShapeBuffer);
					resourceShapeFileWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	private static void loadSysMLProfileAndUMLMetaModel() {
		// load uml.ecore model and get list of all non-abstract uml metaclasses
		Resource umlEcoreResource = loadEcoreModel(URI.createFileURI(new File(
				umlEcoreFileLocation).getAbsolutePath()));
		umlPackage = (EPackage) EcoreUtil.getObjectByType(
				umlEcoreResource.getContents(),
				EcorePackage.eINSTANCE.getEPackage());

		// load sysml.ecore model
		Resource sysmlEcoreResource = loadEcoreModel(URI
				.createFileURI(new File(sysmlEcoreFileLocation)
						.getAbsolutePath()));
		sysmlPackage = (EPackage) EcoreUtil.getObjectByType(
				sysmlEcoreResource.getContents(),
				EcorePackage.eINSTANCE.getEPackage());
		System.out.println(sysmlPackage.getName());

		// load sysml.ecore model and get package containing primitive types
		for (EPackage ePackage : sysmlPackage.getESubpackages()) {
			if (ePackage.getName().equals("libraries")) {
				for (EPackage ePackage2 : ePackage.getESubpackages()) {
					if (ePackage2.getName().equals("primitiveValueTypes")) {
						primitiveSysMLValueTypesPackage = ePackage2;
						break;
					}
				}
			}
		}
		ArrayList<EDataType> umlEDataTypes = new ArrayList<EDataType>();
		for (EClassifier eClassifier : umlPackage.getEClassifiers()) {
			if (eClassifier instanceof EDataType) {
				EDataType eDataType = (EDataType) eClassifier;
				umlEDataTypes.add(eDataType);
			}
		}
	}

	static private ArrayList<EClassifier> getSysMLConceptsToMap() {
		ArrayList<EClassifier> sysmlConceptsToMap = new ArrayList<EClassifier>();
		for (EPackage nestedPackage : sysmlPackage.getESubpackages()) {
			System.out.println("\t" + nestedPackage.getName());
			for (EClassifier eClassifier : nestedPackage.getEClassifiers()) {
				sysmlConceptsToMap.add(eClassifier);
			}
		}
		return sysmlConceptsToMap;
	}

	private static Set<EEnum> getAllEEnumerations(EClass sysmlClass,
			LinkedHashSet<EEnum> linkedHashSet) {
		for (EAttribute eAttribute : sysmlClass.getEAllAttributes()) {
			if (eAttribute.getEType() instanceof EEnum) {
				EEnum eEnum = (EEnum) eAttribute.getEType();
				linkedHashSet.add(eEnum);
			}

		}
		for (EReference eReference : sysmlClass.getEAllReferences()) {
			// System.out.println("\t\t\t" + eReference.getName());
			if (eReference.getName().startsWith("base")) {
				// System.out
				// .println("\t\t\t\t" + eReference.getEType().getName());
				EClassifier umlClassifier = eReference.getEType();
				if (umlClassifier instanceof EClass) {
					EClass umlType = (EClass) umlClassifier;

					for (EAttribute umlAttribute : umlType.getEAllAttributes()) {
						if (umlAttribute.getName().equals("eAnnotations")) {
							continue;
						}
						if (umlAttribute.getEType() instanceof EEnum) {
							EEnum eEnum = (EEnum) umlAttribute.getEType();
							linkedHashSet.add(eEnum);
						}
					}
					for (EClass eSuperType : umlType.getESuperTypes()) {
						getAllEEnumerations(eSuperType, linkedHashSet);
					}
				}

			}
		}
		return linkedHashSet;
	}

	private static LinkedHashSet<EStructuralFeature> getAllEStructuralFeatures(
			EClass eClass, LinkedHashSet<EStructuralFeature> eStructuralFeatures) {
		for (EAttribute eAttribute : eClass.getEAllAttributes()) {
			// System.out.println("\t\t\t" + eAttribute.getName());
			eStructuralFeatures.add(eAttribute);
		}
		for (EReference eReference : eClass.getEAllReferences()) {
			// System.out.println("\t\t\t" + eReference.getName());
			if (eReference.getName().startsWith("base")) {
				// System.out
				// .println("\t\t\t\t" + eReference.getEType().getName());
				EClassifier umlClassifier = eReference.getEType();
				if (umlClassifier instanceof EClass) {
					EClass umlType = (EClass) umlClassifier;
					for (EReference umlReference : umlType.getEAllReferences()) {
						if (umlReference.getName().equals("eAnnotations")) {
							continue;
						}
						// System.out.println("\t\t\t\t\t"
						// + umlReference.getName());
						eStructuralFeatures.add(umlReference);
					}
					for (EAttribute umlAttribute : umlType.getEAllAttributes()) {
						if (umlAttribute.getName().equals("eAnnotations")) {
							continue;
						}
						// System.out.println("\t\t\t\t\t"
						// + umlAttribute.getName());
						eStructuralFeatures.add(umlAttribute);
					}
					for (EClass eSuperType : umlType.getESuperTypes()) {
						getAllEStructuralFeatures(eSuperType,
								eStructuralFeatures);
					}
				}

			}
		}
		return eStructuralFeatures;
	}

	private static Resource loadEcoreModel(URI fileURI) {
		// Create a resource set.
		ResourceSet resourceSet = new ResourceSetImpl();

		// Register the default resource factory -- only needed for stand-alone!
		resourceSet
				.getResourceFactoryRegistry()
				.getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION,
						new XMIResourceFactoryImpl());

		// Register the package -- only needed for stand-alone!
		EcorePackage ecorePackage = EcorePackage.eINSTANCE;

		// Demand load the resource for this file.
		Resource resource = resourceSet.getResource(fileURI, true);
		return resource;
	}

	public static ArrayList<EClassifier> getReusedUMLConceptsToMap() {
		ArrayList<EClassifier> umlEClassifiersToMap = new ArrayList<EClassifier>();
		// process file containing reused UML concepts
		File file = new File(reusedUMLConceptsFileLocation);
		int umlMetaClassesCount = 0;
		int sysmlPrimitiveValueTypesCount = 0;
		int umlPrimitiveTypesCount = 0;
		int nonUmlMetaClassesCount = 0;
		int sysmlMetaClassesCount = 0;
		int totalMetaClassesCount = 0;
		try {
			String content = new Scanner(file).useDelimiter("\\Z").next();
			String[] metaclasses = content.split(",");
			for (String metaclass : metaclasses) {
				metaclass = metaclass.trim();
				String[] metaclassSegments = metaclass.split("::");
				if (metaclassSegments.length == 2) {
					if (metaclassSegments[0].equals("UML")) {
						boolean isUmlMetaclassFound = false;
						String umlMetaclass = metaclassSegments[1];
						for (EClassifier eClassifier : umlPackage
								.getEClassifiers()) {
							if (eClassifier instanceof EClass) {
								if (eClassifier.getName().equals(umlMetaclass)) {
									umlMetaClassesCount++;
									isUmlMetaclassFound = true;
									EClass eClass = (EClass) eClassifier;
									// if (eClass.isAbstract()) {
									umlEClassifiersToMap.add(eClassifier);
									// }
									break;
								}
							} else if (eClassifier instanceof EEnum) {
								EEnum eEnum = (EEnum) eClassifier;
								if (eClassifier.getName().equals(umlMetaclass)) {
									umlMetaClassesCount++;
									isUmlMetaclassFound = true;
									break;
								}
							}
						}
						if (!isUmlMetaclassFound) {
							System.out.println("uml metaclass not found: "
									+ umlMetaclass);
						}
					} else if (metaclassSegments[0]
							.equals("PrimitiveValueTypes")) {
						String umlPrimitiveType = metaclassSegments[1];
						boolean isSysMLPrimitiveTypeFound = false;
						for (EClassifier eClassifier : primitiveSysMLValueTypesPackage
								.getEClassifiers()) {
							if (eClassifier.getName().equals(umlPrimitiveType)) {
								// System.out.println(umlMetaclass);
								sysmlPrimitiveValueTypesCount++;
								isSysMLPrimitiveTypeFound = true;
								break;
							}
						}
						if (!isSysMLPrimitiveTypeFound) {
							System.out.println("primitive type not found: "
									+ umlPrimitiveType);
						}
					} else if (metaclassSegments[0].equals("PrimitiveTypes")) {
						String umlPrimitiveType = metaclassSegments[1];
						boolean isumlPrimitiveTypeFound = false;
						for (EClassifier eClassifier : primitiveSysMLValueTypesPackage
								.getEClassifiers()) {
							if (eClassifier.getName().equals(umlPrimitiveType)) {
								// System.out.println(umlMetaclass);
								umlPrimitiveTypesCount++;
								isumlPrimitiveTypeFound = true;
								break;
							}
						}
						if (!isumlPrimitiveTypeFound) {
							System.out.println("UML Primitive Type not found: "
									+ umlPrimitiveType);
						}
					} else {
						System.out.println("non UML concept: " + metaclass);
						nonUmlMetaClassesCount++;
					}
				} else {
					if (metaclassSegments[0].equals("SysML")) {
						sysmlMetaClassesCount++;
					} else {
						System.out.println("non UML concept: " + metaclass);
						nonUmlMetaClassesCount++;
					}
				}
				totalMetaClassesCount++;
			}
			System.out
					.println("QUICK CHECK TO SEE THAT ALL CONCEPTS HAVE BEEN PROCESSED");
			System.out.println("UML Metaclasses count: " + umlMetaClassesCount);
			System.out.println("UML Primitive Types count: "
					+ umlPrimitiveTypesCount);
			System.out.println(" SysML Primitive ValueTypes count: "
					+ sysmlPrimitiveValueTypesCount);
			System.out.println("SysML Metaclasses count: "
					+ sysmlMetaClassesCount);
			System.out.println("Unknown Concepts Count: "
					+ nonUmlMetaClassesCount);
			System.out
					.println("Total Concepts count: " + totalMetaClassesCount);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return umlEClassifiersToMap;

	}
}

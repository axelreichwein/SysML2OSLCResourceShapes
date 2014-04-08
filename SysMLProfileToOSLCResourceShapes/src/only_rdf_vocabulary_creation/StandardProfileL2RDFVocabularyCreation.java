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

package only_rdf_vocabulary_creation;

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
import java.util.List;
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

public class StandardProfileL2RDFVocabularyCreation {

	static String omgStandardProfileL2NamespaceURI = "http://omg.org/UML/StandardProfileL2/2.4.1/";
	static String omgStandardProfileL2NamespacePrefix = "standardprofilel2";
	static String standard_profile_l2FileLocation = "Original XMI/l2.ecore";
	static EPackage l2Package;
	static Set<String> metaClasses = new HashSet<String>();
	static Set<String> metaProperties = new HashSet<String>();
	static StringBuffer rdfVocabularyBuffer = new StringBuffer();
	static String date = "2014-April-04";

	public static void main(String[] args) {
		loadStandardProfileL2();		
		prepareRDFVocabularyFile();
		convertStandardProfilel2IntoRDFVocabulary();
		closeRDFVocabularyFile();		
		System.out.println("Created " + metaClasses.size() + " RDF resource types");
		System.out.println("Created " + metaProperties.size() + " RDF resource properties");
	}

	private static void closeRDFVocabularyFile() {
		rdfVocabularyBuffer.append("</rdf:RDF>");
		FileWriter rdfsClassFileWriter;
		try {
			rdfsClassFileWriter = new FileWriter("RDF Vocabularies/"
					+ omgStandardProfileL2NamespacePrefix + "RDFVocabulary.rdf");
			rdfsClassFileWriter.append(rdfVocabularyBuffer);
			rdfsClassFileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	private static void convertStandardProfilel2IntoRDFVocabulary() {
		// Convert SysML metaclasses and stereotypes into RDF vocabulary 
		ArrayList<EClassifier> standardProfileL2Concepts = getStandardProfileL2ConceptsToMap();
		mapConceptsToRDFVocabulary(standardProfileL2Concepts);	
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
		rdfVocabularyBuffer.append("\txmlns:" + omgStandardProfileL2NamespacePrefix + "=\""
				+ omgStandardProfileL2NamespaceURI + "\">");
		rdfVocabularyBuffer.append("\r\n");

	}

	private static void mapConceptsToRDFVocabulary(ArrayList<EClassifier> eClassifiers) {
		for (EClassifier eClassifier : eClassifiers) {
			System.out.println("\t\t" + eClassifier.getName());

			if (metaClasses.contains(eClassifier.getName())) {
//				System.err.println(eClassifier.getName() + " ALREADY DEFINED!");
				continue;
			} else {
				metaClasses.add(eClassifier.getName());
			}

			if (eClassifier instanceof EClass) {
				EClass sysmlClass = (EClass) eClassifier;

				// Create RDFS Class
				rdfVocabularyBuffer.append("\t<rdfs:Class");								
				rdfVocabularyBuffer.append(" rdf:about=\""
						+ omgStandardProfileL2NamespacePrefix + ":" + eClassifier.getName()
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
								+ omgStandardProfileL2NamespaceURI + "\"/>");
				rdfVocabularyBuffer.append("\r\n");
				
				// dcterms:issued
				rdfVocabularyBuffer
						.append("\t\t<dcterms:issued>" + date + "</dcterms:issued>");
				rdfVocabularyBuffer.append("\r\n");				
				
				// rdfs:subClassOf
				if(sysmlClass.getEGenericSuperTypes().size() > 0){
					for (EGenericType genericType : sysmlClass.getEGenericSuperTypes()) {
						rdfVocabularyBuffer
						.append("\t\t<rdfs:subClassOf rdf:resource=\""
								+ omgStandardProfileL2NamespacePrefix + ":" + genericType.getEClassifier().getName() + "\"/>");
						rdfVocabularyBuffer.append("\r\n");
					}					
				}				
				rdfVocabularyBuffer.append("\t</rdfs:Class>");
				rdfVocabularyBuffer.append("\r\n");

				
//				Set<EStructuralFeature> eStructuralFeatures = getAllEStructuralFeatures(
//						sysmlClass, new LinkedHashSet<EStructuralFeature>());
				List<EStructuralFeature> eStructuralFeatures = sysmlClass.getEAllStructuralFeatures();								
				for (EStructuralFeature eStructuralFeature : eStructuralFeatures) {					
					if(eStructuralFeature.getName().startsWith("base")){
						// reference refers to referenced metaclass
						continue;
					}
					
					String propertyID = eStructuralFeature
							.getEContainingClass().getName()
							+ "_"
							+ eStructuralFeature.getName();					
					if (metaProperties.contains(propertyID)) {
//						System.err.println(propertyID + " ALREADY DEFINED!");
						continue;
					} else {
						metaProperties.add(propertyID);
					}

					// Create RDF Property
					rdfVocabularyBuffer.append("\t<rdf:Property");
					rdfVocabularyBuffer.append(" rdf:about=\""
							+ omgStandardProfileL2NamespacePrefix + ":" + propertyID
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
									+ omgStandardProfileL2NamespaceURI + "\"/>");
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
													+ omgStandardProfileL2NamespacePrefix + ":" + propertyID2 + "\"/>");
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
							.append("\t\t<dcterms:issued>" + date + "</dcterms:issued>");
					rdfVocabularyBuffer.append("\r\n");
					rdfVocabularyBuffer.append("\t</rdf:Property>");
					rdfVocabularyBuffer.append("\r\n");

				}
				
				// TODO: support for enumerations
				// Set<EEnum> enumerations = getAllEEnumerations(sysmlClass, new
				// LinkedHashSet<EEnum>());
//				Set<EEnum> enumerations = new LinkedHashSet<EEnum>();
			}
		}

	}

	private static void loadStandardProfileL2() {
		// load sysml.ecore model
		Resource sysmlEcoreResource = loadEcoreModel(URI
				.createFileURI(new File(standard_profile_l2FileLocation)
						.getAbsolutePath()));
		l2Package = (EPackage) EcoreUtil.getObjectByType(
				sysmlEcoreResource.getContents(),
				EcorePackage.eINSTANCE.getEPackage());
		System.out.println(l2Package.getName());
	}

	static private ArrayList<EClassifier> getStandardProfileL2ConceptsToMap() {
		ArrayList<EClassifier> conceptsToMap = new ArrayList<EClassifier>();
		conceptsToMap.addAll(l2Package.getEClassifiers());
		return conceptsToMap;
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

}

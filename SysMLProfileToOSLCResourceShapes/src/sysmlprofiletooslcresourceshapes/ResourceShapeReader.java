package sysmlprofiletooslcresourceshapes;

import java.io.InputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class ResourceShapeReader {

	public static void main(String[] args) {
		// create an empty RDF model
		Model model = ModelFactory.createDefaultModel();

		// use FileManager to read OSLC Resource Shape in RDF
		String inputFileName = "file:C:/Users/Axel/git/SysMLProfileToOSLCResourceShapes/SysMLProfileToOSLCResourceShapes/Resource Shapes/SysMLBlock.rdf";
		InputStream in = FileManager.get().open(inputFileName);
		if (in == null) {
			throw new IllegalArgumentException("File: " + inputFileName
					+ " not found");
		}

		// read the RDF/XML file
		model.read(in, null);

		// write it to standard out
		model.write(System.out, "RDF/XML-ABBREV");
		// model.write(System.out);

	}

}

package examples.StAX;

import java.io.FileReader;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.*;

//import java.io.FileNotFoundException;
//import java.util.Iterator;
//import javax.xml.namespace.QName;

/**
 * This is a simple parsing example that illustrates the XMLStreamReader class.
 *
 * @author Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 */
public class SplitXML {

	private static String outputDir ;
	private static String outputPre ;
	private static String outputSuf = ".xml";
	private static XMLOutputFactory xmlof = XMLOutputFactory.newInstance();

	private static String docVersion;
	private static String docEncoding;
	//private static boolean isDocStandalone;

	private static boolean startWriting = false; 
	private static String rootName;
	private static TreeMap<String, String> rootAtts = new TreeMap<String, String>();

	private static TreeMap<String, XMLStreamWriter> xmlSWMap = new TreeMap<String, XMLStreamWriter>();
	private static XMLStreamWriter xmlw;

	public static void main(String[] args) throws Exception {

		boolean isError = false;

		String inputDir = System.getProperty("inputDir");
		System.out.println("inputDir : " + inputDir);
		if (inputDir==null || inputDir.equalsIgnoreCase("") ) {
			inputDir = ".";
		}

		File f;
		f = new File(inputDir);
		if (!f.exists() | !f.isDirectory()) {
			System.out.println("Invalid " + inputDir +" -DinputDir=<Input directory name>, enter valid directory where the input XML file is located. ");
			isError = true;
		}

		String inputFile = System.getProperty("inputFile");
		System.out.println("inputFile : " + inputFile);
		if (inputFile==null ) {
			inputFile = "omnilog.xml";
		}

       	File inFilename = new File(inputDir + File.separator + inputFile);
		if (!inFilename.exists() | !inFilename.isFile()) {
			System.out.println("Invalid " + inputFile +" -DinputFile=<file name>, enter valid file name for the input XML file. ");
			isError = true;
		}

		outputPre = inputFile.substring(0, inputFile.length()-4 );

		outputDir = System.getProperty("outputDir");
		System.out.println("outputDir : " + outputDir);
		if (outputDir==null || outputDir.equalsIgnoreCase("") ) {
			outputDir = ".";
		}

		f = new File(outputDir);
		if (!f.exists() | !f.isDirectory()) {
			System.out.println("Invalid " + outputDir +" -DoutputDir=<batch directory name>, enter valid directory where the output files needs to be stored. ");
			isError = true;
		}

		if (isError) {
			printUsage();
			return;
		}

		processMain(inFilename);
	}

	private static void processMain(File inFilename) throws Exception {
		//
		// Get an input factory
		//
		XMLInputFactory xmlif = XMLInputFactory.newInstance();
		System.out.println("FACTORY: " + xmlif);
		//
		// Instantiate a reader
		//

		XMLStreamReader xmlr = xmlif.createXMLStreamReader(new FileReader(inFilename));
		//XMLStreamReader xmlr = xmlif.createXMLStreamReader(System.in);
		System.out.println("READER:  " + xmlr + "\n");
		//
		// Parse the XML
		//
		while (xmlr.hasNext()) {
			handleEvent(xmlr);
			xmlr.next();
		}
		//
		// Close the reader
		//
		xmlr.close();
   		System.out.println("Processing complete.." );
	}

	private static void printUsage() {
		System.out.println("Usage :  ");
		System.out.println(" $JAVADIR/java.exe " +
		                       " [-DinputDir=<Input directory name>] " +
		                       " [-DinputFile=<input file name>] " +
		                       " [-DoutputDir=<Output directory name>] " +
				               " -cp \"$CLASSDIR/*\" examples.StAX.SplitXML  ");

		System.out.println(" " );
		System.out.println(" where [] : optional parameters " );
		System.out.println("   when optional is not provided, the following is assumed " );
		System.out.println("   inputDir  = \".\" (current directory is assumed) " );
		System.out.println("   inputFile = \"omnilog.xml\"  " );
		System.out.println("   outputDir = \".\" (current directory is assumed) " );
	}

	private static void handleEvent(XMLStreamReader xmlr) throws Exception {
		switch (xmlr.getEventType()) {
			case XMLStreamConstants.START_DOCUMENT:
				handleSDEvent(xmlr);
				break;
			case XMLStreamConstants.START_ELEMENT:
				handleSEEvent(xmlr);
				break;
			case XMLStreamConstants.END_ELEMENT:
				handleEEEvent(xmlr);
				break;
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CHARACTERS:
				handleCHEvent(xmlr);
				break;
		}
	}

	private static void handleCHEvent(XMLStreamReader xmlr) throws Exception {
		if (startWriting) {
			xmlw.writeCharacters(xmlr.getText() );
		}
	}

	private static void handleEEEvent(XMLStreamReader xmlr) throws Exception {

		String[] tagName = getName(xmlr);
		if (tagName[0].equalsIgnoreCase("OmniLog")) {
			writeEndDocument();
		} else {
		    xmlw.writeEndElement();
		    //xmlw.writeCharacters("\n");
		}
	}

	private static void handleSDEvent(XMLStreamReader xmlr)  {

		docVersion = xmlr.getVersion();
		docEncoding = xmlr.getCharacterEncodingScheme();
		//boolean isDocStandalone = xmlr.isStandalone();
	}

	private static void handleSEEvent(XMLStreamReader xmlr) throws Exception {

		String[] tagName = getName(xmlr);
		if (tagName[0].equalsIgnoreCase("OmniLog")) {
			saveRootElement(xmlr);
			return;
		}

		if (tagName[0].equalsIgnoreCase("LgRec")) {
			String recId = getAttribute(xmlr,"RecId");
			setCurrentXMLwriter(recId);
		}

		copyElement(xmlr);
	}

	private static void saveRootElement(XMLStreamReader xmlr) throws Exception {
		//save root name
		rootName = xmlr.getLocalName();
		//copyNamespaces(xmlr);
		//save root attributes
		for (int i = 0; i < xmlr.getAttributeCount(); i++) {
			rootAtts.put(xmlr.getAttributeLocalName(i),xmlr.getAttributeValue(i));
		}
	}

	private static String getAttribute(XMLStreamReader xmlr, String attName) {
		String retval = "";
		for (int i = 0; i < xmlr.getAttributeCount() && retval.equals(""); i++) {
			String localName = xmlr.getAttributeLocalName(i);
			String value = xmlr.getAttributeValue(i);
			if (localName.equalsIgnoreCase(attName)) {
				retval = value;
			}
		}
		return retval;
	}

	private static void setCurrentXMLwriter(String recId) throws Exception {
		if(xmlSWMap.containsKey(recId) ) {
			xmlw = xmlSWMap.get(recId);
		} else {
			String outputFileName = outputDir + File.separator + outputPre + recId + outputSuf;
			System.out.println("  Started processing recId : " + recId + " Output File : " + outputFileName );
			XMLStreamWriter xmlwNew = xmlof.createXMLStreamWriter(new FileOutputStream (outputFileName), docEncoding);
			xmlw = xmlwNew;
			xmlSWMap.put(recId, xmlwNew);
			startWriting = true;

			writeStartDocument();
		}
	}

	private static void writeEndDocument() throws Exception {
       	for(Map.Entry<String, XMLStreamWriter> map1:xmlSWMap.entrySet()){
       		XMLStreamWriter xmlw = map1.getValue();

       		//System.out.println("   output file closed : " + xmlw. );

       		// write the root element end
       	    xmlw.writeEndElement();
            // End the XML document
            xmlw.writeEndDocument();
            // Close the XMLStreamWriter to free up resources
            xmlw.close();
       	}
	}

	private static void writeStartDocument() throws Exception {
		xmlw.writeStartDocument(docEncoding, docVersion);
	    xmlw.writeCharacters("\n");
		// now write root element
		writeRootElement();
	}

	private static void writeRootElement() throws Exception {
		xmlw.writeStartElement(rootName);
	    for(Map.Entry<String, String> rootAtt:rootAtts.entrySet()){
			xmlw.writeAttribute(rootAtt.getKey(),rootAtt.getValue());
		}
	    xmlw.writeCharacters("\n");
	}

	private static void copyElement(XMLStreamReader xmlr) throws Exception {
		//write element
		xmlw.writeStartElement(xmlr.getLocalName());
		//xmlw.writeStartElement(xmlr.getPrefix(), xmlr.getLocalName(), xmlr.getNamespaceURI());
		//write attributes
		for (int i = 0; i < xmlr.getAttributeCount(); i++) {
			xmlw.writeAttribute(xmlr.getAttributeLocalName(i),xmlr.getAttributeValue(i));
		}
	    //xmlw.writeCharacters("\n");
	}

	private static String[] getName(XMLStreamReader xmlr)  {
		String[] tagName = new String[3];
		if (xmlr.hasName()) {
			tagName[0] = xmlr.getLocalName();
			//tagName[1] = xmlr.getPrefix();
			//tagName[2] = xmlr.getNamespaceURI();
		}
		return tagName;
	}

}
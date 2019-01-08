package examples.StAX;

import java.io.FileReader;
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


	private static String inputDir = "C:/EclipseNeonWorkSpace/OmniXML/omni_data/";
	private static String inputFile = "omnilog20181212-135913.xml";

	private static String outputDir = "C:/EclipseNeonWorkSpace/OmniXML/omni_data/out/";
	private static String outputPre = "omnilog_";
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
		String inFilename = null;
		inFilename = inputDir + inputFile;
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
		/*
		int start = xmlr.getTextStart();
		int length = xmlr.getTextLength();
		xmlw.writeCharacters(new String(xmlr.getTextCharacters(), start, length));
		*/
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
		/*
		System.out.print("</");
		printName(xmlr);
		System.out.print(">");
		*/
	}

	private static void handleSDEvent(XMLStreamReader xmlr)  {

		docVersion = xmlr.getVersion();
		docEncoding = xmlr.getCharacterEncodingScheme();
		//boolean isDocStandalone = xmlr.isStandalone();

		/*
		System.out.print("<?xml");
		System.out.print(" version='" + xmlr.getVersion() + "'");
		System.out.print(" encoding='" + xmlr.getCharacterEncodingScheme() + "'");
		if (xmlr.isStandalone())
			System.out.print(" standalone='yes'");
		else
			System.out.print(" standalone='no'");
		System.out.print("?>");
		*/
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
		/*
		System.out.print("<");
		printName(xmlr);
		printNamespaces(xmlr);
		printAttributes(xmlr);
		System.out.print(">");
		*/
	}

	private static void saveRootElement(XMLStreamReader xmlr) throws Exception {
		saveRootName(xmlr);
		//copyNamespaces(xmlr);
		saveRootAttributes(xmlr);
	}

	private static void saveRootName(XMLStreamReader xmlr) throws Exception {
		rootName = xmlr.getLocalName();
		//xmlw.writeStartElement(xmlr.getPrefix(), xmlr.getLocalName(), xmlr.getNamespaceURI());
	}

	private static void saveRootAttributes(XMLStreamReader xmlr) throws Exception {
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
			String outputFileName = outputDir + outputPre + recId + outputSuf;
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
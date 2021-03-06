package examples.StAX;

import java.sql.Timestamp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 */
public class SplitXmlGZipStreamTry {

	private static String outputDir ;
	private static String outputPre ;
	private static String outputSuf = ".xml.gz";
	private static int  messageLim ;
	private static long recCount = 0;
	private static long lineCount = 0;

	private static XMLOutputFactory xmlof = XMLOutputFactory.newInstance();

	private static String docVersion;
	private static String docEncoding;
	//private static boolean isDocStandalone;

	private static boolean startWriting = false;
	private static String rootName;
	private static TreeMap<String, String> rootAtts = new TreeMap<String, String>();

	//Stores the current tag
	private static String currentTag;
	private static String currentValue;
	private static String RecId;
	private static String RecSubType;  //Some Record types needs to be split into
	//Stores attributes for the current tag
	private static TreeMap<String, String> attributeMap = new TreeMap<String, String>();
	private static Object[] obj;
	//Stores all the tag -
	//  String stores tag name
	//   and Object[0] stores Attribute map
	//   and Object[1] stores tag value
	// At a time this only stores one record
	//    At the beginning it gets initialized, at the end it gets written to each record type xml
	private static TreeMap<String, Object[]> ElementMap = new TreeMap<String, Object[]>();

	//This map stores for each record type, the xml writer object
	private static TreeMap<String, Object[]> xmlSWMap = new TreeMap<String, Object[]>();
	private static XMLStreamWriter xmlw;


	public static void main(String[] args)  {

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
			inputFile = "omnilog.xml.gz";
		}

       	File inFilename = new File(inputDir + File.separator + inputFile);
		if (!inFilename.exists() | !inFilename.isFile()) {
			System.out.println("Invalid " + inputFile +" -DinputFile=<file name>, enter valid file name for the input XML file. ");
			isError = true;
		}

		//outputPre = inputFile.substring(0, inputFile.length()-4 );
		int posDot = inputFile.indexOf(".");
		outputPre = inputFile.substring(0, posDot);
		outputSuf = inputFile.substring(posDot, inputFile.length());

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

		String msgLim = System.getProperty("messageLim");
		System.out.println("messageLim : " + msgLim);
		if (msgLim==null || msgLim.equalsIgnoreCase("") ) {
			messageLim = 1000000;
			//messageLim = 1000;
		} else {
			try {
			messageLim =  Integer.parseInt(msgLim);
			} catch ( NumberFormatException e) {
				System.out.println("Invalid Message limit value " + messageLim  +" -DmessageLim=<Message Limit>, Enter a valid integer value. ");
				isError = true;
			}
		}

		if (isError) {
			printUsage();
			return;
		}

		//return;
		try {
			processMain(inFilename);
		} catch (Exception e) {
			System.out.println("Processed Record count : " + recCount + " Line count : " + lineCount + " Time : " + (new Timestamp(System.currentTimeMillis())) );
			e.printStackTrace();
		}
	}

	private static void processMain(File inFilename) throws Exception {

		XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
		System.out.println("FACTORY: " + xmlInFactory);

		GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(inFilename));
		XMLStreamReader xmlReader = xmlInFactory.createXMLStreamReader(gzis);
		System.out.println("READER:  " + xmlReader + "\n");

		System.out.println("Started processing at Time : " + (new Timestamp(System.currentTimeMillis())) );
		while (xmlReader.hasNext()) {
			handleEvent(xmlReader);
			xmlReader.next();
		}
		System.out.println("Processed Record count : " + recCount + " Line count : " + lineCount + " Time : " + (new Timestamp(System.currentTimeMillis())) );

		xmlReader.close();
		gzis.close();
   		System.out.println("Processing complete.." );
	}

	private static void printUsage() {
		System.out.println("Usage :  ");
		System.out.println(" $JAVADIR/java.exe " +
		                       " [-DinputDir=<Input directory name>] " +
		                       " [-DinputFile=<input file name>] " +
		                       " [-DoutputDir=<Output directory name>] " +
		                       " [-DmessageLim=<Messagae Limit>] " +
				               " -cp \"$CLASSDIR/*\" examples.StAX.SplitXmlGZipStream  ");

		System.out.println(" " );
		System.out.println(" where [] : optional parameters " );
		System.out.println("   " );
		System.out.println("   when optional is not provided, the following is assumed " );
		System.out.println("   inputDir  = \".\" (current directory is assumed) " );
		System.out.println("   inputFile = \"omnilog.xml\"  " );
		System.out.println("   outputDir = \".\" (current directory is assumed) " );
		System.out.println("   messageLim = 100000  " );
		System.out.println("   " );
		System.out.println("   " );
	}

	private static void handleEvent(XMLStreamReader xmlReader) throws Exception {
		switch (xmlReader.getEventType()) {
			case XMLStreamConstants.START_DOCUMENT:
				handleSDEvent(xmlReader);
				break;
			case XMLStreamConstants.START_ELEMENT:
				handleSEEvent(xmlReader);
				break;
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CHARACTERS:
				handleCHEvent(xmlReader);
				break;
			case XMLStreamConstants.END_ELEMENT:
				handleEEEvent(xmlReader);
				break;
		}
	}

	//START_DOCUMENT
	private static void handleSDEvent(XMLStreamReader xmlReader)  {

		// start document store the Version and the Encoding
		docVersion = xmlReader.getVersion();
		docEncoding = xmlReader.getCharacterEncodingScheme();
		lineCount++;
		//boolean isDocStandalone = xmlReader.isStandalone();
	}

	//START_ELEMENT
	private static void handleSEEvent(XMLStreamReader xmlReader) throws Exception {

		String[] tagName = getName(xmlReader);
		currentTag = tagName[0];
		if (currentTag.equalsIgnoreCase("OmniLog")) {
			saveRootElement(xmlReader);
			return;
		}

		//All other tags save them
		saveRecord(xmlReader);

		/*
		if (tagName[0].equalsIgnoreCase("LgRec")) {
			recCount++;
			lineCount++;  //LgRec is usually 2 lines
			if ( (recCount % messageLim) == 0 ) {
				System.out.println("Processed Record count : " + recCount + " Line count : " + lineCount + " Time : " + (new Timestamp(System.currentTimeMillis())) );
			}
			String recId = getAttribute(xmlReader,"RecId");
			setCurrentXMLwriter(recId);
		}
		lineCount++;

		copyElement(xmlReader);
		*/
	}

	private static void saveRootElement(XMLStreamReader xmlReader) throws Exception {
		//save root name
		rootName = xmlReader.getLocalName();
		//copyNamespaces(xmlReader);
		//save root attributes
		for (int i = 0; i < xmlReader.getAttributeCount(); i++) {
			rootAtts.put(xmlReader.getAttributeLocalName(i),xmlReader.getAttributeValue(i));
		}
	}

	private static void saveRecord(XMLStreamReader xmlReader) throws Exception {

		if (currentTag.equalsIgnoreCase("LgRec")) {
			lineCount++;  //LgRec is usually 2 lines
			ElementMap = null; //LgRec is the root for each record, so initialize it when it is LgRec
		}
		lineCount++;

		attributeMap = null;
		obj = new Object[2];
		//save attributes
		for (int i = 0; i < xmlReader.getAttributeCount(); i++) {
			String attName = xmlReader.getAttributeLocalName(i);
			String attVal  = xmlReader.getAttributeValue(i);
			if (currentTag.equalsIgnoreCase("LgRec") &&
					attName.equalsIgnoreCase("RecId")) {
				RecId = attVal;
			}
			if (currentTag.equalsIgnoreCase("DE") &&
					attName.equalsIgnoreCase("RecId")) {
				RecId = attVal;
			}
			attributeMap.put(attName, attVal);
		}
		obj[0] = attributeMap;
		//save element
		ElementMap.put(currentTag, obj);
	}

	//SPACE CHARACTERS
	private static void handleCHEvent(XMLStreamReader xmlReader) throws Exception {
		/*
		if (startWriting) {
			xmlw.writeCharacters(xmlReader.getText() );
		}
		*/
		currentValue = xmlReader.getText();
		obj[1] = currentValue;
		ElementMap.put(currentTag, obj);
	}

	//END_ELEMENT
	private static void handleEEEvent(XMLStreamReader xmlReader) throws Exception {

		String[] tagName = getName(xmlReader);

		// count line only when not DE tag
		if (!tagName[0].equalsIgnoreCase("DE")) {
			lineCount++;
		}

		if (tagName[0].equalsIgnoreCase("OmniLog")) {
			writeEndDocument();
		} else if (tagName[0].equalsIgnoreCase("OmniLog"))  {
			writeRecord();
		    //xmlw.writeEndElement();
		}
	}

	private static void writeRecord() throws Exception {

		/*
		String[] tagName = getName(xmlReader);

		// count line only when not DE tag
		if (!tagName[0].equalsIgnoreCase("DE")) {
			lineCount++;
		}

		if (tagName[0].equalsIgnoreCase("OmniLog")) {
			writeEndDocument();
		} else if (tagName[0].equalsIgnoreCase("OmniLog"))  {
			writeRecord();
		    //xmlw.writeEndElement();
		}
		*/
	}

	private static void writeEndDocument() throws Exception {
       	for(Map.Entry<String, Object[]> map1:xmlSWMap.entrySet()){
    		Object[] objArr = new Object[3];
    		String key = map1.getKey();
    		objArr = map1.getValue();
       		XMLStreamWriter xmlw = (XMLStreamWriter) objArr[0];
       		GZIPOutputStream  gzos = (GZIPOutputStream) objArr[1];
       		int recTypeCount = (Integer) objArr[2];

       		System.out.println("   Done with Record type : " + key + " Records written : " + recTypeCount );

       		// write the root element end
       	    xmlw.writeEndElement();
            // End the XML document
            xmlw.writeEndDocument();
            // Close the XMLStreamWriter to free up resources
            xmlw.close();
            gzos.close();
       	}
	}

	private static String getAttribute(XMLStreamReader xmlReader, String attName) {
		String retval = "";
		for (int i = 0; i < xmlReader.getAttributeCount() && retval.equals(""); i++) {
			String localName = xmlReader.getAttributeLocalName(i);
			String value = xmlReader.getAttributeValue(i);
			if (localName.equalsIgnoreCase(attName)) {
				retval = value;
			}
		}
		return retval;
	}

	private static void setCurrentXMLwriter(String recId) throws Exception {
		Object[] objArr = new Object[3];
		Integer recTypeCount;
		if(xmlSWMap.containsKey(recId) ) {
			objArr = xmlSWMap.get(recId);
			xmlw = (XMLStreamWriter) objArr[0];
			objArr[2] = (Integer) objArr[2] + 1 ;
			xmlSWMap.put(recId, objArr);
		} else {
			String outputFileName = outputDir + File.separator + outputPre + "_" + recId + outputSuf;
			System.out.println("  Started processing recId : " + recId + " Output File : " + outputFileName );
			GZIPOutputStream  gzos = new GZIPOutputStream ( new FileOutputStream(outputFileName));
			XMLStreamWriter xmlwNew = xmlof.createXMLStreamWriter(gzos, docEncoding);
			//XMLStreamWriter xmlwNew = xmlof.createXMLStreamWriter(gzos, "us-ascii");
			objArr[0] = xmlwNew;
			objArr[1] = gzos;
			objArr[2] = new Integer(1);    //counter
			xmlw = xmlwNew;
			xmlSWMap.put(recId, objArr);
			//startWriting = true;

			writeStartDocument();
		}
	}

	private static void writeStartDocument() throws Exception {
		xmlw.writeStartDocument(docEncoding, docVersion);
		//xmlw.writeStartDocument("us-ascii", docVersion);
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

	private static void copyElement(XMLStreamReader xmlReader) throws Exception {
		//write element
		xmlw.writeStartElement(xmlReader.getLocalName());
		//xmlw.writeStartElement(xmlReader.getPrefix(), xmlReader.getLocalName(), xmlReader.getNamespaceURI());
		//write attributes
		for (int i = 0; i < xmlReader.getAttributeCount(); i++) {
			xmlw.writeAttribute(xmlReader.getAttributeLocalName(i),xmlReader.getAttributeValue(i));
		}
	    //xmlw.writeCharacters("\n");
	}

	private static String[] getName(XMLStreamReader xmlReader)  {
		String[] tagName = new String[3];
		if (xmlReader.hasName()) {
			tagName[0] = xmlReader.getLocalName();
			//tagName[1] = xmlReader.getPrefix();
			//tagName[2] = xmlReader.getNamespaceURI();
		}
		return tagName;
	}

}
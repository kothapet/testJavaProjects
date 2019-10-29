package examples.StAX;

import java.io.FileReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class XmlGZipStream2Csv {


	private static final String BACKSLASH = "\\";
	private static final String QUOTE = "\"";
	private static final String COMMA = ",";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static final String outputSuf = ".csv.gz";

	private static int messageLim ;
	private static int count = 0;
	private static int totCount = 0;
	private static boolean processField;


	// reset these for each logical record type
	private static TreeMap<String, String>  metaInfo ;      //for each element in meta, has field Names(headings) key would be num
	private static TreeMap<String, String>  emptyDataRec;   //for each element in meta, has empty field, key would be num
	private static TreeMap<String, Integer> missingElemRec ;
	private static GZIPOutputStream out;
	// reset these for each physical record type
	private static TreeMap<String, TreeMap<String, String>>  metaTab ;
	private static TreeMap<String, TreeMap<String, String>>  emptyDataTab ;
	private static TreeMap<String, TreeMap<String, Integer>> missingElemTab ;
	private static TreeMap<String, GZIPOutputStream>  outMap ;

	// reset these for each record
	private static TreeMap<String, String>  tempDataRec ;
	private static TreeMap<String, String>  realDataRec ;
	private static TreeMap<String, String>  deAttMap ; //has element attributes key/value pairs (meta and data)

	//private static boolean isCodeField;
	//private static boolean isFoundDefault;
	private static String recType;          //BABT - record type of the file
	private static String recId;            //BAUD - sub record type
	private static String prevRecId;            //BAUD - sub record type
	private static String currentKey;
	private static boolean isKeyFound = false;

	private static String inputDir;
	private static String inputPattern;
	private static String outputDir;
	private static String outputPrefix;
	private static String metaDir;


	public static void main(String[] args) throws Exception {

		boolean isError = false;

		inputDir = System.getProperty("inputDir");
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

		inputPattern = System.getProperty("inputPattern");
		System.out.println("inputPattern : " + inputPattern);
		if (inputPattern==null ) {
			inputPattern = "omnilog*.xml.gz";
		}

		outputDir = System.getProperty("outputDir");
		System.out.println("outputDir : " + outputDir);
		if (outputDir==null || outputDir.equalsIgnoreCase("") ) {
			outputDir = ".";
		}

		f = new File(outputDir);
		if (!f.exists() | !f.isDirectory()) {
			System.out.println("Invalid " + outputDir +" -DoutputDir=<output directory name>, enter valid directory where the output files needs to be stored. ");
			isError = true;
		}

		metaDir = System.getProperty("metaDir");
		System.out.println("metaDir : " + metaDir);
		if (metaDir==null || metaDir.equalsIgnoreCase("") ) {
			metaDir = ".";
		}

		f = new File(metaDir);
		if (!f.exists() | !f.isDirectory()) {
			System.out.println("Invalid " + metaDir +" -DmetaDir=<Metadata directory name>, enter valid directory where the output files needs to be stored. ");
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


		//inputDir = "C:/EclipseNeonWorkSpace/OmniXML/omni_data/out/";
		//inputPattern = "omnilog20181212*.xml";
		//outputDir = "C:/EclipseNeonWorkSpace/OmniXML/omni_data/out/csv/";
		//metaDir = "C:/EclipseNeonWorkSpace/OmniXML/omni_data/OmniXML/In/";
		processMain( );

	}

	private static void printUsage() {
		System.out.println("Usage :  ");
		System.out.println(" $JAVADIR/java.exe " +
		                       " [-DinputDir=<Input directory name>] " +
		                       " [-DinputPattern=<input file Pattern>] " +
		                       " [-DoutputDir=<Output directory name>] " +
		                       " [-DmetaDir=<MetaData directory name>] " +
		                       " [-DmessageLim=<Messagae Limit>] " +
				               " -cp \"$CLASSDIR/*\" examples.StAX.XmlGZipStream2Csv  ");

		System.out.println(" " );
		System.out.println(" where [] : optional parameters " );
		System.out.println("   " );
		System.out.println("   when optional is not provided, the following is assumed " );
		System.out.println("   inputDir   = \".\" (current directory is assumed) " );
		System.out.println("   inputFile  = \"omnilog.xml\"  " );
		System.out.println("   outputDir  = \".\" (current directory is assumed) " );
		System.out.println("   metaDir    = \".\" (current directory is assumed) " );
		System.out.println("   messageLim = 100000  " );
		System.out.println("   " );
		System.out.println("   " );
	}

	private static void processMain( ) {

		try {
			Path inputPath = Paths.get(inputDir);

			System.out.println("" );
			System.out.println("Started processing at Time : " + (new Timestamp(System.currentTimeMillis())) );
			DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, inputPattern);
		    for (Path entry: stream) {
		    	count = 0;
		    	missingElemTab = new TreeMap<String, TreeMap<String, Integer>>();
		    	metaTab = new TreeMap<String, TreeMap<String, String>>();
		    	emptyDataTab = new TreeMap<String, TreeMap<String, String>>();
				outMap = new TreeMap<String, GZIPOutputStream>();


		    	File inputFileName = entry.toFile();
		    	String inputFile = inputFileName.getName();
				//String outputFile = inputFile.substring(0, inputFile.length()-4) + outputSuf;
				int posDot = inputFile.indexOf(".");
		    	recType =  inputFile.substring(posDot-4, posDot);
		    	prevRecId = "";
				outputPrefix = inputFile.substring(0, posDot-4);

				processDataFile(inputFileName);

				for(Map.Entry<String, GZIPOutputStream> outEntry : outMap.entrySet()) {
					outEntry.getValue().close();
				}
		    }
			System.out.println("" );
			System.out.println("Processed Record Type count : " + count + "  Total count : " + totCount + " Time : " + (new Timestamp(System.currentTimeMillis())) );

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void processDataFile(File inputFileName) throws Exception {

		System.out.println("" );
		System.out.println("STARTED Record Type " + recType  + " at Time : " + (new Timestamp(System.currentTimeMillis())) );
		XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
		//System.out.println("FACTORY: " + xmlInFactory);

		GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(inputFileName));
		XMLStreamReader xmlReader = xmlInFactory.createXMLStreamReader(gzis);
		//System.out.println("READER:  " + xmlReader + "\n");

		//System.out.println("Started processing at Time : " + (new Timestamp(System.currentTimeMillis())) );
		while (xmlReader.hasNext()) {
			handleDataEvent(xmlReader);
			xmlReader.next();
		}

		String key1;
		int val1;
		for(Map.Entry<String, TreeMap<String, Integer>> tabEntry : missingElemTab.entrySet()) {
			String RecordId = tabEntry.getKey();
			missingElemRec = tabEntry.getValue();
			for(Map.Entry<String, Integer> entry : missingElemRec.entrySet()) {
				  key1 = entry.getKey();
				  val1 = entry.getValue();
				  System.out.println("   WARN :  key : " + key1 + " occured " + val1 + " times, doesn't exist in Record type : " + RecordId);
			}
		}

		System.out.println("COMPLETED Record Type " + recType + " Processed row count : " + count + "  Total count : " + totCount + " Time : " + (new Timestamp(System.currentTimeMillis())) );
		//
		// Close the reader
		//
		xmlReader.close();
   		//System.out.println("Processing complete.." );
	}

	//Process data
	private static void handleDataEvent(XMLStreamReader xmlReader) throws Exception {
		switch (xmlReader.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
				handleDataSEEvent(xmlReader);
				break;
			case XMLStreamConstants.END_ELEMENT:
				handleDataEEEvent(xmlReader);
				break;
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CHARACTERS:
				handleDataValEvent(xmlReader);
				break;
		}
	}

	private static void handleDataSEEvent(XMLStreamReader xmlReader) throws Exception {

		String[] tagName = getName(xmlReader);
	    //System.out.println("tagName : " + tagName[0] );

		if (tagName[0].equalsIgnoreCase("LgRec")) {
			processDataLgRec(xmlReader);
		} else if (tagName[0].equalsIgnoreCase("DE")) {
			processDataDE(xmlReader);
		}
	}

	//@SuppressWarnings("unchecked")
	private static void processDataLgRec(XMLStreamReader xmlReader) throws Exception {

		//tempDataRec = (TreeMap<String, String>) emptyDataRec.clone();
		tempDataRec = new TreeMap<String, String>();
    	//deAttMap = new TreeMap<String, String>();

		deAttMap = getAttributeMap(xmlReader);

		tempDataRec.put("#00", deAttMap.get("RecId") );
		tempDataRec.put("#01", deAttMap.get("Action") );
		tempDataRec.put("#02", deAttMap.get("Date") );
		tempDataRec.put("#03", deAttMap.get("Time") );
		//tempDataRec.put("#04", deAttMap.get("Seqn") );
	}

	private static void processDataDE(XMLStreamReader xmlReader) throws Exception {
		deAttMap = getAttributeMap(xmlReader);
		//currentKey = String.format("%03d", (Integer.parseInt(deAttMap.get("num")) + 3));
		currentKey = deAttMap.get("num");
		isKeyFound = true;
	    //System.out.println("currentKey : " + currentKey  );
	}

	private static void handleDataValEvent(XMLStreamReader xmlReader) throws Exception {
		if (isKeyFound) {
			String val = xmlReader.getText();
		    //System.out.println("currentKey : " + currentKey + " val : " + val  );
			tempDataRec.put(currentKey, val);
			isKeyFound = false;
		}
	}

	private static void handleDataEEEvent(XMLStreamReader xmlReader) throws Exception {

		String[] tagName = getName(xmlReader);
	    //System.out.println("tagName : " + tagName[0] );

		if (tagName[0].equalsIgnoreCase("LgRec")) {
			processDataLgRecEnd();
		} else if (tagName[0].equalsIgnoreCase("DE")) {
		}
	}

	private static void processDataLgRecEnd() throws Exception {

		String RecString = "";
		String key;

    	recId = recType;
		if (recType.equalsIgnoreCase("BABT")) {
		    String type = tempDataRec.get("008");
			if( type.startsWith("U") ) {
				recId = "BAUD";
			} else if (type.equalsIgnoreCase("LOG") ) {
				recId = "BALO";
			}
		}

		if(!recId.equalsIgnoreCase(prevRecId)) {
			if (!prevRecId.equalsIgnoreCase("")) {
				missingElemTab.put(prevRecId, missingElemRec);
			}
			if (metaTab.containsKey(recId) ) {
				metaInfo = metaTab.get(recId);
				emptyDataRec = emptyDataTab.get(recId);
				missingElemRec = missingElemTab.get(recId);
				out = outMap.get(recId);
			} else {
				processMetaFile(recId);
			}
			prevRecId=recId;
		}

		realDataRec = (TreeMap<String, String>) emptyDataRec.clone();
		for(Map.Entry<String, String> entry : tempDataRec.entrySet()) {
			realDataRec.put(entry.getKey(), entry.getValue() );
		}

		for(Map.Entry<String, String> entry : realDataRec.entrySet()) {
			  key = entry.getKey();
			  if (metaInfo.containsKey(key)) {
				  String value = entry.getValue().replace(QUOTE, BACKSLASH+QUOTE  ) ;
				  RecString = RecString + QUOTE + value + QUOTE + COMMA;
			  } else {
				  //String key1 = String.format("%03d", (Integer.parseInt(key) - 3));
				  String key1 = key;
				  int val1;
				  if (missingElemRec.containsKey(key1) ) {
					  val1 = missingElemRec.get(key1) + 1;
					  missingElemRec.put(key1, new Integer(val1));
				  } else {
					  missingElemRec.put(key1, new Integer(1));
				  }
				  //System.out.println("WARN :  key : " + key1 + " doesn't exist in Record type : " + tempDataRec.get("000"));
			  }
			  //System.out.println(key + " => " + value);
		}
		//System.out.println(RecString);
		out.write( (RecString + LINE_SEPARATOR).getBytes() );
		count++;
		totCount++;
		if ( (count % messageLim) == 0 ) {
			System.out.println("   Record Type " + recType + " Processed row count : " + count + "  Total count : " + totCount + " Time : " + (new Timestamp(System.currentTimeMillis())) );
		}
	}

	private static TreeMap<String, String> getAttributeMap(XMLStreamReader xmlReader) {
		TreeMap<String, String> retval = new TreeMap<String, String>();

		for (int i = 0; i < xmlReader.getAttributeCount(); i++) {
			retval.put(xmlReader.getAttributeLocalName(i), xmlReader.getAttributeValue(i) );
		}
		return retval;
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

	// Meta data processing

	private static void processMetaFile(String recId) throws Exception {

		String outputFile = outputPrefix + recId + outputSuf;
		File outputFileName = new File(outputDir + File.separator + outputFile);
		out =  new GZIPOutputStream ( new FileOutputStream(outputFileName));
		outMap.put(recId, out);

		String metaFile = recId + ".xml" ;
		//System.out.println("metaFile : " + metaFile);
		File metaFileName = new File(metaDir + File.separator + metaFile);
		//processMetaFile(metaFileName);
    	metaInfo = new TreeMap<String, String>();
    	emptyDataRec = new TreeMap<String, String>();
    	missingElemRec = new TreeMap<String, Integer>();

		metaInfo.put("#00", "RecId");
		emptyDataRec.put("#00", "");

		metaInfo.put("#01", "Action");
		emptyDataRec.put("#01", "");

		metaInfo.put("#02", "Date");
		emptyDataRec.put("#02", "");

		metaInfo.put("#03", "Time");
		emptyDataRec.put("#03", "");

		//metaInfo.put("#04", "Seqn");
		//emptyDataRec.put("#04", "");

		XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
		//System.out.println("FACTORY: " + xmlInFactory);

		XMLStreamReader xmlReader = xmlInFactory.createXMLStreamReader(new FileReader(metaFileName));
		//System.out.println("READER:  " + xmlReader + "\n");

		while (xmlReader.hasNext()) {
			handleMetaEvent(xmlReader);
			xmlReader.next();
		}

		//print heading at the end
		String Heading = "";
		for(Map.Entry<String, String> entry : metaInfo.entrySet()) {
			  String value = entry.getValue();
			  Heading = Heading + QUOTE + value + QUOTE + COMMA;
			  //System.out.println(key + " => " + value);
		}
		//System.out.println(Heading);
		out.write( (Heading + LINE_SEPARATOR).getBytes() );

		/*
		String EmptyString = "";
		for(Map.Entry<String, String> entry : emptyDataRec.entrySet()) {
			  String value = entry.getValue();
			  EmptyString = EmptyString + QUOTE + value + QUOTE + COMMA;
			  //System.out.println(key + " => " + value);
		}
		System.out.println(EmptyString);

		System.out.println("Processed event count : " + count + " Time : " + (new Timestamp(System.currentTimeMillis())) );
        */

		xmlReader.close();
   		//System.out.println("Processing complete.." );
	}

	private static void handleMetaEvent(XMLStreamReader xmlReader) throws Exception {
		switch (xmlReader.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
				handleMetaSEEvent(xmlReader);
				break;
			case XMLStreamConstants.END_ELEMENT:
				handleMetaEEEvent(xmlReader);
				break;
		}
	}

	private static void handleMetaSEEvent(XMLStreamReader xmlReader) throws Exception {

		String[] tagName = getName(xmlReader);
	    //System.out.println("tagName : " + tagName[0] );

		if (tagName[0].equalsIgnoreCase("DE")) {
			//isCodeField = false;
			//isFoundDefault = false;
			processField = false;
			processMetaDEElement(xmlReader);
		} else if (tagName[0].equalsIgnoreCase("valName")) {
			/*
			if (isCodeField && !isFoundDefault) {
				processCodeValElement(xmlReader);
			}
			*/
		} else if (tagName[0].equalsIgnoreCase("deValue")) {
		} else { //root tag.
		}
	}

	private static void processMetaDEElement(XMLStreamReader xmlReader) throws Exception {
		deAttMap = getAttributeMap(xmlReader);
		if (deAttMap.containsKey("CobName")) {
			processField = true;
			/*
			if (deAttMap.get("fieldtype").equalsIgnoreCase("Code") ) {
				isCodeField = true;
			}
			*/
		}

	}

	private static void handleMetaEEEvent(XMLStreamReader xmlReader) throws Exception {

		String[] tagName = getName(xmlReader);
	    //System.out.println("End tagName : " + tagName[0] );

		if (tagName[0].equalsIgnoreCase("DE")) {
			if (processField) {
				emptyDataRec.put("#00", recType);

				//String key = String.format("%03d", (Integer.parseInt(deAttMap.get("num")) + 3));
				String key = deAttMap.get("num");
				String val = deAttMap.get("Name").replace(" ", "_")
							                    .replace("#", "Num")
							                    .replace(";", "_")
							                    .replace("/", "_")
							                    .replace("<", "Lt")
							                    .replace(">", "Gt")
							                    .replace("(", "")
							                    .replace(")", "")
							                    .replace("'", "")
							                    .replace(".", "")
							                    .replace("%", "Pct");

				metaInfo.put(key, val);
				emptyDataRec.put(key, "");

			}
		} else if (tagName[0].equalsIgnoreCase("valName")) {
		} else if (tagName[0].equalsIgnoreCase("deValue")) {
		} else { //root
			metaTab.put(recId, metaInfo);
			emptyDataTab.put(recId, emptyDataRec);
		}
	}


	/*
	private static void processCodeValElement(XMLStreamReader xmlReader) throws Exception {
		boolean isDone = false;
		for (int i = 0; i < xmlReader.getAttributeCount() && !isDone; i++) {
			String AttName =  xmlReader.getAttributeLocalName(i);
			String AttVal  =  xmlReader.getAttributeValue(i);
			System.out.println("    Att Name : " +  AttName +
					           "    Att Value : " + AttVal);
			try {
				if (AttName.equalsIgnoreCase("value") &&
						(AttVal.trim().equalsIgnoreCase("") ||  Integer.parseInt(AttVal) == 0 ) ) {
					isDone = true;
					isFoundDefault = true;
					System.out.println("      Def Att Name : " +  AttName +
					                   "      Def Att Value : " + AttVal);
				}
			} catch (NumberFormatException e) {

			}
		}
	}
	*/

}
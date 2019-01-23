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


	private static final char QUOTE = '\"';
	private static final char COMMA = ',';
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static final String outputSuf = ".csv.gz";

	private static int messageLim ;
	private static int count = 0;
	private static int totCount = 0;
	private static boolean processField;


	private static TreeMap<String, String>  deAttMap ;
	private static TreeMap<String, String>  metaInfo ;
	private static TreeMap<String, String>  emptyDataRec;
	private static TreeMap<String, String>  realDataRec ;
	private static TreeMap<String, Integer> metaMissingElem ;

	private static boolean isCodeField;
	private static boolean isFoundDefault;
	private static String recType;
	private static String currentKey;
	private static boolean isKeyFound = false;
	private static GZIPOutputStream out;


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

		String inputPattern = System.getProperty("inputPattern");
		System.out.println("inputPattern : " + inputPattern);
		if (inputPattern==null ) {
			inputPattern = "omnilog*.xml.gz";
		}

		String outputDir = System.getProperty("outputDir");
		System.out.println("outputDir : " + outputDir);
		if (outputDir==null || outputDir.equalsIgnoreCase("") ) {
			outputDir = ".";
		}

		f = new File(outputDir);
		if (!f.exists() | !f.isDirectory()) {
			System.out.println("Invalid " + outputDir +" -DoutputDir=<output directory name>, enter valid directory where the output files needs to be stored. ");
			isError = true;
		}

		String metaDir = System.getProperty("metaDir");
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
		Path inputPath = Paths.get(inputDir);

		processMain(inputPath,  inputPattern, outputDir, metaDir );

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

	private static void processMain(Path inputDir,  String inputPattern, String outputDir, String metaDir ) {

		try {
			System.out.println("" );
			System.out.println("Started processing at Time : " + (new Timestamp(System.currentTimeMillis())) );
			DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, inputPattern);
		    for (Path entry: stream) {
		    	count = 0;
		    	deAttMap = new TreeMap<String, String>();
		    	metaInfo = new TreeMap<String, String>();
		    	emptyDataRec = new TreeMap<String, String>();
		    	realDataRec = new TreeMap<String, String>();
		    	metaMissingElem = new TreeMap<String, Integer>();

		    	File inputFileName = entry.toFile();
		    	String inputFile = inputFileName.getName();
				//String outputFile = inputFile.substring(0, inputFile.length()-4) + outputSuf;
				int posDot = inputFile.indexOf(".");
				String outputFile = inputFile.substring(0, posDot) + outputSuf;
				File outputFileName = new File(outputDir + File.separator + outputFile);

				String metaFile = inputFile.substring(posDot-4, posDot) + ".xml" ;
				//System.out.println("metaFile : " + metaFile);
				File metaFileName = new File(metaDir + File.separator + metaFile);

				out =  new GZIPOutputStream ( new FileOutputStream(outputFileName));
				processMetaFile(metaFileName);
				processDataFile(inputFileName);
				out.close();
		    }
			System.out.println("" );
			System.out.println("Processed Record Type count : " + count + "  Total count : " + totCount + " Time : " + (new Timestamp(System.currentTimeMillis())) );

		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	private static void processMetaFile(File metaFileName) throws Exception {

		metaInfo.put("000", "RecId");
		//emptyDataRec.put("000", "");

		metaInfo.put("001", "Action");
		emptyDataRec.put("001", "");

		metaInfo.put("002", "Date");
		emptyDataRec.put("002", "");

		metaInfo.put("003", "Time");
		emptyDataRec.put("003", "");

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
		for(Map.Entry<String, Integer> entry : metaMissingElem.entrySet()) {
			  key1 = entry.getKey();
			  val1 = entry.getValue();
			  System.out.println("   WARN :  key : " + key1 + " occured " + val1 + " times, doesn't exist in Record type : " + realDataRec.get("000"));
		}

		System.out.println("COMPLETED Record Type " + recType + " Processed row count : " + count + "  Total count : " + totCount + " Time : " + (new Timestamp(System.currentTimeMillis())) );
		//
		// Close the reader
		//
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

	private static void handleMetaSEEvent(XMLStreamReader xmlReader) throws Exception {

		String[] tagName = getName(xmlReader);
	    //System.out.println("tagName : " + tagName[0] );

		if (tagName[0].equalsIgnoreCase("DE")) {
			isCodeField = false;
			isFoundDefault = false;
			processField = false;
			processDEElement(xmlReader);
		} else if (tagName[0].equalsIgnoreCase("valName")) {
			/*
			if (isCodeField && !isFoundDefault) {
				processCodeValElement(xmlReader);
			}
			*/
		} else if (tagName[0].equalsIgnoreCase("deValue")) {
		} else { //LgRec tag.
			TreeMap<String, String> rootAttMap = getAttributeMap(xmlReader);
		    recType = rootAttMap.get("recType");
		    //System.out.println("recType : " + recType );
		    //xmlw.writeCharacters("\n");
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

		realDataRec = (TreeMap<String, String>) emptyDataRec.clone();

		deAttMap = getAttributeMap(xmlReader);

		realDataRec.put("000", deAttMap.get("RecId") );
		realDataRec.put("001", deAttMap.get("Action") );
		realDataRec.put("002", deAttMap.get("Date") );
		realDataRec.put("003", deAttMap.get("Time") );
	}

	private static void processDataDE(XMLStreamReader xmlReader) throws Exception {
		deAttMap = getAttributeMap(xmlReader);
		currentKey = String.format("%03d", (Integer.parseInt(deAttMap.get("num")) + 3));
		isKeyFound = true;
	    //System.out.println("currentKey : " + currentKey  );
	}

	private static void handleDataValEvent(XMLStreamReader xmlReader) throws Exception {
		if (isKeyFound) {
			String val = xmlReader.getText();
		    //System.out.println("currentKey : " + currentKey + " val : " + val  );
			realDataRec.put(currentKey, val);
			isKeyFound = false;
		}
	}

	private static void processDEElement(XMLStreamReader xmlReader) throws Exception {
		deAttMap = getAttributeMap(xmlReader);
		if (deAttMap.containsKey("CobName")) {
			processField = true;
			if (deAttMap.get("fieldtype").equalsIgnoreCase("Code") ) {
				isCodeField = true;
			}
		}

	}

	private static void handleMetaEEEvent(XMLStreamReader xmlReader) throws Exception {

		String[] tagName = getName(xmlReader);
	    //System.out.println("End tagName : " + tagName[0] );

		if (processField) {
			if (tagName[0].equalsIgnoreCase("DE")) {
				emptyDataRec.put("000", recType);

				String key = String.format("%03d", (Integer.parseInt(deAttMap.get("num")) + 3));
				String val = deAttMap.get("Name").replace(" ", "_")
							                    .replace("#", "Num")
							                    .replace(";", "_")
							                    .replace("/", "_")
							                    .replace("(", "")
							                    .replace(")", "")
							                    .replace("%", "Pct");


				metaInfo.put(key, val);
				emptyDataRec.put(key, "");

			}
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
		for(Map.Entry<String, String> entry : realDataRec.entrySet()) {
			  key = entry.getKey();
			  if (metaInfo.containsKey(key)) {
				  String value = entry.getValue();
				  RecString = RecString + QUOTE + value + QUOTE + COMMA;
			  } else {
				  String key1 = String.format("%03d", (Integer.parseInt(key) - 3));
				  int val1;
				  if (metaMissingElem.containsKey(key1) ) {
					  val1 = metaMissingElem.get(key1) + 1;
					  metaMissingElem.put(key1, new Integer(val1));
				  } else {
					  metaMissingElem.put(key1, new Integer(1));
				  }
				  //System.out.println("WARN :  key : " + key1 + " doesn't exist in Record type : " + realDataRec.get("000"));
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
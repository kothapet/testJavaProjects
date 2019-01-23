package examples.StAX;

import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.stream.*;

/**
 */
public class testReadZipXml {
	private static String filename = null;

	private static void printUsage() {
		System.out.println("usage: java examples.basic.Parse <xmlfile>");
	}

	public static void main(String[] args) throws Exception {
		try {
			//String inputDir = "C:/EclipseNeonWorkSpace/OmniXML/data/";
			//String inputFile = "test1.xml";
			String inputDir = "C:/EclipseNeonWorkSpace/OmniXML/omni_data/out/";
			//String inputFile = "omnilog20181212-135913.zip";
			String inputFile = "TEST1.zip";

			// filename = args[0];
			filename = inputDir + inputFile;
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			printUsage();
			System.exit(0);
		}

		XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
		System.out.println("FACTORY: " + xmlInFactory);
		XMLStreamReader xmlReader = null;

		ZipFile zf = new ZipFile(filename);

        for (Enumeration<? extends ZipEntry> entries = zf.entries(); entries.hasMoreElements();)
        {
        	ZipEntry entry = entries.nextElement();
            String s = String.format("Entry: %s len %d added %TD",
                    entry.getName(), entry.getSize(),
                    new Date(entry.getTime()));
            System.out.println(s);
            InputStream in = zf.getInputStream(entry);
    		xmlReader = xmlInFactory.createXMLStreamReader(in);
    		System.out.println("READER:  " + xmlReader + "\n");

    		while (xmlReader.hasNext()) {
    			printEvent(xmlReader);
    			xmlReader.next();
    		}

    		xmlReader.close();
        }
	}

	private static void printEvent(XMLStreamReader xmlReader) {
		System.out.print("EVENT: " + getEventString(xmlReader.getEventType())
		        + " :[" + xmlReader.getLocation().getLineNumber() + "]["
				+ xmlReader.getLocation().getColumnNumber() + "] ");
		System.out.print(" [");
		switch (xmlReader.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
				System.out.print("<");
				printName(xmlReader);
				printNamespaces(xmlReader);
				printAttributes(xmlReader);
				System.out.print(">");
				break;
			case XMLStreamConstants.END_ELEMENT:
				System.out.print("</");
				printName(xmlReader);
				System.out.print(">");
				break;
			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CHARACTERS:
				int start = xmlReader.getTextStart();
				int length = xmlReader.getTextLength();
				System.out.print(new String(xmlReader.getTextCharacters(), start, length));
				break;
			case XMLStreamConstants.PROCESSING_INSTRUCTION:
				System.out.print("<?");
				if (xmlReader.hasText())
					System.out.print(xmlReader.getText());
				System.out.print("?>");
				break;
			case XMLStreamConstants.CDATA:
				System.out.print("<![CDATA[");
				start = xmlReader.getTextStart();
				length = xmlReader.getTextLength();
				System.out.print(new String(xmlReader.getTextCharacters(), start, length));
				System.out.print("]]>");
				break;
			case XMLStreamConstants.COMMENT:
				System.out.print("<!--");
				if (xmlReader.hasText())
					System.out.print(xmlReader.getText());
				System.out.print("-->");
				break;
			case XMLStreamConstants.ENTITY_REFERENCE:
				System.out.print(xmlReader.getLocalName() + "=");
				if (xmlReader.hasText())
					System.out.print("[" + xmlReader.getText() + "]");
				break;
			case XMLStreamConstants.START_DOCUMENT:
				System.out.print("<?xml");
				System.out.print(" version='" + xmlReader.getVersion() + "'");
				System.out.print(" encoding='" + xmlReader.getCharacterEncodingScheme() + "'");
				if (xmlReader.isStandalone())
					System.out.print(" standalone='yes'");
				else
					System.out.print(" standalone='no'");
				System.out.print("?>");
				break;
		}
		System.out.println("]");
	}

	private static String getEventString(int eventTypeC) {
		String eventTypeStr = "";
		switch (eventTypeC) {
		case XMLStreamConstants.START_ELEMENT:
			eventTypeStr = "START_ELEMENT";
			break;
		case XMLStreamConstants.END_ELEMENT:
			eventTypeStr = "END_ELEMENT";
			break;
		case XMLStreamConstants.SPACE:
			eventTypeStr = "SPACE";
			break;
		case XMLStreamConstants.CHARACTERS:
			eventTypeStr = "CHARACTERS";
			break;
		case XMLStreamConstants.PROCESSING_INSTRUCTION:
			eventTypeStr = "PROCESSING_INSTRUCTION";
			break;
		case XMLStreamConstants.CDATA:
			eventTypeStr = "CDATA";
			break;
		case XMLStreamConstants.COMMENT:
			eventTypeStr = "COMMENT";
			break;
		case XMLStreamConstants.ENTITY_REFERENCE:
			eventTypeStr = "ENTITY_REFERENCE";
			break;
		case XMLStreamConstants.START_DOCUMENT:
			eventTypeStr = "START_DOCUMENT";
			break;
		}
		return eventTypeStr;
	}

	private static void printName(XMLStreamReader xmlReader) {
		if (xmlReader.hasName()) {
			String prefix = xmlReader.getPrefix();
			String uri = xmlReader.getNamespaceURI();
			String localName = xmlReader.getLocalName();
			printName(prefix, uri, localName);
		}
	}

	private static void printName(String prefix, String uri, String localName) {
		if (uri != null && !("".equals(uri)))
			System.out.print("['" + uri + "']:");
		if (prefix != null && !("".equals(prefix)))
			System.out.print(prefix + ":");
		if (localName != null)
			System.out.print(localName);
	}

	private static void printAttributes(XMLStreamReader xmlReader) {
		for (int i = 0; i < xmlReader.getAttributeCount(); i++) {
			printAttribute(xmlReader, i);
		}
	}

	private static void printAttribute(XMLStreamReader xmlReader, int index) {
		String prefix = xmlReader.getAttributePrefix(index);
		String namespace = xmlReader.getAttributeNamespace(index);
		String localName = xmlReader.getAttributeLocalName(index);
		String value = xmlReader.getAttributeValue(index);
		System.out.print(" ");
		printName(prefix, namespace, localName);
		System.out.print("='" + value + "'");
	}

	private static void printNamespaces(XMLStreamReader xmlReader) {
		for (int i = 0; i < xmlReader.getNamespaceCount(); i++) {
			printNamespace(xmlReader, i);
		}
	}

	private static void printNamespace(XMLStreamReader xmlReader, int index) {
		String prefix = xmlReader.getNamespacePrefix(index);
		String uri = xmlReader.getNamespaceURI(index);
		System.out.print(" ");
		if (prefix == null)
			System.out.print("xmlns='" + uri + "'");
		else
			System.out.print("xmlns:" + prefix + "='" + uri + "'");
	}
}
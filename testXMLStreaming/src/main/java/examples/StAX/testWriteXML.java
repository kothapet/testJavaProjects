package examples.StAX;


import java.io.FileOutputStream;
import javax.xml.stream.*;
 /**
 */
 public class testWriteXML {
   public static void main(String args[]) throws Exception {
     //
	String outputDir = "C:/EclipseNeonWorkSpace/OmniXML/data/";
	String oututFile = "testWrite.xml";
	String outputFileName = outputDir + oututFile;


    XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
    System.out.println("FACTORY:  " + xmlof);

    XMLStreamWriter xmlw = xmlof.createXMLStreamWriter(new FileOutputStream (outputFileName));
    System.out.println("WRITER:  " + xmlw + "\n");

    // Write the default XML declaration
    xmlw.writeStartDocument();
    xmlw.writeCharacters("\n");
    xmlw.writeCharacters("\n");

    // Write a comment
    xmlw.writeComment("this is a comment");
    xmlw.writeCharacters("\n");

    // Write the root element "person" with a single attribute "gender"
    xmlw.writeStartElement("person");
    xmlw.writeNamespace("one", "http://namespaceOne");
    xmlw.writeAttribute("gender","f");
    xmlw.writeCharacters("\n");

    // Write the "name" element with some content and two attributes
    xmlw.writeCharacters("    ");

    xmlw.writeStartElement("one", "name", "http://namespaceOne");
    xmlw.writeAttribute("hair","pigtails");
    xmlw.writeAttribute("freckles","yes");
    xmlw.writeCharacters("Pippi Longstocking");

    // End the "name" element
    xmlw.writeEndElement();

    xmlw.writeCharacters("\n");

    // End the "person" element
    xmlw.writeEndElement();

    // End the XML document
    xmlw.writeEndDocument();

    // Close the XMLStreamWriter to free up resources
    xmlw.close();
   }
 }
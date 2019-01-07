package examples.StAX;


import java.io.FileOutputStream;
import javax.xml.stream.*;
//import java.util.Iterator;
//import javax.xml.namespace.QName;
 /**
 * This is a simple example that illustrates how to use the
 * the XMLStreamWriter class to generate XML.
 *
 * The generated XML file looks like this:
 *
 *   <?xml version='1.0' encoding='utf-8'?>
 *
 *   <!--this is a comment-->
 *   <person xmlns:one="http://namespaceOne" gender="f">
 *       <one:name hair="pigtails" freckles="yes">Pippi Longstocking</one:name>
 *   </person>
 *
 *
 * @author Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 */
 public class testWriteXML {
   public static void main(String args[]) throws Exception {
     //
	String outputDir = "C:/EclipseNeonWorkSpace/OmniXML/data/";
	String oututFile = "testWrite.xml";
	String outputFileName = outputDir + oututFile;

	// Get an output factory
    //
    XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
    System.out.println("FACTORY:  " + xmlof);
     //
    // Instantiate a writer
    //
    XMLStreamWriter xmlw = xmlof.createXMLStreamWriter(new FileOutputStream (outputFileName));
    System.out.println("WRITER:  " + xmlw + "\n");
     //
    // Generate the XML
    //
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
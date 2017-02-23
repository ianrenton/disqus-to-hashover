package com.ianrenton.disqustohashover;

import disqus.Disqus;
import disqus.Post;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 * Converter from a Disqus export XML file to HashOver XML structure.
 *
 * @author Ian Renton
 */
public class DisqusToHashOver {

  private static final String PATH_TO_COMMENT_XML = "comments.xml";
  private static final String PATH_TO_SCHEMA = "xml-resources/jaxb/disqus/disqus.xsd";
  private static final String OUTPUT_ROOT = "hashover/pages";

  public static void main(String[] args) throws FileNotFoundException, IOException, JAXBException, SAXException {
    // Set up JAXB
    JAXBContext jaxbContext = JAXBContext.newInstance(Disqus.class);
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = factory.newSchema(new StreamSource(new File(PATH_TO_SCHEMA)));
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    jaxbUnmarshaller.setSchema(schema);

    // Read DISQUS export file
    Disqus d = (Disqus) jaxbUnmarshaller.unmarshal(new File(PATH_TO_COMMENT_XML));

    // Build a map of Thread ID to the page name that HashOver wants
    Map<String, File> outputDirByThreadID = new HashMap<>();
    for (disqus.Thread t : d.getThread()) {
      String threadPath = t.getId().replaceFirst("/", "");
      String outputDirPath = threadPath.replaceAll("/", "-");

      // Create dir if it doesn't exist
      File outputDir = new File(OUTPUT_ROOT + "/" + outputDirPath);
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }

      // Store dir reference
      outputDirByThreadID.put(t.getIdAttribute(), outputDir);
    }

    // Get each post, and output to files in the right directory
    for (Post p : d.getPost()) {
      // Find the right dir to output to
      disqus.Post.Thread t = p.getThread();
      File hashOverDir = outputDirByThreadID.get(t.getId());

      // Find the next available file to write to
      int count = 1;
      File outputFile = new File(hashOverDir, Integer.toString(count) + ".xml");
      while (outputFile.exists()) {
        outputFile = new File(hashOverDir, Integer.toString(++count) + ".xml");
      }

      // Convert post date format to e.g. 02/23/2017 - 2:37pm
      XMLGregorianCalendar pd = p.getCreatedAt();
      int hour = pd.getHour();
      String suffix = "am";
      if (hour >= 12) {
        suffix = "pm";
      }
      if (hour >= 13) {
        hour -= 12;
      }
      if (hour == 0) {
        hour = 12;
      }
      String postDateString = String.format("%02d", pd.getMonth()) + "/"
              + String.format("%02d", pd.getDay()) + "/"
              + String.format("%04d", pd.getYear()) + " - "
              + hour + ":"
              + String.format("%02d", pd.getMinute()) + suffix;

      // Build the file content to write
      String fileContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
              + "<comment likes=\"0\" notifications=\"yes\" ipaddr=\"\">\n"
              + "	<name>" + p.getAuthor().getName() + "</name>\n"
              + "	<email>" + p.getAuthor().getEmail() + "</email>\n"
              + "	<passwd></passwd>\n"
              + "	<website></website>\n"
              + "	<date>" + postDateString + "</date>\n"
              + "\n"
              + "	<body>" + escapeHTML(p.getMessage()) + "</body>\n"
              + "</comment>";

      // Write file
      PrintWriter out = new PrintWriter(outputFile);
      out.print(fileContent);
      out.close();
    }
  }

  private static String escapeHTML(String s) {
    StringBuilder out = new StringBuilder(Math.max(16, s.length()));
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
        out.append("&#");
        out.append((int) c);
        out.append(';');
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }
}

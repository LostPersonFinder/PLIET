/*
 * /*
 * Informational Notice:
 * This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
 * an agency of the Department of Health and Human Services, United States Government.
 *
 * The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
 *
 * The license does not supersede any applicable United States law.
 *
 * The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
 *
 * Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
 * (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
 * The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
 * non-commercial products.
 *
 * LICENSE:
 *
 * Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
 * as provided by Federal law.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
 *
 * -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 *
 * -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
 * of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nih.nlm.lpf.xmlUtil;

/**
 *
 * @author 
 */

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;


import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;


import org.apache.log4j.Logger;

/****************************************************************/

public class XMLUtil
{
    private static Logger log = Logger.getLogger(XMLUtil.class);
    // Convert an XML file to String, with no escapes
    
    static String[] SpecialChars = {"&", "<", ">", "\"", "\'"};   // "& " must hava a " "
    static String[] ReservedSeq = {"&amp;", "&lt;", "&gt;", "&quot;", "&apos;"};
    
    /*------------------------------------------------------------------------------------------------------*/
     public static String convertXMLFileToString(String fileName)
    {
        try
        { 
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            InputStream inputStream = new FileInputStream(new File(fileName));

            Document doc = documentBuilderFactory.newDocumentBuilder().parse(inputStream);
            StringWriter stw = new StringWriter();

            Transformer serializer = TransformerFactory.newInstance().newTransformer();
            serializer.transform(new DOMSource(doc), new StreamResult(stw));
            String s= stw.toString();
        
            String xmlStr = s;
            return xmlStr;
        }
        catch (Exception e) 
        {
            log.error("Error converting XML file " + fileName+ "  to String", e);
        }
        return null;
    }     
     
    /*****************************************************************************************
    // Create an XML Document from a template file
    /*****************************************************************************************/
  public static Document readXMLFile(String xmlFile)
    {
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	   DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
	   Document xmlDoc = docBuilder.parse(xmlFile);
            xmlDoc.getDocumentElement().normalize();;
            return xmlDoc;
        }
        catch (Exception e)
        {
            log.error("Could not create input stream for XML file", e);
            return null;
        }
    }
  
  
/*******************************************************************
 * Output the given Document to an XML formatted file.
 *******************************************************************/
    public static int writeDocumentToXMLFile(Document doc, String xmlFile)
    {
        try
        {            
            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(xmlFile));
             transformer.transform(source, result);
            System.out.println("File saved!");
            return 1;
        }

        catch (Exception e)
        {
            log.error("Error writing data to XML file ", e );
            return 0;
        }
    }
  
    
/*******************************************************************
 * Output the given Document to a an XML String
 *******************************************************************/
    public static String  convertDocumentToString(Document doc)
    {
        try
        {
            // write the content into xml string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");     // remove XML declaration     
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "xml");  // don't chunk data as in "text/xml" ??
            
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);

            StringWriter writer = new StringWriter();
            transformer.transform(source, new StreamResult(writer));        
            
            String xmlString = result.getWriter().toString();
            return xmlString;
        } 
        catch (TransformerException e)
        {
            log.error(e);
        }
        return null;
    }
    
    
 /* ***********************************************************************************
 * Output the given formatted  XML String (from an XML Document) to a  file.
 ****************************************************************************************/
    public static int writeXMLStringToFile(String xmlString, String xmlFilename)
    {
        int status = 0;
        try
        {
            FileOutputStream outStream =  new FileOutputStream(xmlFilename);
            outStream.write(xmlString.getBytes());
            outStream.flush();
            outStream.close();
            log.info("Created XML file: " + xmlFilename);
            status = 1;
        }
        catch (Exception e)
        {
            log.error("Error writing data to XML file ", e );
        }
        return status;
    }
  
    /*------------------------------------------------------------------------------------------------------------*/
    // Escape the  xml special characters in the given String 
    // These character are:  &, >, <, ", and '.
//-------------------------------------------------------------------------------------------------------------*/
        public static String escapeXMSLtring(String inputString)
        {
            String s = inputString;
           String xmlStr = s.replaceAll("&", "&amp;").replaceAll(">", "&gt;").
               replaceAll("<", "&lt;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
            return xmlStr;
        }     
        
     /****************************************************************
     * If there are XML Tag character (< or > etc. ) convert it to
     * to proper XML escape sequences
     * **************************************************************/
    public static String convertReservedChars(String text)
    {
        if (text == null)
            return text;
        {
            int n = SpecialChars.length;
            for (int i = 0; i < n; i++)
                    text = text.replaceAll(SpecialChars[i], ReservedSeq[i]);
        }
        return text;
    }
    /****************************************************************
     * If there are XML escape sequences such as &lt; or &gt; convert them
     * to normal representation
     * **************************************************************/
    public static String replaceReservedSeq(String text)
    {
        if (text == null)
            return text;
        int n = ReservedSeq.length;
        for (int i = 0; i < n; i++)
                text = text.replaceAll(ReservedSeq[i], SpecialChars[i]);
        return text;
    }

    
    /************************************************************************
     * Create the node list for a given tag name from a Document
     * This is more efficient than creating the node list every time.
     *************************************************************************/
    public static NodeList getNodeList(Document doc, String tag)
    {
        if (doc == null)
            return null;
        return doc.getElementsByTagName(tag);
    }

/************************************************************************
 * Create the nodelist for a given tag name, for an Element node
 * This is more efficient than creating the node list every time.
 *************************************************************************/

    public static NodeList getNodeList(Element node, String tag)
    {
        if (node == null)
             return null;
        return node.getElementsByTagName(tag);
    }

 /************************************************************
   * Get the XML node with a given unique "integer" attribute Value
   * Arguments:
   *    nodeList - A list of Element Nodes to search
   *    attrName - Name of the attribute to look for
   *    attrVal - The integer value of the attribute to match against
   ************************************************************/

    public static Element getTagNode(NodeList nodeList,
		String attrName, int attrVal)
    {
        String stag = String.valueOf(attrVal);
        return getTagNode(nodeList, attrName, stag);
    }

   /************************************************************
   * Get the XML node with a given "String" unique attribute Value
   * Arguments:
   *    nodeList - A list of Element Nodes to search
   *    attrName - Name of the attribute to look for
   *    attrVal - The String value of the attribute to match against
   ************************************************************/
    public static Element getTagNode(NodeList nodeList,
        String attrName, String attrVal)
    {
        if (nodeList == null || nodeList.getLength() == 0)
        {
            return null;
        }

        Element node = null;
        int n = nodeList.getLength();
        for (int i = 0; i < n; i++)
        {
            Element tagNode = (Element) nodeList.item(i);
            if (tagNode.getAttribute(attrName).equals(attrVal))
            {
                node = tagNode;
                break;
            }
        }
        return node;
    }

    /*********************************************************
     * Get the Text value of a tagged field, with the specified
     * attribute as String
     ***********************************************************/
    public static String[] getTagValues(NodeList tagNodes,
        String attrName, String attrValue)
    {
        String[] values = null;
        Element elem = (Element) getTagNode(tagNodes, attrName, attrValue);
        if (elem != null)
        {
            values = getSpecifiedValues(elem);
        }

        if (values == null || values.length == 0)
        {
            log.warn("--- Could not find value of node with attribute: "
                + attrName + "=" + attrValue);
        }
        return values;
    }
    
    /*********************************************************
     * Get the Text value of a tagged field, with the specified
     * attribute as String
     * If it is a multi-values item, we simply return the first value
     * We don't call getTagValues() first for better efficiency
     ***********************************************************/
    public static String getTagValue(NodeList tagNodes,
        String attrName, String attrValue)
    {
        String value = "";
        Element elem = (Element) getTagNode(tagNodes, attrName, attrValue);
        if (elem != null)
        {
            value = getSpecifiedValue(elem);
        }

        if (value.equals("") || value == null)
        {
            log.warn("--- Could not find value of node with attribute: "
                + attrName + "=" + attrValue);
        }
        return value;
    }


    /**********************************************************
     * Get the text value of a node fron its <value> child node
     * Do not go though getSpecifiedValues for effciency
     * ********************************************************/
    public static String getSpecifiedValue(Element elem)
    {
        String value = "";
        NodeList nL = elem.getElementsByTagName("value");
        if (nL.getLength() > 0)
        {
            value = getTextValue(nL.item(0));
        }
        return value;
    }
 /**********************************************************
 * Get the text value of a node fron its <value> child node
 * ********************************************************/
    public static String[] getSpecifiedValues(Element elem)
    {
        String[] values = null;
        NodeList nL = elem.getElementsByTagName("value");
        int n = nL.getLength();
        if (n > 0)
        {
            values = new String[n];
            for (int i = 0; i < n; i++)
                values[i] = getTextValue(nL.item(i));
        }
        return values;
    }

/*********************************************************
 * Get the text value of a node from its "#text" child
 * ********************************************************/
    public static String getTextValue(Node node)
    {
        String textVal = "";
        Node child = node.getFirstChild();
        if (child != null && child.getNodeType() == Node.TEXT_NODE)
            textVal = child.getNodeValue();
        if (textVal == null || textVal.length() == 0)
            return "";

        String newText = replaceReservedSeq(textVal);
        String newVal = trimText(newText);
        return newVal;
    }

  /*****************************************************
  * To fix an unexplained bug:
  * Change the non-printable character to blank
  *******************************************************/
    private static String trimText(String textVal)
    {
        int n = textVal.length();
        StringBuffer sbuf = new StringBuffer(n);
        boolean changed = false;

        int nw = 0;   // number of consecutive white spaces
        for (int i = 0; i < n; i++)
        {
            char ch = textVal.charAt(i);
            if (!Character.isWhitespace(ch))  // a regular char
            {
                sbuf.append(ch);
                nw = 0;
            } else      // set only the first blank char
            {
                if (nw == 0)
                {
                    sbuf.append(' ');
                    nw++;
                }
            }
        }
        //----------------//
        if (sbuf.length() != n)
        {
            return new String(sbuf);
        } else
        {
            return textVal;
        }
    }

    /*********************************************************************/
    // Set the value of a metadata node, either in insert or in update mode
    // Get the child node with a tag of <value>
    // doc is a generic Document object used to creaet a text node
    /***********************************************************************/
    public static int updateNodeValue(Document doc, NodeList list,
        String attrname, String attrval, String nodeValue, boolean insertMode)
    {
        // get the element whose specified attribute has the given (unique) value
        Element elem = getTagNode(list, attrname, attrval);
        if (elem == null)
        {
            log.warn("No node with attribute " + attrname + "=" + attrval
                + " exists in node list");
            return 0;
        }
        NodeList childNodes = elem.getElementsByTagName("value");

        if (childNodes != null && childNodes.getLength() > 0)
        {
            setTextValue(doc, childNodes.item(0), nodeValue, insertMode);
            if (log.isDebugEnabled())
            {
                log.debug("Value of node " + elem.getAttribute(attrname)
                    + " set to " + nodeValue);
            }
        } else
        {
            // If in insert mode, create the <value> node
            if (insertMode)
            {
                Element valueNode = doc.createElement("value");
                elem.appendChild(valueNode);
                setTextValue(doc, valueNode, nodeValue, true);
                if (log.isDebugEnabled())
                {
                    log.debug("Value of node " + elem.getAttribute(attrname)
                        + " set to " + nodeValue);
                }
            }
        }
        return 1;
    }

    /*********************************************************************/
    // Set the value of a metadata node, either in insert or in update mode
    // Get the child node with a tag of <value>
    // doc is a generic Document object used to creaet a text node
    /***********************************************************************/
    public static int updateNodeValues(Document doc, NodeList list,
        String attrname, String attrval, String[] textValues, boolean insertMode)
    {

        if (textValues.length == 1)
        {
            return updateNodeValue(doc, list, attrname, attrval,
                textValues[0], insertMode);
        }

        // multivalued element:
        // get the element whose specified attribute has the given (unique) value
        Element elem = getTagNode(list, attrname, attrval);
        if (elem == null)
        {
            log.warn("No node with attribute " + attrname + "=" + attrval
                + " exists in node list");
            return 0;
        }
        NodeList valueNodes = elem.getElementsByTagName("value");
        int nv = (valueNodes == null ? 0 : valueNodes.getLength());

        int nr = Math.min(nv, textValues.length);
        // replace/insert the first set of text values	   
        for (int i = 0; i < nr; i++)
        {
            setTextValue(doc, valueNodes.item(i), textValues[i], insertMode);
        }
        if (nv == textValues.length)
        {
            return 1;
        }

        // Handle additional initial or final value strings
        if (nr < nv)   // if more value nodes exist, delete them
        {
            for (int i = nr; i < nv; i++)
            {
                elem.removeChild(valueNodes.item(i));
            }
        } else        // more text values given, insert new value nodes
        {
            for (int i = nr; i < textValues.length; i++)
            {
                Element valueNode = doc.createElement("value");
                elem.appendChild(valueNode);
                setTextValue(doc, valueNode, textValues[i], true);
            }
        }
        return 1;
    }
/////////////////////////////////////////////////////////////////////////////////
//    Write operations on DOM
/////////////////////////////////////////////////////////////////////////////////    
    public static Document createDocument(String topNodeName)
    {
        try
        {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement(topNodeName);
            doc.appendChild(rootElement);          
            return doc;
        }
        catch (Exception e)
        {
            log.error("Could not create oytputt Document", e);
            return null;
        }  
    }
    
  /****************************************************************
 * Set the text value associated with the specified node by accessing
 * its #textnode. Ignore any linefeeds reported as a text node
 * Note: This is the <value> node of each element
 ************************************************************/
    public static void setTextValue(Document doc, Node valueNode,
                String textValue, boolean insertMode)
    {
        Node textNode = doc.createTextNode(textValue);
        if (insertMode)
            valueNode.appendChild(textNode);
        else            // replace mode, replace the first one
        {
            Node child = valueNode.getFirstChild();
            while (child != null && child.getNodeType() != Node.TEXT_NODE)
            {
                child = child.getNextSibling();
            }
            if (child != null)    // TEXT_NODE found
                valueNode.replaceChild(textNode, child);
            else        // node not found, so insert as first one
                valueNode.appendChild(textNode);
        }
    }

/*******************************************************************
 * Convert the information in the document to a hashMap
 *******************************************************************/
  public static HashMap<String, String> getValuesFromDocument(Document doc, String[] fields)
  {
      HashMap <String, String> fieldMap = new HashMap();
      for (int i = 0; i < fields.length; i++)
      {
          NodeList nodes = doc.getElementsByTagName(fields[i]);
          if (nodes != null)
          {
              Element elem = (Element) nodes.item(0);
               String value = elem.getNodeValue();
               fieldMap.put(fields[i], value);
          }
      }
      return fieldMap;
  }
  /*******************************************************************/
  public static HashMap<String, String> getValuesFromFile(String xmlFile, String[] fields)   
  {
      Document doc = readXMLFile( xmlFile);
      if (doc != null)
          return getValuesFromDocument(doc, fields);
      else
         return null;
  }
  
  /*******************************************************************/

  public static Element createChildElement(Document doc, 
      Element parentNode,  String childName)

  {
      Element elem  = doc.createElement(childName);
      parentNode.appendChild(elem);
      return elem;
  }
  
  public static void addAttribute(Element elem, String attribute, String value)
  {
      Document doc = elem.getOwnerDocument();
      Attr attr = doc.createAttribute(attribute);
      attr.setValue(value);
      elem.setAttributeNode(attr);
  }
      
  
  /************************************************************/
    public static void setChildValues(Element parentNode,
		   String childName,  String[] textValues)
    {
        Document doc = parentNode.getOwnerDocument();
        Element childNode = doc.createElement(childName);
         parentNode.appendChild(childNode);
       for (int i = 0; i < textValues.length; i++)
        {
             Node textNode = doc.createTextNode(textValues[i]);
             childNode.appendChild(textNode);
        }
    }
    
      /************************************************************/
    public static void setChildValue(Element parentNode,
		   String childName,  String textValue)
    {
        Document doc = parentNode.getOwnerDocument();
        Element childNode = doc.createElement(childName);
        parentNode.appendChild(childNode);

         Node textNode = doc.createTextNode(textValue);
         childNode.appendChild(textNode);
    }
  
 public static  String node2String(Node node)
 {
     try
     {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(node);
        transformer.transform(source, result);

        String xmlString = result.getWriter().toString();
        return xmlString;
     }
     catch (Exception e)
     {
         return null;
     }
        
 }
        
/*******************************************************************
 * Output the given Node in XML format to a  file.
 *******************************************************************

    public static int printNode(Element elemNode, String xmlFile)
    {
        int status = 0;

        // Now serialize the node and write to a file
        try
        {
            if (elemNode != null)
            {
                // Set output format for pretty printing
                OutputFormat format = new OutputFormat();
                format.setLineWidth(65);
                format.setIndenting(true);
                format.setIndent(2);

                FileOutputStream outStream = new FileOutputStream(xmlFile);
                XMLSerializer serializer = new XMLSerializer(
                    outStream, format);
                serializer.serialize(elemNode);
                outStream.flush();
                outStream.close();
                log.info("Created XML file" + xmlFile);
                status = 1;
            } else
            {
                log.error("Could not create XML file " + xmlFile);
            }
        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        return status;
    }*/
}
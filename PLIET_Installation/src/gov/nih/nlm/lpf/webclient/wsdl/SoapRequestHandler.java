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
package gov.nih.nlm.lpf.webclient.wsdl;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.SOAPFault;

import java.net.URL;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;


// needed for debugging only
import java.io.StringWriter;
import java.util.Locale;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import  org.w3c.dom.Element;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class SoapRequestHandler
{
    private Logger log = Logger.getLogger(SoapRequestHandler.class);

    // Example: final static QName _ReportPerson_QNAME = new QName("soap/plusWebServices", "reportPerson");

    String nsPrefix;        // namespace Prefix of Web service for creating QNames ("soap/plusWebServices")
    String wsURL;        // WebServer URL  provided in the WSDL ("https://plstage.nlm.nih.gov/?wsdl&api=33")

    boolean debugging = false;          // true for testing and trouble shooting

    public SoapRequestHandler(String url, String prefix)
    {
        nsPrefix = prefix;
        wsURL = url;
    }

/*------------------------------------------------------------------------------------------------------------------*/    
// Perform the desired function by sending a SOAP message to the service end point
// and get a response
    public SOAPMessage sendSoapRequest(String wsFunction, LinkedHashMap params)
    {
        try
        {
           SOAPConnection soapConnection  = createSoapConnection();
           SOAPMessage message = createSoapMessage();       // generate the structure, to be filled
           SOAPHeader header = message.getSOAPHeader();
           SOAPBody body = message.getSOAPBody();
           message.setContentDescription("text/xml");           // SOAP message protocol as seen by HTTP
       
           header.detachNode();         //  delete the header
        
           QName bodyName = new QName(nsPrefix, wsFunction, "tns");
           SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

           // add all service parameters to this bodyElemt
           addParameters(bodyElement, params);

           if (debugging)
               printMessage(message);

           // save the message in this form
           message.saveChanges();

           // send it to the WS endpoint and get reply ( >>> Do not append  wsFunction <<<)
           URL endpoint = new URL (wsURL);
           SOAPMessage response = soapConnection.call(message, endpoint);
           soapConnection.close();
           return  response;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
     /*------------------------------------------------------------------------------------------------------------------*/
    // Note: parameters must be added in the same order as in the LinkedHashMap
    //
    protected void addParameters( SOAPBodyElement bodyElement, 
        LinkedHashMap<String, String> paramSet) throws Exception
    {
        if (paramSet == null || paramSet.isEmpty())         // no parameters for this call
            return;
        Iterator<String>  it = paramSet.keySet().iterator();
        while (it.hasNext())
        {
            String param = it.next();
            String value = paramSet.get(param);
            SOAPElement  pelement =  bodyElement.addChildElement(param);
            pelement.addAttribute(new QName("xsi:type"), "xsd:string");
            pelement.addTextNode(value);
            if (debugging)
                System.out.println("param: " + param + ", value: " + value);
        }
        return;
    }
   /*------------------------------------------------------------------------------------------------------------------*/
    /**
    * Create a SOAP Connection
    * @return
    * @throws UnsupportedOperationException
    * @throws SOAPException
    */
    private SOAPConnection createSoapConnection() throws 
        UnsupportedOperationException, SOAPException
    {
        final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        final SOAPConnection soapConnection = soapConnectionFactory.createConnection();
        return soapConnection;
    }
 /*---------------------------------------------------------------------------------------------*/
 /**
* Create the SOAP Message
*
* @return
* @throws SOAPException
*/
    private SOAPMessage createSoapMessage() 
        throws SOAPException
    {
        final MessageFactory messageFactory = 
            MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);  // use 1.1 protocol
        final SOAPMessage soapMessage = messageFactory.createMessage();

        // Object for message parts
        final SOAPPart soapPart = soapMessage.getSOAPPart();
        final SOAPEnvelope envelope = soapPart.getEnvelope();

        envelope.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");
        envelope.addNamespaceDeclaration("env", "http://schemas.xmlsoap.org/soap/envelop/");
        envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        envelope.addNamespaceDeclaration("enc", "http://schemas.xmlsoap.org/soap/encoding/");

        return soapMessage;
    }
    /*---------------------------------------------------------------------------------------------*/
    public  HashMap<String, String> decodeSoapResponse(SOAPMessage response, String wsFunction)
    {
        try
        {
             // decode the Response from the server
            boolean error = isSoapFault(response);
            if (error)
            {
               log.error( "Received SOAPFault for call " + wsFunction);
               return null;
            }
            SOAPBody soapBody = response.getSOAPBody();  
            QName bodyName = new QName(nsPrefix, wsFunction);
            Iterator iterator =  soapBody.getChildElements(bodyName);
            SOAPBodyElement bodyElement =  (SOAPBodyElement)iterator.next();
            if (bodyElement == null)
            {
                log.error("Invalid SOAP function " + wsFunction + " specified for Server response.");
                return null;
            }
            // get the child nodes and their values
              if (debugging)
                    System.out.println("Server return parms for " + wsFunction );
            HashMap responseMap = new HashMap();
            Iterator it1 = bodyElement.getChildElements();
            while(it1.hasNext())
            {
               Element child = (Element)  it1.next();
               String name = child.getLocalName();
               String value  = child.getTextContent();
               responseMap.put(name, value);
               if (debugging)
                    System.out.println( "\t"+ name +", value: " + value);
            }    
            return responseMap;
        }
        catch(Exception e)
        {
            log.error("Error decoding server response for " + wsFunction , e);
            return null;
        }
    }
 /*------------------------------------------------------------------------------------------------*/           
  protected boolean isSoapFault(SOAPMessage response)     
  {
      try
      {
        SOAPBody soapBody = response.getSOAPBody();  
        if (! soapBody.hasFault() )
            return false;               // no soap faults
        else
        {
             SOAPFault soapFault = soapBody.getFault();
             String fstring = soapFault.getFaultString();
             String details ="\n FaultCode: " + soapFault.getFaultCode() 
                 + "\n URI: " + soapFault.getNamespaceURI() +"\n Nodename:  " + soapFault.getNodeName()
                 +"\n: Containts: " + soapFault.getTextContent();
                 
             log.error("Fault String : " + fstring +"\n Details: " + details);
        }
      }
      catch (Exception e)
      {
          log.error(e);
      }
      return true;
  } 
  /*------------------------------------------------------------------------------------------------------------*/
  // print the contents of a message (with parameters) on the system output.
  protected void printMessage(SOAPMessage message)
  {
      try
      {    
        // System.out.println("Message Contents: " + message.getSOAPPart().getContent());
         javax.xml.transform.dom.DOMSource contents = 
             (javax.xml.transform.dom.DOMSource)message.getSOAPPart().getContent();
        
        StringWriter stw = new StringWriter();
        Transformer serializer = TransformerFactory.newInstance().newTransformer();
        serializer.transform(contents,  new StreamResult(stw));
        System.out.println("Message Contents: " + stw.toString());
      }
      catch (Exception e)
      {
          log.error(e);
      }
  }
  /*------------------------------------------------------------------------------------------------------------*/
}
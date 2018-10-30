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
package gov.nih.nlm.lpf.emails.util;

import  gov.nih.nlm.lpf.xmlUtil.XMLUtil;

import  org.w3c.dom.Document;
import  org.w3c.dom.NodeList;
import  org.w3c.dom.Node;
import  org.w3c.dom.Element;

import java.util.HashMap;

/**
 *
 * @author 
 */
public class MessageSetIO
{
    Document messageDoc = null;
    NodeList messageNodes = null;           // List of messages
    int currentIndex = -1;                                  // position at node #
    Element currentMessage = null;
         
         
    int status = -1;
    protected String messageId = null;
    
    public MessageSetIO(String fileName, boolean readOnly)
    {
        if (readOnly)
        {
           messageDoc = openFileForReading(fileName);
           status = (messageDoc == null) ? 0 : 1;
        }
    }
    
   /*-------------------------------------------------------------------------------*/
    protected Document openFileForReading(String fileName)
    {
        try
        {
            Document xmlDoc = XMLUtil.readXMLFile(fileName);
            messageNodes = xmlDoc.getElementsByTagName("m");
            System.out.println("Number of messages in file " + fileName + ": " + messageNodes.getLength());
            currentIndex = -1;          // initial position
            return xmlDoc;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
/*-------------------------------------------------------------------------------------------------------*/    
    // read  the record in the file
   public   String  readNextMessage()
    {
        int n = messageNodes.getLength() ;
        if ( messageDoc == null || n == 0 || (currentIndex == n-1))     // already read the last record
            return null;
     
        Node messageNode = (Element) messageNodes.item(++currentIndex);
        currentMessage = (Element) messageNode;
        String messageId = currentMessage.getAttribute("id");
        return messageId;           // "" if no id in message; null if no more record
    }

    // Get the field values of the last read record
    public HashMap <String, String> getMessageData(String[] fieldNames)
    {
        HashMap<String, String> msgData = new HashMap();
        // descend the message element and the the text value associated with each child node
        for (int i = 0; i < fieldNames.length; i++)
        {
            String field = fieldNames[i];
            String value = "";
            Element parentNode = currentMessage;
     /*       String[] hierarchy = field.trim().split("\\.");
            int nl  = hierarchy.length;
            for (int j = 1; j < nl; j++)
            {
                String chfield = hierarchy[j-1];
                NodeList childNodes = parentNode.getElementsByTagName(chfield);
                if (childNodes != null && childNodes.getLength() > 0)
                    parentNode = (Element) childNodes.item(0);
            }*/
            NodeList childNodes = parentNode.getElementsByTagName(field);
            if (childNodes != null && childNodes.getLength() > 0)
                value = childNodes.item(0).getTextContent();  
            msgData.put(field, value);
        }
        return msgData;
    }
        
//--------------------------------------------------------------------------------------------------------*/        
// TBD: write the record to the file 
    
    public static void main (String[] args)
    {
        String msgFileName = "C:/DevWork/LPF/EmailProc/testData/groundTruth/sets/AllMessages_Set1.xml";
        String[]  fieldNames = {"subject", "body", "name", "healthStatus", "gender", "age", "location"};
        MessageSetIO msgIO = new MessageSetIO(msgFileName, true);
        int n = 1;
        while(true)
        {
            String msgId = msgIO.readNextMessage();
            if (msgId == null)
                break;
            HashMap <String, String> fieldData = msgIO.getMessageData(fieldNames);
           System.out.println(n + ":: Id  "+  msgId +" - " + fieldData.toString() +"\n");
           n++;
        }
    }
    
  
}

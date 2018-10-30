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
package gov.nih.nlm.lpf.emails.xml;

// import XMLUtil.java

import  gov.nih.nlm.lpf.emails.nlpproc.structure.ReportedPersonRecord;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class ResultWriter
{
    
    private static Logger log = Logger.getLogger(ResultWriter.class);
    
    Document outputDoc;
    Element topNode;
    
    int status;
    
    
    public ResultWriter ()
    {
        // Read the XML template for output  into a memory Document
        status = initDocumentForWrite();
        
    }
   
    // open the file in write mode, if already exists change to _old
    protected int initDocumentForWrite()
    {  
        outputDoc =  XMLUtil.createDocument("ExtractionResults");
         if (outputDoc == null)
             log.error("Could not initialize XML Document  for writing output.");
         topNode = outputDoc.getDocumentElement();
         return (topNode == null ? 0 : 1);
    }
    
    /*----------------------------------------------------------------------------------------*/
    public  int addRecord(String id, ReportedPersonRecord record)
    {
        if (status != 1)
            return 0;
        
        String[][] valueSpecs = new String[][] {
            {"name",  record.personName},
            {"healthStatus", record.reportedStatus},
            {"ageGroup", record.age},
            {"gender", record.gender},
            {"location", record.location}
        };
        
        // TBD: for multiple records with the same ID (i.e, from one email)
        // find the person node matching the given id attribute.
        // If exists,don't create it, simple add the new <person> recird
     
        Element messageNode = XMLUtil.createChildElement(outputDoc, topNode, "m");
        XMLUtil.addAttribute(messageNode, "id", id);
        Element personNode = XMLUtil.createChildElement(outputDoc,messageNode, "Person");

        
        // set the child nodes with the corresponding values;
        for (int i = 0; i < valueSpecs.length; i++)
        {
            String value = (valueSpecs[i][1] == null )? "" :  valueSpecs[i][1];
            XMLUtil.setChildValue(personNode, valueSpecs[i][0], value );
             String personStr = XMLUtil.node2String(personNode);
             System.out.println(personStr);
        } 

        return 1;
    }
    
    /*********************************************************************************/
    // Write the output to a file. If the name already exists, rename it to _old
    // and create the new file
    /*********************************************************************************/
    public int writeOutputToXMLFile(String filePath)
    {
        File file = new File(filePath);
        if (file.exists())
        {
            String fileName = file.getAbsolutePath();
            String newName = fileName.replace(".xml", "_old.xml");
            file.renameTo(new File(newName));
        }
        int status = XMLUtil.writeDocumentToXMLFile(outputDoc, filePath);
        return status;
    }
}

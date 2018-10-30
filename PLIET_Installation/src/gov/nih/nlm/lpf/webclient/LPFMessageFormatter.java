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
package gov.nih.nlm.lpf.webclient;


import gov.nih.nlm.lpf.webclient.util.ImageUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


import org.apache.log4j.Logger;
import gov.nih.nlm.lpf.xmlUtil.XMLUtil;

import java.util.HashMap;

/**
 * @author 
 */
public class LPFMessageFormatter
{
    private static Logger log = Logger.getLogger(LPFMessageFormatter.class);
    
    // PL codes for gender
    private static String[][] genders = { 
        {"male", "mal"}, {"female", "fml"}, {"unknown", "unk"}
    };
    
    // PL codes for health status
    private static String[][] healthStatus = { 
        {"alive", "ali"}, {"alive and well", "ali"}, {"found", "fou"}, {"missing", "mis"},
        {"injured", "inj"}, {"deceased", "dec"}, {"unknown", "unk"}
    };
    
    protected static  Document  personDoc = null;
    
    public LPFMessageFormatter(String plTemplate)
    {
           // Read the template each time to initialize the document
            personDoc   = XMLUtil.readXMLFile(plTemplate);
    };
    
    public  String formatMessage(HashMap<String, String>personInfo)
    { 
        String eventShortName =  personInfo.get("eventShortName");
        if (eventShortName == null)
            eventShortName = "dmtest";
         setNodeValue("eventShortname", eventShortName);
            
        // set the value of each element from ReportedPerson object
        String givenName = personInfo.get("firstName");
        String familyName = personInfo.get("lastName");
        
        setNodeValue("givenName", givenName);
        setNodeValue("familyName", familyName);
       
        String age = personInfo.get("age");
        if (age == null || age.length() == 0 || age.equalsIgnoreCase("unknown"))
           ;
        else
            setNodeValue("estimatedAge", age);
        String genderVal = personInfo.get("gender");
        String gender = "unk";      // default:  unknown
        for (int i = 0; i < genders.length;  i++)
        {
            if (genderVal.equalsIgnoreCase(genders[i][0]))
            {
                gender = genders[i][1];
                break;
            }
        }
        setNodeValue("gender", gender);
        setNodeValue("city", personInfo.get("city"));
        setNodeValue("region", personInfo.get("region"));
        setNodeValue("country", personInfo.get("country"));
        
        String lat = personInfo.get("lat");
        if (lat == null || lat.length() == 0)        // use the default lat 0.0
            lat = "0.0";
        setNodeValue("lat", lat);     
        String lng = personInfo.get("lng");
        if (lng == null || lng.length() == 0)        // use the default long 0.0
            lng = "0.0";
        setNodeValue("lon", lng);    // replace the default lng  0.0
        
        setNodeValue("note", personInfo.get("messageText"));
          
        //set the healthStatus
        String status = "";
        String statusVal = personInfo.get("reportedStatus");
        if (statusVal == null || statusVal.length() == 0 || statusVal.equalsIgnoreCase("unknown"))
            status = "unk" ;
        else
        {
            for (int i = 0; i < healthStatus.length && status.length() == 0; i++)
            {
                if (statusVal.equalsIgnoreCase(healthStatus[i][0]))
                    status =healthStatus [i][1];
            }
        }
         setNodeValue("status", status);
       
         // check if Photos are attached. If yes, read and convert the image data for tranport
         String photos = personInfo.get("photos");
         if ( photos != null && photos.length() > 0)
             addPhotoData(personInfo.get("photos"));
        
        // now convert it to string 
        String reportPersonStr = XMLUtil.convertDocumentToString(personDoc);
        XMLUtil.writeDocumentToXMLFile(personDoc, "c:/tmp/imageSend.xml");
        return reportPersonStr;
    }
    /*---------------------------------------------------------------------------------------------------------------------*/
    // Add the image data for each specified photo in base64encoded form to the output document
    // note: Photos node value is a concatenated set of image file names, separated by ";"
    protected int addPhotoData(String photoFiles)
    {
        String[]  photoFilenames = photoFiles.split(";");  
        int np = 0;     // number of photos successfully added
        boolean isPrimary = true;
        for (int i = 0; i < photoFilenames.length; i++)
        {
            isPrimary = (np == 0);          // only the first good one is the primary
           int status =  formatImageData(photoFilenames[i], isPrimary);
           if (status == 1)   np++;
        }
        return np;
    };

    /*---------------------------------------------------------------------------------------------------------------------*/
    protected int formatImageData( String photoFilename, boolean isPrimary)
    {
        String extension = "";
        int i = photoFilename.lastIndexOf('.');
        if (i >= 0)
            extension = photoFilename.substring(i+1);
        else
        {
            log.error ("Cannot transfer photo " + photoFilename + " of unknown image type");
            return 0;
        }
        
        String imageType = extension;
       // String encodedImageData = ImageUtils.encodeFileToString(photoFilename, imageType);
        
        HashMap imageInfo = ImageUtils.getImageData(photoFilename, imageType);
        String encodedImageData = (String) imageInfo.get("base64data");
        Integer height = (Integer) imageInfo.get("height");
        Integer width = (Integer) imageInfo.get("width");
        
        // Set the data in the XML document 
        Element photonode = (Element) personDoc.getElementsByTagName("photos").item(0);
        Element photo;
        Element data;
        Element dataDesc;
        Element x ;
        Element y;
        Element w;
        Element h;
        Element text;
        
        if (isPrimary)
        {
            photo = (Element)photonode.getElementsByTagName("photo").item(0);
            Element primary =  (Element)photo.getElementsByTagName("primary").item(0);
            XMLUtil.setTextValue(personDoc, primary, "1", true);
            
            data =  (Element)photo.getElementsByTagName("data").item(0);
            dataDesc =  (Element)photo.getElementsByTagName("dataDesc").item(0);
            Element tags =  (Element)photo.getElementsByTagName("tags").item(0);
            Element tag = (Element)tags.getElementsByTagName("tag").item(0);
            x = (Element)tag.getElementsByTagName("x").item(0);
            y = (Element)tag.getElementsByTagName("y").item(0);
            w = (Element)tag.getElementsByTagName("w").item(0);
            h = (Element)tag.getElementsByTagName("h").item(0);
        }       
        else
        {
            photo =  XMLUtil.createChildElement(personDoc, photonode, "photo");
            data =  XMLUtil.createChildElement(personDoc, photo, "data");
            dataDesc =  XMLUtil.createChildElement(personDoc, photo,"dataDesc");
            Element tags = XMLUtil.createChildElement(personDoc, photo, "tags");
            Element tag = XMLUtil.createChildElement(personDoc, tags, "tag");
            x =  XMLUtil.createChildElement(personDoc, tag, "x");
            y = XMLUtil.createChildElement(personDoc, tag, "y");
            w = XMLUtil.createChildElement(personDoc, tag, "w");
            h = XMLUtil.createChildElement(personDoc, tag, "h");
            text = XMLUtil.createChildElement(personDoc, tag, "text");
    }
    XMLUtil.setTextValue(personDoc, data, encodedImageData, true);   
    XMLUtil.setTextValue(personDoc, dataDesc, "Photo of Person", true);  
    XMLUtil.setTextValue(personDoc, x, "0", true);
    XMLUtil.setTextValue(personDoc, y, "0", true);
    XMLUtil.setTextValue(personDoc, w, width.toString(),  true);
    XMLUtil.setTextValue(personDoc, h, height.toString(),  true);
    return 1;
}

    /*---------------------------------------------------------------------------------------------------------------------*/
    protected  int setNodeValue(String nodeName, String nodeValue)  
    {
        int status = 0;
        if (nodeValue == null) nodeValue = "";
        try
        {
            Element namedNode = (Element) personDoc.getElementsByTagName(nodeName).item(0);
            XMLUtil.setTextValue(personDoc, namedNode, nodeValue, true);
            status = 1;
        }
        catch (Exception e)
        {
            log.error("Could not set value of Node " + nodeName + " to " + nodeValue);
        }
        return status;
    }
}

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
package gov.nih.nlm.lpf.webclient.ttclient;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import gov.nih.nlm.lpf.webclient.util.ImageUtils;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;
import java.io.File;

import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * @author 
 */
public class TTReportPersonFormatter
{
    private static Logger log = Logger.getLogger(TTReportPersonFormatter.class);
    
    // PL codes for gender
    private static String[][] genders = { 
        {"male", "M"}, {"female", "F"}, {"unknown", "U"}
    };
    
    protected static  JSONObject  personObject  = null;
    private boolean debugging = true;;
    
    public TTReportPersonFormatter(String tpPersonTemplate)
    {
        try
        {
            // Read the template each time to initialize the document
            FileReader fileReader = new FileReader(tpPersonTemplate);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(fileReader);
            personObject = (JSONObject) obj;
            return;
        }        
        catch (FileNotFoundException e) {
            log.error("Template " + tpPersonTemplate + " not found.", e) ;
        }  catch (IOException e)  {
             log.error("Error reading template " + tpPersonTemplate, e) ;  
        }  catch (ParseException e) {
            log.error("Error parsing data in  template " + tpPersonTemplate );
        }
       personObject = null;
       return;
 };
/*------------------------------------------------------------------------------------------------------
*set the value of each element from the input data  in the outgoing JSON object
*------------------------------------------------------------------------------------------------------*/
    public  String formatMessage( String  patientId, Integer hospitalUuid,
                    HashMap<String, String>personInfo)
    { 
        if (personObject == null)
            return null;
            
        // add hospital info
        personObject.put("patientId", patientId);
        personObject.put("hospitalUUID", hospitalUuid);
        personObject.put("zone", "Unknown");
   
        // add name
        String givenName = personInfo.get("firstName");
        String familyName = personInfo.get("lastName");
        personObject.put("givenName", givenName);
        personObject.put("familyName", familyName);
        
        // add gender
        String genderVal = personInfo.get("gender");
        String gender = "U";      // default:  unknown
        for (int i = 0; i < genders.length;  i++)
        {
            if (genderVal.equalsIgnoreCase(genders[i][0]))
            {
                gender = genders[i][1];
                break;
            }
        }
        personObject.put("gender", gender);
        
        // add age info
        String age = personInfo.get("age");
        if (age == null || age.length() == 0 || age.equalsIgnoreCase("unknown"))
            personObject.remove("ped");
             //personObject.put("ped", false);                // default to adult
        else
        {
            String ageValue = age.replaceAll("[^0-9]", "");
            int ageNum = Integer.parseInt(ageValue);
            boolean  isChild = (ageNum < 18) ? true : false;
            personObject.put("ped", isChild);
            personObject.put("age", ageNum);
        }
        
        // add the Sender, Date, Subject and Body of the email as the comment
         // Since the message would be in XML format, escape any XML characters in the message
        String commentStr = personInfo.get("emailData");
       //  commentStr = XMLUtil.escapeXMSLtring(commentStr);
        personObject.put("comment", commentStr);
        
        // add location info
       JSONObject  location = (JSONObject )personObject.get("location");
        location.put("city", personInfo.get("city"));
        location.put("region", personInfo.get("region"));
        location.put("country", personInfo.get("country"));
       //
        location.put("street1", "");
        location.put("street2", "");
        location.put("neighborhood", "");
        location.put("postalCode", "");
        
        JSONObject  gpsObject = (JSONObject) location.get("gps");
        String lat = personInfo.get("lat");
        Float latitude =  new Float(0.0);
        if (lat != null &&  lat.length() > 0)        // use the default lat 0.0
            latitude = Float.valueOf(lat);
        
        Float longitude = new Float (0.0); 
        String lng = personInfo.get("lng");
        if (lng != null &&  lng.length() > 0)        // use the default long 0.0
           longitude = Float.valueOf(lng); 
        
        gpsObject.put("latitude", latitude);     
        gpsObject.put("longitude", longitude);    // replace the default lng  0.0
        
         // check if Photos are attached. If yes, read and convert the image data for tranport
         String photos = personInfo.get("photos");
         if ( photos != null && photos.length() > 0)
         {
             int np = addPhotoData(personInfo.get("photos"));
             log.info ("Number of photos added to message = " + np);
         }
         else
             addPhotoData(null);
       
        // now convert the JSON object to string 
        String reportPersonStr = personObject.toString();
        // store locally for debug
        if (debugging)
        {
            try {
                FileWriter writer =  new FileWriter("../tmp/tpimageSend.json");
                writer.write(reportPersonStr);
                writer.close();
            } catch (Exception e) {
                log.error("Error in saving outgoing JSON info in file " , e);
            }
        }
        return reportPersonStr;
    }
    /*---------------------------------------------------------------------------------------------------------------------*/
    // Add the image data for each specified photo in base64encoded form to the output document
    // note: Photos node value is a concatenated set of image file names, separated by ";"
    protected int addPhotoData(String photoFiles)
    {
        if (photoFiles ==null)
        {
            personObject.remove("addImage");
            return 0;
        }
        
        String[]  photoFilenames = photoFiles.split(";");  
        int np = 0;     // number of photos successfully added
        for (int i = 0; i < photoFilenames.length; i++)
        {
           int status =  formatImageData(i, photoFilenames[i]);
           if (status == 1)   np++;
        }
        return np;
    };

    /*---------------------------------------------------------------------------------------------------------------------*/
    protected int formatImageData( int photoNum, String photoFilename)
    {
        JSONArray photoNodes = ( JSONArray ) personObject.get("addImage");
        JSONObject primaryPhoto = (JSONObject)(photoNodes.get(0));
        boolean isPrimary = (photoNum == 0);
        String imageName = (new File(photoFilename)).getName();
        
        // Set the data in the JSON object
         JSONObject photoNode;
         if (photoNum ==  0)
             photoNode = primaryPhoto;
         else
         {
              photoNode = (JSONObject) primaryPhoto.clone();
              photoNodes.add(photoNum, photoNode);
         }

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
        
        // Set the data in the JSON object
        photoNode.put("primary", isPrimary);
        photoNode.put("data", encodedImageData);
        JSONArray tagsArray = (JSONArray) photoNode.get("tags");
        JSONObject tagObject = (JSONObject) tagsArray.get(0);
        tagObject.put("x", new Integer(0));
        tagObject.put("y", new Integer(0));
        tagObject.put("w", width);
        tagObject.put("h", height);
        tagObject.put("text", imageName);
        
        return 1;
    }
}

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
package gov.nih.nlm.lpf.plapp;

/**
 *
 * @author 
 */

import gov.nih.nlm.lpf.emails.util.Utils;
import gov.nih.nlm.lpf.webclient.PLUSEmailClient;
import gov.nih.nlm.lpf.webclient.plclient.PLEmailClient;
import gov.nih.nlm.lpf.webclient.ttclient.TTEmailClient;
import gov.nih.nlm.lpf.webclient.LPFEvent;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class PLUSServiceHandler
{
    private Logger log = Logger.getLogger(PLUSServiceHandler.class);
    Properties ctxProperties;
    PLUSEmailClient plusClient;
     String  plusRecordTemplate;            // template of reportRecord to be sent to the server
    
    public PLUSServiceHandler(Properties contextProperties)
    {
        ctxProperties = contextProperties;
        plusRecordTemplate =  Utils.getDereferencedProperty(ctxProperties,"PlusTemplate");
    }
    
    public int checkAuthorization()
    {
        // get user name and password for connecting to the PL Reunite Server
        String serviceName = ctxProperties.getProperty("PlusService");
        String plClientUserName = ctxProperties.getProperty("PlusUsername");
        String plClientPassword = ctxProperties.getProperty("PlusPassword");
        String serviceUrl = ctxProperties.getProperty("PlusUrl");
        String targetNS = ctxProperties.getProperty("PlusTargetNS");
        
         plusClient = createClient(serviceName, serviceUrl,  targetNS, plClientUserName,plClientPassword);
          
          // pings to check if the server is running now and check correctness of username/password
          int status = plusClient.init();  
           if (status == -1)
                log.error("Could not connect  PLIET to  PLUS Web service" );
           else if (status == -2)
                log.error("User Authorization failed for PLIET connection to PLUS service" );
           else if (status == -3)
               log.error("Invalid hospital infomation specified for the TriagePic  PLUS service" );
           return (status == 1 ? 1 : -1);
    }
  
    //---------------------------------------------------------------------------------------------------------
    // Instantiate the Web client depending upon the Service (PL or TP) required
    
    protected PLUSEmailClient createClient(String serviceName, String serviceUrl,  String targetNS, 
                        String clientUserName, String clientPassword)
    {
        if (serviceName.equalsIgnoreCase("PeopleLocator"))
        {
            PLEmailClient plClient = new PLEmailClient( serviceUrl, targetNS, clientUserName, clientPassword);
            plusClient = (PLUSEmailClient)plClient;
        }
        else if  (serviceName.equalsIgnoreCase("TriageTrak"))
        {
            TTEmailClient tpClient = new TTEmailClient( serviceUrl, targetNS, clientUserName, clientPassword);
            
            // if non-default hospital and zone, set it now. (Note:  Either none or both must be spefied.)
            String hospitalName = ctxProperties.getProperty("hospitalShortName");
            String zoneName = ctxProperties.getProperty("enquiryZoneName");
            if (hospitalName != null && zoneName != null)
                tpClient.setEnquiryInfo(hospitalName, zoneName);
            
            plusClient = (PLUSEmailClient)tpClient;
        }
        else
        {
            log.error ("Unknown PLUS Web service name "+ serviceName +"specified in the configuration file.");
            plusClient = null;
        }
        return plusClient;
    }
 /*------------------------------------------------------------------------------------------------------------*/   
 //   Get the list of events from the server, as formatted LPFEvent Structures
 /*------------------------------------------------------------------------------------------------------------*/
    public ArrayList<HashMap<String, String>> getOpenEventInfo()
    {
       ArrayList<LPFEvent> eventList =  plusClient.getEventsAsList();
       ArrayList<HashMap<String, String>> openEventInfo = new ArrayList();
       if (eventList == null || eventList.isEmpty())
           return null;         // nothing established yet.)
       for (int i = 0; i < eventList.size(); i++)
       {
           if (eventList.get(i).isOpen())
            openEventInfo.add(eventList.get(i).getInfo());
       }
       return openEventInfo;
    }   
 /*------------------------------------------------------------------------------------------------------------*/   
 //   Get the list of events from the server, as formatted LPFEvent Structures
 /*------------------------------------------------------------------------------------------------------------*/
    public ArrayList<LPFEvent> getEventList()
    {
        return plusClient.getEventsAsList();
    }
    /*----------------------------------------------------------------------------------------------------------*/
    // Return the list of open events for which messages are being sent from the public
    //-----------------------------------------------------------------------------------------------------------*/
    public  ArrayList<LPFEvent> getOpenEvents()
    {
        return plusClient.getOpenEvents();
    }
    /*----------------------------------------------------------------------------------------------------------*/
    // Return the current event for which messages are being sent from the public
    //-----------------------------------------------------------------------------------------------------------*/
    public LPFEvent getCurrentEvent()
    {
        return plusClient.getLatestEvent();
    }
    /*------------------------------------------------------------------------------------------------------------*/
    // Send a ReoprtedPerson record in XML format to the PL ReUnite server and
    // get the returned uuid
    // Note: Certain email contents are also added to the sent info
    /*------------------------------------------------------------------------------------------------------------*/
    public String reportRecordToServer(HashMap<String, String> reportedPersonInfo, 
                HashMap<String, String>messageContents, String eventName)
    {
        // email field data that should be sent to the  server by this client
        String[] emailFields = plusClient.getEmailFieldNames();
        String contentStr = "";         // convert to a String with separate line for each field
        Iterator<String> it = messageContents.keySet().iterator();
        int i = 0;
        while (it.hasNext())
        {
            String key = it.next();
            if (i > 0) contentStr += "\n\r";
            String field = key.equals("Body") ? "Message" : key;
            contentStr = contentStr + field + ":  " + messageContents.get(key);
            i++;
        }
        // set in the info map
        reportedPersonInfo.put("emailData", contentStr);
        
        // sent to server
        String record_uuid = plusClient.reportPerson (plusRecordTemplate, reportedPersonInfo, eventName);
        
        
        return record_uuid;
    }
}

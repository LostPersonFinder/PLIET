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

import gov.nih.nlm.lpf.webclient.wsdl.SoapRequestHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;

import javax.xml.soap.SOAPMessage;


import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public abstract class PLUSEmailClient implements PLUSServiceNames
{
    private static Logger log = Logger.getLogger(PLUSEmailClient.class);

    protected String Username;
    protected String Password;
    protected String serviceURL;                             //  e.g. "https://plstage.nlm.nih.gov/:443/?wsdl&api=33";
    protected String targetNamespace;                 //"soap/plusWebServices" for generating QNames
    
    protected SoapRequestHandler soapHandler;
    protected int authStatus;
    
    protected String UserToken = null;                // initialize, to be returned by the server
    boolean reportPersonDebugging = true;
    //------------------------------------------------------------------------------------------
    public static  BigInteger SUCCESS = new BigInteger("0");
    
    public PLUSEmailClient(String wsURL, String tns, String clientName, String clientPw)
   {
        serviceURL = wsURL;         // URL for comuter and 
        targetNamespace= tns;
        
        Username = clientName;
        Password = clientPw;
        
    }
   //---------------------------------------------------------------------------------------/ 
    // initializate  communication with the server by pinging it and 
    // validating authorization through username/password (or assigned token(
    // Save token for  use in follow up calls
    //---------------------------------------------------------------------------------------/
    public  int init()
    {
       try
       {
            soapHandler = new SoapRequestHandler(serviceURL, targetNamespace);
            if  (checkConnection() != 1)            // => server  running, but no user token supplied in parameters
               return -1;
             // Check if the user is properly authorized and get the token
              if (checkUserAuthorization() != 1)
                return -1;
             else 
              return 1;
        }
       catch(Exception e)
       {
           log.error("Could not connect to the " + serviceURL + "  Web Service",e);
           return -1;
       }
   } 
  //---------------------------------------------------------------------------------------/  
             
    // Ping the server to make sure that it is on-line
    public  int   checkConnection()
    {
         LinkedHashMap <String, String> params = new LinkedHashMap();
        params.put("token", "");
        SOAPMessage  pingResponse  = soapHandler.sendSoapRequest(_PingEcho, params);
         if (pingResponse == null)      // server not alive
             return -1;
         
      //  We expect to get an error  since no token is provided  
       HashMap errorMap = soapHandler.decodeSoapResponse(pingResponse, _PingEchoResponse);
       String value = (String) errorMap.get("errorCode") ;
       if (value != null && value.equals("1"))
             return 1;
       return -2;           // some other error
    }
    
    //--------------------------------------------------------------------------------------------------------------
    // Verify that this is an authorized user and retrieve the token allocated to this client
    //---------------------------------------------------------------------------------------------------------------
    public int checkUserAuthorization()
    {
        LinkedHashMap <String, String> authParams = new LinkedHashMap();
        authParams.put("username", Username);
        authParams.put("password", Password);
        
         SOAPMessage  authResponse  = soapHandler.sendSoapRequest(_RequestUserToken, authParams);
         if (authResponse == null)
             return -1;
         
        HashMap<String, String> retData = soapHandler.decodeSoapResponse(
            authResponse, _RequestUserTokenResponse);
        if (retData == null || retData.isEmpty())
            return -1;

        long lval = Long.parseLong( retData.get("errorCode"));
       
        BigInteger statusCode = BigInteger.valueOf(lval);
        if (!statusCode.equals(SUCCESS))
            {
                String errorMessage = retData.get("errorMessage");

                log.error("Error in Authorization of  Email Client with PLUS server . -->" + errorMessage);
                return -1;                // uuid of reported person record;
            } 
            UserToken = retData.get("token");
            log.info("Authorization of  Email Client with PLUS server is successful, Token: " + UserToken);
            return 1;
    }
    
    /*-----------------------------------------------------------------------------------------------------------------------*/
    public String getEventList()
    {
         LinkedHashMap <String, String> serviceParams = new LinkedHashMap();
         serviceParams.put("token", UserToken);
        
         SOAPMessage  authResponse  = soapHandler.sendSoapRequest(_GetEventList, serviceParams);
         if (authResponse == null)
             return null;
         
        HashMap<String, String> retData = soapHandler.decodeSoapResponse(authResponse, _GetEventListResponse);
        if (retData == null || retData.isEmpty())
            return "";

        long lval = Long.parseLong( retData.get("errorCode"));
       
        BigInteger statusCode = BigInteger.valueOf(lval);
        if (!statusCode.equals(SUCCESS))
        {
            String errorMessage = retData.get("errorMessage");

            log.error("Error in getting Event list. -->" + errorMessage);
            return null;                // uuid of reported person record;
        } 
        log.info("Reception of Event list from PL server is successful");
        String eventList = retData.get("eventList");
        return eventList;
    }
    
/*---------------------------------------------------------------------------------------------------------------*/
    // Get the list of PL Events from the Web Service
    //
    public  ArrayList<LPFEvent> getEventsAsList()
    {
        String eventStr = getEventList();
        if (eventStr == null)
            return null;                // error message already out
        // System.out.println(eventStr);
        ArrayList<LPFEvent> eventList = parseEventListString(eventStr);
        return eventList;
    }

 /****************************************************************/
   protected ArrayList<LPFEvent>  parseEventListString(String eventStr)     
   {
       //remove enclosing brackets [ ...]
       eventStr = eventStr.replaceAll("^(\\[)", "").replaceAll("$(\\])", "").trim();
       // split each segment on event boundary "}"
       String[] events = eventStr.split("\\},");
       //remove the leading "{"
       for (int i = 0; i < events.length; i++)
           events[i] = events[i].replaceAll("^(\\{)", "");
       
       // now split each component to build a LPFEvent  with "," as field separator
       ArrayList<LPFEvent> eventObjs = new ArrayList();
       for (int i = 0; i < events.length; i++)
       {
           LPFEvent event = parseEventFields(events[i]);
           if (event != null)
               eventObjs.add(event);
       }
       return eventObjs;
   }
   /*---------------------------------------------------------------------------------------------------------------*/
   protected LPFEvent  parseEventFields(String eventLine)
   {
       //Example "incident_id":"1","parent_id":null,"name":"Test Exercise","shortname":"test","date":"2000-01-01",
       //                 "type":"TEST","latitude":"0","longitude":"0","street":"","group":null,"closed":"0"},
       String[] segments = eventLine.split(":");
       int n = segments.length;
       int nf = n-1;
       String[] names = new String[nf];
       String[] values = new String[nf];
       
       names[0]  = segments[0];
        for (int i = 1; i < nf; i++)
        {
            int index = segments[i].lastIndexOf(",");
            values[i-1] = segments[i].substring(0, index);
            names[i] = segments[i].substring(index+1);
        }
        values[nf-1] = segments[nf];
   
        LPFEvent pev = new LPFEvent();
        // assign values to each field
       for (int i = 0; i < nf; i++)
       {
           String name = names[i].replaceAll("\"", "");
           String value = values[i].replaceAll("\"", "");
           if (value.length() == 0 || value.equals("null"))
               continue;
          
           // now check the component name and add to the Object
           if (name.equals("incident_id"))
               pev.incident_id = Integer.parseInt(value);
           else if (name.equals("parent_id") )
               pev.parent_id = Integer.parseInt(value);
           else if (name.equals("name"))
               pev.name = value;
           else if (name.equals("shortname"))
               pev.shortname = value;
           else if (name.equals("date"))
               pev.date = value;
           else if (name.equals("type"))
               pev.type = value;
           else if (name.equals("latitude"))
               pev.latitude = Double.parseDouble(value);
          else if (name.equals("longitude"))
               pev.longitude =  Double.parseDouble(value);
           else if (name.equals("street"))   
               pev.street = value;
           else if (name.equals("group"))   
                pev.group = value;
           else if (name.equals("closed"))   
                pev.closed= Boolean.parseBoolean(value);

              // tbd: other fields 
       }
      return pev;
   }
   
   /*------------------------------------------------------------------------------------------------------------------------*/
   // Get the latest/open  event(s) for which emails are  expected to be received
   /*------------------------------------------------------------------------------------------------------------------------*/
   public ArrayList< LPFEvent>  getOpenEvents()
   {
        ArrayList<LPFEvent> allEvents = getEventsAsList();
        ArrayList<LPFEvent> openEvents = new ArrayList();
        for (LPFEvent event : allEvents)
        {
            if (event.isOpen())
                openEvents.add(event);
        }
        return openEvents;
   }
   
/*------------------------------------------------------------------------------------------------------------------------*/
// Get the current (latest) event for which emails are  expected to be received
// If more than one event is open, get the latest one
/*------------------------------------------------------------------------------------------------------------------------*/
   public LPFEvent   getLatestEvent()
   {
        ArrayList<LPFEvent> openEvents = getOpenEvents();
        if (openEvents == null)
                return null;
        else if (openEvents.size() == 1)
            return openEvents.get(0);
        else
         {
             LPFEvent latestEvent = openEvents.get(0);
             String dateStr1 =  latestEvent.date;
             Calendar cal1 = convertDateStr2Calendar(dateStr1);

             for (int i = 1; i < openEvents.size(); i++)
             {
                 LPFEvent nextEvent = openEvents.get(i);
                 String dateStr2 =  nextEvent.date;
                 Calendar cal2 = convertDateStr2Calendar(dateStr2);
                 // compare the two
                 if (cal2.after(cal1))
                     latestEvent = nextEvent;
             }
             return latestEvent;
         }
   }
   /*-----------------------------------------------------------------------------------------------*/
   //Convert date in the form of "yyyy-mm-dd" to Calendar
   protected Calendar convertDateStr2Calendar(String dateStr)
   {
         String[] dateComps = dateStr.split("-");           // in format as  2012-04-17
        int year = Integer.parseInt(dateComps[0]);
        int month = dateComps.length > 1 ? Integer.parseInt(dateComps[1]) : 1;     // default: Jan
        int day = dateComps.length > 2 ?  Integer.parseInt(dateComps[2]) : 1;
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return cal;
   }
   /*--------------------------------------------------------------------------------------------*/
   // abstract methods : to be implemented by the actual service client
   // email fields that need to be passed to the server
    public  abstract String[]  getEmailFieldNames();
   
    public  abstract String  reportPerson(String reportTemplate, HashMap <String, String>personInfo, 
        String defaultEvent);
 
    
    /*------------------------------------------------------------------------------------------------------------------------
      public static void main(String[] args)
    {
        //defaults
        String WS =  "PL";
        String targetNamespace = "";
        String serviceUrl; 
        String user;
        String pw;
        
        if (WS.equals("PL"))
        {
            serviceUrl = "https://plstage.nlm.nih.gov/?wsdl&api=33";
            targetNamespace = "soap/plusWebServices";
            user = "disasterN1";
            pw = "password$1";
            
            PLEmailClient plClient = new PLEmailClient( serviceUrl, targetNamespace, user, pw);
        }
        else
        {
            serviceUrl = "https://triagetrakstage.nlm.nih.gov/?wsdl&api=33";
            user = "disasterN2";
            pw = "password$2";
        }
       
         DefaultLogger.enableInfoLogging();
        try
        {
            PLEmailClient plClient = new PLEmailClient( serviceUrl, targetNamespace, user, pw);
            int status = plClient.init();
            if (status < 0 )
            {
                log.error("Error in initializing client for PL Email client" );
                System.exit(-1);
            }
            
            // get event list
           plClient.getEventList();
            
               // send the Record for a person
           
            String template = "C:/DevWork/LPF/EmailProc/config/plus/templates/ReportPersonWithPhotosTemplate.xml";
            String commentStr = "This is Disaster-N2 email client interface test.";
            
            ReportedPersonRecord  record = new ReportedPersonRecord();
            record.eventShortname = "test";
            record.firstName = "Laura";
            record.lastName = "Wilson";
            record.gender = "female";
            record.age="15";
            record.address = "";
            record.city = "Rockville";
            record.region = "Maryland";
            record.country= "USA";
            record.reportedStatus = "";
            record.messageText = "Laura Wilson. I am in London, looking for a  girl named Laura Wilson. "
                + "She was last seen in Rockville. She is 15 years old.";
            
            HashMap personInfo = record.geFieldMap(); 
            
            String photos="C:/DevWork/LPF/EmailProc/config/resource/LauraWilson1.gif";
            personInfo.put("photos", photos);

            // send record to PLUS server
            plClient.reportPerson(template, personInfo, commentStr, "test");
        } 
        catch(Exception e)
        {
            log.error("Error in sending message to server from PL Email client", e );
            System.exit(-1);
        }
      }
 */ 
}

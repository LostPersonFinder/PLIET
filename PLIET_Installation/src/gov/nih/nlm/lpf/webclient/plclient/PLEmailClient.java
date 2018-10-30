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
package gov.nih.nlm.lpf.webclient.plclient;

import gov.nih.nlm.lpf.webclient.PLUSEmailClient;

import gov.nih.nlm.lpf.emails.nlpproc.structure.ReportedPersonRecord;
import gov.nih.nlm.lpf.emails.util.DefaultLogger;
import java.util.HashMap;
import java.util.LinkedHashMap;

import java.math.BigInteger;
import javax.xml.soap.SOAPMessage;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class PLEmailClient extends PLUSEmailClient
{
    private static Logger log = Logger.getLogger(PLEmailClient.class);

  
    public PLEmailClient(String wsURL, String targetNamespace, String clientName, String clientPw)
   {
      super(wsURL, targetNamespace, clientName, clientPw);
    }
    
    public String[] getEmailFieldNames() 
    {
        return new String[] {"Subject", "Body"};
    }
   //---------------------------------------------------------------------------------------/ 
    // initializate  communication with the server by pinging it and 
    // validating authorization through username/password (or assigned token(
    // Save token for  use in follow up calls
    //---------------------------------------------------------------------------------------/
    public  int init()
    {
        return super.init();
   } 
    /*------------------------------------------------------------------------------------------------------------------------*/
    // Report a person with supplied information. Use the default event if no event is provided
    // in the person's info
    // Note: Email contents not used here
    /*------------------------------------------------------------------------------------------------------------------------*/
        public String  reportPerson(String reportTemplate, HashMap <String, String>personInfo, 
                    String defaultEvent)
        {
            String playloadFormat =  "REUNITE4";
            String event = personInfo.get("eventShortname");
            if (event == null || event.isEmpty())
            {
                event = defaultEvent;
                personInfo.put("eventShortname", event);
            }

            PLReportPersonFormatter  formatter = new PLReportPersonFormatter(reportTemplate);
            String  recordStream = formatter.formatMessage(personInfo);
           
             LinkedHashMap <String, String> serviceParams = new LinkedHashMap();
             serviceParams.put("token",  UserToken);
             serviceParams.put("payload", recordStream);
             serviceParams.put("payloadFormat", playloadFormat);
             serviceParams.put("shortname", event);           // must be the same as eventShortname in XML

             SOAPMessage  reportPersonResponse  = soapHandler.sendSoapRequest(   _ReportPerson, serviceParams);
             if (reportPersonResponse == null)
                 return "-1";

            HashMap<String, String> retData = soapHandler.decodeSoapResponse(
                reportPersonResponse, _ReportPersonResponse);  // "reportResponse");   
            if (retData == null || retData.isEmpty())
                return "-1";

        long lval = Long.parseLong( retData.get("errorCode"));
        BigInteger statusCode = BigInteger.valueOf(lval);
        if (!statusCode.equals(SUCCESS))
        {
            String errorMessage = retData.get("errorMessage");

            log.error("Error in submitting ReportPerson record. -->" + errorMessage);
            return "-1";                // uuid of reported person record;
        } 
        String uuid = retData.get("uuid");
        log.info("Transfer of ReportedPerson record is successful;  Person's UUID="+uuid);
         return uuid;
    }

/*--------------------------------------------------------------------------------------------------------*/
      public static void main(String[] args)
    {
        //defaults
        String WS =  "PL";
        String targetNamespace = "";
        String serviceUrl; 
        String user;
        String pw;
        
        if (!WS.equals("PL"))
            return;

        serviceUrl = "https://plstage.nlm.nih.gov/?wsdl&api=33";
        targetNamespace = "soap/plusWebServices";
        user = "disasterN1";
        pw = "password$1";

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
            String commentStr = "This is Disaster-1 email client interface test.";
            
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
            record.reportedStatus = "missing";
            record.emailData = "Subject:  Laura Wilson\n\rMessage:  I am in London, looking for a  girl named Laura Wilson. "
                + "She was last seen in Rockville. She is 15 years old.";
            
            HashMap personInfo = record.geFieldMap(); 
            
            String photos="C:/DevWork/LPF/EmailProc/config/resource/LauraWilson1.gif";
            personInfo.put("photos", photos);

            // send record to PLUS server
            plClient.reportPerson(template, personInfo, "test");
            
      /*      // continue to send request - a non-existing one
            plClient.reportAbuse("12345678", "reportAbuseTest");           // simply  for testing
           
            // a real one
            plClient.reportAbuse("12345678", "reportAbuse");           // simply  for testing
          */
        } 
        catch(Exception e)
        {
            log.error("Error in sending message to server from PL Email client", e );
            System.exit(-1);
        }
      }
}

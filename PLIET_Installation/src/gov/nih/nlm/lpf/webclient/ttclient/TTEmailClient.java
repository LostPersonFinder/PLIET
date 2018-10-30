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

import gov.nih.nlm.lpf.webclient.PLUSEmailClient;

import gov.nih.nlm.lpf.emails.nlpproc.structure.ReportedPersonRecord;
import gov.nih.nlm.lpf.emails.util.DefaultLogger;
import java.util.HashMap;
import java.util.LinkedHashMap;

import java.math.BigInteger;
import java.util.ArrayList;

import javax.xml.soap.SOAPMessage;


import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class TTEmailClient  extends PLUSEmailClient
{
    private static Logger log = Logger.getLogger(TTEmailClient.class);
    
    
    
    protected class HospitalInfo
    {
        Integer uuid;
        String npi;
        String name;
        String shortName;
        Float latitude;
        Float longitude;
    }
    
    // default values
    String hospitalName = "unknown";
    String enquiryZoneName = "Unknown";;
    
    Integer hospitalUuid = -1;

    public TTEmailClient(String wsURL, String tns, String clientName, String clientPw)
   {
        super( wsURL,  tns,  clientName,  clientPw);
    }
    
    public String[] getEmailFieldNames() 
    {
        return new String[] {"From", "Sent Date", "Subject", "Body"};
    }
    
    // override the defaults (from configutation data)
    public  void  setEnquiryInfo(String hospital, String zone)
    {
        hospitalName = hospital;
        enquiryZoneName = zone;
    }
        
      public  int init()
      {
          int status = super.init();
          if (status != 1)
              return status;            // -1 or -2
          hospitalUuid =  getHospitalUuid(hospitalName);
          return (hospitalUuid == null) ? -3 : 1;   
      }
   /*-----------------------------------------------------------------------------------------------------------------------
   * Get the uuid of the specified hospitals to which this message would be routed.
   *----------------------------------------------------------------------------------------------------------------------*/
    protected  Integer getHospitalUuid(String hospitalShortName)
    {
        String hospitalListStr =  getHospitalList();
        if (hospitalListStr == null)
            return null;
        
         ArrayList<HospitalInfo>  hospitalList  = getHospitalInfo( hospitalListStr);
         if (hospitalList == null)
             return null;
         Integer huuid = getHospitalUuid(hospitalList, hospitalShortName);
         if (huuid == null)
         {
             log.error("Invalid hospital shortname \" " + hospitalShortName + "\" provided" );
             return null;
         }
         return huuid;
    }
    
   /*-----------------------------------------------------------------------------------------------------------------------
   * Get the list of hospitals to which a  message may be routed.
   *----------------------------------------------------------------------------------------------------------------------*/
    public String getHospitalList()
    {
         LinkedHashMap <String, String> serviceParams = new LinkedHashMap();
         serviceParams.put("token", UserToken);
        
         SOAPMessage  authResponse  = soapHandler.sendSoapRequest(_GetHospitalList, serviceParams);
         if (authResponse == null)
             return null;
         
        HashMap<String, String> retData = soapHandler.decodeSoapResponse(authResponse, _GetHospitalListResponse);
        if (retData == null || retData.isEmpty())
            return "";

        long lval = Long.parseLong( retData.get("errorCode"));
       
        BigInteger statusCode = BigInteger.valueOf(lval);
        if (!statusCode.equals(SUCCESS))
        {
            String errorMessage = retData.get("errorMessage");

            log.error("Error in getting Hospital list. -->" + errorMessage);
            return null;                // uuid of reported person record;
        } 
        log.info("Reception of Hospital  list from PL server is successful");
        String hospitalList = retData.get("hospitalList");
        System.out.println("Hospital List\n" + hospitalList);
        return hospitalList;
    }
    /*-----------------------------------------------------------------------------------------------------*/
    protected ArrayList<HospitalInfo> getHospitalInfo(String hospitalListStr)
    {
        ArrayList <HospitalInfo> hospitalList = new ArrayList();
        JSONParser parser = new JSONParser();
        try
        {
            Object obj = parser.parse(hospitalListStr);
            JSONArray  hospitalInfos = (JSONArray) obj;
            for (int i = 0; i < hospitalInfos.size(); i++)
            {
                JSONObject  hobj = (JSONObject)hospitalInfos.get(i);
                HospitalInfo hinfo = new HospitalInfo();
                hinfo.name =  (String) hobj.get("name");
                hinfo.shortName = (String) hobj.get("shortname");
                hinfo.npi = (String)hobj.get("npi");
                hinfo.uuid =  new Integer((String)hobj.get("hospital_uuid"));
                hinfo.latitude = new Float((String) hobj.get("latitude"));
                hinfo.longitude = new Float((String) hobj.get("longitude"));
                hospitalList.add(hinfo);
            }
        }
        catch (Exception e)
        {
            log.error("Got JSON Exception in parsing Hospital List", e);
            return null;
        }
        return hospitalList;
    }
    
   /*-----------------------------------------------------------------------------------------------------------------------
   * Get the uuid of the specified hospitals to which this message would be routed.
   *----------------------------------------------------------------------------------------------------------------------*/
    protected Integer getHospitalUuid(ArrayList<HospitalInfo> hospitalList, String hospitalShortName)
    {
        for (int i = 0; i < hospitalList.size(); i++)
        {
            HospitalInfo hinfo = hospitalList.get(i);
            if (hinfo.shortName.equalsIgnoreCase(hospitalShortName))
                return hinfo.uuid;
        }
        return null;
    }

    /*------------------------------------------------------------------------------------------------------------------------*/
     // Report a person with supplied information. Use the default hospital if no hospital is provided
    // in the person's info
    // Note: emailContents are converted to a String form and added as comments in the
    // formatted record
    /*------------------------------------------------------------------------------------------------------------------------*/
    public String  reportPerson(String reportTemplate, HashMap <String, String>personInfo, 
               String defaultEvent)
    {
        if (hospitalUuid == -1)
              hospitalUuid =  getHospitalUuid(hospitalName);
        if (hospitalUuid == null)
            return  null;          // error return;
        
        String playloadFormat = "JSONPATIENT1";
        String event = personInfo.get("eventShortname");
        if (event == null || event.isEmpty())
        {
            event = defaultEvent;
            personInfo.put("eventShortname", event);
        }

        String patientId = "000-000000";
        TTReportPersonFormatter  formatter = new TTReportPersonFormatter(reportTemplate);
        String  recordStream = formatter.formatMessage(patientId, hospitalUuid, personInfo);

         LinkedHashMap <String, String> serviceParams = new LinkedHashMap();
         serviceParams.put("token",  UserToken);
         serviceParams.put("payload", recordStream);
         serviceParams.put("payloadFormat", playloadFormat);
         serviceParams.put("shortname", event);           // must be the same as eventShortname in XML


         SOAPMessage  reportPersonResponse  = soapHandler.sendSoapRequest(_ReportPerson, serviceParams);
         if (reportPersonResponse == null)
             return "-1";

        HashMap<String, String> retData = soapHandler.decodeSoapResponse(
            reportPersonResponse, _ReportPersonResponse);;
        if (retData == null || retData.isEmpty())
            return "-1";

        // send request to server and get back response
        boolean realCase = true; //false;
        if (!realCase)
            return "0";

        long lval = Long.parseLong( retData.get("errorCode"));
        BigInteger statusCode = BigInteger.valueOf(lval);
        if (!statusCode.equals(SUCCESS))
        {
            String errorMessage = retData.get("errorMessage");

            log.error("Error in submitting ReportPerson record. -->" + errorMessage);
            return null;                // uuid of reported person record;
        } 
        String uuid = retData.get("uuid");
        log.info("Transfer of ReportedPerson record is successful;  Person's UUID="+uuid);
         return uuid;
    }

    
    /*------------------------------------------------------------------------------------------------------------------------*/
      public static void main(String[] args)
    {
        //defaults
        String WS =  "TP";
        
        String serviceUrl = "https://triagepicstage.nlm.nih.gov/?wsdl&api=33";
        String targetNamespace = "soap/plusWebServices";
        String user = "disasterN2";
        String pw = "Password$2";
      
         DefaultLogger.enableInfoLogging();
        try
        {
            TTEmailClient tpClient = new TTEmailClient( serviceUrl, targetNamespace, user, pw);
            int status = tpClient.init();
            if (status < 0 )
            {
                log.error("Error in initializing client for TT Email client" );
                System.exit(-1);
            }
            
            
            
            // get event list
           tpClient.setEnquiryInfo("unknown", "Unknown");      // unknown or  suburban hospital
            
               // send the Record for a person
           
            String template = "C:/DevWork/LPF/EmailProc/config/trtrak/templates/ReportPersonWithPhotosTemplate.json";
            
            ReportedPersonRecord  record = new ReportedPersonRecord();
            record.eventShortname = "test";
            record.firstName = "Lisa";
            record.lastName = "Jones";
            record.gender = "female";
            record.age="";
            record.address = "";
            record.city = "Rockville";
            record.region = "Maryland";
            record.country= "USA";
            record.reportedStatus = "";
            record.emailData = "Subject: Female missing\nSender: dmiller@gmail.com\nDate: May 24, 2014\n"
                +"Message: I am looking for my friend's daughter Lisa Jones who is 12 years old and was last seen in  Washington, DC.";
                   
            
            HashMap personInfo = record.geFieldMap(); 
            
            String photos= null; // "C:/DevWork/LPF/EmailProc/config/resource/Sephali1.gif";
                                        // + "; C:/DevWork/LPF/EmailProc/config/resource/Sephali2.gif"
                                        // + ", C:/DevWork/LPF/EmailProc/config/resource/Sephali3.gif" ;

            personInfo.put("photos", photos);

            // send record to PLUS server
            tpClient.reportPerson(template, personInfo,"test");
        } 
        catch(Exception e)
        {
            log.error("Error in sending message to server from PL Email client", e );
            System.exit(-1);
        }
      }
  
}

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
 * This is the People Locator Email Processing Server.
 * It is started up by a PLEPClient program (run by the operator from command line)
 * The interfaces are:
 *  Startup - Receives a current  Event name
 *                  Sends a handshake greeting message
 *                  Processes in a loop to fetch and process email
 * Switch Event - Change the current event to the provided one
 * 
 * Close - Shut down the processing
 *                  
 * 
 * Author: 
 * Date: August 14, 2013
 * 
 */

import gov.nih.nlm.lpf.emails.control.PLEmailProcessor;

import gov.nih.nlm.lpf.emails.util.Utils;
import gov.nih.nlm.lpf.emails.util.ConfigReader;

import javax.mail.Message;
import java.io.File;

import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;

import gate.util.GateException;
import java.util.LinkedHashMap;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

public class PLIETApp 
{
    private static Logger log;
 
    // static variables
    public static String PLEventNameField = "eventShortName";
    
    Properties ctxProperties;
    
    boolean procInitiated = false;
    boolean sendMsgToPLUSServer = true; //false;          // set to true for actual transfer, false for testing NLP part
    boolean testEmails = false;
    
    PLEmailProcessor emailProcessor  = null;
    PLUSServiceHandler plusServiceHandler = null;
    EmailMonitor emailMonitor = null;
    String imageStoreDir = null;
    String serviceUrl = "";
    int maxImageNum = 4;        // default
    
    // Accounting and "Undeliverable" emails received at this account that should not be 
    // processed for disasterInfo
    String [] emailSenderFilters = null;
    String[] emailSubjectFilters = null;
    
    ArrayList<HashMap<String, String>> eventList  = null;
   
   private static String intro = "The following information was exctracted from your email.\n" +
        "If it is not correct, please log in to the PL Website %s and edit the fields."; 
   private String replyIntro;
    
    // emailAddr,  String folder, String name, String pwd for testing
    static String[] emailAccessInfoTest = 
        {"xxx@mail.nih.gov", "INBOX/PLMessages",  "nih\\xxx", "myPasswd"};  

   // Per email variables
    String[] imageAttachments = null;
    LinkedHashMap<String, String> emailFieldData = null;
    
    /*------------------------------------------------------------------------------------------------------------*/
    // For monitoring of the PL application, we send test message from an App
    // If so: PLIET sends the posted record ID along with the NLP extracted info
    protected String monitorApp;
    protected String monitorEmailAddress;
    protected boolean monitorMessage;
    protected boolean hasPersonInfo;           // Is it a valid message with NLP info
    /*------------------------------------------------------------------------------------------------------------*/
    
    public PLIETApp(String contextFile) throws GateException
    {
        // initialize in a Server context (when invoked through a Web service) 
        if (!procInitiated)
        {
            try
            {
                int initStatus =  initApp(contextFile);
                if (initStatus == 1)
                    procInitiated = true;
                else
                {
                    log.error("Terminating execution due to invalid configuration or user authorization for the Web Setvice" );
                    System.exit(-1);
                }
            }
            catch (Exception me)
            {
               log.error("Could not start PLIET Email Processing Application.", me);
            } 
        }
        log.info(" >>>> PLIET Email Processing Application initialized <<<<\n");
    }

  /***********************************************************************************************
   *  initialize the pipeline processing functions:
   *     - Get the name of  the Properties file for initializing the Email processing pipeline
   *    - initialize logging though Log4j
   *     - Initialize the NLP email processing pipeline with the Context Properties file name
   *     - Initialize the Email client to retrieve messages from the Disaster  email  mailbox
    ***********************************************************************************************/  
    protected int  initApp(String ctxFile)
    {
        ctxProperties  = ConfigReader.getProperties(ctxFile);
        if (ctxProperties == null)
            return -1;          // could not open/read the properties file
        
        // set all logging for the application as specified in the log4j properties file
        initLogging(ctxProperties);
        
        // For checking if a message is received simply for monitoring PL reliability
        monitorApp = ctxProperties.getProperty("monitorApp");
        monitorEmailAddress = ctxProperties.getProperty("monitorAddress");
        
        String serviceName = ctxProperties.getProperty("PlusService");
        if (serviceName == null)
        {
              log.error("PLIET application initialization failure,no PLUS service name provided in the configuration.");
                 return -1;
        }   
       else if (!(serviceName.equalsIgnoreCase("PeopleLocator") || serviceName.equalsIgnoreCase("TriageTrak")))
        {
            log.error("PLIET application initialization failure, Unknown PLUS service name "+ serviceName +" provided in the configuration.");
            return -1;
        }
        String serviceUrl = ctxProperties.getProperty("PlusUrl");
        int paramStart = serviceUrl.indexOf("?");
        if (paramStart > 0)
            serviceUrl = serviceUrl.substring(0, paramStart);
        replyIntro = intro.replaceFirst("%s", serviceUrl);
        
       imageStoreDir = getImageStoreDir(ctxProperties);
        boolean okay = verifyImageDirWriteStatus(   imageStoreDir);
        if (!okay)
            return -1;
    
        log.info("\r\n\r\n"+
            "*******************************************************************************************************\r\n"
            +"PLIET application started up for sending records to the " + serviceName +" Web Service \r\n" 
            +"*******************************************************************************************************\r\n");

        String[] emailAccessInfo = getEmailAccessInfo(ctxProperties);
        String mi = ctxProperties.getProperty("maxImageAttachments");
        if (mi != null && !mi.isEmpty())
            maxImageNum = Integer.parseInt(mi);
     
         emailMonitor = new EmailMonitor(this, emailAccessInfo);
         
         // Check the senders or Subjects of emails that we should ignore (e.g. accounting/undeliverable emails)
         getEmailFilters(ctxProperties);
        
        // initialize connection to the PL server as an authorized  PL/Reunite client
        plusServiceHandler = new PLUSServiceHandler( ctxProperties);
        int status =  plusServiceHandler.checkAuthorization();
        if (status == -1)       // error message already published
            return -1;
        eventList  = plusServiceHandler.getOpenEventInfo();
        emailProcessor = new PLEmailProcessor(ctxProperties, eventList);
         return 1;
    } 
    
    //--------------------------------------------------------------------------------------------------------*/
    // Initialize logging to a log file and to stdout  as specified in the log4j properties file
    //--------------------------------------------------------------------------------------------------------*/
        
    protected void initLogging(Properties ctxProperties)
    {
        String log4jConfProp = Utils.getDereferencedProperty(ctxProperties,"Log4j.properties");
         PropertyConfigurator.configure(log4jConfProp);
         setLog();
         
    }
    
 /**************************************************************************************/   
    protected String[] getEmailAccessInfo(Properties ctxProperties)
    {
        String testing = ctxProperties.getProperty("emailTesting"); 
       testEmails = (testing != null && testing.trim().equalsIgnoreCase("true"));
        
        if (testEmails)
            return emailAccessInfoTest;
        
        // real case - retrieve data from Disaser8 email folder @mail.nih.gov
        // Elements are: emailAddr,  emailFolder, emailUser, emailPassword
        String[] emailAccessInfo  = new String[4];
        emailAccessInfo[0] = ctxProperties.getProperty("emailAddress").trim();
        emailAccessInfo[1] = ctxProperties.getProperty("emailFolder").trim();
        emailAccessInfo[2] = ctxProperties.getProperty("emailUser").trim();
        emailAccessInfo[3] = ctxProperties.getProperty("emailPassword").trim();
        return emailAccessInfo;
    }
    //--------------------------------------------------------------------------------------------------------------
    // Save filters for email senders and Subjects
    //--------------------------------------------------------------------------------------------------------------
    protected void getEmailFilters(Properties ctxProperties)
    {
        String emailFilterFile = Utils.getDereferencedProperty(ctxProperties,"EmailFilterFile");
        if (emailFilterFile == null ||  ! (new File(emailFilterFile)).exists())
        {   
            log.warn("No Email Filters specified for Disaster messages");
            return;
        }
         Properties filterProperties  = ConfigReader.getProperties(emailFilterFile);
         String senderFilters =  filterProperties.getProperty("sender_filters").trim();
         if (senderFilters!= null && senderFilters.length() > 0)
            emailSenderFilters = senderFilters.split(";");
         String subjectFilters =  filterProperties.getProperty("subject_filters").trim();
         if (subjectFilters!= null && subjectFilters.length() > 0)
            emailSubjectFilters = subjectFilters.split(";");
         return;
    }
     //-----------------------------------------------------------------------------------------------------------//
    // Get the directory name where attached images are to be stored 
    // before sent to the PL Server
    // If the directory does not exist, create it
    //------------------------------------------------------------------------------------------------------------//
    protected String  getImageStoreDir(Properties ctxProperties)
    {
        String  imageDir = Utils.getDereferencedProperty( ctxProperties, "imageStoreDirectory"); 
        if (imageDir == null)
            imageDir = "./tmp";
        
       return  imageDir.trim();
    }
   //------------------------------------------------------------------------------------------------------------//
    // Ensure that the image store directory exists and is writable for storing photos
    //------------------------------------------------------------------------------------------------------------//
    protected boolean verifyImageDirWriteStatus( String  imageDir)
    {     
        String  errorMsg = null;
        File dir = new File(imageDir);
        try
        {
            if (dir.exists())
            { 
                if (dir.isDirectory() && dir.canWrite())
                    return true;
                else
                    errorMsg = "ImageStore directory " + imageDir +
                        " is not a writable directory. Please assign file creation permission";
            }
            else    // directory does not exist - create it
            {
                if (dir.mkdirs())   
                        return true;
                else
                    errorMsg = "Error in creating writable image store directory " + imageDir;
            }
            log.error(errorMsg);
            return false;
        }
        catch (Exception e)
        {
            log.error ("Error accessing/creating image Store Directory " + imageDir, e);
            return false;
        }
    }

 /**************************************************************************************/ 
    // Run the Email Service App - by running the Monitor continuously  in a new thread
    //***********************************************************************************((/
    public void  runApp()
    {
        emailMonitor.run();
        log.info("Successfully  processing emails");
    }
    

  /**************************************************************************************/
   /* Process an email, retrieved from the EMailMonitor, to extract required information
     * from it , and return the info to the sender as a reply message
     * This is the main functioning module of this program - and this is done by 
     * sending the Email message to the object PLEmailProcessor.
    * The attached image, if any, is not sent to the NLP pipeline, but directly to the PL server
     **************************************************************************************/   
  protected String  processEmail(Message incomingMsg)
  {
      String replyMsg = "";
      try
      {
          HashMap <String, Object> emailContents =
             EmailAgent.retrieveMessageContents(incomingMsg,  imageStoreDir, maxImageNum );
          
          monitorMessage = checkForMonitorMessage(emailContents);                // for PL activity monitoring
         
         String  emailString = (String)emailContents.get("Message");
          imageAttachments = (String[]) emailContents.get("Attachments");
         // print the stored image attachments
         //System.out.println("Message \r\n"  + emailString);
         if (imageAttachments != null)
         {
                System.out.println("Stored Attachment files: ");
                for (String img : imageAttachments)
                    System.out.println("\t"+img);
         }
         
         // Extract relevant information from the email text through NLP processing
         // currently we only send the Subject and Body fields
         LinkedHashMap<String, String> emailData = new LinkedHashMap();
         emailData.put("From",  (String)emailContents.get("From")); 
         emailData.put("Sent Date",  (String)emailContents.get("Sent Date")); 
         emailData.put("Subject", (String)emailContents.get("Subject"));
         emailData.put("Body", (String)emailContents.get("Body"));
         
         // save locally
         emailFieldData = emailData;
         
         int status =  emailProcessor.processEmailMessage(emailData);
         
         if (status == 1&&  emailProcessor.getReportedPersons() != null   &&
                        emailProcessor.getReportedPersons().size() > 0)
         {
             hasPersonInfo = true;
             replyMsg =  emailProcessor.getReportedPersons().get(0).toString();              //.getProcessedResults();
         }
         else  
         {
             hasPersonInfo = false;
             replyMsg = "No relevant information found in the email" ;
         }
        
      }
      catch (Exception e)
      {
          log.error("Could not format email msg to string.", e);
      }
      
      return replyMsg;
  }
  //------------------------------------------------------------------------------------------------
  // Check if this is a standard message or PL monitoring message
  // which is sent from a known App (X-Mailer and known email address)
  protected boolean  checkForMonitorMessage(HashMap<String, Object>emailContents)              
  {
      String emailSender = (String)emailContents.get("X-Mailer");
      if (emailSender  != null && emailSender.equals(monitorApp))
          return true;
     
    // a regular email
          return false;
  }
  
  //----------------------------------------------------------------------------------------------//
  // Perform postprocessing function after the email was analyzed and we get 
  // back the results.
  // Currently, it sends  the reported person info from  the email to the PL Server.
  //----------------------------------------------------------------------------------------------//
  protected String  reportPersonToWebServer()
  {
     String record_uuid = "";
     ArrayList<HashMap<String, String>> personInfoList =  emailProcessor.getReportedPersonInfo();
     //ArrayList<HashMap<String, String>> personInfoList =   getDummyReportedPersonInfo();
       if (personInfoList == null)
           return null;

      int nm = 1;   // Later: personInfoList.size();        // Currently send only one message
      for (int i = 0; i <nm; i++)
       {
           HashMap<String, String> personInfo = personInfoList.get(i);
           if (imageAttachments != null)
            {
                String photos = "";
                // create a concatenated list, separated by ";"
                for (String attch : imageAttachments)
                    photos = photos+attch+";"; 
                 
                photos = photos.replace("(;)$", "");
                personInfo.put("photos", photos);
            }           
           
           String eventName = personInfo.get(PLEventNameField);
           if (eventName == null || eventName.length() == 0)
               eventName =  "test";  // TBI: plusServiceHandler.getDefaultEvent().shortname;
         // hardcoded event (shortname) for for testing only
                  
        if (!sendMsgToPLUSServer)               // for internal testing 
             return "";
        // send the extracted info to the PLUS server as a formatted  record
          record_uuid =  sendMessageToPLUSServer(personInfo, eventName, emailFieldData);
       }
       return record_uuid;
  }
  
  private  ArrayList <HashMap<String, String>>  getDummyReportedPersonInfo()
  {
        LinkedHashMap<String, String> personInfo = new LinkedHashMap();
        personInfo.put  ("firstName", "Shyam");
        personInfo.put("lastName", "Meher" );
        personInfo.put("gender", "male" );
        personInfo.put("age", "35" );
        personInfo.put("address", "");
        personInfo.put("city", "Puri"); 
        personInfo.put("region", "Odisha");
        personInfo.put("country", "India");
        personInfo.put("lat", "19.8160");
        personInfo.put("lng", "85.8330");
        personInfo.put("reportedStatus", "found");
        personInfo.put("messageData", "Shyam Meher was found near Puri after the flood.");
        personInfo.put("eventShortName", "test");
        personInfo.put(">>NLP ConfidenceFactor",  " 85 "+" out of 100");
        ArrayList <HashMap<String, String>> retInfo = new ArrayList();
        retInfo.add(personInfo);
        return retInfo;
  }

  //----------------------------------------------------------------------------------------------//
    protected String  sendMessageToPLUSServer(HashMap<String, String> personInfo, 
        String eventName, LinkedHashMap<String, String>emailConents)
    {
     //  if (sendMsgToPLServer)
      String record_uuid = plusServiceHandler.reportRecordToServer(personInfo, emailConents, eventName);
      return record_uuid;
      
    }

 //*********************************************************************************************/
 //  Inner class to retrieve email messages and send replies to the email addresses
 // When a message is retrieved:
  //    - it is sent to thr emailApp for processing
 //     - Then a reply is sent to the email oroginator
 //     - Then the emailapp is invoked for postProcessing (sending the info to PL Server)
//*********************************************************************************************/
    protected class EmailMonitor extends Thread 
    {
        PLIETApp emailApp; 
        EmailAgent emailAgent;
        
        public EmailMonitor(PLIETApp parent, String[] accessInfo)
        {
            emailApp = parent;   
            if (accessInfo == null)
                emailAgent = new EmailAgent();          // use defaults
            else
                emailAgent = new EmailAgent(accessInfo[0], 
                    accessInfo[1], accessInfo[2], accessInfo[3]);
        }    
        
       /**************************************************************************************/
        // Run the EmailMonitor to retrieve messages in a loop
        public void run()
        {
            log.info(this.getName()+": New monitoring thread started.");
            boolean hasError = false;
            int count = 1;
            
            int nc = 0;
            while(!hasError) // && nc < count)
            {
                try 
                {
                    Message[] emailMessages = emailAgent.getMessages();
                    if (emailMessages == null || emailMessages.length == 0)
                        Thread.sleep(2000);                //  for 2 seconds
                    else
                    {
                        for (int i = 0; i < emailMessages.length; i++)
                        {
                            Message incomingMsg = emailMessages[i];
                             if(filterMessage(incomingMsg))          // if  the message to be filtered out
                            {
                                emailAgent.setMessageAsRead(incomingMsg);
                                continue;
                            }
                            String  replyMsg = emailApp.processEmail(incomingMsg);
                            if (replyMsg== null)
                            {
                                log.error("Could not process email message due to internal problems, quitting");
                                hasError  = true;
                                break;
                            }
                            sendReply(incomingMsg, replyMsg, replyIntro);
                        }
                          
                        // done with these message, release resources
                        emailAgent.closeResources();   
                    }
                }
                catch (InterruptedException e)
                {
                    log.error("Email monitoring is interrupted! Stopping peocessing  emails.", e);
                    break;
                }
                catch(Exception e)
                {
                    log.error(e.getMessage(), e);
                }	
                nc++;
            }   // end-while
        }

    /*---------------------------------------------------------------------------------------------------------------------
     * Send a reply to the original email sender. If a monitorMessage, first send the record to 
     * the Web server and enclose the PL record ID in the retirned message
     *-------------------------------------------------------------------------------------------------------------------*/
        protected void sendReply(Message incomingMsg, String replyMsg, String replyIntro)
            throws Exception
        {
            if (!monitorMessage)            // regular email from disaster site
            {
                emailAgent.sendReply(incomingMsg, replyMsg, replyIntro);
                emailApp.reportPersonToWebServer();
            }
            else
            {
                // a monitor message,  first get the record ID and then send reply adding it
                String recordIDStr = "";
                String  reportedRecordId =  emailApp.reportPersonToWebServer();
                 String recordIDPattern =  ctxProperties.getProperty("recordIDPattern");
                 
                 // add the record ID through agreed upon formatting
                 recordIDStr  = recordIDPattern + 
                      ( (reportedRecordId == null) ?  " = NULL" : " "  + reportedRecordId);
                
                replyMsg = recordIDStr +"\n\r"+replyMsg;
                emailAgent.sendReply(incomingMsg, replyMsg, replyIntro);
            }   
        }
    } // End of inner class EMailMonitor
    //-----------------------------------------------------------------------------------------------/
    // Check if the incoming message should be filtered out
    protected boolean filterMessage(Message emailMessage)
    {
        try
        {
            if (emailSenderFilters != null && emailSenderFilters.length > 0)
            {
                String sender = emailMessage.getFrom()[0].toString();
                for (String senderFilter : emailSenderFilters)
                {
                    if (sender.equalsIgnoreCase(senderFilter))
                    {
                        log.info("Ignoring message from sender: " + sender);
                        return true;
                    }
                }
            }
            if (emailSubjectFilters != null && emailSubjectFilters.length > 0)
            {
                String subject = emailMessage.getSubject();
                for (String subjectFilter : emailSubjectFilters)
                {
                    if (subject.equalsIgnoreCase(subjectFilter) || subject.contains(subjectFilter))
                    {
                        log.info("Ignoring message containing  Subject heading: " + subject);
                        return true;
                    }
                }
            }
            return false;
        }
        catch (Exception e)
        {
            log.error("Error in retrieving sender/subject from email. Ignoring the message", e);
            return true;            // something wrong with this email, ignore it
        }
    }
    
  //-----------------------------------------------------------------------------------------------/
  /*------------------------------------------------------------------------------------------*/
    public static void main(String[] args)
    {
        // Standard settings for operational use: Service name in config file.
       String Server ="";           // default
       String configFile = "";
       if( (args.length >= 2) && args[0].equals("-config"))
        {
           configFile = args[1];
        }
       // Default settings for test/development)
       else if ((args.length >= 2) && args[0].equals("-server"))
       {
            Server = args[1]; 
            if (! (Server.equalsIgnoreCase("PL") || Server.equalsIgnoreCase("TT")))
            { 
               System.err.print("Unknown LPF service specified, must be PL or TT");
                System.exit(-1);   
            }
           String TEST_HOME = "C:/DevWork/LPF/EmailProc";
           //String TEST_HOME = "C:/DevWork/LPF/PLIETDemo_V1_2";
           String config = (Server.equalsIgnoreCase("PL")) ?  "/config/plus/PLEmailProc.cfg" : "/config/trtrak/TTEmailProc.cfg";
           configFile = TEST_HOME +  config;
       } 
       
        //DefaultLogger.enableInfoLogging();
        try
        {
             PLIETApp  emailApp  = new PLIETApp(configFile);
             emailApp.runApp();
        }
        catch (Exception e)
        {
            log.error(e);
        }
        System.exit(-1);
    }
/*------------------------------------------------------------------------------------------*/    
      private static void setLog()
    {
        log = Logger.getLogger(PLIETApp.class);
    }
}   
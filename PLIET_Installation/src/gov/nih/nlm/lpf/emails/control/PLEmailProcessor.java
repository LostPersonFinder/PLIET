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
package gov.nih.nlm.lpf.emails.control;

import gov.nih.nlm.lpf.emails.util.ConfigReader;
import gov.nih.nlm.lpf.emails.util.GeocodeUtil;

import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexicon;
import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexiconReader;

import gov.nih.nlm.lpf.emails.nlpproc.structure.ProcessedResults;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ReportedPersonRecord;


import gov.nih.nlm.lpf.emails.gate.pipeline.AnnotatedEmailDocument;
import gov.nih.nlm.lpf.emails.gate.pipeline.AnnotGeneratorFactory;
import gov.nih.nlm.lpf.emails.gate.pipeline.PLAnnieApp;
import gov.nih.nlm.lpf.emails.util.Utils;

import gov.lpf.resolve.GeoLocationInfo;

import gate.Document;


import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;

 /* 
 * This is the main class in processing LPF Email messages. It is invoked  when the 
 * application starts up, It initializes once, and then executes in a loop to process each received message
 *      Intialization:
  *         Reads the configuration file and accordinglr build the Lexicon etc.
 *          Establishes connection with the PL WebService, 
 *          Creates and initializes  a ProcessedResults object
 *   Execution loop, for each received Email message, as follows:
 *          Connects to the PL WEb Service and retrieves the Event List (as it might have changed)
 *          Updates the ContrextInfo object
 *          Invokes the ANNIE annotatior to annotate the Text in the email message as a Text Document
 *          Invokes  the EmailPipelineProcessor with the Annotated Gate Text Document, and the context information
****/

/**
 *
 * @author 
 */
public class PLEmailProcessor
{

    private static Logger log = Logger.getLogger(PLEmailProcessor.class);
    
    static String LexiconFile = null;     // initialized only once
    static PLLexicon plLexicon = null;
    
 
   // frequently used words in a message that should be lower case
   static  String[]  Keywords ={"Name" , "Person", "Age", "Gender", 
       "Location", "Male", "Female", "Old", "Condition"};
  
   // Interjections in a sentence. Must be removed - confuses NLP parser
   static String interjectionPattern  = "(?i)(please|hi|hello|hallo)\\W+";
   
   static String prepositionalAdjectives ="(?i)(\\Winside|outside|near\\W)";
   
   static String[][]  otherEdits  = {
       {"where about", "whereabout"}
   };
   
     
   //-----------------------------------------------------------------------------------------------------------
   
    protected Properties ctxProperties;          //
    protected  ArrayList<String> healthConditionWords = null;
    
    Document gateDoc;
   
     boolean useClause = true;      // default
     boolean testing = false; //true;
     
     // For each processed message
     protected ArrayList<ReportedPersonRecord> reportedPersons = null;
     protected String formattedResult = "";
     
     
         /** The Corpus Pipeline application to contain ANNIE */
    //private static boolean gateInited = false;
     /* not needed here
    final static String TEST_PREFIX = "TESTLPFEmail";
    final static String mailhost = "mail.nih.gov";
    final static String smtphost = "smtp.nih.gov";
    //String host = "NIHMLBXBB01.nih.gov";
    final static String username = "NIH\\disasterNN";
    final static String password = "xxxxyyyyzzzz";
    final static String sendHost = "SMTP.nih.gov";
    final static String fromAddr = "disasterNN@mail.nih.gov";
    final static String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
   

    final static String[] includeHeaders =
    {
        "To",
        "From",
        "Subject",
        "Date",
        "Content-Type",
        "MIME-Version",
        "Content-Language",
        "Message-ID"
    }; 
      * 
      */
    //-----------------------------------------------------------------------------------------------------------
    // the GATE/ANNIE Pipeline processr
    private PLAnnieApp plAnnotGenerator;    
    private ProcessedResults processedResults;
    
   // private ArrayList<HashMap<String, String>> plEventList = null;
    //private HashMap<String, GeoLocationInfo> eventLocations = null;
    
    int initStatus = -1;
    //-------------------------------------------------------------------------------------------------------------
  //**************************************************************************** 
  // Constructor
  //  Initialize the Email Processing application using the specified  configuration
  //  information and list of events, with <field, value> as a HashMap
 //******************************************************************************
    public PLEmailProcessor(Properties userContext, ArrayList<HashMap<String, String>> plEventList)
    {
        initStatus = init(userContext, plEventList);
    }
  /********************************************************************************************/
    // Retrieve various configuration parameters from the Properties file.
    // Set some to the Properties object for later referral by GATE pipeline etc.
    // Note: Usually parameters representing file paths are deferenced here.
    //------------------------------------------------------------------------------------------------------------------*/
    protected int  init(Properties userContext,  ArrayList<HashMap<String, String>> plEventList)
    { 
        try
        {
            ctxProperties = userContext;
            
            System.setProperty("GateHome", 
                Utils.getDereferencedProperty(ctxProperties,"GateHome")) ; // \ "C:/DevWork//LPFGate");
            System.setProperty("gate.plugins.home",  
                 Utils.getDereferencedProperty(ctxProperties, "GatePluginsHome"));    //"C:/Devwork/LPFGate/plugins");
            System.setProperty("ANNIEHome",
                 Utils.getDereferencedProperty(ctxProperties, "ANNIEHome"));     //"C:/DevWork//LPFGate/plugins/ANNIE");
            System.setProperty("gate.user_plugins.home", 
                  Utils.getDereferencedProperty(ctxProperties, "GateUserPluginsHome")); //"C:/DevWork//LPFGate/user_plugins");

            //System.setProperty("gooch.resource", "C:/DevWork/LPFGate/user_plugins/Pronoun_Annotator/resources");
             
            //ctxProperties.setProperty( "userConfig",userConfig); 
            String conditionFile =  Utils.getDereferencedProperty(ctxProperties, "PLConditionsFile");
            ctxProperties.setProperty("ConditionsFile", conditionFile);
                     
            healthConditionWords = readLPFConditionWords(conditionFile);
            
            // read the lexicon and save in the Lexicon structre
            String plLexiconFile = Utils.getDereferencedProperty(ctxProperties, "PLLexicon");
             int status = readLexicon(plLexiconFile);       // creates plLexicon object
             if (status == -1)
                 return -1;
             
            // Create the GATE ANNIE pipeline application to process each email
            String gateApp =    Utils.getDereferencedProperty(ctxProperties,"GatePipelineApp"); //"C:/DevWork/LPF/EmailProc/config/coreftext1.gapp.xml";
            ctxProperties.setProperty("default-LPF-app", gateApp);  
            plAnnotGenerator  = AnnotGeneratorFactory.appInstance(ctxProperties);
            if (plAnnotGenerator == null)
                return -1;
            
            processedResults = new ProcessedResults();
            processedResults.plLexicon = plLexicon;
            processedResults.plEventList = plEventList;
            processedResults.eventLocationList = getEventLocations(plEventList);
        }
          catch (Exception e)
        {
            log.error("Could not initlatize LPF application for Email processing", e);
            return -1;
        }
        return 1;
    }
 
    /*--------------------------------------------------------------------------------------------------------------*/
    // Get the details of a OPEN location, based upon its LAT/LNG from  Geocode service
    //*--------------------------------------------------------------------------------------------------------------//
    protected HashMap<String, GeoLocationInfo> getEventLocations(
                        ArrayList <HashMap <String, String>>plEventList)
    {
        if (plEventList == null || plEventList.isEmpty())           // NLP testing, with no events
            return null;
        
        // get data for each open event, key is event shortname
        HashMap<String, GeoLocationInfo> eventLocations = new HashMap();
        for (int i = 0; i < plEventList.size(); i++)
        {
            HashMap <String, String> plEvent = plEventList.get(i);
            GeocodeUtil  gcUtil = new GeocodeUtil(plEvent);
            GeoLocationInfo  gcLocInfo = gcUtil.getEventLocationInfo();
            if (gcLocInfo == null)          // a closed or fake event
                continue;

            String eventName = plEvent.get("shortName");
            // Get the acutal country/region info about this event from the Goecoder service
            eventLocations.put(eventName, gcLocInfo); 
            log.debug("PL Event: " + eventName + ", Date: " +plEvent.get("date") + ", City/State/Country: " + 
               gcLocInfo.city +", " + gcLocInfo.region+ ", " + gcLocInfo.country);
            
        }
        return eventLocations;  
    }  

     /*--------------------------------------------------------------------------------------------------------------*/
    // Read the Health Status (Condition) words from the specified filefile 
    //*--------------------------------------------------------------------------------------------------------------//
    protected ArrayList <String> readLPFConditionWords(String conditionFileName)
    {
        ArrayList<String> conditions = new ArrayList();
        try
        {
            File cfile = new File(conditionFileName);
            BufferedReader inputReader = new BufferedReader(new FileReader(conditionFileName));
            String  word = inputReader.readLine();
            while (word != null) 
            {
                conditions.add(word);
                word = inputReader.readLine();
            }
            inputReader.close();
        }
        catch (Exception e)
        {
            log.error("Could not read  Health Status words from file " + conditionFileName, e);
        }
        return conditions;
    }

    /*------------------------------------------------------------------------------------------*/
   protected int readLexicon(String lexiconFileName)
    {
       if (plLexicon == null)
       {
           LexiconFile = lexiconFileName;
           PLLexiconReader lexiconReader =  new PLLexiconReader(LexiconFile);
           int status = lexiconReader.readLexicon();
           if (status == -1)
           {
               log.error ("Error reading LPF lexicon from file " + lexiconFileName);
               return -1;
           }
           plLexicon = lexiconReader.getLexicon();
       }
       return 1;
   }
   //---------------------------------------------------------------------------------------------------------------------/
   // End of initialization.
   // The rest are performed for each email received from the user
   //----------------------------------------------------------------------------------------------------------------------/
  
   
   /*--------------------------------------------------------------------------------------------------------------/
 *  Process an email message provided as a Hashmap of Fields and values
 *
 * This method is invoked for testing the NLP processing part. 
  *      It creates a Gate Text document with Subject and body fields
  *     - Sends it through the GATE pipeline for annotating it
  *     - Recieve/process the message through Clausal Analysis, etc
  *     - Send back data to build reply to the email sender
 *      - Send the formatted data to the PeopleLocator server
 *---------------------------------------------------------------------------------------------------------------*/
   public int processEmailMessage(HashMap <String, String> emailFields)
   {
        // retrieve the required fileds from the Email document
        String   subjectStr = emailFields.get("Subject");
        String   bodyStr = emailFields.get("Body");
         int status = processMessageFields(subjectStr, bodyStr);
         return status;
   }
   
 /******************************************************************************************
 *  Process an email message, received as formatted String
 *  >>> Used for testing using archived messages <<<
 * This method is invoked by the caller  for each message, It performs the following: :
  *     - Convert the Email message string  to an annotated  Gate Email Document 
  *      - invokes method for processing the annotated Email
  *     - Send back data to build reply to the email sender
 *      - Send the formatted data to the PeopleLocator server
 /*--------------------------------------------------------------------------------------------------------------*/
    public int processEmailMessage(String formattedEmaiStr)
    { 
        try
        {
            AnnotatedEmailDocument  annotatedEmailDoc = 
                new AnnotatedEmailDocument (formattedEmaiStr, "");
            
            processAnnotatedEmail(annotatedEmailDoc);
            return 1;
        }
        catch (Exception e)
        {
            log.error("Could not excute ANNIE Pipeline for processing message. ", e);
            return -1;
        }
    }
    
     public String getProcessedResults()
     {
         return formattedResult;
     }
   
    /*--------------------------------------------------------------------------------------------------------------/
 *  Process an email message
 * This method is invoked by the caller  in a loop for each message as follows:
  *     - Converts the Email message to a Gate Text Document 
  *     - Sends it through the GATE pipeline for annotating it
  *     - Recieve/process the message through Clausal Analysis, etc
  *     - Send back data to build reply to the email sender
 *      - Send the formatted data to the PeopleLocator server
 *---------------------------------------------------------------------------------------------------------------*/
   public int processAnnotatedEmail(AnnotatedEmailDocument emailDocument)
   {
       // retrieve the required fields from the Email document
        String   subjectStr = emailDocument.getFieldValue("Subject");  
        String   bodyStr = emailDocument.getFieldValue("Body");
         int status = processMessageFields(subjectStr, bodyStr);
         return status;
}

   /*---------------------------------------------------------------------------------------------------------------*/
   
protected int processMessageFields(String subjectStr, String bodyStr)
{
        // We do some clean ups with the body text  before sending through the GATE Pipeline
        // so as not to confuse the NLP parser later with certain word types
        
        subjectStr = cleanupText(subjectStr);
        bodyStr = cleanupText( bodyStr);
        
        // process the Subject and body fields as a combined Text document
        // by sending it through the GATE pipeline
        Document gateTextDoc;
        try
        {
            Document annotatedMsgText = plAnnotGenerator.createAnnotatedDocument(
            subjectStr, bodyStr, "text/plain");
            gateTextDoc = annotatedMsgText;
        }
        catch (Exception e)
        {
            log.error("Could not excute ANNIE Pipeline for processing message. ", e);
            return -1;
        }

       int status = extractLPFInfoFromDocument( gateTextDoc);
       gateTextDoc.cleanup();
       
       try
       {
            plAnnotGenerator.destroyGateDoc(gateTextDoc);
       }
       catch (Exception e)
        {
            log.error("Error in destroying the Gate Document ", e);
            return -1;
        }
       
       return status;
   }
   
 //===========================================================/
 // Remove interjections and lower Keywords (LPF field tags)
   protected String cleanupText(String message)
   {
        message = message.replaceAll(interjectionPattern, " ");
        message = message.replaceAll(prepositionalAdjectives, " in ");
        
        // lowercase the keywords
   /*    for (int i = 0; i < Keywords.length; i++)
           message = message.replaceAll(Keywords[i], Keywords[i].toLowerCase());*/
       
         for (int i = 0; i <healthConditionWords.size(); i++)
             message = message.replaceAll(healthConditionWords.get(i), healthConditionWords.get(i).toLowerCase());
       
            for (int i = 0; i <otherEdits.length; i++)
             message = message.replaceAll(otherEdits[i][0], otherEdits[i][1]);
         
       return message;
   }
   
//====================================================================/
 // This is the main module for invoking the NLP Pipeline with the "annotated" text document,
 // consisting of the email's subject and body, returned by the ANNIE app.
 // The returned information is retrieved by the calling module from the "prpcessedResult" object
//====================================================================/
   
   protected int   extractLPFInfoFromDocument(Document gateDoc)
   {
        // Initialize the LPFPipelineProcessing task
       processedResults.messageDoc = gateDoc;
        EmailPipelineProcessor plPipeline = new EmailPipelineProcessor(processedResults);
        if (plPipeline == null)
            return -1;
        int status =  plPipeline.processMessage(gateDoc);
        if (status <= 0)
            return -1;
       
        // Save results to be returned to caller
       reportedPersons = plPipeline.getReportedPersons();
       
       // same information in formatted form
       formattedResult = plPipeline.getInferredResults();
         return 1;
   }
   
   // return the final results (Merged with Message search)
   public  ArrayList<ReportedPersonRecord> getReportedPersons()
   {
       return reportedPersons;
   }
 
     // return the initiall results (Without merge with Message search)
      public  ArrayList<ReportedPersonRecord> getInitialReportedPersons()
   {
       return processedResults.inferredPersonRecords;
   }
 
   //====================================================================/
   public  ArrayList<HashMap<String, String>> getReportedPersonInfo()
   {
      ArrayList<ReportedPersonRecord> persons = processedResults.plPersonRecords;
      if (persons == null || persons.isEmpty())
          return null;
      
     ArrayList<HashMap<String, String>> personInfo = new ArrayList();
     for (int i = 0; i < persons.size(); i++)
     {
         personInfo.add(persons.get(i).geFieldMap());
     }
     System.out.println(personInfo);
     return personInfo;
   }      
   //====================================================================/
}
   
  
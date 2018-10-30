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

import gov.nih.nlm.lpf.emails.gate.pipeline.AnnotatedEmailDocument;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.ReportedPersonRecord;
import gov.nih.nlm.lpf.emails.util.MessageSetIO;
import  gov.nih.nlm.lpf.eval.PLIETEvaluator;

import  gov.nih.nlm.lpf.xmlUtil.ResultWriter;
    
import gate.util.GateException;
import gate.Document;
import gov.nih.nlm.lpf.emails.util.ConfigReader;

/*import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
*/
import javax.mail.internet.AddressException;
import javax.mail.MessagingException;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import java.util.Properties;
import java.util.HashMap;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import gov.nih.nlm.lpf.emails.util.DefaultLogger;

/**
 * This is an internal test program to check the ANNIE modules and LPF enhancements to process 
 * ReUnite emails. 
 * It works in a stand-alone (non-Web) mode, where , the messages are read from an input text file, 
 * rather than being  retrieved from the disasterNN email site.
 *
* @author 
 * 
 */
public class PLEmailProcController
{
    private static Logger  log = Logger.getLogger(PLEmailProcController.class);

    public static boolean debugAny = false;
    
    String ConfigFile;
    boolean isPlain = true;         // plain text vs, formatted email
    boolean isTagged;                // true if  the input messages are XML formatted (with  ground truth data)
    boolean headerTest = false;
    boolean outputResult = false;
    boolean outputInitial = false;
    
    String resultDirectory = null;
    String initialResultDirectory = null;
    
    ResultWriter resultWriter = null;           // for writing output results
    ResultWriter resultWriterInitial = null;           // for writing output results
  
     PLEmailProcessor emailProcessor;
    
    //protected gate.Document emailDoc;  
    
    /*
     * ctxProprties: File to provide context for initialization
     */
    public PLEmailProcController(String ConfigFile)
    {
        init(ConfigFile);
    }

    protected void init(String userConfig)
    {   
        ConfigFile = userConfig;
         Properties ctxProperties  = ConfigReader.getProperties(userConfig);
        emailProcessor = new PLEmailProcessor(ctxProperties, null);        // no events  for  initial testing
    }
    
            
    // For convenience of testing
    public void setPlainMessage(boolean plain, boolean tagged)
    {
        isPlain = plain;
        isTagged = tagged;
    }
    
    /*/*---------------------------------------------------------------------------------------------------------------------*/
    // We do an accuracy evaluation only if the message has  formatted ground truth 
    // embedded in it
    public void outputResults(boolean output, boolean initial)
    {
        if (output)
        {
            outputResult = true;
            resultWriter = new ResultWriter();
            if (initial)
            {
                outputInitial = true;
                resultWriterInitial = new ResultWriter();       // also the initial results
            }
        }
        else 
        {
            outputResult = false;
            resultWriter = null;
        }
    }
/*---------------------------------------------------------------------------------------------------------------------*/   
      public  void processEmailFromFile(String filePath, String resultDir, 
          String initialResultDir  ) throws AddressException, 
                        MessagingException, IOException, GateException
      {
          if (resultDir != null)
              this.resultDirectory = resultDir;
         if (initialResultDir != null)
              this.initialResultDirectory = initialResultDir;
         
          if (isPlain)
          {
              if (isTagged)
                  processEmailFromTaggedFile( filePath);
              else
                  processEmailFromPlainFile(filePath);
          }
          else      // read from email archive (or synthetic email documents)
          {
                   // an (synthetic) email document
               int mc = 0;
               String allMsg = "";
               AnnotatedEmailDocument  emailDocument  = createEmailDocument(allMsg);
               emailProcessor.processAnnotatedEmail(emailDocument);
               printResults(mc, emailProcessor.getReportedPersons());
               mc++;
           }    
          return;
      }

    /*---------------------------------------------------------------------------------------------------------------------
  This public method is invoked to process an email file using the GATE/ANNIE  pipeline 
 **********************************************************************************************/
   
    public  void processEmailFromTaggedFile(String filePath) throws AddressException, 
                        MessagingException, IOException, GateException
    {
         String[] fieldNames = {"subject", "body"};                      // fields we want
         // read the  given fields for each message in the file 
         int mc = 0;            // message count
         boolean readOnly = true;
         MessageSetIO msgIO = new MessageSetIO(filePath, readOnly);
         
         // If we are ttesting a set of XML tagged plain messages
         while (true)
         {
            String msgId = msgIO.readNextMessage();            // read with ground truth
            if (msgId == null)
                break;
            HashMap <String, String>  msgFields  = msgIO.getMessageData(fieldNames);
            String subjectStr = msgFields.get("subject");
            String bodyStr =  msgFields.get("body");
            HashMap<String, String> email = createEmailFromTemplate(subjectStr, bodyStr);      
            emailProcessor.processEmailMessage(email);
            
           ArrayList<ReportedPersonRecord>  initialRecords = emailProcessor.getInitialReportedPersons();
            ArrayList<ReportedPersonRecord>  finalRecords = emailProcessor.getReportedPersons();
            printResults(mc, finalRecords);
            
            if (outputResult && (finalRecords != null && !finalRecords.isEmpty()))
            {
                // Add the record to the output file
                resultWriter.addRecord(msgId, finalRecords.get(0));
                if (outputInitial && initialRecords != null && !initialRecords.isEmpty())
                      resultWriterInitial.addRecord(msgId, initialRecords.get(0));
            }
            mc++;
         }  
         // write the  results file
         if (outputResult)
         {
             String resultsFileName = new File(filePath).getName();
             int index = resultsFileName.lastIndexOf(".");
             String resultsFile  = resultsFileName.substring(0, index) +"_results.xml";
             String outFilePath = resultDirectory+"/"+resultsFile;
             int status = resultWriter.writeOutputToXMLFile(outFilePath);
             if (status == 1) 
                 log.info("Results stored in file " + outFilePath);
             else
                  log.error("Could not store results in file " + outFilePath);
             
             if (outputInitial)
             {
                  outFilePath = initialResultDirectory+"/"+resultsFile;
                 status = resultWriterInitial.writeOutputToXMLFile(outFilePath);
                 if (status == 1) 
                     log.info("Results stored in file " + outFilePath);
                 else
                      log.error("Could not store results in file " + outFilePath);
             }
         }
        return;     
     }
    
    // repreat the same for initial Results
     

         
 
/*---------------------------------------------------------------------------------------------------------------------
  This public method is invoked to process an email file using the GATE/ANNIE  pipeline 
 **********************************************************************************************/
   
    public  void processEmailFromPlainFile(String filePath) throws AddressException, 
                        MessagingException, IOException, GateException
    {
         String allMsg = "";
         BufferedReader inputReader = new BufferedReader(new FileReader(filePath));
           String  line  = "";
          while ( line  != null) 
          {
              line  = inputReader.readLine();
              allMsg += line + " ";
          }
          inputReader.close();
          
          int mc = 0;
  
           String bodyStr = "";
           String subjectStr = "";
            
         // NOTE: for testing several emails at a time, the messages are concatenated with <m> tag
        // parse the input stream and break into messages 

           String[] messages = allMsg.split("<m>");
           // split  each message to subject and body and treat as an email
           for (int i = 0; i < messages.length; i++)
           {
                String message = messages[i];
               if (message.length() == 0)
                   continue;

               String[] parts = message.split("\\<b\\>", 2);
               if (parts.length == 2)
               {
                   subjectStr = parts[0];
                   bodyStr = parts[1];
               }
               else         // no subject header
               {
                   if (headerTest)
                   {
                       subjectStr = message;
                        bodyStr = "";
                   }
                   else
                   {
                       subjectStr = "";
                        bodyStr = message;
                   }
               }
               HashMap<String, String> email = createEmailFromTemplate(subjectStr, bodyStr);      
                emailProcessor.processEmailMessage(email);  
                printResults(mc, emailProcessor.getReportedPersons());                   
                mc++;
           }
    }
 /*----------------------------------------------------------------------------------------------------------------------------------------*/   
    protected void printResults(int count, ArrayList<ReportedPersonRecord> personInfo)
    {
        System.out.println("\n*************** "+(count+1)+":   FINAL PROCESSED RESULTS   ********** *************************");
         if (personInfo == null || personInfo.size() == 0)
         {
            log.warn("No PL  information found  in the email message");
            System.out.println("************************************************************************** *************************");
             return;
         }
             
        for (int j = 0; j < personInfo.size(); j++)
            {
                String pstr = personInfo.get(j).toString();
                System.out.print(pstr);
                System.out.println("************************************************************************** *************************");
            }
    } 

    //--------------------------------------------------------------------------------------------------------------
    // Create a dummy email using the subject and body strings and the following email template
    //--------------------------------------------------------------------------------------------------------------
    
    protected  HashMap <String, String> createEmailFromTemplate(String subject, String body) 
    {
        HashMap emailTemplate =  new HashMap();
        emailTemplate.put(    "From", " xxxx@gmail.com");
        emailTemplate.put( "Date", "  Thu, 23 Aug 2012 11:05:52 -0400 (EDT)");
        emailTemplate.put( "To", "disasterNN@mail.nih.gov");
        emailTemplate.put( "Message-ID", "<19247642.0.1345754201318.JavaMail.xxxx@yyyy>");
        emailTemplate.put( "MIME-Version", "1.0");
        emailTemplate.put( "Content-Type", "text/plain; charset=us-ascii");
        emailTemplate.put( "Content-Transfer-Encoding", "7bit");
        emailTemplate.put( "Subject", subject);
        emailTemplate.put( "Body", body);

        return emailTemplate;
    }

    /***********************************************************************************************/
    protected  AnnotatedEmailDocument  createEmailDocument(String msgstr)
    {
         try
        {
            Document emailDoc = gate.Factory.newDocument(msgstr);
            // convert to it a local TextDocument
            AnnotatedEmailDocument emailDocument = new AnnotatedEmailDocument(emailDoc, "");
            return emailDocument;
        }
         catch (Exception e)
        {
            log.error(e);
          return null;
        }
    }
    

 
 
    /*------------------------------------------------------------------------------------------*/
    protected void complete()
    {
        //
    };
    
    
    /*------------------------------------------------------------------------------------------*/
    public static void main(String[] args)
    {
        String DEV_HOME = "C:/DevWork/LPF/EmailProc/";
        String configFile = DEV_HOME +  "/config/EmailProc.properties";
        
        DefaultLogger.enableInfoLogging();
        
       PLEmailProcController testEmail = new PLEmailProcController(configFile);
       String directory;
       String[] filenames;
       
        boolean accuracyTesting = false;  //true; 
        if (!accuracyTesting)           // initial testing
        {   
            /* directory = "C:/DevWork/LPF/EmailProc/testData/MsgSets";    
             filenames = new String[]  {
              "BatchTest.txt",   
             // "AcapulcoFlood.xml"
             //    "FirstPersonTest1_tagged.txt"
             };*/
             
            // { "AcapulcoFlood.xml"} ;  //"PLResolve.txt"};
                 
                 // {"regine.txt"};   //{"FirstPersonTest1_tagged.xml"}; 
       /* {  "AnnieTestSet2.txt"  //  "FirstPersonTest1.txt "      //"FirstPersonTest.txt",  //"CorelationTest.txt "  //"verbTest.txt", //
            //  "subjectHeaders.txt"  //"ReporterMatch.txt" // "AnnieTestSet1.txt" 
            //"fragmentTest.txt"    // "ClauseTest2.txt"     //"anaphoraTest.txt" //"emailSet_A.txt" ,  // ExampleOne.eml"
        };   */ 
            
          directory = "C:/DevWork/LPF/EmailProc/testData/V2_tests";
          filenames = new String[]  {"batchTest.txt "   //prepositions.xml"  //"conjunction.xml"  //"set1.xml"
          };
            testEmail.setPlainMessage(true, true);                // not emails, but plain text
            testEmail.outputResults(false, false);                  // no XML output of results to files
        }
        else         // For real testing with ground truth
        {
            directory = "C:/DevWork/LPF/EmailProc/testData/groundTruth/sets";
            filenames =   new String[] {
           // "Goodset_tagged.xml"
            "AllMessages_Set1.xml"      
            // "AttributeTest_tagged.xml"
            //"FragmentTest_tagged.xml"
            } ;           
            testEmail.setPlainMessage(true, true);                // not emails, but plain text
            testEmail.outputResults(true, true);                      //  output both inial and final results to file 
        }
        //----------------------------------------------------------------------------
        try
        {
            for (int i = 0; i < filenames.length; i++)
            {
               
                String filePath = directory + "/" + filenames[i];
                if (!accuracyTesting)
                {
                     testEmail.processEmailFromFile(filePath, null, null);
                }
                else        // output results to compute precision and recall 
                {
                    String resultDirectory = directory +"/results";
                    String initialResultDirectory = directory +"/results_initial";

                    testEmail.processEmailFromFile(filePath, resultDirectory, initialResultDirectory);

                    String refPath = directory;
                    String[]  testSets = filenames;   // {"AttributeTest_tagged"};  // { "AllMessages_Set1"};

                    System.out.println("\n\n<<<<<<<<< Initial Results >>>>>>>>");
                    PLIETEvaluator evaluator1 = new PLIETEvaluator(refPath, initialResultDirectory, testSets);    
                    evaluator1.evaluateResultForSet();

                    System.out.println("\n\n<<<<<<<<< Final Results >>>>>>>>\n\n");
                    PLIETEvaluator evaluator = new PLIETEvaluator(refPath, resultDirectory, testSets);    
                    evaluator.evaluateResultForSet();

                   
                }
            }
        } 
        catch (Exception e)
        {
            log.error("Null Pointer Eception", e);
        }
        testEmail.complete();
    }
}
   
 


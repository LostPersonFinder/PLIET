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
package gov.nih.nlm.lpf.emails.util.emailUtil;

/**
 * @author 
 * 
 * Date: September 26, 2013
 */
import gov.nih.nlm.lpf.emails.util.DefaultLogger;

import gov.nih.nlm.lpf.emails.util.MessageSetIO;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;
    
    
public class EmailSender
{
     private static Logger log = Logger.getLogger(EmailSender.class);
    
    int status = 0;
    int emailsSent = 0;
    
    ArrayList <HashMap<String, String>> messageSet;
    
// constants - for sending emails from the PLIET task    
    final static String mailhost = "mail.nih.gov";
    final static String smtphost = "smtp.nih.gov";
    final static String sendHost = "SMTP.nih.gov";   //"SMTP.live.com";  for hotmail
    final static String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
  
    // set by the application planning to send the  emails
    protected String sentFrom = "xxx@yyy.nih.gov";  
    protected  String username = "NIH\\xxx";
    protected String password = "my_passwd";
    protected String sendTo = "myname@hotmail.com";
    
    final static String TEST_PREFIX = "TESTLPFEmail";


   
     //create emails with the following fields
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
    
   protected Properties props = new Properties();     

    public EmailSender(String from, String username, String password, String recipient)
    {
        this.sentFrom = from;
        this.username = username;
        this.password= password;
        this.sendTo = recipient;
        
        init();
    }
    
   /*----------------------------------------------------------------------------------------------------------*/
   // Initialize Sesion Properties
  /*----------------------------------------------------------------------------------------------------------*/
    public void init() 
    {
        props.setProperty("mail.imap.host", mailhost);
        props.setProperty("mail.imap.socketFactory.class", SSL_FACTORY);
        props.setProperty("mail.imap.socketFactory.fallback", "false");
        props.setProperty("mail.imap.port", "993");
        props.setProperty("mail.imap.socketFactory.port", "993");

        props.setProperty("mail.smtp.host", smtphost);
        props.setProperty("mail.smtp.socketFactory.port", "465");
        props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.port", "465");
    }  
    
    
    /*************************************************************/
    public int createEmailsFromMessages(String inputMessageFile, int numMsg)
    {
        String[]  emailFields = {"subject", "body"};
        boolean readOnly = true;
        MessageSetIO msgIO = new MessageSetIO(inputMessageFile, readOnly);
        messageSet = new ArrayList();
        int n = 0;
        try
        {
             while (true)
             {
                 n++;
                 String msgId = msgIO.readNextMessage();            // read with ground truth
                 if (msgId == null ||  n > numMsg)
                     break;
                 HashMap <String, String> emailData   = msgIO.getMessageData(emailFields);
                 messageSet.add(emailData);
             }
        }
        catch (Exception e)
        {
           System.out.println ("Error reading message " + n + " from file");
           log.error(e);
            return 0;
        }
        return 1;
    }
    
    /*------------------------------------------------------------------------------------*/
    // send the emails using messages retrieved from the file
    public int sendEmails()
    {  
        // initialize once 
         Email email;
         try
         {
             email = new Email(props, sentFrom, username, password,  sendTo );
            if (email == null)
                return 0;
         }
          catch (Exception e)
          {
              log.error(e);
                return 0;
          }
    
        // create and send an email using the subject/body
        for (int i = 0; i < messageSet.size(); i++)
        {
            try
            {
                HashMap<String, String> msgData = messageSet.get(i);
                String subject = msgData.get("subject");
                String body = msgData.get("body");
                
                String emailSubject = (subject == null || subject.length() == 0) ? "" : subject;
                String emailContent= (body == null || body.length() == 0) ? "" : body;
                if (emailSubject.length() == 0 && emailContent.length() == 0)
                    return -1;

                email.sendEmail(emailSubject, emailContent, TEST_PREFIX);
            }
            catch (Exception e)
            {
                log.error(e);
                return 0;
            }
         }
        return 1;
     }
 
/*------------------------------------------------------------------------------------*/    
    public static void main(String[] args)
    {

        String messageFile = "C:/DevWork/LPF/EmailProc/testData/groundTruth/sets/Goodset_tagged.xml";

        String[] testValues = {"xxx@yyy.nih.gov", "NIH\\xxx",  "my_passwd", "myname@hotmail.com"};
        String[] realValues = {"xxx@yyy.nih.gov", "NIH\\xxx",  "my_passwd",  "disasterNN@yyy.nih.gov"};

       DefaultLogger.enableInfoLogging();
       
        boolean test = false;  //true;
        String[] parameters = test ? testValues : realValues;
        String from = parameters[0];
        String username = parameters[1];
        String password = parameters[2];
        String recipient = parameters[3];

        EmailSender emailSender = new EmailSender(from, username, password,  recipient);
        emailSender.createEmailsFromMessages(messageFile, 200);
        emailSender.sendEmails();
    }
}

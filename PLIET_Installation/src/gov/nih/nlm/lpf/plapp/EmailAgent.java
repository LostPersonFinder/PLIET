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
import com.sun.mail.imap.IMAPSSLStore;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

import org.apache.log4j.Logger;

/**
 *
* @author 
 * 
 */
@SuppressWarnings("serial")
public class EmailAgent
{
    private static Logger log = Logger.getLogger(EmailAgent.class);
    
    /** The Corpus Pipeline application to contain ANNIE */
    //private static boolean gateInited = false;
    final static String TEST_PREFIX = "TESTLPFEmail";
    final static String mailhost = "mail.nih.gov";
    final static String smtphost = "smtp.nih.gov";
    //String host = "NIHMLBXBB01.nih.gov";

    final static String sendHost = "SMTP.nih.gov";
    
    // defaults - for actual case , override for test case
   final static String defaultFromAddr = "disasterNN@mail.nih.gov";
   final static String defaultFromFolder = "INBOX";
   final static String defaultUsername = "nih\\disasterNN";
   final static String defaultPassword = "xxxxyyyyzzzz";
    
   static String fromAddr = "";
   static String fromFolder = "";
   static String username = ""; 
   static String password = "";
   
    final static String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
    
    // match emails with the following fields
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
    public static Properties props = new Properties();
    private int ncycles = 0;       // number of cycles for outputting message
    private boolean testing = false;;
    
    private Folder folder = null;
    private Store store = null;
    private  String imageStoreFolder = null;
    
  
    // connect to the   PL distaster related email address  "disasterNN "(real case)
   public EmailAgent()
    {
        this(null, null, null, null);
    }
  
   // connect to a different PL distaster related email address (real case)
     public EmailAgent(String emailAddr)
    {
        this(emailAddr, null, null, null);
    }
   
   /*--------------------------------------------------------------------------------------------*/  
     
    // connect to a test  email address (for testing)
    public EmailAgent(String emailAddr,  String folder, String name, String pwd)
    {
        init();
       fromAddr = emailAddr == null ? defaultFromAddr : emailAddr;
       fromFolder = (folder == null)  ? defaultFromFolder : folder;
       username = (name == null) ? defaultUsername : name;
       password = (pwd == null) ? defaultPassword : pwd;
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
    
    //----------------------------------------------------------------------------------------------------//
    // Set to true for testing email message retrieval and decoding
    // If true, all messages in an inbox are processed regardless of if they are 
    // seen before.  Should not be set to true in a recursive timer mode
    //
    protected void setTesting(boolean flag)
    {
        testing = flag;
    }
   
    //----------------------------------------------------------------------------------------------------//
    // where to store images coming as email attachments
    public void setImageStoreFolder(String dir)
    {
        imageStoreFolder  = dir;
    }

/*----------------------------------------------------------------------------------------------------------*/
    // Following modules are for processing of real emails from the specified Inbox
    /*----------------------------------------------------------------------------------------------------------*/

    Message[] getMessages() throws MessagingException
    {
        Message[] retMessages = null;
        folder = null;
        store = null;
        
        // connect to the Email store
        store = getIMAPStore();
        if (store == null)
        {
            // ("No more messages found");
            return null;
        };

        // Get folder, if not there, create it
        folder = store.getFolder(fromFolder);
        if (!folder.exists())
        {
            folder = store.getDefaultFolder();
        }
        folder.open(Folder.READ_WRITE); 

        // Get the set of messages currently in the store
        if (testing)
            retMessages = folder.getMessages();
        else
        {
           // retrieve only the new messages that were not seen previously
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            retMessages = folder.search(ft);
        }

    if (retMessages.length > 0)
    {
       log.info("Received " + retMessages.length + " messages from mailbox "+ username );
       ncycles = 0;
    }
    else 
    {
          ncycles++;
        if (ncycles == 1)
           log.info ("No more messages found at "+username);
        else
        {
            if (ncycles == 1800) ncycles = 0;         // reset every 300 cycles (1 hour)
        }
    }
    return retMessages;
    }
  
    //--------------------------------------------------------------------------------------------------//
    // Close the store from which messages were retrieved.
    // This sgould be called only after all operations are completed on
    // the set of messages obtained through getMessage() call, since those messages
    // are lightweight objects, which need the store for further data
    //--------------------------------------------------------------------------------------------------//
    public void closeResources()  throws MessagingException
    {
        if (folder != null)
            folder.close(false);
        if (store != null)
            store.close(); 
        
        // tbd: delete saved attachments
        }
    /*----------------------------------------------------------------------------------------------------------*/
    // Connect to the Email message store, using username and password
    // and return the Store object
    //-------------------------------------------------------------------------------------------------------*/
        public Store getIMAPStore() throws MessagingException
    {
        URLName url = new URLName("imap", mailhost, 993, "", username, password);

        try
        {      
            Session session = Session.getInstance(props, null);
            Store store = new IMAPSSLStore(session, url);
            store.connect();
            return store;
        }
        catch (Exception e)
        {
           ; // log.error("Error trying to get messages fron IMPAP Store", e);
            return null;
        }
        
    }

  /*----------------------------------------------------------------------------------------------------------------*/
 // Send the reply to the original message received from the Email Processoing Pipeline
  public   void sendReply(Message userMessage,  Message replyMessage, String replyIntro)
        throws AddressException, MessagingException, IOException
    {
        //Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props,
            new javax.mail.Authenticator()
            {

                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(username, password);
                }
            });

        // Define message
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(
            userMessage.getRecipients(Message.RecipientType.TO)[0].toString()));
        
        message.addRecipient(Message.RecipientType.TO,
            new InternetAddress(replyMessage.getFrom()[0].toString()));
        
        String subject = replyMessage.getSubject();
        if (subject != null && !subject.startsWith("Re: "))
        {
            subject = "Re: " + subject;
        }
        message.setSubject(subject);
        message.setHeader("X-Mailer", TEST_PREFIX);
        message.setSentDate(new Date());
        
        // Add the contents
        StringBuffer buf = new StringBuffer();
        try
        {
            buf = buf.append(replyIntro+"\n");
            buf = buf.append((String) replyMessage.getContent());
        } 
        catch (Exception e)
        {
           log.error(e.getMessage(), e);
        } finally
        {
            buf.append("\r\n");
            buf.append("----------------------- Your message ------------------------");
            buf.append("\r\n");
            buf.append(formatMessage(replyMessage));
            message.setText(buf.toString());
            // Send message
            Transport.send(message);
           
            // set the original message as read
            userMessage.setFlag(Flags.Flag.SEEN, true);

        }
    }
    /*----------------------------------------------------------------------------------------------------*/
    // Send the reply to the original message received from the Email Processing Pipeline
    // userMessage - message for which the reply is being set
   //*----------------------------------------------------------------------------------------------------*/
    public  void sendReply(Message userMessage, String replyStr, String replyIntro)
        throws AddressException, MessagingException, IOException
    {
        //Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props,
            new javax.mail.Authenticator()
            {

                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(username, password);
                }
            });

        // Define message
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(
            userMessage.getRecipients(Message.RecipientType.TO)[0].toString()));
        
        message.addRecipient(Message.RecipientType.TO,
            new InternetAddress(userMessage.getFrom()[0].toString()));
        
        String subject = userMessage.getSubject();
        if (subject != null && !subject.startsWith("Re: "))
        {
            subject = "Re: " + subject;
        }
        message.setSubject(subject);
        message.setHeader("X-Mailer", TEST_PREFIX);
        message.setSentDate(new Date());
        
        // set the In-Reply-To field in the header
        String refId = message.getMessageID();
        message.setHeader("In-Reply-To", refId);
        
        // Add the contents
        StringBuffer buf = new StringBuffer();
        try
        {
            buf = buf.append(replyIntro + "\n");
            buf = buf.append(replyStr);
            buf.append("\r\n");
            buf.append("----------------------- Your message ------------------------");
             buf.append("\r\n");
            buf.append(formatMessage(userMessage));
            message.setText(buf.toString());
            System.out.println(buf.toString());
            // Send message
            Transport.send(message);
              
            userMessage.setFlag(Flags.Flag.SEEN, true);
        }
        catch (Exception e)
        {
           log.error("Could not send confirmation reply to Email sender",  e);
        }
    }
 /*----------------------------------------------------------------------------------------------------*/   
    protected void setMessageAsRead(Message userMessage)  throws Exception
    {
         userMessage.setFlag(Flags.Flag.SEEN, true);
    }
    /*----------------------------------------------------------------------------------------------------*/
    
    public static String formatMessage(Message m) throws MessagingException
    {
        String ret = "";
        try
        {
            Session session = Session.getInstance(props);
            Message newMsg = new MimeMessage(session);
            
            Enumeration<Header> eh = m.getMatchingHeaders(includeHeaders);
            while(eh.hasMoreElements())
            {
                Header h = eh.nextElement();
                newMsg.addHeader(h.getName(), h.getValue());
            }
            Object content = m.getContent();

            
            if (content instanceof Multipart)           // if there are attachments to the email
            {
                Part bodyPart = ((Multipart) content).getBodyPart(0);
                newMsg.setContent(bodyPart.getContent(), bodyPart.getContentType());
            } 
            else if (content instanceof Part)
            {
                newMsg.setContent(content, ((Part) content).getContentType());
            } 
            else
            {
                newMsg.setText(content.toString());
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Date receivedDate = m.getReceivedDate();
            String received = (receivedDate == null) ? "" : receivedDate.toString();
            StringBuffer firstLine = new StringBuffer();
            firstLine.append("From ").
                append(m.getFrom()[0].toString()).
                append(" ").
                append(receivedDate).
                append("\r\n");
            os.write(firstLine.toString().getBytes());

            newMsg.writeTo(os);

            String encoding = "UTF-8";
            try
            {
                encoding = getMessageCharset(newMsg);
            } 
            catch (Exception e)
            {
            }

            ret = os.toString(encoding);

            /*
            if (m instanceof IMAPMessage) {
            ArrayList<String> ignoreHeaders = new ArrayList<String>();
            for (Enumeration<Header> e = m.getNonMatchingHeaders(includeHeaders); 
            e.hasMoreElements();) {
            ignoreHeaders.add(e.nextElement().getName());
            }
            // Add first line
            StringBuffer firstLine = new StringBuffer();
            firstLine.append("From ").
            append(m.getFrom()[0].toString()).
            append(" ").
            append(m.getReceivedDate().toString()).
            append("\r\n");
            os.write(firstLine.toString().getBytes());
            ((IMAPMessage) m).writeTo(os, (String[]) ignoreHeaders
            .toArray(new String[0]));
            } else {
            m.writeTo(os);
            }
            ret = os.toString(encoding);
             */
        } catch (IOException ioe)
        {
            ret = ioe.getMessage();
        }
        return ret;
    }
    
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    // retrieve and store both the textual contents of the message and the attached images if any
    // Note: FormattedLine is built to display the whole email for test/debug only
    //--------------------------------------------------------------------------------------------------------------------------------*/
    public static  HashMap  retrieveMessageContents(Message m, String imageStoreFolder,
                    int maxImages) throws MessagingException
    {
        Message message = m;
        HashMap<String, Object> emailContents= new HashMap();
        try
        {
            ArrayList<String> storedAttachments = new ArrayList();   
            
            // Write out the message as a String 
            StringBuffer  formattedLine = new StringBuffer();
            formattedLine.append("");
            
             // 1a: Email sender Program (App)
            String[] xmailers  = message.getHeader("X-Mailer");
            String xmailer = null;
            if (xmailers != null && xmailers.length > 0)
            {
                xmailer = xmailers[0];
                formattedLine.append("X-Mailer: " ).append(xmailer).append("\r\n");
            }
            // 1b: Sender    
            String senderAddress = message.getFrom()[0].toString();
            formattedLine.append("From: " ).append(senderAddress).append("\r\n");
            
            // 2. parse the sent date
             Date sent = message.getSentDate();
             String sentDate = (sent == null) ? "" : sent.toString();
             formattedLine.append("Sent: ").append(sentDate).append("\r\n");    
            
            
            // 3. parses recipient address in To field
            String toAddresses = "";
            Address[] listTO = message.getRecipients(Message.RecipientType.TO);
            if (listTO != null)
            {
                for (int toCount = 0; toCount < listTO.length; toCount++) {
                    toAddresses += listTO[toCount].toString() + ", ";
                }
            }  
            if (toAddresses.length() > 1) {
                toAddresses = toAddresses.substring(0, toAddresses.length() - 2);
            }
            formattedLine.append("To: " ).append(toAddresses).append("\r\n");    
            
            // 4.  parses recipient addresses in CC field
            String ccAddresses = "";
            Address[] listCC = message.getRecipients(Message.RecipientType.CC);
            if (listCC != null) {
                for (int ccCount = 0; ccCount < listCC.length; ccCount++) {
                    ccAddresses = listCC[ccCount].toString() + ", ";
                }
            }          
            if (ccAddresses.length() > 1) 
            {
                ccAddresses = ccAddresses.substring(0, ccAddresses.length() - 2);
                formattedLine.append("CC: ").append(ccAddresses).append("\r\n");    
            }  

            // 5. parse the Subject
            String subject = message.getSubject();
            if (subject == null) subject = "";
            formattedLine.append("Subject: ").append(subject).append("\r\n");    
         
            //6. Parse Text message and Attachments
            String contentType = message.getContentType();
            String textMessage = "";
            String attachFiles = "";
            
            if (contentType.contains("text/plain") || contentType.contains("text/html"))
            {
                textMessage = message.getContent() != null ? message.getContent().toString() : "";
            } 
            else if (contentType.contains("multipart")) 
            {
                int numImages=0;
                Multipart multiPart = (Multipart) message.getContent();
                int numberOfParts = multiPart.getCount();
                for (int partCount = 0; partCount < numberOfParts; partCount++)
                {
                    BodyPart part = multiPart.getBodyPart(partCount);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
                    {
                        String attachmentFile = part.getFileName();
                        if (numImages < maxImages && isImageFile(attachmentFile))
                        {
                            attachFiles +=  attachmentFile + ", ";
                            String attachment = storeAttachment(part, imageStoreFolder);
                            numImages++;
                            storedAttachments.add(attachment);
                        }
                        else
                        {
                            log.warn("Ignoring attachment: " + attachmentFile + " Format not  supported for Images.") ;
                        }
                    }
                    else
                    {
                        textMessage = part.getContent() != null ? part.getContent().toString() : "";
                    }
                }
                
                if (attachFiles.length() > 1) {
                    attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
                }
            }  
            // 6a. Add the actual message
            formattedLine.append("Message: ").append(textMessage).append("\r\n");
            
            //6b. Add the attachments
            formattedLine.append("Attachments: ").append(attachFiles).append("\r\n");
            
          
            // prints out the message details
            System.out.println("Message: ");
            System.out.println("\t From: " + senderAddress);
            System.out.println("\t To: " + toAddresses);
            System.out.println("\t CC: " + ccAddresses);
            System.out.println("\t Subject: " + subject);
            System.out.println("\t Sent Date: " + sentDate);
            System.out.println("\t Message: " + textMessage);
            System.out.println("\t Attachments: " + attachFiles);
            System.out.println("--------------------------------------"); 
            
            emailContents.put("Message", formattedLine.toString());
             if (!storedAttachments.isEmpty())
             {
                 String[] attachments = new String[storedAttachments.size()];
                 storedAttachments.toArray(attachments);
                 emailContents.put ("Attachments", attachments);
             }
            // also returned  the parsed fields
            emailContents.put("Subject", subject);
            emailContents.put("Body", textMessage);
            emailContents.put("To", toAddresses);
            emailContents.put("From", senderAddress);
            emailContents.put("X-Mailer",  xmailer);
            emailContents.put("CC", ccAddresses);
            emailContents.put("Sent Date", sentDate);
        }
        catch(Exception e)
        {
            log.error(e);
        }
        return  emailContents;
    }
   //-------------------------------------------------------------------------------------------------// 
    // Check file extension to determine if it is an image file
    protected static boolean isImageFile(String filename)
    {
        return filename.matches(".*(\\.gif|\\.jpg|\\.bmp|\\.tif|\\.png)$");
    }    

    /**
     * Saves an attachment part to a file on disk
     * @param part a part of the e-mail's multipart content.
     * @throws MessagingException
     * @throws IOException
     */
    private static String  storeAttachment(BodyPart part, String attachFolder) 
                        throws MessagingException, IOException {
        String destFilePath =   attachFolder+"/" + part.getFileName();
        
        FileOutputStream output = new FileOutputStream(destFilePath);
        
        InputStream input = part.getInputStream();
        
        byte[] buffer = new byte[4096];
        
        int byteRead;
        
        while ((byteRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, byteRead);
        }
        output.close();
        System.out.println("Stored attachment as " + destFilePath );
         return destFilePath ;
    }
    
    
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    
    public static  String getMessageCharset(Message m) throws MessagingException, MimeTypeParseException
    {
        MimeType mt = new MimeType(m.getContentType());
        String charset = mt.getParameter("charset");
        if (charset == null)
        {
            return "UTF-8";
        }

        return charset;
    }
    
    protected void complete()
    {};
 

    /*------------------------------------------------------------------------------------------*/
    public static void main(String[] args)
    {
         String DEV_HOME = "C:/DevWork/LPF/Developmen/EmailProc";
        String  configFile = DEV_HOME+"/config/user-gate.xml";
            
        String[] filenames = { "testMsg1.eml", "testMsg2.eml"}; //, "testMsg2.eml"};
        String directory = DEV_HOME+"/emaildata";
        
 /*       EmailAgent testEmail = new EmailAgent(configFile);
        try
        {
            for (int i = 0; i < filenames.length; i++)
            {
                String filePath = directory+"/"+filenames[i];
               testEmail.processEmailFromFile(filePath);
            }
        }
     */
        try
        {
            EmailAgent emailAgent = new EmailAgent();
            emailAgent.setTesting(true);
            processMessages(emailAgent);
            emailAgent.complete();
        }
            
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
    protected static void processMessages(EmailAgent emailAgent) throws Exception
    {
        String imageStoreFolder = "C:/tmp/plImages";
        int maxImages = 5;
        Message[] emailMessages = emailAgent.getMessages();
        if (emailMessages == null || emailMessages.length == 0)
            return;
        else
        {
            for (int i = 0; i < emailMessages.length; i++)
            {
                if (i < 6)
                    continue;
                Message incomingMsg = emailMessages[i];
                HashMap  <String, Object> emailContents =
                EmailAgent.retrieveMessageContents(incomingMsg, imageStoreFolder, 5);
                String  emailString = (String)emailContents.get("Message");
                String[]  imageAttachments = (String[]) emailContents.get("Attachments");
                
                // print the stored image attachments
                System.out.println("Message \r\n"  + emailString);
                if (imageAttachments != null)
                 {
                        System.out.println("Stored Attachment files: ");
                        for (String img : imageAttachments)
                             System.out.println("\t"+img);
                  }
            }
        }
    }
}

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

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;


/**
 *
 * @author 
 */
public class EmailMessage
{
    private static Logger log = Logger.getLogger(EmailMessage.class);
    public EmailMessage(Message aMessage, String attachmentFolder)
    {
        try
        {
    
            Message message = aMessage;
            String senderAddress = message.getFrom()[0].toString();
            String subject = message.getSubject();
            
            // parses recipient address in To field
            String toAddresses = "";
            Address[] listTO = message.getRecipients(RecipientType.TO);
            if (listTO != null) {
                for (int toCount = 0; toCount < listTO.length; toCount++) {
                    toAddresses += listTO[toCount].toString() + ", ";
                }
            }  
            if (toAddresses.length() > 1) {
                toAddresses = toAddresses.substring(0, toAddresses.length() - 2);
            }
            
            // parses recipient addresses in CC field
            String ccAddresses = "";
            Address[] listCC = message.getRecipients(RecipientType.CC);
            if (listCC != null) {
                for (int ccCount = 0; ccCount < listCC.length; ccCount++) {
                    ccAddresses = listCC[ccCount].toString() + ", ";
                }
            }          
            if (ccAddresses.length() > 1) {
                ccAddresses = ccAddresses.substring(0, ccAddresses.length() - 2);
            }          
            
            String sentDate = message.getSentDate().toString();
            
            String contentType = message.getContentType();
            String textMessage = "";
            String attachFiles = "";
            
            if (contentType.contains("text/plain") || contentType.contains("text/html"))
            {
                textMessage = message.getContent() != null ? message.getContent().toString() : "";
            } 
            else if (contentType.contains("multipart")) 
            {
                Multipart multiPart = (Multipart) message.getContent();
                int numberOfParts = multiPart.getCount();
                for (int partCount = 0; partCount < numberOfParts; partCount++)
                {
                    BodyPart part = multiPart.getBodyPart(partCount);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
                    {
                        attachFiles += part.getFileName() + ", ";
                        storeAttachment(part, attachmentFolder);
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
            
        }
        catch(Exception e)
        {
            log.error(e);
        }
    }

    /**
     * Saves an attachment part to a file on disk
     * @param part a part of the e-mail's multipart content.
     * @throws MessagingException
     * @throws IOException
     */
    private void storeAttachment(BodyPart part, String attachFolder) 
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
    }
    
}

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
package gov.nih.nlm.lpf.emails.util;

/**
 *
 * @author 
 * 
 * Date: August 8, 2012
 * 
 */

import java.io.BufferedReader;
import java.io.FileReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.Date;

import java.util.ArrayList;
    
    
public class EmailGenerator
{
    // Structure of an email message we re interested in
    // Note: in_reply_to and msg_id fields are hardcoded for testing
    private class Email
    {
         String first_line="From ";
         String status = "Status: O";
         String xstatus = "X-Status:";
         String date =  "Date:";
         String from = "From:";
         String xsender = "X-Sender:";
         String to = "To: ReUnite@nlm.nih.gov";
         String subject  = "Subject:";
         String in_reply_to =  "In-Reply-To: <002301c01843$18977b30$5f09a78f@yyyy.gmail.com>";
         String msg_id =  "Message-ID: <22884961.1.1346257806980.JavaMail.yyy@zzz.com>";
         String mime="MIME-Version: 1.0";
         String content_type = "Content-Type: text/plain; charset=us-ascii";
         String body = "";

         // Create te text representation of the (populated) email message 
         public String getTextForm()
         {   
             String str =  new String(
             first_line+"\n"+status+"\n"+xstatus+"\n"+
             date+"\n"+from+"\n"+xsender+"\n"+
             to+"\n"+subject+"\n"+in_reply_to+"\n"+
             msg_id+"\n"+mime+"\n"+content_type + "\n\n");
             
            // Check if body contains more than one line
           String bodyStr = cvtLineTerminators(body);
           str = str+bodyStr;
           return str;
         }
    }   
    
    static String cvtLineTerminators(String s)
    {
        StringBuffer sb = new StringBuffer(80);
        int oldindex = 0, newindex;
        while ((newindex = s.indexOf("\\n", oldindex)) != -1)
        {
            String substring = s.substring(oldindex, newindex);
            sb.append(substring);
            if (substring.length() > 0 && !substring.matches("$[\\.\\;\\,]"))
                sb.append(".");
            oldindex = newindex + 2;
            sb.append('\n');
        }
        sb.append(s.substring(oldindex));
        return sb.toString();
    }




    
    static String[] months = {
        "Jan", "Feb", "Mar", "Apr", "May", "June",
        "July", "Aug", "Sep", "Oct", "Nov", "Dec"};
                
    
    BufferedReader msgFile = null;
    int status = 0;
    
    String emailOutputDir = null;
    int emailsWritten = 0;
    
    /*************************************************************/
    public EmailGenerator(String inputFile, String outputDir)
    {
        try
        {
            msgFile  = new BufferedReader(new FileReader(inputFile));
            
            String filename = new File(inputFile).getName();
            File outdir = new File(outputDir);
            if (!outdir.exists()) 
               outdir.mkdirs();
           emailOutputDir = outputDir;
        }
        catch (Exception e)
        {
            e.printStackTrace();    
            return;
        }
        status = 1;
    }
    
    public int getStatus()
    {
        return status;
    }
        
    
    /*******************************************************************/
    public int generateEmails(int numEmails)
    {
        int nrec = 0;
        try
        {
            while ( (numEmails == -1)  || (nrec < numEmails))
            {
                String line = msgFile.readLine();
                if (line == null)
                    break;
                Email email  = generateEmail(line);
                if (email != null)        // ignore the message if status == 0 or -1
                    emailsWritten++;
            }
            msgFile.close();
            System.out.println("Number of emails generated: " + emailsWritten);
            return 1;
        }
        catch(Exception e)
        {
            e.printStackTrace();
           return  0;
        }
    }
   
    /*****************************************************************************************/
    // parse the line and create an email message
    // the message fields are separated by "|", and are as follows:
    //      Complexity number (optional),  note_id,  person_uuid,
    //      name,  status,  location,  message
    //*******************************************************************************************/
    protected Email   generateEmail(String msgString)
    {
        if (msgString.startsWith("--"))
            return null;           // a comment line
        String[] segments = msgString.split("\\|");
        if (segments.length < 9)
        {
            System.out.println("Incomplete input fields: " + msgString );
            return null;
        }
        // make each file name by concatenating the note_id and person_id
        String emailFileName = segments[0].replace(".", "")+ "_"
                                                    + segments[1].replace(".", "")+".eml";
        String emailFilePath = emailOutputDir+"/"+emailFileName;
        
        int startSeg = 0;
        Email email = new Email();
        String body = segments[segments.length-1];  // the last segment;
        // remove all quotes in the start and end of body - if present
        body = body.replaceAll("^(\")", "").replaceAll("(\")$", ""); 
        email.body = body;      
        
        // create a subject line with a person's name, status and location etc.
        String author_address = segments[startSeg+2];
        if (author_address.equalsIgnoreCase("NULL"))
        {
            // create a fake one
            String author_name =segments[startSeg+3]; 
            author_address = author_name.replaceAll(" ", "_").toLowerCase()+"@google.com";
        }
        email.from += " " +author_address;  
        
        String date = segments[startSeg+4];
        String formattedDate = formatDate(date);
        email.date += " " +formattedDate + " 4000";         // just add the sample last part
        
        String lp_name = segments[startSeg+5];
        String lp_status = segments[startSeg+6];
        String lp_location = segments[startSeg+7];
        
        String subject = "";
        if (!lp_name.equalsIgnoreCase("NULL"))
            subject += " " +lp_name;
        
        // encode status
        if (lp_status.equalsIgnoreCase("believed_alive"))
            subject += "  is alive";
        else if (lp_status.equalsIgnoreCase("believed_dead"))
            subject += "  is dead";
         else if (lp_status.equalsIgnoreCase("believed_missing"))
            subject += "  is missing";
        else if (lp_status.equalsIgnoreCase("is_note_author"))
            subject += "  is okay";
        else if (lp_status.equalsIgnoreCase("information_sought"))
            subject += "is sought";
        
        // encode location
        if (!lp_location.equalsIgnoreCase("NULL"))
            subject += " at " + lp_location;
        email.subject += " " +subject;
        email.first_line += author_address + " " + formattedDate + " -4000";
        int status = writeEmail(email, emailFilePath);
        if (status == 0)
            return null;

        return email;
    } 

        /****************************************************************************************/
    // convert from "dd/mm/yy mm:ss  format to email date format
    protected String formatDate (String numberDate)
    {
        String[] seg = numberDate.split(" ");
        String[] parts = seg[0].split("[/-]");
        String month ="";
        if (parts[0].length() == 4)  // in format yyyy-mm-dd
           month = months[ Integer.parseInt(parts[1])+1];
        else    //in format mm/dd/yyyy
            month = months[ Integer.parseInt(parts[0])+1];
        String strDate = month + " " + parts[1]+", "+parts[2] +" " + seg[1];
        return strDate;
    }  
        
    /****************************************************************************************/
    public int  writeEmail(Email email, String emailFilePath)
    {
         try
         {
            File emailFile = new File(emailFilePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(emailFile));

            String emailStr = email.getTextForm();
            writer.write(emailStr);
      
            writer.flush();
            writer.close();
         }
         catch (Exception e)
         {
            System.out.println("Could not write email to file " + emailFilePath);
            e.printStackTrace();
            return 0;
         }
         return 1;
    }
        
    /************************************************************************************************
     * @param args the command line arguments
     * Arguments: 0 => Input message file
     *                       1 => Output directory for emails
     *                       2 => Number of message to process. if not given: all
     *************************************************************************************************/
    public static void main(String[] args)
    {
        // default
         //String msgFile =  "C:/DevWork/LPF/Development/EmailProc/data/inputDB/christchurch_samples.log";
         String msgFile =   "C:/DevWork/LPF/Development/EmailProc/data/inputDB/philippines_flood.log";
         String outputDir = "C:/DevWork/LPF/Development/EmailProc/data/outputEmails";
         int numEmails = -1;          // all
         
        int numArgs = args.length;
        if (numArgs >= 1)                       // input file name or number of messages
        {
             if (args[0].indexOf("/") >=  0  || args[0].indexOf(".") > 0 )    
             {
                msgFile = args[0];
             }
             else
                   numEmails = Integer.parseInt(args[0]);
         }   
        
        if (numArgs >= 2)        // input file name, and (output dir name or number of messages)
        {
             if (args[1].indexOf("/") >=  0  || args[1].indexOf(".") > 0 )    
             {
                outputDir = args[1];
             }
             else
                   numEmails = Integer.parseInt(args[1]);
         }    
            
        if   (numArgs == 3)   // input file name, output dir name,  number of messages
             numEmails = Integer.parseInt(args[2]);
             
        else    if ( numArgs > 3)
        {
            System.out.println("Arguments are: input msg file, output email directory, number of messages to process");
            System.exit(-1);
        }
        
          
         EmailGenerator emailGen = new EmailGenerator(msgFile, outputDir);
         emailGen.generateEmails(numEmails);
    }
}

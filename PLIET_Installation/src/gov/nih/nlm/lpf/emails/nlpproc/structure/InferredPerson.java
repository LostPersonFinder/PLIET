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
package gov.nih.nlm.lpf.emails.nlpproc.structure;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;

/**
 *
 * @author 
 */

import java.util.ArrayList;


// Structire with all  retrieved information about a lost person
// It represents either a Reported person, or a reporter or both

public class InferredPerson implements  Comparable <InferredPerson>
{
    public static int REPORTER = 1;
    public static int REPORTED_PERSON = 2;
    public static int REPORTER_REPORTED = 3;       // both types
    public static int NOT_PRESENT = 4;                        // not provided in the data
    public static int UNKNOWN_TYPE = 5;                     // not known or resolved
     
    /***************************************************************/
    // Required data
    public TextAnchor person;                           // The  reported person's name
    public String age;
    public String gender;                                    // male /female/unknown
    public ArrayList<TextAnchor>  locations;      // if a location mentioned
    public String healthStatus;                              // as derived by the inference engine
    public TextAnchor  reporter;                           // reporting person
    
    // ancillary information
    public int type;
    public String  adjective;
    public TextAnchor verb;
    public String verbString;                                   // as in message
    public boolean isNegative;                              // verb String preceded with a -ve word (not seen, don't know, etc.)
    public  ArrayList<SubjectEntity>  refferingSubjects;     // LPFSubject instances referring to it
    
    // For a missing person
    // for a reporting person
    public String reportType;                                 // statement, seek info, question, etc.
    
    public int confidenceRanking;

    public InferredPerson(int ptype) 
    {
        this.type = ptype;                           // type of person: reporter, reported, both etc.
        confidenceRanking = 0;              // to be set later for Reported Persons
    }
    
    //----------------------------------------------------------------------------------------------------
    // compare two Inferred Persons according to their ranking and 
    // return result  in "descending" order
     public int  compareTo ( InferredPerson other)
     {
         if (this.confidenceRanking == other.confidenceRanking)
             return 0;
         return (this.confidenceRanking <  other.confidenceRanking) ? 1 : -1;
     }  
  //----------------------------------------------------------------------------------------------------
     
    public String toString()
    {
          String str = "";
        if (type == REPORTED_PERSON  || type == REPORTER_REPORTED)
        {
            String locstr = "";
            if (locations != null  &&  !locations.isEmpty())
            {
                for (int i = 0; i < locations.size(); i++)
                {
                    if (i > 0) locstr += "; ";
                   locstr +=  locations.get(i).getTextWithAppos();
                }
             }
                    
            
                str =  "ReportedPerson:" 
                        +"\n   Name:" + person.getFullTextWithConjunct()
                        +"\n   HealthStatus: " + (healthStatus  == null ? "unknown" : healthStatus)
                        +"\n      (from information: " + verbString +  (isNegative ? " - negative" : "") +")"
                        +"\n   Age:" + (age == null ? "unknown" : age)
                        +"\n   Gender: "+ (gender  == null ? "unknown" : gender) 
                        +"\n   Location: " +  ( (locations == null  || locations.isEmpty()) ? "--" :  locstr)
                        + "\n  ** Confidence Ranking: " +  confidenceRanking +"/100 ** "
                        +"\n   ---------------------------------------------------------------";
        }
  /*     if (type == REPORTER  || type == REPORTER_REPORTED)
       {
                 str +=  "\n<Reporter>" + " \n <name>" + person.getFullTextWithConjunct()+"</> "
                        +"\n  <reportedStatus>" + reportingStatus + "</>"
                        +"\n <reportType>" + reportType + "</>"
                        +"\n  <location>" + (locations == null ? "--" : locations.get(0).getTextWithAppos()) + "</>"  
                     +"\n  <reportedPerson>" + (persons == null ? "--" : persons.get(0).toString()) + "</>"  
                     +"\n</Reporter>";
        }*/
    /*   if (linkedSubject != null)
       {
           String tag = ( type == REPORTER ? "<ReportedPerson>"  : "<Reporter>" );
           String endTag = ( type == REPORTER ? "</ReportedPerson>"  : "</Reporter>") ;
           String subjectStr =( linkedSubject.getSubject() == null) ? "NULL" : linkedSubject.getSubject().getFullTextWithConjunct() ;
           str += "\n" + tag + subjectStr
               + "\n  <verbString>"+  linkedSubject.assertions.get(0).verb.getFullTextWithConjunct()
               +"</>" + " <negative> " + isNegative + "</>\n "+endTag;
       }*/
        return str;
    }
    
    public  String getName()
    {
          return person.getPhraseText();
    }
    
    public String getStatus()
    {
         return (healthStatus  == null ? "unknown" : healthStatus);
    }
    
    public String getAge()
    {
         return (age == null ? "unknown" : age);
    }
    
    public String[] getLocations()
    {
         if (locations == null  || locations.isEmpty()) 
             return null;
         
         int n = locations.size();
         String[]  locstr = new String[n];
         for (int i = 0; i < n; i++)
            locstr[i]  =  locations.get(i).getTextWithAppos();
         return locstr;
    }
    
    public String[] getGeoLocations()
    {
         return (null);
    }
    
    public String getGender()
    {
         return gender;
    }
 
}
     

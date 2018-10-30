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

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NounAnchor;

/**
 *
 * @author 
 */
public class PersonAttributes implements NERConstants
{
    public TextAnchor   person;                        //  person  to whom these attributes apply 
    public NounAnchor name;                          // person's name
    public NounAnchor location;                       // person's location
    public TextAnchor statusWord;                   // health status related word
    public TextAnchor age;
    public TextAnchor marks;
    public TextAnnotation   anaphorSubject;  //  subject of these attribute (may be anaphoric)
    public String  gender;                                   // male, female, unknown
    
    // constructor
    public PersonAttributes(TextAnchor subject)
    {
        person = subject;                               // person to whom ehe attributes are assigned
        gender = "";
    }     
    
    // Add the given attribute to this instance
    public void addAttribute(String type,  TextAnchor  value)
    {
         if (type.equals(NAME_ATTR))
         {
             name =  (NounAnchor)value;
         }
         else if (type.equals(LOCATION_ATTR))
         {
             location = (NounAnchor)value;
         }
         else if (type.equals(AGE_ATTR))
         {
             age = value;
         }
          else if (type.equals(MARKS_ATTR))
         {
             marks  = value;
         }
         else if (type.equals(STATUS_ATTR))
         {
            statusWord  = value;
         }
         System.out.println(this.toString());
    }
    
    // Note: Gender is not retrieved directly from Attribute Annotation, but indirectly
    // from the  FeatureMap of a Person. So, it  has no Anchors and is created/set differently .
      public void addAttribute(String type,  String  value)
      {
          if (!type.equals("gender"))
              return;
           gender = value;
           System.out.println(this.toString());
      }   
    
    /*-----------------------------------------------------------------------------------*/
    // Merge attributes of a single person (or its coref) from another one
    public int mergeAttributes(PersonAttributes  pattr)
    {
        if (name == null && pattr.name != null)
            name = pattr.name;
        
          if (location == null && pattr.location != null)
            location = pattr.location;
          
         if (statusWord == null && pattr.statusWord != null)
            statusWord = pattr.statusWord;
         
          if (age == null && pattr.age != null)
            age = pattr.age;
          
          if (gender.length() == 0 && pattr.gender.length() > 0)
            gender = pattr.gender;

           return 1;
    }
    
    public  String toString()
    {
        String str  = ""; 
        str += "Person: " + person.getPhraseText();
        str += ", Name: " + (name  == null ? "null" : name.getCoveringText());                             // person's name
        str += ", Location: " +  (location == null ? "null" : location.getTextWithAppos());             // person's location
        str += ", Age: " + (age == null  ? "null" : age.getTextWithAppos());  
        str += ", Gender "+ gender;
        str += ", StatusWord:  " +   ( statusWord == null ? "null" : statusWord.getCoveringText());
        return str;
    }
}
    

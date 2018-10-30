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
package    gov.nih.nlm.lpf.emails.rule;

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import   gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import   gov.nih.nlm.lpf.emails.nlpproc.nlp.NounAnchor;
import   gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import   gov.nih.nlm.lpf.emails.util.Utils;

import  java.util.ArrayList;

/**
 *
 * @author  
 */
public class NameRule
{
    public static String getPersonName(TextAnchor  name) {return "";}
    
    NounAnchor nameField;
    public NameRule(NounAnchor aname)
    {
        nameField = aname;
    }
    
    // return the name of the person, eliminating associated terms (pronoun: my, or  relations (brother, friend...)
    public TextAnnotation[]  getPersonNameAnnots()
    {
        TextAnnotation[] personAnnotations = nameField.getCoveringAnnotationArray();
        ArrayList<TextAnnotation> nameAnnotList = new ArrayList();  
        for (int i = 0; i < personAnnotations.length; i++)
        {
            TextAnnotation annot = personAnnotations[i];
            if (TextAnchor.isPronoun(annot) || TextAnchor.isAdjective(annot))
                continue;
            else if (TextAnchor.isCardinal(annot) || TextAnchor.isNumber(annot))
                continue;
            else if (isTextARelative(annot.text))
                continue;
           else
            {
                nameAnnotList.add(annot);
            }
        }
        TextAnnotation[] nameAnnots = new TextAnnotation[nameAnnotList.size()];
        nameAnnotList.toArray(nameAnnots);
        return nameAnnots;
    }
    
    //--------------------------------------------------------------------------------------------//
     public String getPersonName()
     {
         TextAnnotation[] nameAnnots = getPersonNameAnnots();
         if (nameAnnots == null || nameAnnots.length == 0)
             return "";
         String pname = nameAnnots[0].text;
         for (int i = 0; i < nameAnnots.length; i++)
             pname += " " + nameAnnots[i].text;
         return pname;
     }
     
     //------------------------------------------------------------------------------------------------------
     // Check if the text matches relationship with a person 
     public static boolean  isTextARelative(String text)
     {
         text = text.toLowerCase();
         if (text.matches(NERConstants.PersonRelative_male) || 
             text.matches(NERConstants.PersonRelative_female) ||
             text.matches(NERConstants.PersonRelative_either))
             return true;
         
         // may be a plural, convert ot singu;at (assuming "s")
         String singular = text.replaceAll("(s)$", "");
         if (singular.equals(text))
             return false;          // already a singular term
         
         return (singular.matches(NERConstants.PersonRelative_male) || 
             singular.matches(NERConstants.PersonRelative_female) ||
             singular.matches(NERConstants.PersonRelative_either));
     }
//------------------------------------------------------------------------------------------------------
 public static boolean isTextAPronoun(String text)
 {
     text = text.toLowerCase();
     if (text.matches(NERConstants.FEMALE_PRONOUN) ||
        text.matches(NERConstants.MALE_PRONOUN) )
         return true;
     else if (Utils.isInList(text, NERConstants.ANONYMOUS_PERSON_TYPES) ||
         Utils.isInList(text, NERConstants.FIRST_PERSON_TYPES) ||
          Utils.isInList(text, NERConstants.GENERIC_PERSON_NOUNS))
         return true;
     else
         return false;
 }
 
 //------------------------------------------------------------------------------------------------------
 public static boolean isTextANonPerson(String text)
{          
    text = text.toLowerCase();       
    if (Utils.isInList(text, NERConstants.ENQUIRY_OBJECTS)
             || Utils.isInList(text, NERConstants.ASSISTANCE_OBJECTS)
             || Utils.isInList(text, NERConstants.ENVIRONMENTAL_OBJECTS))
        return true;
    return false;
 }
 
//------------------------------------------------------------------------------------------------------
// Get the First, middle and last name of a person
// Note: If Jumior or Senior are at the end, they are added to the last name
  public static String[] getNameComponents(String aname)
  {
        // discard salutations at rge front
      aname = aname.replaceAll("(?i)(mrs|mr|miss|ms|dr|sri|shri|smt)", "");      // discard salutations
      aname = aname.replaceAll("^(\\.*\\W+)", "");       // any leftover punctuarions
      String postfix = "(?i)(senior|sr|junior|jr)[.,]+";
      String firstName = ""; 
      String middleName = "";
      String lastName = "";
      String[] emptySegs  = new String[]{"", "", ""};
      if (aname.length() == 0)
          return emptySegs;                 // all empty
      
      String[] words = aname.split("\\s+");
      int n = words.length;
      int mn = n;               // totat length  to consider for  middle name/last name
      
      firstName = words[0];
      if (n == 1)
      {
          return new String[] {firstName, "", ""};
      }
      
      // If a Sr. Jr. etc. term occurs, add to the first name
      String last = words[n-1];
      if (last.matches(postfix))
      {
          firstName  = words[0] + " " + last;
          mn = n-1;          // don't consider the last word any more
      }
      if (mn > 1)
          lastName = words[mn-1];        // the current last word
      // check if the previous part is a connector such as "-", or ",";
      if (mn > 3)
      {
          if (words[mn-2].equals("-") || words[mn-2].equals("'"))
          {
              lastName = words[mn-3]+words[mn-2]+words[mn-1];
              mn =mn-2;         // for checking the end of middle name
          }
      }
      if (mn > 2)
      {
          middleName  = words[1];
          for (int i = 2; i < mn-1; i++)
              middleName += " " + words[i];
      }
      firstName = firstName.replaceAll("(,)$", "");
      middleName = middleName.replaceAll("(,)$", "");
      lastName = lastName.replaceAll("(,)$", "");
      return  new String[] {firstName, middleName, lastName};
  }
  
  public static void main(String[] args)
  {
      
      String[]  names = {"Mr. James MacBride", "Mrs Sarala Devi Jain", "Smt. Mina Devi", 
          "George W. Washington",  "John Smith, Jr.", "Gregory  O'Hara", 
          "Alicia Mary Rotherham", "Mary Smythe-Miller", "Lisa"};
      
      for (int i = 0; i < names.length; i++)
      {
          String[] segments = NameRule.getNameComponents(names[i]);
          System.out.println("Name: " + names[i] + ", First name: " + segments[0] + ", Middle name: " + 
              segments[1] + ", Lastname: " + segments[2]);
      }
      System.exit(0);
  } 
}

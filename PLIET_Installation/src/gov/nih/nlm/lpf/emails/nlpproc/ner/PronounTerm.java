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
package gov.nih.nlm.lpf.emails.nlpproc.ner;

/**
 *
 * @author 
 */

// import   gov.nih.nlm.ceb.lpf.emails.nlp.pronominal.PronounTerm;
import java.util.HashMap;

public class PronounTerm
{
    //------------- Static Constants -----------------------------//
        // Count
    public  final static int SINGULAR = 1;
    public  final static int PLURAL = 2;
    public  final static int SINGULAR_PLURAL = 3;          // both singular and plural
   
    // Type
    public  final static int PERSON = 1;
    public  final static int LOCATION = 2;
    public final static int  NON_PERSON = 3;            // Either location or an inanimate object
    public  final static int OTHER_REF = 4;            // Other things excluding persons and places
    
      //Gender - for PERSON onl
    public  final static int MASCULINE = 1;
    public  final static int FEMININE  = 2;
    public  final static int ANY_GENDER = 3;          // both singular and plural
    public  final static int NO_GENDER = 0;            // inanimate 
   
    // Subject type
    public  final static int SUBJECT = 1;
    public  final static int OBJECT = 2;
    public  final static int SUBJECT_OBJECT = 3;       // either subject or object
 
    public static HashMap <String, PronounTerm> PronounMap = null;
    
    /*************************************************************************************
     * Internal structure  to represent a Single Pronoun word
     */
  //     public static class PronounTerm
 //      {   
              // local variables  
            public String word;                       // a given pronoun

            // properties of a pronoun
            public int numberRef ;               // singular or plural,
            public int typeRef;                    // person, place,...
            public int genderRef;              // male, female, either
            public int subjectRef;

            // booleans
            public boolean isReflexive;                //myself,  yourself
            public boolean isInterrogative;         // who, where, when...
            public boolean  isPossessive;         // my, mine, his her, hers    
            public boolean isSubjective;


            //---------------------------------------------------------------------------------------------------------------
            // Constructor

            public PronounTerm(String wd, int count, int type, int gender, int subject, 
                boolean reflexive, boolean interrogative, boolean possessive, boolean subjective)
            {
                word = wd;
                numberRef = count; typeRef = type; genderRef = gender; subjectRef = subject;
                isReflexive = reflexive; isInterrogative = interrogative; 
                isPossessive = possessive; isSubjective = subjective;
            }

     //---------------------------------------------------------------------------------------------------------------
   
    // Static Table of  relevant pronouns
    
    public static PronounTerm[] pronouns = new PronounTerm[] {
        // Personal Pronouns, subject
        new PronounTerm("i", SINGULAR,  PERSON, ANY_GENDER, SUBJECT, false, false, false, true),
        new PronounTerm("we", PLURAL, PERSON, ANY_GENDER, SUBJECT, false, false, false, true),
        new PronounTerm("you", SINGULAR_PLURAL,  PERSON, ANY_GENDER, SUBJECT_OBJECT, false, false, false, true),
        new PronounTerm("he", SINGULAR,  PERSON, MASCULINE, SUBJECT, false, false, false, true),
        new PronounTerm("she", SINGULAR,  PERSON, FEMININE, SUBJECT, false, false, false, true),
        new PronounTerm("they", PLURAL,  PERSON, ANY_GENDER, SUBJECT, false, false, false, true),
        
        // Personal pronouns: Object
        new PronounTerm("me", SINGULAR,  PERSON, ANY_GENDER, OBJECT, false, false, false, false),
        new PronounTerm("us", PLURAL,  PERSON, ANY_GENDER, OBJECT, false, false, false, false),
         new PronounTerm("him", SINGULAR,  PERSON, MASCULINE, OBJECT, false, false, false, false),
        new PronounTerm("her", SINGULAR,  PERSON, FEMININE, OBJECT, false, false, false, false),
        new PronounTerm("them", PLURAL,  PERSON, ANY_GENDER, OBJECT, false, false, false, false),
        
        //Possessive Personal
         new PronounTerm("my", SINGULAR,  PERSON, ANY_GENDER, SUBJECT, false, false, true, true),
         new PronounTerm("our", PLURAL, PERSON, ANY_GENDER, SUBJECT, false, false, true, true),
        new PronounTerm("your", SINGULAR_PLURAL,  PERSON, ANY_GENDER, SUBJECT, false, false, true, true),
        new PronounTerm("his", SINGULAR,  PERSON, MASCULINE, SUBJECT, false, false, true, true),
        new PronounTerm("her", SINGULAR,  PERSON, FEMININE, SUBJECT, false, false, true, true),
        new PronounTerm("their", PLURAL,  PERSON, ANY_GENDER, SUBJECT, false, false, true, true),
        
        // Reflexive Personal
        new PronounTerm("myself", SINGULAR,  PERSON, ANY_GENDER, SUBJECT, true, false, false, true),
         new PronounTerm("ourselves", PLURAL, PERSON, ANY_GENDER, SUBJECT, true, false, false, true),
        new PronounTerm("yourself", SINGULAR_PLURAL,  PERSON, ANY_GENDER, SUBJECT, true, false, false, true),
        new PronounTerm("yourslves", SINGULAR_PLURAL,  PERSON, ANY_GENDER, SUBJECT, true, false, false, true),
        new PronounTerm("himself", SINGULAR,  PERSON, MASCULINE, SUBJECT, true, false, false, true),
        new PronounTerm("herself", SINGULAR,  PERSON, FEMININE, SUBJECT, true, false, false, true),
        new PronounTerm("themselves", PLURAL,  PERSON, ANY_GENDER, SUBJECT, true, false, false, true),
        
        // Interrogative Personal Pronouns
        new PronounTerm("who", SINGULAR_PLURAL,  PERSON, ANY_GENDER, SUBJECT,  false, true, false, true),
        new PronounTerm("whose", SINGULAR_PLURAL,  PERSON, ANY_GENDER, SUBJECT_OBJECT,  false, true, false, true),
        new PronounTerm("whom", SINGULAR_PLURAL,  PERSON, ANY_GENDER, OBJECT,  false, true, false, false),
        
        // Non-Person
         new PronounTerm("it", SINGULAR,  NON_PERSON, NO_GENDER, SUBJECT_OBJECT, false, false, false, true),     
         new PronounTerm("its", SINGULAR,  NON_PERSON, NO_GENDER, SUBJECT_OBJECT, false, false, true, true),
         new PronounTerm("this", SINGULAR,  NON_PERSON, NO_GENDER, SUBJECT_OBJECT, false, false, true, true),
          new PronounTerm("that", SINGULAR,  NON_PERSON, NO_GENDER, SUBJECT_OBJECT, false, false, false, true),
         new PronounTerm("these", PLURAL,  NON_PERSON, NO_GENDER, SUBJECT_OBJECT, false, false, false, true),
         new PronounTerm("those", PLURAL,  NON_PERSON, NO_GENDER, SUBJECT_OBJECT, false, false, false, true),
          new PronounTerm("here", SINGULAR,  LOCATION, NO_GENDER, SUBJECT_OBJECT, false, false, true, true),
         new PronounTerm("there", SINGULAR,  LOCATION, NO_GENDER, SUBJECT_OBJECT, false, false, true, true),
         
         // Interrogative
         new PronounTerm("what", SINGULAR,  NON_PERSON, NO_GENDER, SUBJECT, false,true,  false,  true),
         new PronounTerm("which", SINGULAR,  NON_PERSON, NO_GENDER, SUBJECT_OBJECT, false, true, false, true),
         new PronounTerm("where", SINGULAR,  LOCATION, NO_GENDER, SUBJECT_OBJECT, false, true, true, true),
        
         // TBD: Indefinite Pronouns: Not needed for LPF (???)
         
    };
    
    // Build the static table
    public static  void buildPronounMap()
    {
        if (PronounMap != null)
            return;
       
        PronounMap = new HashMap();
        for (int i = 0; i < pronouns.length; i++)
        {
            PronounMap.put(pronouns[i].word, pronouns[i]);
        }
    }
    
    public static PronounTerm getPronounTerm(String pword)
    {
          if (PronounMap == null)
              buildPronounMap();
          return PronounMap.get(pword);
    }

}

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
public interface  NERConstants
{
    public static int  CLAUSAL_SUBJECT = 1;                     // used as subject in a clause
    public static int  CLAUSAL_OBJECT = 2;                         // used as the object in  a clause
    public static int  CLAUSAL_SUBJ_OBJ = 3;                    // both a subject and an object in (multiple) clauses
    
    // The person may not refer to a real person, but other type of data
    // classification of the person
    public static int NAMED_PERSON = 11;                                // name of a person 
    public static int ANONYMOUS_PERSON  = 12;                   // anonymous person - for enquiry
    public static int PERSONAL_DATA_TYPE = 13;                  // information about a  person (name,  age, ...)
     public static int ENVIRONMENTAL_DATA_TYPE = 14;    // information about a  person (name,  age, ...)
    public static int  GENERAL_INFO_TYPE = 15 ;                   // general info - as in NON_PERSON_OBJECTS 
    public static int  OTHER_INFO_TYPE = 16;                         // not  the above two categories
    
    public static String[] ANONYMOUS_PERSON_TYPES =  new String[] {
            "anyone", "someone", "anybody", "somebody", "any person", "some person",  "you", "your", "yours", "they", "their"};
    public static String[] FIRST_PERSON_TYPES = {
            "I",  "me", "my", "mine",  "myself", "we", "us", "our", "ours", "ourselves"};
    
    
    // Singular third person male/female 
    public static String FEMALE_PRONOUN = "she|her|herself";
    public static String MALE_PRONOUN = "he|him|his|himself";
    
    
    public static String[] ENQUIRY_OBJECTS =  new String[]{"information", "news", "contact", "whereabouts",
            "everything", "things", "nothing", "status", "condition"};
      
    public static String[] ENVIRONMENTAL_OBJECTS =  new String[]{"flood",  "earthquake","cyclone", "hurricane", "tsunami",  
         "fire", "wind", "home", "road", "bridge", "town"};
    
    public static String[]  ASSISTANCE_OBJECTS  = new String[] {"treatment", "help",
            "assistance", "rescue", "treatment", "food", "water", "medicine", "supply",  "Red Cross"};

   // public static String[] PERSONAL_DATA = new String[] {"name", "address",  "contact", "location", "age", "eye color"};
    
    public static String[] STATUS_DATA = new String[] {"health", "condition", "shape", "environment", "situation", "surrounding"};
    
    
    // Identifier for various personal data, 
    public static String NAME_ID = "name|named";
    public static String LOCATION_ID = "location|address|contact";
    public static String AGE_ID = "age|years|old";
    public static String MARKS_ID = "eye color";
    public static String STATUS_ID = "health|condition|situation|prognosis";
    
    public static String NAME_ATTR = "name";
    public static String LOCATION_ATTR = "location";
    public static String AGE_ATTR = "age";
    public static String MARKS_ATTR = "marks";
    public static String STATUS_ATTR = "healthStatus";
    
    public static String AGE_COMPLEMENT = "year";
      
   // Note: also includes all relatives not mentioned here.
     public static String[]  GENERIC_PERSON_NOUNS = {"man", "men",  "woman", "women", "person","persons",
         "child", "children", "boy",  "boys", "girl", "girls", "friend", "friends", "relative", "relatives"};
    
    
    
    public static String[]  NONPERSON_NOUNS = {
            "information", "news", "contact", "whereabouts", "status",
            "everything", "things", "nothing", "disaster",  "flood", "wind", "tornado", "fire", "earthquake",
            "phone", "cell phone", "mailbox",  "inbox", "help"
    };
    
  // Adjectival modifiers for a person's name indicating a relationship  (e.g. my son Johm Smith)
    public static String PersonRelative_female = 
        "(grand)?mother|sister|(grand)?daughter|neice|stepdaughter|stepmother|wife|aunt|aunties|girl|girlfriend";
    public static String PersonRelative_male =
        "(grand)?father|brother|(grand)?son|nephew|stepson|stepfather|husband|uncle|boy|boyfriend";
    public String PersonRelative_either = 
        "(grand)?parent|(grand)?(child|children)|spouse|sibling|family|friend|in(-)?law|"
        +"relative|neighbo(u)?r|attorney|doctor|teacher|senior|junior";
    
    public static String KNOWN_GENDERS = "male|female";
    
    // Classification of a clausal subject depending upon the information is conveys
    public static int PERSON_ENTITY = 1;           //subject referes to a person by name  [John is fine, The girl is injured]
    public static int PERSONAL_ATTRIB_ENTITY = 2;     // subject refers to attributes of  a person [My name is John.]
    public static int NONPERSON_ENTITY = 3;                  // subject is a known LPF noun,  not a  person [Information sought for John]
    public static int UNKNOWN_TYPE_ENTITY = 4;         // subject is not of interest to LPF [The sky was blue.]
    
    // The following should be "OR"ed with any of the above (really with PERSON_ENTITY)
    // since the same subject may contain both types of information (David is lost. He is 10 years old")
    public static int OBJECT_ATTR_ENTITY = 8;            // Object (Predicate) of the clause contains attribute

}

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
package gov.nih.nlm.lpf.emails.nlpproc.nlp;

/**
 *
 * @author 
 */
public  interface ClausalConstants
{
    public static int UNKNOWN_SENTENCE_TYPE = -1;   // Not yet established  or no-clause sentence
    public static int SIMPLE_SENTENCE = 1;            // 1 subject, 1 predicate, only 1 Finite Verb  
    public static int COMPOUND_SENTENCE = 2;   // 2 or more independent (main) clauses
    public static int COMPLEX_SENTENCE = 3;        //1 main clause, One or more subordinate clauses
    
        
    // "if" is not really an interrogative word, but processed here the same way
    public String[]  interogations = {                      
       "who", "whom", "whose", "what",  "where", "when",  "why",  "which", "whatever",   "how"};
    public String[] questions = {
        "has", "have", "had", "be", "shall", "should", "would", "could", "if"};
    
    // Type of  words joining  two Compound  clauses in a sentence
    public static String[] CopulativeJoin = {"", "and", "also", "not only", "nor", "as well as"};
    public static String[] AdversativeJoin = {"but", "nevertheless", "yet", "still"};
    public static String[] AlternativeJoin = {"either", "or", "neither:, :nor", "else"};
    public static String[] InferenceJoin = {"therefore", "so", "consequently"};


   // types of  subordinate  clauses
    public static int NOUN_CLAUSE = 11;                // Clause acting as a Noun ("xsubj"/ "xsubjpass")
    public static int ADJECTIVE_CLAUSE = 12;     // Clause acting as an adjective
    public static int ADVERB_CLAUSE = 13;          // Clause acting as an Adverb

     // Further classifification of Adverb clauses, showing the usage:
    public static int  TEMPORAL_CLAUSE = 131;
    public static int  LOCATION_CLAUSE = 132;
    public static int  PURPOSE_CLAUSE  = 133;
    public static int  REASON_CLAUSE  = 134;        // indicates a clause
    public static int  CONDITION_CLAUSE  = 135;
    public static int RESULT_CLAUSE = 136;
     public static int RESULT_PRECEDE_CLAUSE = 137;
    public static int  COMPARISION_CLAUSE = 138;
    public static int  SUPPOSITION_CLAUSE = 139;

    // Words introducing different types of adverbial clauses
    public static String[] TemporalIntro = {"whenever", "when", "while", "after", "before", "since", "as"};
     public static String[] LocationIntro =  {"where", "wherever"};
     public static String[] PurposeInfo = {"so that", "so", "in order that", "in order to", "lest"};
     public static String[] ReasonIntro =  {"because", "as", "since", "that"};
     public static String[] ConditonIntro = {"if", "otherwise",  "whether", "unless", "how"};
     public static String[] ResultIntro = {"that"};;
     public static String[] ResultPrecede ={"such", "so"};
     public static String[] ComparisionIntro = {"than", "as"};
     public static String[] SuppositionIntro = {"though", "although", "even if"};
     
     // Words introducing some Noun Phrases
     String[] ApposIntro = {"that"};
     
     // Note Apposition is a grammatical construction in which two elements, normally noun phrases, 
     //are placed side by side, with one element serving to define or modify the other. 
     // my friend John , here John  is an appostion to my friend

     public static int PRIMARY_CLAUSE = 1;
     public static int  SECONDARY_CLAUSE = 2;  
     
     // Clause Attribute - way it is being used in the sentence
    public static int  STATEMENT_CLS = 1;              // top level  clause
    public static int  COMPLEMENT_CLS = 2;          // complemental clause
    public static int  OBJECT_CLS  = 3;                        // used as  an object of the  complement  term (verb)
    public static int   MODIFIER_CLS  = 4;                   // used as  a modifier of the  complement  (noun/verb/adjective)
    public static int  PARTMOD_CLS = 5;                     // participal verb modifying a noun/verb/clause
    public static int  RCMOD_CLS = 6;                           // relative clause modifying a noun
    public static int  PURPOSE_CLS  = 7;                    // used to express a purpose)
    public static int  PREPOSITION_CLS = 8;              // complement to a preposition
    public static int  UNKNOWN_DEP_CLS  = 9;         // dependency could not be resolved

}

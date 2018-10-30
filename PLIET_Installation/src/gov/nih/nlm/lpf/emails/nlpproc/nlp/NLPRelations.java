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
public interface NLPRelations
{
    public static int NOUN = 1;
    public static int ADJECTIVE = 2;
    public static int VERB = 3;
    public static int CARDINAL  = 4;                 // Cardinal number
    public static int PRONOUN = 11;                 // included in noun
    
    public static String[] VERB_CATEGORIES =  {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"}; 
    public static String[] ADJECTIVE_CATEGORIES =  {"JJ", "JJR", "JJS", "RB", "RBR", "RBS" , "WRB"}; 
    public static String[] NOUN_CATEGORIES =  {"NN", "NNS", "NNP", "NNPS","PRP",  "PRP$", "WP", "WP$", "CD"};
     public static String[] CARDINAL_CATEGORIES =  {"CD"};
    // just the pronouns
    public static String[] PRONOUN_CATEGORIES =  {"PRP",  "PRP$", "WP", "WP$"};
    
    public static String[] PERSON_NOUN_CATEGORIES = {"NNP",  "NNPS", "PRP$", "WP", "WP$"};       // has non-person pronouns too
    
    public static String pronounSet = 
        "he|she|i|you|we|they|his|him|her|hers|himself|herself|my|mine|me|myself|yours|youself|who|whom";
    
    
     /*******************************************************************************************************
    // Types of direct relationships for words and clauses, excluding auxiliary, conjuction, preposition, etc
     *******************************************************************************************************/
     //>>> VERBS: <<<
     //Word/Phrase dependenies - Note: npadvmod is for copular verbs (where governor is adjective)
    public static String[] VERB_PHRASAL_DEPENDENCIES =                // for verbs
            {"acomp", "advmod", "cop",  "dobj", "iobj","nsubj", "nsubjpass", "partmod", "npadvmod" };       
    
      // Relations which may represent a Person or Location for a  Verb 
    public static String[] VERB_CLAUSAL_DEPENDENCIES =                // for verbs
            { "advcl", "ccomp", "purpcl", "xcomp", "dep", "csubj", "csubjpass" };         
    // Note: Partmod could be a verb, to be checked from the corresponding Verb to determine a "subject"
    //  "dep" is for resolving hard to define relationships such as in "to my brother Mchael "
    //  where brother is the governor and Michael is "dep"  
    
    //>>> NOUNS <<<
    // Note: infmod referes to "infinitival" verbs, i.e. verbs preceded by "to" -> to eat, to search, ...
    public static String[] NOUN_PHRASAL_DEPENDENCIES =               // for nouns
            {"amod", "appos",  "dep", "dobj",  "infmod",  "poss", "partmod", "nsubj", "cop"};              
    
    public static String[] NOUN_CLAUSAL_DEPENDENCIES =               // for nouns
            { "rcmod"};     
    
     public static String[] ADJECT_PHRASAL_DEPENDENCIES =              
            {"xcomp", "npadvmod"};       
    
     public static String[] ADJECT_CLAUSAL_DEPENDENCIES =     // for adjectives
            {"nsubj", "cop", "advmod", "csubj", "csubjpass",  "tmod"};   
     
     public static String[] PREP_CLAUSAL_DEPENDENCIES =     // for prepositions (prepc)
            { "pcomp"};   
     
     // Adjectival dependecies of  a phrase, amod could be a verb
     public static String[] adjectiveDeps = {"advmod", "tmod", "acomp", "amod",  "infmod", "npadvmod"};
     
     public static String[] nounMods = {"amod",  "det", "neg"};     // noun modifiers, used as adjectives
     
     public static String appos = "appos";      // appositional modifier of a noun
     
    // 
   // >>> PREPOSITION <<<
    public static String  PREP = "prep";                // word introducing a prepositional phrase
    public static String  PREPC = "prepc";           // word introducing  a prepositional clause
    public static String[]  PREPS  = {"prep", "prepc"};       // both types
    
    public static String  POBJ = "pobj";                 // word/phrase following"prep"
    public static String  PCOMP  = "pcomp";        // clause introduced by "prepc"
    public static String POSS = "poss";                  // possessive relation
    public static String POSSESSIVE = "possessive";                  // possessive notation (e.g. 's)

     
     // modifiers
     public static String NEG = "neg";                      // negative modifier
     public static String NOUN_COMP = "nn";          // Noun compound modifier
     public static String TEMP_MOD = "tmod";            // temporal modifier (night, yesterday, ...}
     
     public static String DISCOURSE = "discourse";   // for unrelated expressions in informal text, e.g. "Please"
  
     public static String  NEGATIVE_ADJECTIVES = "not|last|never|neither";
     
     // conjuction related relations
     public static String CC  = "cc";
     public static String CONJ = "conj";
     
   // Relationships by which a Phrasal "Object"  is related to another object
    public static String[] PHRASAL_OBJECT_RELATIONS =  {"dobj", "iobj", "prep", "copobj"};   
    public static String COPULA_LINK = "copobj";        // synthetic link for copula
     
    public static String[] PERSON_PREPS = {
        "for", "with" };

     
    // prepositional relations indicating a location refering to a town, city, state, river, mountain, etc. as  (some are also used for other relations)
     public static String[] LOCATION_PREPS = {
          "at", "along", "around", "beside", "beyond", 
          "in", "inside",  "in front of", "near", "nearby",  "outside", 
           "toward", "towards", "within",  "through"};

     // may be location or person or time etc.  -  context dependent
     public static String[] COMMON_PREPS = {"about",  "by", "from", "of", "to"};

     // Types of relations indicating an object
     public static String[] OBJECT_TYPES = {"dobj", "iobj", "pobj"};
     
     // Annotation for the morphological root of a Token
     public static String  MORPHO_ROOT  = "morph";     
     
     // A primary or complementary Verb, and their subclassifications in a sentence
     public static int PRIMARY_VERB = 1;
     public static int  COMPLEMENTARY_VERB = 2;
    
    public int PRIMARY_TYPE= 10;
    
    // Clauses dependencies
    public int ADVCL_TYPE = 11;
    public int CCOMP_TYPE= 12;
    public int CONJ_TYPE = 13;
    public int CSUBJ_TYPE = 14;                   // Unresolved dependency of a verb
    public int CSUBJPASS_TYPE = 15;                   // Unresolved dependency of a verb
    public int DEP_TYPE = 16;                   // Unresolved dependency of a verb
  
    public int PCOMP_TYPE = 17;
    public int PREPC_TYPE = 18;
    public int XCOMP_TYPE = 19;
    public int CC_XCOMP_TYPE = 20;    
    public int TO_XCOMP_TYPE = 21;      // Treated separately: the  verb here is a part of the parent verb, not really  independent


    public int PARTMOD_TYPE = 30;         // Adjectival modifiers for Noun (or some Verbs)
    public int RCMOD_TYPE = 31;

    
    
   /* Defined in VerbAnchor.
     * public static int[] ClauseClasses = { 
                    PRIMARY_TYPE, CCOMP_TYPE, PCOMP_TYPE,
                   XCOMP_TYPE, ADVCL_TYPE, PURPCL_TYPE, PARTMOD_TYPE, RCMOD_TYPE, DEP_TYPE};
      public static   String[] ClauseTypes = {"advcl", "dep", "ccomp",  "purpcl", "xcomp", "partmod", "rcmod"};
      public static   String[] ConnectorTypes = { "marker", "aux", "complm", "", "aux", "", ""};
     * 
     */
}

      
/*****************************************************************************
Dependency Relations between different types of Words/Phrases in a sentence

Word/Phrase:
==============
Noun: 
----
amod: (red meat)
apppos: (Sam, my brother) or (Sam (my brother))
cc: (and or but)
det: (A, the, no, some, which, ...)
infmod: Infinitive verb as modifier (I don�t have anything to say (anything, say)).
neg:
nn: (Oil future prices)
partmod: (Truffles picked during the spring - (truffles, picked))
poss
prep: preposition (I saw a cat in a hat - prep(cat, in))
tmod: temporal modifier
=>dep: Unresolved dependency: (I am looking for my brother Sam - (Sam, brother)


Verb
----
acomp (looks very beautiful) 
advmod (genetically modified) 
cc (looked and searched)
neg
partmod (Bill tried to shoot demonstrating...(shoot, demonstrating))
prep
tmod: (Last night, I swam in the pool - tmod(swam, night))

=>dep Unresolved dependency:

Adjective
--------- 
advmod(less often)
cc (alive and well)
neg
nsubj(The baby is cute (cute, baby), He is alive(alive,he))
prep
tmod
xcomp: (I am ready to leave - (ready, leave))

Preposition
-----------
pobj


Clauses
===========
Noun
-----
rcmod: (I saw the book which you bought - (book, bought) ref: which)

Verb
----- 
advcl: (If you know who did it, you should tell the teacher) [Marker: if]
ccomp: (He says that you like to swim) - [complm: complementizer = "that"]
purpcl:(He talked to him in order to secure the account - purpcl(talked, secure))
prepc: (He purchased it without paying a premium - prepc_without(purchased, paying)))
xcomp: (He says that you like to swim - (like, swim))
Adjective
---------
ccomp (I admire the fact that you are honest), (I am certain that he did it)
prepc	 


Preposition
------------
pcomp: (They heard about you missing classes - (about, missing)



========================================
Subjects:
Verb:
nsubj (Clinton defeated Dole)
nsubjpass (Dole was defeated by Clinton)
csubj: Clausal subject /A clause as a subject) (What she said makes sense.)
ccsujpass: Passive Form (That she lied was suspected by everyone- csubjpass(suspected, lied))
xsubj: External subject of the xcomp clause (Tom likes to eat fish - (eat, Tom))

Adjective:
csubj: (What she said is true.)
ccubjpass


========================================
Objects:

Verb:
dobj: (direct Object: She gave me a raise (gave, raise))
iobj: (indirect Object: She gave me a raise (gave, me))

=================================================
Notes: 
xcomp - does not have an internal subject, refers to the subject of the governor
ccomp - Has an internal subject (I admire the fact that you are honest: subject->you)

----------------------------------------------------------------------------------

*****************************************************************************/
     

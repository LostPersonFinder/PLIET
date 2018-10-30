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


import java.util.ArrayList;
import  java.util.TreeMap;
import java.util.Collection;
import java.util.Iterator;



/**
 * @author 
 * 
 */
public class PLLexicon implements LPFVerbs
{
    
    public static int STATUS_ENTRY = 1;
    public static int RESOLVED_STATUS_ENTRY = 2;
    public static int  EXTERNAL_STATUS_ENTRY = 3;           // other:, external types
    

    // Note: Category number is 1-based
    public static int getCategoryByNumber(String category)
    {
        for (int i = 0; i < VERB_CATEGORY_NAMES.length; i++)
        {
            if (category.equals(VERB_CATEGORY_NAMES[i]))
                return i+1;           // assumes sequential, should have a value array
        }
        return -1;          // not in list
    }
    
    public static String getCategoryName(int  catNum)
    {
        if (catNum < 1  || catNum > 4)
            return "";
        return VERB_CATEGORY_NAMES[catNum-1];
    }
      
    public static String getHealthStatusType(String condition)
    {
        return VerbRules.getHealthStatusType(condition);
    }

    /*---------------------------------------------------------------------------------------*/

    // set of  terms  used both as Reporting verb and Health Status, and associated attribute values
    // for distingushing them and put into the right category
    // Note that only a single attribute type will be specified for each verb category. 
    // Here we group the entryTerms for convenience of specificarion 
    
    
     //--------------  Attributes of each entry -----------------------------
    public class StatusEntry
    {
        // Note that depending upon the POS tags, some attributes may not be defined/used
        public String  entryWord;                   // The word as itself -  in lowercase
        public String morphWord;                 // morohological representation
        public String alternateForms;           // other representations of the same word, if any (e.g. okay/ok)
        public String posTag;                          // Part of speech tag: Adjective/Noun or Verb
        public int        category;                       // Information Category (HEALTH_STAUS or REPORTING)
        public String  healthCondition;          //  PL person's condition represented by the word
        public String  negativeCondition;      //  condition represented by the negative of the word (e.g. not found)   
    }
    
    // Note: here category implied category after ambiguity resolved according to resCriteria
        public class ResolutionEntry 
        {
             public String resCriteria;                // criteria used in resolving (further distingishing) the verb's category "voice/object etc  
             public String[] words;                      // set of ambiguous words being resolved
             // only one of the following populated according to resCriteria
             public String[] objects;
             public String[] objectTypes;           // Person, non-person  etc
             public String[] subjects;
             public String[] complements;
             public String[] adjectives;                // adjectives for the verb
             public String voice;                           // Active or passive     
             public String tense;                           // past, present or future
            
             // resolved information
            public int        resolvedCategory;         // (HEALTH_STAUS or REPORTING) as resolved
            public String  healthCondition;           //  PL person's condition represented by the word
            public String   negativeCondition;      //  condition represented by the negative of the word (e.g. not found)   
        }
        
 
    //-------------- LexiconObject -----------------------------
    public String lexiconSouce;          // files from which it was created
    
    public ArrayList<ResolutionEntry> resolutionList;
    public TreeMap <String, StatusEntry> statusMap;
    
    // List of  verbs/adjectives  in the PL Lexicon
    public ArrayList<String> lexiconVerbs = new ArrayList();

    public PLLexicon(String sourceFile)
    {
        lexiconSouce = sourceFile;
        statusMap = new TreeMap();
        resolutionList = new ArrayList();
    }
    
    // instantiation of private classes
    public StatusEntry createStatusEntry()
    {
        return new StatusEntry();
    }
    
    // Create an entry to hold ambiguity resolution information
    public ResolutionEntry createResolutionEntry()
    {
        return new ResolutionEntry();
    }

    public  void addResolutionEntry (ResolutionEntry entry)
    {
        resolutionList.add(entry);
        addToVerbSet(entry.words);
    }
    
    // Add an entry corresponding to a status verb
    public void addStatusEntry(StatusEntry entry)
    {
        String verb =  entry.entryWord;
        statusMap.put(verb, entry);
        addToVerbSet(entry.morphWord);
    }
   //--------------------------------------------------------------------------------------------------------
   //  Add a term to the set of health status related terms recognized by PL 
    // The term is a the morphological value of the verv
    protected void addToVerbSet(String term)
    {
        if  (!lexiconVerbs.contains(term))
            lexiconVerbs.add(term);
    }
        
    //--------------------------------------------------------------------------------------------------------  
    protected void addToVerbSet(String[]  terms)
    {
        for (int i = 0; i < terms.length; i++)
            addToVerbSet(terms[i]);
    }
    
    //--------------------------------------------------------------------------------------------------------
    // The following methods deals with matching of the Verb with Lexicon info
    // We match the verb for an exact match, and morpho is not null, also match the 
    // morohplogical root
    //-------------------------------------------------------------------------------------------------------*/   
        public StatusEntry getStatusEntry(String verb, String morph)
        {
            verb = verb.toLowerCase();
            StatusEntry entry = statusMap.get(verb);     
            if (entry != null )          // exact match
                return entry;
            
            // Iterator over all entries and check for a morphological match or alternate form match
            Collection <StatusEntry>   allEntries =statusMap.values();
            Iterator <StatusEntry> it = allEntries.iterator();
            while(it.hasNext())
            {
                entry =it.next();
                if (entry.entryWord.equals(morph))
                    return entry;
                if (entry.morphWord.length() > 0 && entry.morphWord.equals(morph))
                    return entry;
                else if( entry.alternateForms.length() > 0 && verb.matches(entry.alternateForms))
                    return entry;
            }
           return null;
        }
    //-------------------------------------------------------------------------------------------------------*/          
    // Check if a given word or its root is in the Adjective list for LPF verbs
    //-------------------------------------------------------------------------------------------------------*/
        public boolean  inAdjectiveStatusList(String word)
        {
             return inAdjectiveStatusList(word, word);
        }
     //-------------------------------------------------------------------------------------------------------*/     
       public boolean  inAdjectiveStatusList(String word, String rootWord)
       {
           StatusEntry entry = getStatusEntry(word, rootWord);
           if (entry == null)
               return false;                // word not in Lexicon
           return (entry.posTag.equals("Adjective") || entry.posTag.equals("Verb"));       // can be used either way
       }
      //-------------------------------------------------------------------------------------------------------*/     
       // Is a given word recognized by PL as a  direct/indirect status related verb
       public boolean isRecognizedPLVerb(String word)
       {
           return (lexiconVerbs.contains(word.toLowerCase()));
       }
      //-------------------------------------------------------------------------------------------------------*/
}

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
package gov.nih.nlm.lpf.emails.nlpproc.infer;

import gov.nih.nlm.lpf.emails.nlpproc.analysis.PersonAnalyzer;

import gov.nih.nlm.lpf.emails.nlpproc.ner.LPFVerbs;

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbUtils;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;
import gov.nih.nlm.lpf.emails.nlpproc.structure.PredicateModel;

import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexicon;
import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexicon.StatusEntry;
import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexicon.ResolutionEntry;

import gov.nih.nlm.lpf.emails.util.Utils;

import java.util.HashMap;
import java.util.ArrayList;


import org.apache.log4j.Logger;


/**
 *
 * @author 
 */
public class VerbInterpreter implements  LPFVerbs
{
    private static Logger log = Logger.getLogger(VerbInterpreter.class);

    // A data structure for holding info for a PLStatus word in the text
    protected  class PLStatus
    {
        public String word;                              // word that matched
        public String morph;                              // word that matched
        public int        category;                       // Information Category 
        public String  healthCondition;          //  PL person's condition represented by the word
        public String   negativeCondition;     //  condition represented by the negative of the word (e.g. not found)   
        public int type;                                      // status word entry/resolved entry
        public Object   lexiconSpec;               // lexicon line  that matched
    }
   //---------------------------------------------------------------------------------------------/   
    protected static VerbInterpreter theInterpreter = null;
    
    protected PLLexicon lexicon;                // with all PL Status related info
    protected ArrayList<ResolutionEntry> resolutionData;
       
   //---------------------------------------------------------------------------------------------/     
     // Create the singleton Analyzer object
     //
     protected   VerbInterpreter(PLLexicon lexicon)
     {
         if (theInterpreter == null)
         {
            this.lexicon = lexicon;      // assume already initialized
            resolutionData = lexicon.resolutionList;
            theInterpreter = this;
         }
     }

     /*******************************************************************
      * Get the singleton VerbInterpreter object
      ******************************************************************/
     public static VerbInterpreter getInterpreter(PLLexicon lexicon)
     {
         if (theInterpreter == null)
             theInterpreter = new  VerbInterpreter( lexicon);
         return theInterpreter;
     }

      /*******************************************************************
      * Get the singleton VerbInterpreter object after the Lexicon is read
      ******************************************************************/
     public static VerbInterpreter getInterpreter()
     {
         return theInterpreter;
     }

 /*
* Determine the type of a PL Verb, that is: if it is a reporter verb or Reported person's status related verb
* based upon its properties and relationshis to other words in a sentence
* 
* If a verb is  in a common list (both for reporter and reported person)  analyze that
* Otherwise, the verb is in an exclusive Reporter or status verb list, return that.
* If still not resolved, return "unknown"
*/
/*********************************************************************************************************************/
    public  int  getPLVerbCategory( PredicateModel vbModel, TextAnchor subjectAnchor, 
            ArrayList<TextAnnotation> personList)
    {
        VerbAnchor vbr = vbModel.verb;
        String verb = vbr.getMainVerb().text.toLowerCase();
        String morph = ((String) vbr.getMainVerb().getFeature("morph")).toLowerCase();      // in lower case
       
         int verbCategory  = LPFVerbs.UNKNOWN_VERB;      // initialize
         StatusEntry statusEntry = lexicon.getStatusEntry(verb, morph);
         if (statusEntry != null)
             verbCategory =statusEntry.category;
         
        if (statusEntry != null && (statusEntry.category == LPFVerbs.HEALTH_STATUS_VERB ||
           statusEntry.category == LPFVerbs.REPORTING_VERB ))
        {
            setVerbAttributes(vbModel, statusEntry);
        }
 
        // check for adjectives - in copular verbs
        else if (vbr.isAdjectivalCopula())
        {
            TextAnnotation adjective = vbr.getCopularAdjective();
            verb = adjective.text;          // used as status word
            statusEntry = lexicon.getStatusEntry(verb, morph);
            if (statusEntry != null)
            {
                verbCategory = statusEntry.category;
                setVerbAttributes(vbModel, statusEntry);
            }
        }
        // Check if an adjective of this verb is used as status       
       else if (vbr.getAdjectives() != null)
        {
            String[] adjectives = vbr.getAdjectives();
            for (int i = 0; i < adjectives.length; i++)
            if (inAdjectiveStatusList(adjectives[i]))
            {
                verb = adjectives[i];          // used as status word
                statusEntry = lexicon.getStatusEntry(verb, morph);
                if (statusEntry != null)
                {
                    verbCategory = statusEntry.category;
                    setVerbAttributes(vbModel, statusEntry);
                    break;
                }
            }   // end adjectives
        }
        // not yet resolved, check for and  resolve ambiguity
        if (verbCategory ==  PLLexicon.UNKNOWN_VERB || 
                    verbCategory == PLLexicon.AMBIGUOUS_VERB)
        {
           PLStatus plStatusEntry = resolveVerbAmbiguity(vbModel, subjectAnchor, personList);
           if (plStatusEntry != null)
           {
              verbCategory  =  plStatusEntry.category;
              setVerbAttributes(vbModel, plStatusEntry);
           }
        }
       if (verbCategory   == PLLexicon.UNKNOWN_VERB)
       {
           log.warn ("Did not find verb " + verb + " in the lexicon");
       }

        return  verbCategory ;
    }
    
   /**********************************************************************************************/ 
    protected void setVerbAttributes(PredicateModel vbModel, StatusEntry entry )
    {
        vbModel.lpfVerbCategory = entry.category;
        vbModel.healthStatusCondition = entry.healthCondition;
        vbModel.negativeCondition = entry.negativeCondition;
    }
    
   /**********************************************************************************************/ 
    protected void setVerbAttributes(PredicateModel vbModel, PLStatus entry )
    {
        vbModel.lpfVerbCategory = entry.category;
        vbModel.healthStatusCondition = entry.healthCondition;
        vbModel.negativeCondition = entry.negativeCondition;
    }

         /*-----------------------------------------------------------------------------------------------------------------------*/      
    protected PLStatus  getStatusFromAdjective(VerbAnchor vbr)
    {
        String morph = ((String) vbr.getMainVerb().getFeature("morph")).toLowerCase();   
        String[] adjectives = vbr.getAdjectives();
     
        for ( int i = 0; i < adjectives.length;  i++)
        {
            String adjstr = adjectives[i];
            if (adjstr.matches(NLPRelations.NEGATIVE_ADJECTIVES))
                    continue;
            if (lexicon.inAdjectiveStatusList(adjstr))
                    return createPLStatus(adjstr, adjstr, LPFVerbs.HEALTH_STATUS_VERB);
        }    
        return  null;         // cannot resolve
    }

      /**********************************************************************************************
      * Various algorithms to determine if a common verb represents information about 
      * a reported person (health status), or a reporter, by analyzing the associated context
      *
      * Algorithm: Examine the top level objects of this verb
      *  if no such object: 
      *     check for xcomp verbs (e.g.want to know...) in ReporterComplements set
      *  else:
      *     check the verb and its object combination against  ReporterObject//StatusObjects
      * 
      *  If cannot resolve using verb relations,  check if the subject of this verb is a listed person
      *  if (true)   check if the subject of this verb is a listed person
      *         For listed third person: return health status verb, 
      *         else (listed first person) return reporter verb
      * If cannot resolve:
      *     Check if  one of the top objects is a listed third person
      *     if true, return health_status verb
      * else
      *     return unknown verb
      * 
      ***********************************************************************************************/ 

    protected PLStatus  resolveVerbAmbiguity(PredicateModel vbModel , TextAnchor subjectAnchor, 
            ArrayList<TextAnnotation> personList)
    {
         VerbAnchor vbr = vbModel.verb;

         // 1. First check for active/passive category
         PLStatus  plStatus = matchEntryWithVerbVoice(vbr);
         if ( plStatus != null)
            return  plStatus;
         
         plStatus = matchEntryWithVerbTense(vbModel);
         if ( plStatus != null)
            return  plStatus;
         
       // 2. Check for complement verbs to a parent  verb
        ArrayList<TextAnchor> topObjects =VerbUtils.getTopLevelObjects(vbr);
        if (topObjects == null || topObjects.isEmpty())
        {
            plStatus = matchEntryForComplementVerb(vbr);
            if ( plStatus != null)
                return  plStatus;
        }
        
         // 3. compare objects of this verb against known noun terms
        if (plStatus == null)
        {
            int n = topObjects.size();
            String[]  objstr = new String[n];
            for (int i = 0; i < n; i++)
                objstr[i] = topObjects.get(i).getText();
            plStatus = resolveAmbiguityFromObjects(vbr, objstr);
            if ( plStatus != null)
                return  plStatus;   
        }  
        
           if (plStatus == null)
        {
            int n = topObjects.size();
            String[]  objstr = new String[n];
            for (int i = 0; i < n; i++)
                objstr[i] = topObjects.get(i).getText();
            plStatus = resolveAmbiguityFromObjects(vbr, objstr);
            if ( plStatus != null)
                return  plStatus;   
        }  
        
         // 4 check if there is a default category specified for this verb
        if (plStatus == null)
        {
           plStatus = resolveAmbiguityByDefault(vbr);
            if ( plStatus != null)
                    return  plStatus;   
        }
       // 5. Finally, Check the subject categories to see if  it referes to a third person, then assume status of  reported person
        if (plStatus == null)
        {
            plStatus  =  resolveAmbiguityFromPersonRefs( vbr, subjectAnchor,  topObjects,  personList);
            if (plStatus != null)
            return plStatus;
        }
        return null;        // not yet resolved
     }

    /*------------------------------------------------------------------------------------------------*/  
    protected PLStatus  matchEntryWithVerbVoice(VerbAnchor vbr)
    {   
        String voice = vbr.isActive() ? "active" : "passive";
        String morph = getVerbForStatusCheck(vbr).getRootWord();
        for ( int i = 0; i < resolutionData.size(); i++)
        {
           ResolutionEntry  resInfo = resolutionData.get(i);
           if (!resInfo.resCriteria.equals("VOICE"))
               continue;
           if (Utils.isInList(morph, resInfo.words)  && voice.equalsIgnoreCase(resInfo.voice))
               return createPLStatus(vbr, resInfo);
        }
        return null;    
    }
    
        /*------------------------------------------------------------------------------------------------*/  
    protected PLStatus  matchEntryWithVerbTense(PredicateModel vbModel)
    {
       
        VerbAnchor vbr = vbModel.verb;
        String morph = getVerbForStatusCheck(vbr).getRootWord();
        for ( int i = 0; i < resolutionData.size(); i++)
        {
           ResolutionEntry  resInfo = resolutionData.get(i);
           if (!resInfo.resCriteria.equals("TENSE"))
               continue;
           String tense = resInfo.tense;
           int verbTense = vbModel.verbTense;
           String tenseStr = VerbTense[verbTense];          // in String format
           
           if (Utils.isInList(morph, resInfo.words)  && tenseStr.equalsIgnoreCase(resInfo.tense))
               return createPLStatus(vbr, resInfo);
        }
        return null;    
    }
  
 /*-------------------------------------------------------------------------------------------------------------------------*/
    protected  PLStatus  matchEntryForComplementVerb(VerbAnchor vbr)
    {
        // check if this verb has an xcomp verb as a complement as "want to know"
        TextAnnotation  xcompObj = vbr.getXcompObject();
        if (xcompObj != null)
        {
            String morph =   getVerbForStatusCheck(vbr).getRootWord();
            String xcomp =  xcompObj.getRootWord();
            for ( int i = 0; i < resolutionData.size(); i++)
            {
               ResolutionEntry resInfo = resolutionData.get(i);
               if (!resInfo.resCriteria.equalsIgnoreCase("TO_COMPLEMENT"))
                   continue;
               if (Utils.isInList(morph, resInfo.words) &&  Utils.isInList(xcomp, resInfo.complements))
                   return createPLStatus(vbr, resInfo);
            }
        }         
        return null;        // cannot proceed   
     }    
    
      /*-----------------------------------------------------------------------------------------------------------------------*/      
    protected PLStatus  resolveAmbiguityFromObjects(VerbAnchor vbr,  String[] objstr)
    {
        String morph = getVerbForStatusCheck(vbr).getRootWord();
        for ( int i = 0; i < resolutionData.size(); i++)
        {
            ResolutionEntry resInfo = resolutionData.get(i);
            if (!resInfo.resCriteria.equalsIgnoreCase("OBJECT"))
               continue;
            for (int j = 0; j < objstr.length; j++)
            {
                String ostr = objstr[j].toLowerCase();
                if (Utils.isInList(morph, resInfo.words) &&  Utils.isInList(ostr, resInfo.objects))
                    return createPLStatus(vbr, resInfo);
            }
        }    
        return  null;         // cannot resolve
    }
   /*-----------------------------------------------------------------------------------------------------------------------------*/       
        protected PLStatus  resolveAmbiguityFromObjectType(VerbAnchor vbr,  String[] objstr)
    {
        String morph = getVerbForStatusCheck(vbr).getRootWord();
        for ( int i = 0; i < resolutionData.size(); i++)
        {
            ResolutionEntry resInfo = resolutionData.get(i);
            if (!resInfo.resCriteria.equalsIgnoreCase("OBJECT_TYPE"))
               continue;
            for (int j = 0; j < objstr.length; j++)
            {
                if (Utils.isInList(morph, resInfo.words) &&  Utils.isInList(objstr[j], resInfo.objects))
                    return createPLStatus(vbr, resInfo);
            }
        }    
        return  null;         // cannot resolve
    }
   /*-----------------------------------------------------------------------------------------------------------------------------*/      
    protected PLStatus  resolveAmbiguityFromPersonRefs(VerbAnchor vbr,    TextAnchor subjectAnchor,
        ArrayList<TextAnchor> topObjects,   ArrayList<TextAnnotation> personList)
    {
      // Check the subject categories to see if  it referes to a third person, then assume status of  reported person
          TextAnnotation listedPerson = null; 
          int personType;

          listedPerson  = PersonAnalyzer.getPersonInList(personList, subjectAnchor);
         /*  << This does not work -- simply check for objects>>
          if (listedPerson != null)
          {
               personType = PersonAnalyzer.getPersonCategory(listedPerson) ;
               if (personType == PersonAnalyzer.THIRD_PERSON)
                    return  PLVerbs.HEALTH_STATUS_VERB;
               else if (personType == PersonAnalyzer.FIRST_PERSON)
                   return  PLVerbs.REPORTING_VERB;
          }
          // Subject of the clause not in listed Person List; check if one of the top level objects is
           * /
           */
          // if the Object matches, then it is a REPORTING_VERB
        String word =   getVerbForStatusCheck(vbr).text;
        String morph = getVerbForStatusCheck(vbr).getRootWord();
        for (int i = 0; i < topObjects.size(); i++)
        {
          listedPerson = PersonAnalyzer.getPersonInList(personList, topObjects.get(i));
          if (listedPerson != null)
          {
              // If the object is me, assume it is about a reported person -> John spoke to me
               if ( PersonAnalyzer.getPersonCategory(listedPerson) == PersonAnalyzer.FIRST_PERSON)
                      return  createPLStatus(word, morph, LPFVerbs.HEALTH_STATUS_VERB);
               else
                    return  createPLStatus(word, morph, LPFVerbs.REPORTING_VERB);
          }
        }
        return  null;           // TBD: make ambiguous
     }
// --------------------------------------------------------------------------------------------------------
// Get the copular verb to check for status if it is a copula, otherwise return the main verb
//---------------------------------------------------------------------------------------------------------
protected TextAnnotation getVerbForStatusCheck(VerbAnchor vbr)
{
    if (vbr.isAdjectivalCopula())
         return  vbr.getCopularAdjective();
    else
       return vbr.getMainVerb();
}
 
    //---------------------------------------------------------------------------------------------------------------
    // Check if a "default category specified" for verbs that may not be resolved otherwise
    // --------------------------------------------------------------------------------------------------------------
     protected PLStatus   resolveAmbiguityByDefault(VerbAnchor vbr)
     {
        String morph = getVerbForStatusCheck(vbr).getRootWord();
        for ( int i = 0; i < resolutionData.size(); i++)
        {
           ResolutionEntry resInfo = resolutionData.get(i);
           if (!resInfo.resCriteria.equals("DEFAULT"))
               continue;
           if ( Utils.isInList(morph, resInfo.words))
               return  createPLStatus(vbr, resInfo);
        }
        return null;    // not in Default list
     }
     
     //----------------------------------------------------------------------------------------------------------*/    
     // Get personal attribute information contained in the verb's object relationships
     // For example: David is 10 years old or David is 10 years in age. return (<Age, 10 years>
     // We are simply looking for AGE here
     //----------------------------------------------------------------------------------------------------------*/
     public static HashMap <String, TextAnchor>  getPersonAttribute(PredicateModel vbModel,  
         ArrayList<TextAnnotation> attributeList)
     {
         TextAnchor value = null;
         String key = NERConstants.AGE_ATTR;
         HashMap <String, TextAnchor>   pattributes = new HashMap();
         VerbAnchor vbr = vbModel.verb;
        
         TextAnchor[]  copulaObjects = vbr.getCopularObjects();
         if (copulaObjects == null)
             return null;
         
         for (int i = 0; i < copulaObjects.length; i++)
         {
             TextAnchor cop = copulaObjects[i];
             String copStr = cop.getText().toLowerCase();
             if (TextAnchor.isCardinal(cop.getGovernorToken()))         // simply a number:  He is 10.
             {
                value = cop;
                pattributes.put(key, value);
                continue;
             }
             else if (copStr.contains("year"))        // He is 10 years of/in age
             {
                value = cop;
                pattributes.put(key, value);
                continue;
             }
             else
             {
                 if (copStr.contains("old"))            // he is 10 years old (cop = is old, npadvmod = "10 years")
                 {
                       TextAnchor[] va = cop.getDependentAnchors("npadvmod");
                       if (va != null && va.length > 0)
                            pattributes.put(key, va[0]);
                 }
                 continue;
             }  // end else
         } // end for
         return pattributes;        
     }
         
     
      //-------------------------------------------------------------------------------------------------------*/      
     // create a new entry with external info - this is a cludge
    public PLStatus createPLStatus(String word, String morph, int category)
    {
        PLStatus plStatus = new PLStatus();
        plStatus.word = word;
        plStatus.morph=morph;
        plStatus.category = category;
        plStatus.type = PLLexicon.EXTERNAL_STATUS_ENTRY;
        plStatus.lexiconSpec = null;
        return plStatus;
    }
   //-------------------------------------------------------------------------------------------------------*/      
    protected PLStatus createPLStatus(StatusEntry sentry)
    {
        PLStatus plStatus = new PLStatus();
        plStatus.word = sentry.entryWord;
        plStatus.morph=sentry.morphWord;
        plStatus.category = sentry.category;
        plStatus.healthCondition = sentry.healthCondition;
        plStatus.negativeCondition = sentry.negativeCondition;
        plStatus.type = PLLexicon.STATUS_ENTRY;
        plStatus.lexiconSpec = sentry;
        return plStatus;
    }

     //-------------------------------------------------------------------------------------------------------*/      
    protected PLStatus createPLStatus(VerbAnchor vbr, ResolutionEntry rentry)
    {
        PLStatus plStatus = new PLStatus();
        plStatus.word = vbr.getMainVerb().text;
        plStatus.morph = vbr.getMainVerb().getRootWord();
        plStatus.category = rentry.resolvedCategory;
        plStatus.healthCondition = rentry.healthCondition;
        plStatus.negativeCondition = rentry.negativeCondition;
        plStatus.type = PLLexicon.RESOLVED_STATUS_ENTRY;
        plStatus.lexiconSpec = rentry;
        return plStatus;
    }
        
     //-------------------------------------------------------------------------------------------------------*/
    // check if a word is a known PL Verb
     //-------------------------------------------------------------------------------------------------------*/
    public  boolean  isPLVerb(TextAnnotation annot)
    {
        String word = annot.text.toLowerCase();
        String root =annot.getRootWord();           // morphological root

        return (lexicon.getStatusEntry(word, root) != null);
    }      
  
       
     //-------------------------------------------------------------------------------------------------------*/
     public  static String getVerbTypeString(int type)
     {
         return PLLexicon.getCategoryName(type);
     }
     
  
     //-------------------------------------------------------------------------------------------------------*/      
    // Denote a verb type is resolved 
    private boolean isResolved(int verbType)
    {
         return (verbType == HEALTH_STATUS_VERB || verbType == REPORTING_VERB) ;
    }

    //-------------------------------------------------------------------------------------------------------*/          
    // Check if a given word or its root is in the Adjective list for PL verbs
    //-------------------------------------------------------------------------------------------------------*/
    public boolean  inAdjectiveStatusList(String word)
    {
         return lexicon.inAdjectiveStatusList(word);
    }
        
 }
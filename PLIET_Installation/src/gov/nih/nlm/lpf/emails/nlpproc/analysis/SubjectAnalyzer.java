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
package gov.nih.nlm.lpf.emails.nlpproc.analysis;

//import gov.nih.nlm.ceb.lpf.emails.analysis.NameMatcher;
import gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve.PersonResolver;
import gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve.NameResolver;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.ClausalAssertion;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.SubjectEntity;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.AnaphoraInfo;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.CorefInfo;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.EmailHeaderInfo;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;

import java.util.TreeMap;
import java.util.ArrayList;

import  org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class SubjectAnalyzer
{
  private static Logger log = Logger.getLogger(SubjectAnalyzer.class);
    
    protected ArrayList<ClausalAssertion> lpfAssertionList;
    protected TreeMap<TextAnchor, CorefInfo> corefMap;
    protected EmailHeaderInfo headerInfo;
    protected ArrayList<TextAnnotation> personList;
    protected SubjectEntity NullSubject = null;        // Assertions for which no subjects could be determined
    
    
    // output
    protected TreeMap <TextAnchor, SubjectEntity> uniqueSubjectMap;   // Key => anchor for each subject
    
    public SubjectAnalyzer(  ArrayList<ClausalAssertion> assertionList, 
        TreeMap<TextAnchor, CorefInfo> corefMap, EmailHeaderInfo  headerlInfo,
        ArrayList<TextAnnotation> personList)
    {
        this.lpfAssertionList= assertionList;
        this.corefMap = corefMap;
        this.headerInfo = headerlInfo;
        this.personList = personList;
    }

    
    /****************************************************************************
     * Create a set of unique SubjectEntities by analyzing the relationships between
     * various anchors, substituting for anaphora, etc.
     *******************************************************************************/
    public void buildSubjectEntityList()
    {
        // merge duplicate objects and create a unique subject list 
        uniqueSubjectMap = 
            generateUniqueSubjectMap(lpfAssertionList, corefMap, headerInfo, personList);
       
    }
    
    /********************************************************************************************
     *   Build a unique SubjectEntity map, as follows:
     *  1. Go over all subjects in all assertions and put them into a list with no duplicates, and also a null subject list
     *  2. for each subject in the list:
     *     Resolve its  coreference , if present, and/or Full Match from the Person List
     *     Get the corresponding FullMatch Anchor from the Person List , or a Longer Match from the Subject list
     *    3.  Add all assertions to the best match subject
     *    4. Add all null subjects to a different LPFSubject list
     *  
     *
    /********************************************************************************************/
   protected TreeMap <TextAnchor, SubjectEntity>  generateUniqueSubjectMap(
            ArrayList<ClausalAssertion> assertionList,
            TreeMap<TextAnchor, CorefInfo> corefMap,
            EmailHeaderInfo headerInfo, ArrayList<TextAnnotation> personList)
    {  
        TextAnchor defaultSubject = null;
        // Stores all names for full or partial match 
      
        TreeMap <TextAnchor,  SubjectEntity> uniqueSubjMap = new TreeMap();
        
        //------------------------------------------------------------------------------------------------------------------
        // Check for "subject" in each "main" clause. If there is no subject, use the default one
        // from the email header or the previous main clause as the subject
        //------------------------------------------------------------------------------------------------------------------
        TreeMap <TextAnchor,  SubjectEntity> initialSubjectMap = buildInitialSubjectMap(assertionList);
        
        // Link each person to the bestmatch person in the message
        TreeMap<TextAnchor, TextAnchor> resolvedPersonMap = 
                    PersonResolver.resolvePersons(new ArrayList(initialSubjectMap.keySet()), 
                    personList, corefMap);
        
        // combine data for the same subject 
         uniqueSubjMap = buildUniqueSubjectMap(initialSubjectMap, resolvedPersonMap);
         return uniqueSubjMap;
    }

   //------------------------------------------------------------------------------------------------------------------
   // Add all asssertions from all causes and group according to the subject Anchor
    protected  TreeMap <TextAnchor,  SubjectEntity> buildInitialSubjectMap( 
        ArrayList<ClausalAssertion> assertionList)
    {
          TreeMap <TextAnchor,  SubjectEntity> initialSubjectMap = new TreeMap();
         for (int i = 0; i < assertionList.size(); i++)
         {
              ClausalAssertion clausalAssert = assertionList.get(i);
              int clauseLevel  = clausalAssert.clause.clauseLevel;
              TextAnchor subject  = clausalAssert.subject ;
              if (subject == null)  
              {
                    // save this assertion under a specially designated "Null subject" entry
                  if (NullSubject == null)
                      NullSubject = new SubjectEntity(null);
                  NullSubject.addAssertion(clausalAssert);
                  System.out.println("-- Added assertion " + clausalAssert.verb.getText() +  " to  Null Subject");
              }
              else      // subject not null: 
              {
                  SubjectEntity lpfSubject = initialSubjectMap.get(subject);
                  if (lpfSubject == null)
                  {
                      lpfSubject = new SubjectEntity(subject, clausalAssert );
                      initialSubjectMap.put(subject, lpfSubject);
                  }
                  else
                        lpfSubject.addAssertion(clausalAssert);     // simply add the assertion
              }
         }
         return initialSubjectMap;
    }

    //------------------------------------------------------------------------------------------------------------------
   // Build the UniqueSubjectMap by comparing the subjects in the list and combining the ones
   // which either refer to the same person in the Person List or have equivalent names
   //------------------------------------------------------------------------------------------------------------------ 
    protected  TreeMap <TextAnchor,  SubjectEntity> buildUniqueSubjectMap( 
            TreeMap <TextAnchor,  SubjectEntity> initialSubjectMap,
             TreeMap<TextAnchor, TextAnchor> resolvedPersonMap)
        {
            TreeMap <TextAnchor,  SubjectEntity>  uniqueSubjMap = new TreeMap();
             ArrayList <TextAnchor> subjectList = new ArrayList(initialSubjectMap.keySet());
             for (int i = 0; i < subjectList.size(); i++)
             {
                 TextAnchor subject = subjectList.get(i);
                 SubjectEntity lpfSubject0 = initialSubjectMap.get(subject);
                 
                 // combine the assertions with that of the bestmatch subject
                 TextAnchor  matchingSubject = resolvedPersonMap.get(subject);
                 if (matchingSubject == null)            // not a person
                        matchingSubject = subject;
 
                 // Check if it is a duplicate entry for the same person already in the uniqueSubjectMap
                 // if so, use the subject already there
                  SubjectEntity lpfSubject  = getReferredSubject(matchingSubject, uniqueSubjMap);
                  if (lpfSubject == null)
                  {
                      lpfSubject = new SubjectEntity(matchingSubject);
                      lpfSubject.addAssertions(lpfSubject0.getAssertions());
                      uniqueSubjMap.put(matchingSubject, lpfSubject);
                      System.out.println("-- Unique Subject: " + matchingSubject.getText() +
                            ", number of Assertions: " +   lpfSubject.getAssertions().size());
                  }
                  else
                  {
                      lpfSubject.addAssertions(lpfSubject0.getAssertions());
                      System.out.println("-- Total number of assertions for  Subject " + lpfSubject.subject.getText() +
                            ":  " +   lpfSubject.getAssertions().size());
                  } 
             }
             return uniqueSubjMap;
        }

    /******************************************************************************************************/
    // Return the  LPFSubject that refers to the same person, with the same or partial name match
    //--------------------------------------------------------------------------------------------------------------------------------/
    protected   SubjectEntity  getReferredSubject(TextAnchor subject,  
        TreeMap <TextAnchor,  SubjectEntity> uniqueSubjectMap)
    {
       if  (uniqueSubjectMap.size() == 0)
           return null;
       SubjectEntity referredSubject = uniqueSubjectMap.get(subject);
       if (referredSubject != null)
           return referredSubject;
       String curName = subject.getText();
       ArrayList<TextAnchor> subjectList = new ArrayList(uniqueSubjectMap.keySet());
       for (int i = 0; i < subjectList.size(); i++)
       {
           String subjectName = subjectList.get(i).getText();
           if (NameResolver.isEquivalentName(curName, subjectName))
               return uniqueSubjectMap.get(subjectList.get(i));
       }
       return null;
  }
     
   
/*--------------------------------------------------------------------------------------------------------------
     Check if the given anchor is either an Anaphor or a partial match for another name.
     If so, return the appropriate reference anchor
  -------------------------------------------------------------------------------------------------------------*/
    protected ArrayList <TextAnchor> findMatchingAnchor( TextAnchor anchor,   TreeMap<TextAnchor, AnaphoraInfo> 
                anaphoraMap, ArrayList  <TextAnchor>nameList)
    {
        if (anchor == null)
            return null;
        
        ArrayList <TextAnchor>  matchingAnchors = new ArrayList();
        if (anaphoraMap != null && anaphoraMap.containsKey(anchor))
        {
             TextAnchor[] antcAnchors = anaphoraMap.get(anchor).antecedentAnchors;
             if (antcAnchors  != null)
             {
                 for (int i = 0; i < antcAnchors.length; i++)
                     matchingAnchors.add(antcAnchors[i]);  
             }    
            // check if there is another anchor with a full  or partial name match)
             else 
             {
                 if (nameList != null)
                 {
                        TextAnchor matchingAnchor =NameResolver.getMatchingName(anchor,  nameList);
                        if (matchingAnchor != null)
                            matchingAnchors.add(matchingAnchor);
                 }
             }
        }
        return (matchingAnchors.isEmpty() ? null : matchingAnchors);
    }
 
 /*****************************************************************************************************/   
  // Assign a subject type to an LPF subject (Person or personal info or other information
    /* 
     * protected void  assignSubjectType( LPFSubject lpfSubject)
    {
        int subjectType = -1;
        
        TextAnchor subject = lpfSubject.getSubject();
        if (PersonAnalyzer.isInPersonList(personList, subject))
        {
           lpfSubject.subjectType =  NERConstants.PERSON_SUBJECT;
           return;
        }
        
        // Check for personal data  etc. His name, her age, ...
        String text = subject.getText();
        String[] words = text.split("\\s");

       for (int i = 0; i < words.length ; i++)
       {
           if (Utils.isInList(words[i], NERConstants.PERSONAL_DATA))
           {
               lpfSubject.subjectType = NERConstants.PESONAL_DATA_SUBJECT;
               return;
           }
       }

       // not personal info, check enquiry type
       for (int i = 0; i < words.length; i++)
       {
           if (Utils.isInList(words[i], NERConstants.ENQUIRY_OBJECTS)
                || Utils.isInList(words[i], NERConstants.ASSISTANCE_OBJECTS))
           {
             lpfSubject.subjectType = NERConstants.GENERAL_INFO_SUBJECT;
              return;
           }
       }
        
       // not in Person List, check if still a "Person"  type word
      TextAnnotation annot = subject.getGovernorToken();
      if (TextAnchor.isPersonNoun(annot))
      {
           lpfSubject.subjectType = NERConstants.PERSON_SUBJECT;
           return;
      }

        lpfSubject.subjectType = NERConstants.OTHER_TYPE_SUBJECT;
        return;   
  }
     */
/*****************************************************************************************************/
    public ArrayList<TextAnnotation> getPersonAnnotations()
    {
        return personList;
    }
    
/*****************************************************************************************************/        
    public TreeMap<TextAnchor, CorefInfo>  getCorefaInfo()
    {
        return corefMap;
    }
/*****************************************************************************************************/        
    // return the set of  Unique Subjects with associated assertions (corresponding to each clause)
      public TreeMap <TextAnchor, SubjectEntity>  getSubjectMap()
      {
          return uniqueSubjectMap;
      }
    /**************************************************************************************************/      
    //  Return the LPFSubject (including the NULLSubject) which refers to the given subject 
    // in its ObjectList, but exclude Copula objects such as My name, my age etc.
//--------------------------------------------------------------------------------------------------------------------------/   
     public static ArrayList<SubjectEntity > getReferringSubjects(SubjectEntity mySubject, 
         ArrayList < SubjectEntity> subjectList )
      {
          ArrayList<SubjectEntity > referringSubjects = new ArrayList();
        
         for (int i =0; i < subjectList.size(); i++)
         {
              SubjectEntity newSubject = subjectList.get(i);
              ArrayList<ClausalAssertion> assertions = newSubject.getAssertions();
              for (int j= 0; j < assertions.size(); j++)
              {
                  ArrayList <TextAnchor> persons = assertions.get(j).persons;       
                  // Check if this person Object is the Subject  of the givenSubject
                  for (int jj = 0; persons != null &&  jj < persons.size(); jj++)
                  {
                      TextAnchor personObject = persons.get(jj);
                      if (personObject == mySubject.subject )
                           referringSubjects.add(newSubject);
                  }
              } // end assertions
          } // end LPFSubjects 
          return referringSubjects;
      }
    /**************************************************************************************************/    
    // Get the assertions for which no subjects could be determined
      // These are analyzed if all other ways of getting relevant information fails
      //-------------------------------------------------------------------------------------------------------------------------
      public SubjectEntity getNullSubject()
      {
          return NullSubject;
      }
//----------------------------------------------------------------------------------------------------------------------------------
// Resolve the coreference based directly or indirectly upon either the subject 
// For indirect reference, we check if the subject is the antecent for a term that
// has a coreference. Then that coref is also the coref of this subject
//-----------------------------------------------------------------------------------------------------------------------------------
/*   protected TextAnchor resolveCoreference(TextAnchor subject,  
       TreeMap <TextAnchor, AnaphoraInfo> anphoraMap,
       TreeMap<TextAnchor, CorefInfo> corefMap)
   {            
       if (corefMap == null || corefMap.size() == 0)
           return subject;
       
       CorefInfo corefInfo = corefMap.get(subject);
       if (corefInfo != null)           //  coref for original subject
       {
           System.out.println(">> Coreference from Map for " + subject.getPhraseText() + " is: " +
               corefInfo.corefAnchor.getPhraseText());
           return corefInfo.corefAnchor;
       }
       
       return subject;   // no coref
 
       /*
       // Check if the subject is an antecedent for an anaphoric term. Then 
       // check for the coref of that anaphore (as in : "looking for my  son. His name id David")
       Iterator<TextAnchor> it = anphoraMap.keySet().iterator();
       while (it.hasNext())
       {
           TextAnchor anaphor = it.next();
           TextAnchor[] antecedents = anphoraMap.get(anaphor).antecedentAnchors;
           if (antecedents == null || antecedents.length == 0)
               return subject;          // nothing to check against
           for (int i = 0; i < antecedents.length; i++)
           {
               if (antecedents[i].getPhraseText().equalsIgnoreCase(subject.getPhraseText()))
               {
                   TextAnchor newSubject = anaphor;
                   corefInfo = corefMap.get(newSubject);
                   if (corefInfo != null  && !corefInfo.corefAnchor.equals(subject))           //  avoid circular relation
                   {
                       log.info(">> Resolved Coreference for " + subject.getPhraseText() + " through Antecedent match  is: " +
                           corefInfo.corefAnchor.getPhraseText());
                       return corefInfo.corefAnchor;
                   }
               }    // end if
           } 
       }    // end while
       return subject;          // no coref
        * 
        */
  /* }*/
                   
  //----------------------------------------------------------------------------------------------------------------------------------
    // Find the best matching person in the Anchor list with respect to a given Person in a Subject
    // If no match found, return the Subject
    //----------------------------------------------------------------------------------------------------------------------------
   public  TextAnchor getBestMatchingPerson(TextAnchor subject, 
           ArrayList <TextAnchor> subjectList,  PersonAnalyzer personAnalyzer)
    {
        // Check if the person's name is already in the list
        TextAnnotation  bestmatchPerson = personAnalyzer.getBestMatchPersonInList(subject);
        if (bestmatchPerson == null)
            return subject;             //may be not a person
         
         // Find the Text Anchor which contains  this Annotation
        TextAnchor matchingAnchor = TextAnchor.getAnchorByOffsets(subjectList,  bestmatchPerson.offsets);
        if (matchingAnchor != null)
            return matchingAnchor;
        return subject;
    }
     
}

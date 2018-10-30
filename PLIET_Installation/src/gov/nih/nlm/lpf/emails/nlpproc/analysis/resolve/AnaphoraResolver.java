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
package gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve;

/**
 * @author 
 */

import  gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.NounAnchor;
//import  gov.nih.nlm.ceb.lpf.emails.nlp.VerbAnchor;

import gov.nih.nlm.lpf.emails.nlpproc.structure.AnaphoraInfo;
import gov.nih.nlm.lpf.emails.nlpproc.ner.PronounTerm;

import gate.Annotation;

import gov.nih.nlm.lpf.emails.nlpproc.analysis.PersonAnalyzer;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Collections;
import org.apache.log4j.Logger;
  

public class AnaphoraResolver
{
    private  static final Logger log = Logger.getLogger(AnaphoraResolver.class);
    //private static int SENTENCE_SPAN = 2;
     
    ArrayList  <AnaphoraInfo> anaphoraList = new ArrayList();                   // list of all pronouns in the set of sentences
    ArrayList <NounAnchor> candidateAntcList = new ArrayList();              //  list of all proper nouns or places in the sentences
    
    // Key: SentenceAnnoation (arranged with increasing sentence Id
    TreeMap<TextAnnotation, ArrayList<TextAnchor>> anchorMap;
    TreeMap<TextAnchor, AnaphoraInfo> anaphoraMap = new TreeMap();      // key is anaphor anchor
     ArrayList <TextAnnotation> personAnnots;
    
    /*******************************************************************************/
    // Constructor
    // Parameters: A set of sentences and the corresponing  anchors.
    // The sentences must be specified in sequential order in the TreeMap 
    // for proper reference resolution.
    // Argument: allAnaphora is the map of AnaphoraInfo against the anaphor AnnotationID as the Keys;
 
    public AnaphoraResolver(  TreeMap<Integer, AnaphoraInfo> allAnaphora, 
        TreeMap<TextAnnotation, ArrayList<TextAnchor>> allAnchors,
        ArrayList <TextAnnotation> personList)

    {
        anchorMap = allAnchors;
        personAnnots = personList;
        Iterator <TextAnnotation>  it = anchorMap.keySet().iterator();
        while (it.hasNext())        // next sentence
        {
            ArrayList<TextAnchor> anchors = anchorMap.get(it.next());
            int ns =  anchors.size();
            for (int i = 0; i <ns; i++)             // for each anchor
            {
                TextAnchor anchor = anchors.get(i);
                if (anchors.get(i).getType() != TextAnchor.NOUN_ANCHOR )
                    continue;

                NounAnchor anAnchor = (NounAnchor)anchor;
                // check if it is a pronoun or a proper noun
                if (!anAnchor.isPronounType())          // && (i < ns-1) )     
                    candidateAntcList.add(anAnchor);
            }       // for each sentence
        }   
        
 /*       Iterator  it1 = allAnaphora.keySet().iterator();
        while (it1.hasNext())        // next anaphor
        {
            anaphoraList.add(allAnaphora.get(it1.next()));
        }*/
        anaphoraList.addAll(allAnaphora.values());
        // save as a treeMap for easy retrieval, skip if null
        for (int i = 0; i < anaphoraList.size(); i++)
        {
            AnaphoraInfo anInfo = anaphoraList.get(i);
            anInfo.anaphorAnchor = getAnaphorAnchor(anInfo);
            if ( anInfo.anaphorAnchor  != null)  
                anaphoraMap.put(anInfo.anaphorAnchor , anInfo);
        }
    }
    /***************************************************************************************************/  
 /* Resolve the anaphoric reference of each pronoun in the list 
  * There are three possible cases:
     *  1) A singular term, with resolved antecedent -> find  antecedent anchor
     *  2) A Plural term, with unresolved antecedent -> match Plural terms and find antecedent
     *  3) A Plural Term: with resolved antecedent -> match the nearest Noun phrase with matching properties
     *        Choose the one nearer  to anaphor.
     * *****************************************************************************************************/         
  
    public int resolveAnaphora()
    {
        int np = 0;     // number of pronouns with antecedent match
        for (int i = 0; i < anaphoraList.size(); i++)
        {
            //TextAnchor antcAnchor;
            AnaphoraInfo anInfo = anaphoraList.get(i);
            anInfo.anaphorAnchor = getAnaphorAnchor(anInfo);
            boolean isPlural = anInfo.isPlural; 
            TextAnchor antcAnchor = null;
            
            // First consider resolved  anaphore (both singular and plural in the Annotation set), 
            // ignore  unresolved  first persons
            if (anInfo.antecedent != null  )            // with known antecedent Id
                antcAnchor = getAntecedentAnchor(anInfo, candidateAntcList);

            if (antcAnchor == null)
            {
                 if (anInfo.person.equals("first"))
                  continue;
                else        // no match in Person/Location List, match with PRN data
                 {
                     if (!isPlural)
                        antcAnchor =  findAnaphoricMatch(anInfo, false);        // check singular PRNs only
                 }
            }

            if (antcAnchor != null)
             {
                 anInfo.antecedentAnchors = new TextAnchor[] {antcAnchor};
                 anInfo.antecedentText = antcAnchor.getTextWithConjunct();
             }

              // Plural Anaphor, may be one or more antecedent anchors
            else                
            {
                antcAnchor =  findAnaphoricMatch(anInfo,  true);        // check for a single Anchor
                if (antcAnchor != null)
                 {
                     anInfo.antecedentAnchors = new TextAnchor[] {antcAnchor};
                     anInfo.antecedentText = antcAnchor.getTextWithConjunct();
                 }
                else
                {
                    // no match with a single anchor, check for multiple anchors
                    TextAnchor[] candidateAnchors =  resolveAnaphoricMatches(anInfo);
                    anInfo.antecedentAnchors =candidateAnchors;     
                }
            } 
            if ( anInfo.antecedentAnchors != null)
            {
                np++;
                String antecedentText = anInfo.antecedentAnchors[0].getTextWithConjunct();
                if (anInfo.antecedentAnchors.length > 1)
                {
                    for (int ii = 1; ii < anInfo.antecedentAnchors.length; ii++)
                        antecedentText += ", " + anInfo.antecedentAnchors[ii].getTextWithConjunct();
                }
                log.info("Resolve: Antecedent Anchor for  " + anInfo.anaphorAnchor.getText()  +" [Id= " +  anInfo.anaphorTokenId +"] is: " + 
                    antecedentText + " [Anchor ID: " + anInfo.antecedentAnchors[0].getAnchorId()+ "]");
            }
            else
              System.out.println("---> No  antecedents found for " +   anInfo.anaphorAnchor.getText());
        }  // for each anaphor
        
        return np;
    }
  
      //--------------------------------------------------------------------------------------------------        
    protected ArrayList<TextAnnotation> reverseSentenceOrder( 
        TreeMap<TextAnnotation, ArrayList<TextAnchor>> anchorMap)
    {
         // Arrange sentences in reverse order for search
        Iterator <TextAnnotation> it = anchorMap.keySet().iterator(); 
        ArrayList <TextAnnotation> reversedSentences = new ArrayList();
        while (it.hasNext())
        {
            TextAnnotation sentenceAnnot = it.next();
             reversedSentences.add(0, sentenceAnnot);
        }
        return reversedSentences;
    }
        
   
    /***************************************************************************************/
    // Determine the anchor  which represent the given anaphor
    // It could be a pronoun (in NounAnchors) or an Adverb (in Adjective Anchor)

    protected TextAnchor getAnaphorAnchor(AnaphoraInfo pinfo)
    {
        // find the noun terms that preceed the given pronoun in the text (sentences)
        int anaphorTokenId  = pinfo.anaphorTokenId;      // token Id
        int sentenceId = pinfo.anaphorSentenceId;
        
        Iterator <TextAnnotation> it = anchorMap.keySet().iterator();
        while (it.hasNext())
        {
            TextAnnotation sentenceAnnot = it.next();
            if (sentenceAnnot.id != sentenceId)
                continue;
            // locate the matching anchor - both nouns and adjectives
            ArrayList<TextAnchor> sentenceAnchors =  anchorMap.get(sentenceAnnot);
            for (int i = 0; i < sentenceAnchors.size(); i++)
            {
                TextAnchor anchor = sentenceAnchors.get(i);
                if (anchor.getType() == TextAnchor.VERB_ANCHOR)
                    continue;
                if (anchor.containsWord(anaphorTokenId))
                    return anchor;
            }
        }
        // something wrong --
        System.out.println(">>> ERROR: No Noun anchor  found for Anaphor  term " +  pinfo.anaphorText);
        return null;
    }      
    
     //----------------------------------------------------------------------------------------------------------------     
    // Determine the noun anchor  which represent the  antecedent to the given anaphor
    // It is assumed that a single Noun anchor will contain the start and end offsets
    // of the antedent Annotation
    // Note that the  antecedent and the first person anaphor (I saw John: I =>John) cannot be 
    // related to each other by a subject/object relationship. May happen for first person anaphors.
    
    protected TextAnchor getAntecedentAnchor(AnaphoraInfo pinfo, ArrayList<NounAnchor> nounList)
    {
        Annotation antecedent = pinfo.antecedent;
        int antcTokenId = pinfo.antcTokenId;          // same as for a text annotation
        if (antcTokenId  < 1)
            return null;

        for (int i = 0; i < candidateAntcList.size(); i++)
        {
            TextAnchor anchor = candidateAntcList.get(i);

            if (anchor.containsWord(antcTokenId))
                return anchor;
        }
        return null;
    }
/* 
    //------------------------------------------------------------------------------------------------------------------------/
    // Check if the anaphora and antedecent are related to each other through subject/object
    // relationships
    //-----------------------------------------------------------------------------------------------------------------------/
    protected boolean isObjectAnchor(AnaphoraInfo anInfo,  TextAnchor anchor, 
        ArrayList<TextAnchor> sentenceAnchors )
    {
        for (int i =0; i < sentenceAnchors.size(); i++)
        {
            TextAnchor sanchor = sentenceAnchors.get(i);
            if (sanchor == anchor)
                continue;
           // check relationships 
        
    }
*/
        
    //----------------------------------------------------------------------------------------------------
   // Determine the noun(s) that  may be antecent to the given  singular  pronoun
    // for which no antecedents were determined (in person/location list)
    //-------------------------------------------------------------------------------------------------------------
      protected TextAnchor   findAnaphoricMatch(AnaphoraInfo pinfo, boolean checkPlural)
      {
        // find the entry in the candidates list matching with this pronoun  
        int sentenceId = pinfo.anaphorSentenceId;

        // check within range 
       TextAnchor antcAnchor = null;
       // Arrange sentences in reverse order for search
        ArrayList <TextAnnotation> reversedSentences = reverseSentenceOrder(anchorMap);
        for (int si = 0; si < reversedSentences.size() && (antcAnchor == null)  ; si++)
        {
            TextAnnotation sentenceAnnot = reversedSentences.get(si);
            if (sentenceAnnot.id > sentenceId)
                continue;
            
            // locate the closest matching anchor - both nouns and adjectives
            ArrayList<TextAnchor> sentenceAnchors =  anchorMap.get(sentenceAnnot);
            for (int i = 0; i < sentenceAnchors.size(); i++)
            {
                TextAnchor anchor = sentenceAnchors.get(i);
                if (anchor.getType() != TextAnchor.NOUN_ANCHOR)
                    continue;
                if (hasMatchingProperties(pinfo, anchor, checkPlural))
                {
                    if (antcAnchor == null)
                        antcAnchor = anchor;
                    else        // select the higher one
                        antcAnchor = getRightmostAnchor(anchor, antcAnchor);     // sort within the same sentence        
                }   // end if isMatchingProperties
            }   // end  anchors lloop
        }   // end sentence loop
        return antcAnchor;       
    }

   //------------------------------------------------------------------------------------------------------------   
   // Determine the noun(s) that  may be antecent to the given "plural" pronoun
   // paramter candidates - set of possible candidates for evaluation
   // This method is the heart of this class. It implements a simplified version of the 
   // algorithm suggested by (a) Hobbs (b) Lasso
    // Note: It is assumed that singular pronouns are already resolved by 
    // external Pronominal tools in the processing pipeline
    // We ignore SENTENCE_SPAN because of a small number of sentences, 
    // and search backword in sentences starting from pronoun term sentemce.
    //-------------------------------------------------------------------------------------------------------------
    protected TextAnchor[]  resolveAnaphoricMatches(AnaphoraInfo pinfo)
    {
        // find the entry in the candidates list matching with this  PLURAL  pronoun 
        int antcSentenceId = pinfo.anaphorSentenceId;

          // check within range 
       ArrayList<NounAnchor>   nounAnchors = new ArrayList();
       // Arrange sentences in reverse order for search
        ArrayList <TextAnnotation> reversedSentences = reverseSentenceOrder(anchorMap);
        for (int si = 0; si < reversedSentences.size(); si++)
        {
            TextAnnotation sentenceAnnot = reversedSentences.get(si);
             if (sentenceAnnot.id > antcSentenceId)
                continue;
           
            // locate the closest matching anchor - both nouns and adjectives
            ArrayList<TextAnchor> sentenceAnchors =  anchorMap.get(sentenceAnnot);
            for (int i = 0; i < sentenceAnchors.size(); i++)
            {
                TextAnchor anchor = sentenceAnchors.get(i);
                if (anchor.getType() == TextAnchor.NOUN_ANCHOR)
                    nounAnchors.add((NounAnchor)anchor);
            }            // not checking for pronouns
        } 
         PronounTerm pterm =PronounTerm.getPronounTerm( pinfo.anaphorText.toLowerCase()) ;
        ArrayList <NounAnchor> antcAnchors = getCandidateMatches(nounAnchors, pterm);
        if (antcAnchors.isEmpty())
            return null;
        TextAnchor[]  candidateAnchors = new TextAnchor[antcAnchors.size()];
        antcAnchors.toArray(candidateAnchors);
        return candidateAnchors;       
    }
   

/***************************************************************************************/
  // Check if the properties of the given anchor matches with that of the Pronoun
 /***************************************************************************************/
      public boolean  hasMatchingProperties(AnaphoraInfo pinfo, TextAnchor anchor, boolean checkPlural)
      {
        if (anchor.getType() != TextAnchor.NOUN_ANCHOR)
            return false;

        NounAnchor na = (NounAnchor) anchor;
        if (na.isPronounType())
            return false;
        
        PronounTerm pterm =PronounTerm.getPronounTerm( pinfo.anaphorText.toLowerCase()) ;
        // check if singular or plural in reverse order
 
        // match person/location etc.
        if (pterm == null)
            return false;
       else if (pterm.typeRef == PronounTerm.PERSON && !na.isPerson)
            return false;
        else if (pterm.typeRef == PronounTerm.LOCATION && !na.isLocation)
            return false;
        
        // Check if the given noun is the last person in the Person list prior to the pronoun.
        boolean preceeding = false;
        int ptermOfffset = pinfo.anaphorTokenId;
        
        if (na.isPerson && (na.isSubject || na.isObject))  
            preceeding =  isPreceedingPerson(na, ptermOfffset);

        if ((pterm.subjectRef == PronounTerm.SUBJECT
                || pterm.subjectRef == PronounTerm.SUBJECT_OBJECT) && !na.isSubject && !preceeding)
            return false;

         if ((pterm.subjectRef == PronounTerm.OBJECT
                 || pterm.subjectRef == PronounTerm.SUBJECT_OBJECT) && !na.isObject && !preceeding)
             return false;
        
         if (!checkPlural)           // only for singular pronouns
            return (pterm.numberRef == 1);
        
         else    // check for plural match - numRef must be > 1
         {
             if (pterm.numberRef  == 1)
                 return false;
             else 
                return  (na.getConjunct() != null);           // has a conjunction, assume same type
         }
      }
      
      /*--------------------------------------------------------------------------------------------------*/
      // Check if the given Person is the one just  preceeging the pronoun 
      protected boolean isPreceedingPerson(NounAnchor na, int startOffset)
      {
           ArrayList<NounAnchor> persons = new ArrayList();
            for (int i = 0; i < candidateAntcList.size(); i++)
            {
                TextAnchor anchor = candidateAntcList.get(i);

                if (anchor.getType() != TextAnchor.NOUN_ANCHOR) 
                    continue;
                NounAnchor pna = (NounAnchor) anchor;
                if (PersonAnalyzer.isInPersonList(personAnnots, pna))
                    persons.add(pna);
            }
             Collections.sort (persons);
             
             NounAnchor preceeding = null;
             for (int i = 0; i < persons.size(); i++)
             {
                 NounAnchor pna = persons.get(i);
                 if (pna.getGovernorToken().offsets[0] > startOffset)
                     break;
                 else
                     preceeding = pna;
             }
             return (preceeding != null && preceeding.equals(na));
      }


      /*****************************************************************************/
      // Find the noun terms in the given list matching the given properties
      // Note: We are only looking for persons and locations
      //----------------------------------------------------------------------------------------------/
      protected ArrayList<NounAnchor> getCandidateMatches(ArrayList <NounAnchor>naList,
            PronounTerm pterm)
      { 
          ArrayList<NounAnchor> matches = new ArrayList();
          for (int i = 0; i < naList.size(); i++)
          {
              // check if singular or plural in reverse order
              NounAnchor na = naList.get(naList.size() - 1 - i);
              if (na.isPronounType())
                  continue;
              
              // match person/location etc.
              if (pterm.typeRef == PronounTerm.PERSON && !na.isPerson)
                  continue;
              else if (pterm.typeRef == PronounTerm.LOCATION && !na.isLocation)
                  continue;

              if ((pterm.subjectRef == PronounTerm.SUBJECT
                  || pterm.subjectRef == PronounTerm.SUBJECT_OBJECT) && !na.isSubject)
                  continue;

              if ((pterm.subjectRef == PronounTerm.OBJECT
                  || pterm.subjectRef == PronounTerm.SUBJECT_OBJECT) && !na.isObject)
                  continue;
              matches.add(na);
          }
          //-----------------
          if (matches != null)
          {
             System.out.println(">>>  Possible Antecedents for \'" + pterm.word + "\' are: " );
              for (int i = 0; i < matches.size(); i++)
                    System.out.println( matches.get(i).getTextWithConjunct());        
          }
            return matches;
      }   

  /*****************************************************************************************************/
  // Resolve which one is the right most one between two anchors based upon their
  // token offset.
   protected TextAnchor getRightmostAnchor ( TextAnchor anchor1, TextAnchor anchor2)
   {
       if (anchor1 == null)
           return anchor2;
       else if (anchor2 == null)
           return anchor1;
       
        return (anchor1.getGovernorToken().id > anchor2.getGovernorToken().id) ?
            anchor1: anchor2;
   }
  /**************************************************************************************************/
  // Resolve which one is the right most one between an anchors  and anthor by checking their
  // tokenIds
    protected TextAnchor resolveRightmostAnchor(TextAnchor anchor1,  int[] tokenIds)
    {
        // Determine the anchor containing the rightmost token
        int rhsTokenId = tokenIds[tokenIds.length-1];
        TextAnchor anchor2 = null;
        Iterator <TextAnnotation> it = anchorMap.keySet().iterator();
        
        while (it.hasNext() && anchor2 != null)
        {
            TextAnnotation sentenceAnnot = it.next();
            // locate the matching anchor - both nouns and adjectives
            ArrayList<TextAnchor> sentenceAnchors =  anchorMap.get(sentenceAnnot);
            for (int i = 0; i < sentenceAnchors.size() && anchor2 != null; i++)
            {
                TextAnchor anchor = sentenceAnchors.get(i);
                if (anchor.containsWord(rhsTokenId))
                    anchor2 = anchor;
            }
        }
        return  getRightmostAnchor (anchor1,  anchor2);
    }

  /*****************************************************************************/
  public TreeMap<TextAnchor, AnaphoraInfo> getAnaphoraData()
  {
      return anaphoraMap;
  }
} 


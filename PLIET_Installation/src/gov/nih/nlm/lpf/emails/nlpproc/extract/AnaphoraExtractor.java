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
package gov.nih.nlm.lpf.emails.nlpproc.extract;

import gov.nih.nlm.lpf.emails.nlpproc.structure.AnaphoraInfo;


import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class AnaphoraExtractor
{
    private  static final Logger log = Logger.getLogger(AnaphoraExtractor.class);
    
     final static String firstPersonSingular = "(?i)i|me|my|mine";
     final static String firstPersonPlural = "(?i)we|us|our|ours";
    
    Document gateDoc;
   // HashMap <Integer, AnaphoraInfo> anaphoraList = new HashMap();
    AnnotationSet docAnnotationSet;
    String documentText;
    
    public AnaphoraExtractor(gate.Document aGateDoc)
    {
        gateDoc = aGateDoc;
        docAnnotationSet = gateDoc.getAnnotations();
        documentText = gateDoc.getContent().toString();
    }
    
     /****************************************************************************
     * Extract each anaphoric term and related information in the document  : 
     * Save the information for later use
     *  Return object:  TreeMap, PRN entry ID vs. all relevanr anaphoric info
     * 
     * NOTE: The Integer key represents the Annotation ID of the Anaphor in the
     * PRN (Pronoun) list with cls = Perspn (not Thing)
     * 
     ****************************************************************************/
     public   TreeMap<Integer, AnaphoraInfo>  extractAnaphoraInfo()
     {
        // Get all required annotations as subsets of the "default" Annotation set      
        // get all terms marked as PRN and WHICH, which contains Pronouns and Adverbs used as pronouns
        AnnotationSet prnSet =  docAnnotationSet.get("PRN");
        AnnotationSet whichSet =  docAnnotationSet.get("WHICH");        // ignored currently

        ArrayList<Annotation> pronounList = new ArrayList();
        pronounList .addAll(prnSet);
        pronounList.addAll(whichSet);
        Collections.sort(pronounList,  new gate.util.OffsetComparator ());

         //ArrayList  <AnaphoraInfo> prnAnaphor = buildPronounAnaphoras(pronounSet);
        TreeMap<Integer, AnaphoraInfo> anaphoraMap = storePass1Info( pronounList);
         return anaphoraMap;
     }
     /*------------------------------------------------------------------------------------------------------------------*/     
     // Save anaphora information doing various Annotation aps in the Pipeline
     //
     // Check the Location annotations and pair-up anaphora/antecedents
     // Get the Location type pronouns in the Pronoun List and perform the match
     // Note: We ignore Thing types as they have an associated Person PRN also
     // annotSet => set of PRN annotations for which we are matching anaphors and antecedents
     //------------------------------------------------------------------------------------------------------------------------
    protected TreeMap<Integer, AnaphoraInfo> storePass1Info(
                ArrayList<Annotation> pronounList )
    {
        String thing = AnaphoraInfo.ThingType;

        // Save first person pronouns with matching antecedents throufg dependency analysis
        FirstPersonAnphMatcher firstPersonMatcher = new FirstPersonAnphMatcher( pronounList, docAnnotationSet);
        TreeMap <String, Annotation> firstPesonAnaphors = firstPersonMatcher.getMatches();

        TreeMap<Integer, AnaphoraInfo> anaphoraMap  = new TreeMap();

        Iterator<Annotation> it = pronounList.iterator();
        while (it.hasNext())
        {
            AnaphoraInfo anInfo = null;         // initialize

            Annotation pannot = it.next();
            FeatureMap pfeatures = pannot.getFeatures();            // Featurs in the PRN annotaton
            String type = (String) pfeatures.get("cls");                      // class or type: should be Person or Location; ignore "Thing"
            if (type.equals(thing))                                                          //"Thing" usally has a matching Person or Location, so ignore it.
                continue;    
            String person =  (String) pfeatures.get("person");
            if (person == null  || person.equals("second"))             // We don't resolve pronouns like "you". "your", etc. here
             continue;
            
            if (person.equals("first")&& firstPesonAnaphors != null)
                anInfo = getFirstPersonAnaphoraInfo(pannot,  docAnnotationSet, firstPesonAnaphors);
            else
            {
               anInfo = getStandardAnaphoraInfo(pannot,  docAnnotationSet);
            }
            if (anInfo == null)
            {     
                 log.warn(" No  antecedents found for pronoun anaphor " + pannot.getFeatures().get("string"));
                 continue;
            }
            anaphoraMap.put(anInfo.anaphor.getId(), anInfo);   
           log.info("Extracttion:  Antecedent Location/Person Annotation for " +  anInfo.anaphorText +  "[Id="+ anInfo.anaphorTokenId+ "]  " +  anInfo.antecedentText );
        }
    return anaphoraMap;
}
 /*---------------------------------------------------------------------------------------------------------------------------*/   
    // Get the annotation in the  Location/Person  set matching the PRN Token annotation "pannot"
    // If not found, PronounAnnotator did not create any anaphor/antecedent entry for it under Location/Person
    // Reason: Plural pronouns (they, their etc, refereing to a set of persons/locations
   /*---------------------------------------------------------------------------------------------------------------------------*/   
  protected AnaphoraInfo getStandardAnaphoraInfo( Annotation pannot, AnnotationSet docAnnotationSet)
  {
        FeatureMap anphFeatures = pannot.getFeatures();
        AnaphoraInfo anInfo;
        Annotation anaphor = getMatchingAnnotation(pannot,  docAnnotationSet, "Person", true);
        if (anaphor == null)             // A Pronoun, with no matching entry in Person/Location list
        {
            log.warn("Could  not find matching Anaphor  for PRN in Location/Person set" + pannot.toString());
            // Save the anaphore data 
            anInfo = new AnaphoraInfo(AnaphoraInfo.PRNType);   // to be resolved later
            anInfo.anaphor = pannot;           // mark the PRN annotation as the anaphor
            Annotation anphToken = getMatchingAnnotation(pannot, docAnnotationSet, "Token", false);
            anInfo.anaphorTokenId = anphToken.getId();
            anInfo.anaphorText = (String) anphFeatures.get("string");
            String poss = (String) anphFeatures.get("case");
            anInfo.isPossessive = (poss != null)  && poss.equalsIgnoreCase("possessive");
        }

        else            // anaphor exists in Location/Person List, make sure it is not like "I am John Doe"
        {
             String person =  (String) anphFeatures.get("person");

            // get its antecedent annotation from the Featurelist of the anaphor
            anphFeatures = anaphor.getFeatures();
              // Store the information in an AnaphoraInfo struct
            anInfo = new AnaphoraInfo(AnaphoraInfo.PersonType);
            anInfo.anaphor = anaphor;

            // get corresponding TokenId from token annotations
            Annotation anphToken = getMatchingAnnotation(anaphor, docAnnotationSet, "Token", true);
           anInfo.anaphorTokenId =  anphToken.getId().intValue();
           anInfo.anaphorText = (String) (anphToken.getFeatures().get("string"));

            // recurse to see if this antecedent refers to another one  for chained references, and save the final antecedent info
            Integer antcId = null;
            Annotation antecedent = null;
            Integer refAntcId = (Integer) anphFeatures.get("antc");
            while (refAntcId != null)
            {
                antcId = refAntcId;
                antecedent = docAnnotationSet.get(antcId);
                refAntcId = (Integer) antecedent.getFeatures().get("antc");
            }
            
            anInfo.antecedent = antecedent;
            anInfo.antecedentText = (String) anphFeatures.get("antcTxt");   

           Annotation antcToken = getMatchingAnnotation(antecedent, docAnnotationSet, "Token", false);
           if (antcToken != null)
           {    
               // get the final antecent by checking the sentence context
               antcToken = finalizetAntcFromContext(antcToken, docAnnotationSet);
               
               anInfo.antcTokenId =  antcToken.getId().intValue();
           }
           else
                anInfo.antcTokenId = -1;            // not a single token
        }
        
        
          // add other features from the anphoric term
        anInfo.person = (String) anphFeatures.get("person");
        anInfo.gender = (String) anphFeatures.get("gender");
        anInfo.isPlural = ((String) anphFeatures.get("number")).equals("plural");
        AnnotationSet sentences =  docAnnotationSet.getCovering("Sentence", 
        anInfo.anaphor.getStartNode().getOffset(), anInfo.anaphor.getEndNode().getOffset());
        anInfo.anaphorSentenceId = sentences.iterator().next().getId().intValue();
        return anInfo;
  }
  
   /*---------------------------------------------------------------------------------------------------------------------------*/   
  // Match the given pronoun against an entry in the TreeMap, 
  // where the key is a prnoun string and the value is a "Person"
  // Use  the Token corresponding to that Person as the antecedent
   /*---------------------------------------------------------------------------------------------------------------------------*/   
  
  protected AnaphoraInfo getFirstPersonAnaphoraInfo(Annotation pannot, 
      AnnotationSet docAnnotationSet, TreeMap<String, Annotation> firstPesonAnaphors)
  {
       Annotation anaphor = pannot;
       FeatureMap anphFeatures = anaphor.getFeatures();
       String anaphorText = ((String) anphFeatures.get("string")).toLowerCase();
       String anphGender = ((String) anphFeatures.get("gender")).toLowerCase();
       Annotation pantc = firstPesonAnaphors.get(anaphorText);      // from Person List
       if (pantc == null)           // no direct matchmatches 
       {    
            // not a direct annotation match; match the text match, such as matching "I" against my or vice versa
            Iterator <String> it = firstPesonAnaphors.keySet().iterator();
            while (it.hasNext() && pantc == null)
            {
                String anphText  =  it.next().toLowerCase();  //
                Annotation person =  firstPesonAnaphors.get(anphText);
                 // check tne is the possessive form of the other. TBD: better check for Plurals
                 if (anaphorText.matches(firstPersonSingular) && anphText.matches(firstPersonSingular))
                     pantc = person;
                 else if (anaphorText.matches(firstPersonPlural) && anphText.matches(firstPersonPlural))
                     pantc =  person;
            }
       }
        if (pantc == null)          // no matchinf person
            return null;
        
        // Find the Token for this Person
        Annotation antcToken  = getMatchingAnnotation(pantc, docAnnotationSet, "Token", false);
        if (antcToken == null) 
        {
            log.error("Could not find matching token for Person, ID = " + pantc.getId());
            return null;
        }
        // Fill in the info
        AnaphoraInfo anInfo = new AnaphoraInfo("Person");
        anInfo.anaphor = pannot;
        Annotation anphToken =  getMatchingAnnotation(pannot, docAnnotationSet, "Token", false);
        anInfo.anaphorTokenId =  anphToken.getId().intValue();
        anInfo.anaphorText = (String) (anphToken.getFeatures().get("string"));

        // save the antecedent info
        anInfo.antecedent = antcToken;
        int start = pantc.getStartNode().getOffset().intValue();
        int end = pantc.getEndNode().getOffset().intValue();
        anInfo.antecedentText = documentText.substring(start, end);     
        anInfo.antcTokenId =  antcToken.getId().intValue();

        // add gender from either term
        String antcGender = (String) pantc.getFeatures().get("gender");         // get from Person
        if (antcGender != null  && antcGender.matches(NERConstants.KNOWN_GENDERS))
            anInfo.gender = antcGender;         // could be "unknown" or "either"
        else if (anphGender != null  && anphGender.matches(NERConstants.KNOWN_GENDERS))
            anInfo.gender = anphGender;
        else
            anInfo.gender = "";
       
        // get rest from the pronoun
        anInfo.person = (String) anphFeatures.get("person");
        anInfo.isPlural = ((String) anphFeatures.get("number")).equals("plural");
        AnnotationSet sentences =  docAnnotationSet.getCovering("Sentence", 
                anInfo.anaphor.getStartNode().getOffset(), anInfo.anaphor.getEndNode().getOffset());
        anInfo.anaphorSentenceId = sentences.iterator().next().getId().intValue();
        return anInfo;       
    }


      

      
  /*---------------------------------------------------------------------------------------------------------------------------*/    
    protected Annotation getFirstPersonMatch(Annotation anaphor, 
       TreeMap<Annotation, Annotation> firstPesonAnaphors)
    {
        Annotation antc = firstPesonAnaphors.get(anaphor);
        if (antc != null)           // matches directly
            return antc;
        // not a direct match, such as matching "I" against my or vice versa
        return null;        // TBD
    }

    /**************************************************************************************************
     * Get the annotation in the Annotation of a given type which corresponds to the input
     * annotation (in a different set). Note the offsets should be the same of both annotations.
     ***************************************************************************************************/
    public Annotation getMatchingAnnotation(Annotation annot, AnnotationSet annotSet, 
            String type, boolean exactMatch)
    {
          Long startOffset = annot.getStartNode().getOffset();
          Long endOffset = annot.getEndNode().getOffset();
           
           // find the matching annotation in the Person list
          Annotation matchingAnnot  = null;
           AnnotationSet  setToMatch = annotSet.get(type, startOffset, endOffset);
           if (setToMatch== null ||setToMatch.isEmpty())
               return null;         // none matching the given annotations offsets
           
           else
           {
               if (exactMatch) 
                  matchingAnnot = getExactMatchingAnnot(setToMatch, startOffset, endOffset);
              else
                   matchingAnnot =  setToMatch.iterator().next();  // return the first one anyway
           } 
          return matchingAnnot;
    }
  /*-------------------------------------------------------------------------------------------------------------*/  
    
    protected Annotation getExactMatchingAnnot(AnnotationSet setToMatch, 
     Long startOffset, Long endOffset)
    {
        Iterator<Annotation> it = setToMatch.iterator();
        while (it.hasNext())
        {
            Annotation next =it.next();
             if ( ( next.getStartNode().getOffset().longValue() == startOffset ) &&
                  ( next.getEndNode().getOffset().longValue() == endOffset))
                 return next;
         }
        return null;
    }

    /*-----------------------------------------------------------------------------------------------------------*/
    // Get  IDs of tokens corresponding to a given annotation (which may span multiple tokens
    //
    public  int[] getTokenIdsInSpan(Long start, Long end)
    {
        AnnotationSet  tokens =   docAnnotationSet.get("Token", start, end);
        if (tokens == null || tokens.isEmpty())
                   return null;
                    
        int[]  tokenIds = new int[tokens.size()];
        Iterator <Annotation> iterator = tokens.iterator();
        int i = 0;
        while (iterator.hasNext())
            tokenIds[i++] = iterator.next().getId().intValue();
        return tokenIds;
    }
 /*---------------------------------------------------------------------------------------------------------------------------
    *  If  the matching annotation not a proper noun but a relative, check  the context to 
    *  distinguish between: (a) I am Samanth's sister and  (b) I am looking for Samantha's sister.
    *      In the  case (a): antecedent  is: Samantha, in  case (b): antecedent  is: sister.
    * The anaphor matcher, in both cases, returns sister as the antecedent..
     * We distinguish the two cases by checking the subject of the token "sister"
   --------------------------------------------------------------------------------------------------------------------------- */
    // To be implemented
    protected Annotation  finalizetAntcFromContext(Annotation  refAnnot , AnnotationSet docAnnots)
    {
            return refAnnot;       
    }    
           // check if is the subject or object of the anaphor
}

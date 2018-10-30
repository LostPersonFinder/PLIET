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
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;
//import  gov.nih.nlm.ceb.lpf.emails.nlp.VerbAnchor;

import gov.nih.nlm.lpf.emails.nlpproc.structure.AnaphoraInfo;
import gov.nih.nlm.lpf.emails.nlpproc.structure.CorefInfo;

import gate.Annotation;

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;
  

    public class CorefResolver
    {
        private  static final Logger log = Logger.getLogger(CorefResolver.class);
        //private static int SENTENCE_SPAN = 2;

     
        protected TreeMap<TextAnnotation, ArrayList <TextAnchor>> anchorMap;
        protected TreeMap<Annotation, Annotation>corefAnnotMap;
        protected TreeMap<TextAnchor, AnaphoraInfo> anaphoraMap;
        protected  ArrayList <TextAnnotation> personList;
        protected ArrayList <TextAnchor> nounList;              //  list of all proper nouns or places in the sentences
        protected TreeMap <TextAnchor, CorefInfo> corefMap = new TreeMap();   // list of all coreferences
    /**
    *
    * @author 
    */
    public  CorefResolver( TreeMap<TextAnnotation, ArrayList <TextAnchor>> anchorMap,
        TreeMap<Annotation, Annotation>corefAnnots, TreeMap<TextAnchor, AnaphoraInfo> anaphoraMap,  
        ArrayList <TextAnnotation> personList)
     {
         this.anchorMap = anchorMap;                  // All sentences and anchors
         this.anaphoraMap = anaphoraMap;        // Pronoun->Noun anaphoric links
         this.corefAnnotMap = corefAnnots;
         this.personList = personList;
         
         nounList  = new ArrayList();                       // List of nouns only, pronouns wiuld be through AnaphoraMap
         Iterator<TextAnnotation> it  =  anchorMap.keySet().iterator();
         while (it.hasNext())
         {
             TextAnnotation sentence = it.next();
             ArrayList <TextAnchor> allAnchors =  anchorMap.get(sentence);       // all anchors for the sentence
             for (int i = 0; i < allAnchors.size(); i++)
             {
                 if (allAnchors.get(i).getType() == NLPRelations.NOUN)
                     nounList.add(allAnchors.get(i));
             }
        }
     }
     /*-------------------------------------------------------------------------------------------------------
     * Resolve the coreference between various Noun Phrases and noun->Pronoun
     * phrases following a set of rules. In the resulting map, the value referes to the
     * final (full) NounAnchor representing a Person, where as the key is either a short
     * form (first name, lastname, or another reference such as "my son", etc.
     * Note: We establish coreference between single persons only, or simple plurals
     *----------------------------------------------------------------------------------------------------------*/
     public void resolveCorefs()
     {
         HashMap <TextAnchor[], String> corefs = new HashMap();
         String rule = "";
         
         for (int i = 0; i < nounList.size(); i++)
         { 
             // Check coreference for each anchor in the Noun List
            TextAnchor phrase = nounList.get(i);
             rule = "Matching name";
            TextAnchor coref   = applyMatchingNameRule(phrase, nounList);
            if (coref  == null)
            {
               rule = "Name Attribute";
               coref  =applyNameAttributeRule(phrase, corefAnnotMap,  anchorMap);
            }
            if (coref  == null)
            {
                rule = "Dep relation";
                coref   = applyDepRelationRule(phrase);
            }
          /* if (coref == null)
            {
                rule = "Self Reference";
                coref = applySelfReferenceRule(phrase, personList);         // points to self for 
            } */
            if (coref  != null)
            {
                log.info("Coreference for " + phrase.getPhraseText() + " [Id="+phrase.getGovernorToken().id +
                "] : " +coref.getPhraseText() +  " [Id="+coref.getGovernorToken().id +"]"   +"; Rule: " + rule);
               
                TextAnchor[] corefAnchors = new TextAnchor[]{phrase, coref};
                corefs.put(corefAnchors, rule);
            }
         }
        TreeMap <TextAnchor, CorefInfo> corefInfoMap = saveCorefInfo(corefs);
        
        // Add anaphora references to this map also, for easier coreference resolution
        corefInfoMap = addAnaphoraInfo(corefInfoMap, anaphoraMap);
        
        // set the gender of each coreferenced person, if not yet set (already set for anaphroric ones)
        corefInfoMap = reconcileCorefs(corefInfoMap);
        corefMap = corefInfoMap;
        
        printCorefs(corefMap);
        return;
     }
/*-----------------------------------------------------------------------------------------------------------*/
  // Resolve the  coreference between the given phrase and other anchors by performing
  // a name match. Return after the best match. Ignore  if self match
  /*-----------------------------------------------------------------------------------------------------------*/
     protected TextAnchor applyMatchingNameRule(TextAnchor phrase,  
         ArrayList<TextAnchor>nounList)
     {
         TextAnchor coref = null;
         
         TextAnchor matchingName = NameResolver.getMatchingName(phrase, nounList);
       
         if (matchingName != null && !matchingName.equals(phrase))
         {   
             if (!TextAnchor.isPronoun(phrase.getGovernorToken()))
                coref = NameResolver.getLongerName(phrase, matchingName);
         }
         else       // no direct matching of one name as substring of another
             coref = matchingName;
         return  coref;
     }

  /*-----------------------------------------------------------------------------------------------------------*/
  // Resolve the  coreference between the given phrase and another anchors  by
  // checking if the later is connected to the former by "Name" attribute, such as
  // My name is David (My -> David). His son's name is Nick. (His son -> Nick)
  /*-----------------------------------------------------------------------------------------------------------*/
       protected TextAnchor applyNameAttributeRule(TextAnchor phrase,
            TreeMap<Annotation, Annotation> corefAnnotMap, 
            TreeMap<TextAnnotation, ArrayList <TextAnchor>> anchorMap)
     {
         // get the Anchor corresponding to the Annotations
         if (corefAnnotMap == null || corefAnnotMap.isEmpty())
             return null;
        Iterator <Annotation>  it = corefAnnotMap.keySet().iterator();
        while (it.hasNext())        // next sentence
        {
            Annotation anaphorToken = it.next();
            TextAnchor anaphorAnchor  = getAnchor(anaphorToken);
            if (anaphorAnchor.equals(phrase) )           // LHS of a corefSet
            {
                 Annotation corefToken= corefAnnotMap.get(anaphorToken);
                 TextAnchor corefAnchor  = getAnchor(corefToken);
                 return corefAnchor;
            }   
        }        
        return null;
     }
   /***************************************************************************************/
   // Apply the "dep" relationship link between two noun anchors two use the RHS 
   // side as the coreference for the LHS side, as in I am looking for my son David. 
   // For example: (my son=>David)
   /***************************************************************************************/
    protected TextAnchor applyDepRelationRule(TextAnchor phrase)  
    {
        if (phrase.getType() != NLPRelations.NOUN)
            return null;
        TextAnchor depAnchors[]  = ((NounAnchor)phrase).getDependentAnchors("dep");
        if ( depAnchors == null)
            return null;
        return depAnchors[0];           // assume single match
    }
 /*-----------------------------------------------------------------------------------------------------------*/
  // Check if it is a person, to be referring to self for matching of other properties
  /*-----------------------------------------------------------------------------------------------------------*/
/*     protected TextAnchor applySelfReferenceRule(TextAnchor candidate,   
         ArrayList <TextAnnotation> persons)
     {
         boolean isPerson = PersonAnalyzer.isInPersonList(persons, candidate);
         if (!isPerson)
             return null;
         else
             return candidate;          // return self as reference
     } */
 
    
   /***************************************************************************************/
    // Determine the anchor  which contains a given TextAnnotation (that has the
    // same ID as the specified Token annotation
    // It could be a pronoun (in NounAnchors) or an Adverb (in Adjective Anchor)
    protected TextAnchor getAnchor(Annotation tokenAnnot)
    {
        // find the noun terms that preceed the given pronoun in the text (sentences)
        Iterator <TextAnnotation> it = anchorMap.keySet().iterator();
        while (it.hasNext())
        {
            TextAnnotation sentenceAnnot = it.next();
            // locate the matching anchor - both nouns and adjectives
            ArrayList<TextAnchor> sentenceAnchors =  anchorMap.get(sentenceAnnot);
            for (int i = 0; i < sentenceAnchors.size(); i++)
            {
                TextAnchor anchor = sentenceAnchors.get(i);
                if (anchor.containsWord(tokenAnnot.getId()))
                    return anchor;
            }
        }
        // something wrong --
        System.out.println(">>> ERROR: No Noun anchor  found for Coref  term " + 
            tokenAnnot.getFeatures().get("string"));
        return null;
    }    
    
  //---------------------------------------------------------------------------------------------------------------- 
  // Save corresponding to each entry by creating a CorefInfo structure 
 // LHS is the referring anchor, RHS is the final resoved anchor (may be the same as LHS)
  //----------------------------------------------------------------------------------------------------------------
    protected TreeMap <TextAnchor, CorefInfo> saveCorefInfo(
       HashMap <TextAnchor[], String> corefs)
    {
       TreeMap <TextAnchor, CorefInfo> corefInfoMap = new TreeMap();
       Iterator <TextAnchor[]> it = corefs.keySet().iterator();
       while (it.hasNext())
       {
           TextAnchor[] corefAnchors = it.next();
           TextAnchor lhsAnchor = corefAnchors[0];
            TextAnchor corefAnchor = corefAnchors[1];
            String rule = corefs.get(corefAnchors);
            CorefInfo corefInfo = new CorefInfo(lhsAnchor, corefAnchor, rule);
            setPersonGender(corefInfo);
            corefInfoMap.put(lhsAnchor, corefInfo);
       }
       return corefInfoMap;
    }
   //---------------------------------------------------------------------------------------------------------------- 
    // Determine the gender of a coreferenced person fron the Person Annotations
    //----------------------------------------------------------------------------------------------------------------
    protected void setPersonGender(CorefInfo corefInfo)
    {
        corefInfo.corefGender  = "";
        
          // Check the Person List for a matching referring or referred name and set the gender if known
        String[] names = {corefInfo.corefText, corefInfo.referringText};
        for (int i = 0; i < names.length; i++)
        {
            String name = names[i];
            TextAnnotation person = null;
            for (int j =0; j < personList.size(); j++)
            {
                if (personList.get(j).text.equals(name))
                {
                    person = personList.get(j);
                    break;
                }
            }
            String gender = (String)personList.get(i).getFeature("gender");
            if (gender != null && gender.matches(NERConstants.KNOWN_GENDERS))
            {
               corefInfo.corefGender = gender;
               return;
            }
        }       
    }

  //---------------------------------------------------------------------------------------------------------------- 
  // Convert the anaphora information to corefInformation (with rule = anaphoric
  // and save together with othe coreference data
  //----------------------------------------------------------------------------------------------------------------
    protected TreeMap <TextAnchor, CorefInfo> addAnaphoraInfo(
    TreeMap <TextAnchor, CorefInfo> corefInfoMap, 
    TreeMap<TextAnchor, AnaphoraInfo>anaphoraMap)
    {
        if (anaphoraMap == null || anaphoraMap.size() == 0)
            return corefInfoMap;
        
        // Add the anaphoric terms
        if (corefInfoMap == null)            // no corefences
           corefInfoMap = new TreeMap();
        Iterator <TextAnchor> it = anaphoraMap.keySet().iterator();
        while (it.hasNext())
        {
            AnaphoraInfo anInfo = anaphoraMap.get(it.next());
            TextAnchor[]  antcAnchors = anInfo.antecedentAnchors;
            if (antcAnchors == null || antcAnchors.length == 0)
                continue;               // may be person=second as "you"
            
            // Check if the coreference map contained the anaphoric term
            CorefInfo cinfo = corefInfoMap.get(anInfo.anaphorAnchor);
            
            if (cinfo == null)      // does not contain the refereing Anchor
            {
                System.out.println("-- Adding " + anInfo.anaphorText + " [Id=" + anInfo.anaphorAnchor.getAnchorId()
                  +  "]  from Anaphor Map to CoreferenceMap");       
                CorefInfo corefInfo = new CorefInfo(anInfo.anaphorAnchor,
                    anInfo.antecedentAnchors[0], "Anaphoric");
                corefInfo.corefGender = anInfo.gender;
                corefInfoMap.put(anInfo.anaphorAnchor, corefInfo);
            }
            // contains the same refering anchor. If yes, continue.
            //Otherwise add theanrecedent as the referringAnchor for the coreference
            else if (!cinfo.corefAnchor.equals(anInfo.antecedentAnchors[0]))
            {
                TextAnchor refAnchor = anInfo.antecedentAnchors[0];
                 System.out.println("-- Adding " + anInfo.antecedentText+ " [Id=" + refAnchor.getAnchorId()
                  +  "]  from Anaphor Map to CoreferenceMap");       
                CorefInfo corefInfo = new CorefInfo(refAnchor, cinfo.corefAnchor, "Anaphoric");
                corefInfo.corefGender = anInfo.gender;
                corefInfoMap.put(refAnchor, corefInfo);
            }   
            else
                continue;
        }
        return corefInfoMap;
    }
    
/************************************************************************************************/
 /*   protected void setPersonGender( TreeMap <TextAnchor, CorefInfo> corefMap)
    {
        ArrayList<CorefInfo> corefs = new ArrayList(corefMap.values());
        for (int i = 0; i < corefs.size(); i++)
        {
            CorefInfo cinfo = corefs.get(i);
            if (cinfo.corefGender.matches(NERConstants.KNOWN_GENDERS))
                continue;           // already set, from anaphor
            TextAnchor canchor = cinfo.corefAnchor;
        }
    }
*/ 
/************************************************************************************************/
    // Examine the coreferences and determine if a coreference is
    // linked to another coreference. Then use the last one.
    // (if A -> B, B-> C then A -> C)

    TreeMap <TextAnchor, CorefInfo> reconcileCorefs(TreeMap <TextAnchor, CorefInfo>corefInfoMap)
    {
        if (corefInfoMap == null || corefInfoMap.isEmpty())
            return corefInfoMap;

        Iterator <TextAnchor> it = corefInfoMap.keySet().iterator();
        while (it.hasNext())
        {
            TextAnchor refAnchor = it.next();
            CorefInfo cinfo = corefInfoMap.get(refAnchor);
            TextAnchor corefAnchor = cinfo.corefAnchor;
            
             // check if it is a reference too
            CorefInfo cinfo1 = corefInfoMap.get(corefAnchor);
            if (cinfo1 == null)
                continue;
            
            // copy the new reference info to the old one
            cinfo.corefAnchor =   cinfo1.corefAnchor;
            cinfo.corefAnnot =  cinfo1.corefAnnot;
            cinfo.corefText = cinfo1.corefText;
            if (cinfo1.corefGender.matches("male|female"))
                cinfo.corefGender = cinfo1.corefGender;
            cinfo.resolutionRule = cinfo.resolutionRule.concat(", ").concat(cinfo1.resolutionRule);
        }
        return corefInfoMap;
    }

    
    protected void  printCorefs(TreeMap<TextAnchor, CorefInfo> corefMap)
    {
        if (corefMap == null || corefMap.size() == 0)
            return;
        Iterator <TextAnchor> it  = corefMap.keySet().iterator();
        while (it.hasNext())
        {
            CorefInfo cin = corefMap.get(it.next());
            System.out.println (">> Referrent: " + cin.referringText +" [ID=" + cin.referringAnchor.getAnchorId() +
                "],  Coref :" + cin.corefText + " [ID=" + cin.corefAnchor.getAnchorId() +"]");
        }
    }

  /******************************************************************************************    *****/
  public TreeMap<TextAnchor, CorefInfo> getCorefData()
  {
      return corefMap;
  }
 //----------------------------------------------------------------------------------------------------------------
}

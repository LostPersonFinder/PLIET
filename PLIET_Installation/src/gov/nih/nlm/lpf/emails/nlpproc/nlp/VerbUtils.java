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
import gate.FeatureMap;

import  gov.nih.nlm.lpf.emails.nlpproc.ner.LPFVerbs;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.LinkedAnchorMap;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.ClausalAnchor;
import gov.nih.nlm.lpf.emails.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class VerbUtils implements NLPRelations, LPFVerbs
{
    private static Logger log = Logger.getLogger(VerbUtils.class);
    
  /*------------------------------------------------------------------------------------------
  // Return the type of clause to which this verb belongs
   /*------------------------------------------------------------------------------------------*/
     public  static String getClauseType(VerbAnchor vbr)
     {
        int ctype  = vbr.clausalType ;
        if (ctype == PRIMARY_TYPE)
            return "";
        for (int i = 0; i < VerbAnchor.DependentClauseDefs.length; i++)
        {
            if (ctype  == VerbAnchor.DependentClauseDefs[i].type)
                return  VerbAnchor.DependentClauseDefs[i].clauseType;
        }
        return null;            // unknown type
    }
     
     
   //----------------------------------------------------------------------------------//  
    // get the auxilary verb for a given verb in the set
    // Dependency "aux" or "auxpass" implies  hasAuxilary 
    //----------------------------------------------------------------------------------//
    public static  TextAnnotation getAuxiliary(TextAnnotation word, ArrayList <TextAnnotation> tokens)
    {
       String rel = "aux";
        int auxId  = word.getDependentAnnotationId("aux"); // is it an auxilary for another 
        if (auxId == -1)
        {
            rel = "auxpass";
            auxId = word.getDependentAnnotationId("auxpass");
            if (auxId == -1)
                return null;            // has no auxiliary

        }
        // recurse for the same relation link
        int nextAuxId = auxId;
        TextAnnotation aux = null;
        while (nextAuxId != -1)
        {
           aux = TextAnnotation.getAnnotationById(tokens, nextAuxId);
            nextAuxId = aux.getDependentAnnotationId(rel);
        } 
        return aux;
    }        

   /************************************************************************************************/
   // find the subject for a given verb, in the following ways (in order of prioriry)
    // 1) A  direct relation: through nsubj or nsubjpass 
    // 2) A  direct relation: through the head of a Conj relation
    // 3) An indirect relation through "xcomp" dependency with another verb
    // 4) A mentioning relation  whose  "rcmod" or "partmod" relation points to this verb
    // Note: Subjects derived through subjects of other Verbs are performed later
    /************************************************************************************************/
   public static TextAnchor getSubjectForVerb(VerbAnchor vbr, ArrayList <TextAnchor> allAnchors)
    {
        // 1)  try direct subjects, either straight or though an xsubj relation
         TextAnchor[] subjs;
        if (vbr.isActive())
        {  
              subjs = vbr.getDependentAnchors("nsubj");
              if (subjs == null)
                    subjs = vbr.getDependentAnchors("xsubj");    
        }
        else // passive
        {
              subjs = vbr.getDependentAnchors("nsubjpass");
              if (subjs == null)
                    subjs = vbr.getDependentAnchors("xsubjpass");    
        }
        if (subjs != null && subjs.length > 0 )
            return subjs[0];
        
       // #2) Conj governor
        if (vbr.isConjunction())
        {
            VerbAnchor conjHead = vbr.getConjunctionHead();
           return  getSubjectForVerb(conjHead, allAnchors);
        }
        
        // #3)  if the Subject is a clause, get the subject of that clause
         TextAnchor[] subjVbrs;
        if (vbr.isActive())
              subjVbrs = vbr.getDependentAnchors("csubj");
        else      
             subjVbrs = vbr.getDependentAnchors("csubjpass");
       if (subjVbrs != null && subjVbrs.length > 0)
       {
           TextAnchor subj  = getSubjectForVerb( (VerbAnchor)subjVbrs[0],allAnchors);
           if (subj != null)
               return subj;
       }

        
        // 4) If it is an xcomp - find if it is in a clausal list
         TextAnchor  clausalGov  =    (TextAnchor) vbr.getClausalGovernor();   
         if (clausalGov != null)
         {
             if (clausalGov.getType() == TextAnchor.NOUN_ANCHOR)
                return  clausalGov;
             else if (clausalGov.getType() == TextAnchor.VERB_ANCHOR)
             {
                VerbAnchor verbAncor  =    (VerbAnchor) vbr.getClausalGovernor();    //vbr.getClausalGovernor(allAnchors, "xcomp"); 
                if (verbAncor != null  )
                 return  (getSubjectForVerb(verbAncor,  allAnchors));
             }
         }
        // 5) try  Mentioning (referencing)  anchor for other relations
        TextAnchor subj = vbr.getMentioningAnchor(allAnchors, "partmod");    
        if (subj == null)
            subj = vbr.getMentioningAnchor(allAnchors, "rcmod");   
       
        // 6) check if the verb is referenced as "dep" -- when the parser cannot fully resolve it: e.g. John missing in Bethesda"
        if (subj == null)
            subj = vbr.getMentioningAnchor(allAnchors, "dep");  
        
        // 7.  check if it has a direci object that is a WHICH word
        if (subj == null)
            subj = checkForWHobjects(vbr);                
        return subj;
    }
         
            
    // WH subjects (as in  "Looking for John, who lives in Rockville",      who is DOBJ of lives! 
    protected static TextAnchor checkForWHobjects(VerbAnchor vbr)
    {
        TextAnchor[] dobj = vbr.getDependentAnchors("dobj");
        if (dobj == null || dobj .length == 0)
            return null;
       for (int j = 0; j < dobj.length; j++)
       {
            TextAnchor  anchor = dobj[j];
            TextAnnotation annot = anchor.getGovernorToken();
            String category = (String) annot.getFeature("category");
            if (category != null && category.equals("WP"))
                return anchor;
       }
       return null;
    }

   /*------------------------------------------------------------------------------------------------*/
   // Get the Phrasal (words or phrases)  acting as the objects of this verb
   // Note: This does not include clauses stch as "ccomp"
   /*--------------------------------------------------------------------------------------------------*/
    public static LinkedAnchorMap  getPhrasalObjects(VerbAnchor vbr)      
    {
        String[] phObjectRels  = {"dobj", "iobj", "prep"};     // word or phrasea
        LinkedAnchorMap objectMap  = new LinkedAnchorMap();
        
        for (int i = 0; i < phObjectRels.length; i++)
        {
            String rel = phObjectRels[i];
            AnchorLink[] depLinks = vbr.getDirectDependencyLinks(rel);
            if (depLinks == null)          // no relations of this type
                continue;    
            
            for (int j = 0; j < depLinks.length; j++)           // for all links of a given type
            {
                AnchorLink objLink = depLinks[j];
                String type = objLink.linkType;
                if (type.equals("prepc"))
                    continue;           // a clausal object, not stored here
                
                if (type.equals("prep") && objLink.linkSubtype != null)
                    type += "_" + objLink.linkSubtype;           // concatenate the prep word 
                objectMap.addAnchor(type, objLink.dependentAnchor);

                if (objectMap.get(type).size() > 1)         // more than one object for the same link type
                    log.info("Number of objects of type " + type + " for  verb " + vbr.getText() + " = " + objectMap.get(type).size() );
            }       // end for depLink
        }   // end for rel
        return objectMap;
    }
    
       /*--------------------------------------------------------------------------------------------------*/
    // get the top level non-clausal direct/indirect objects associated with this verb
    public static ArrayList<TextAnchor>  getTopLevelObjects(VerbAnchor vbr)      
    {
        String[] objectRels  = {"dobj", "iobj", "prep"};     // word or phrasea
        ArrayList <TextAnchor>   topObjs= new ArrayList();
        
        for (int i = 0; i < objectRels.length; i++)
        {
            String rel = objectRels[i];
            AnchorLink[] depLinks = vbr.getDirectDependencyLinks(rel);
            if (depLinks == null)          // no relations of this type
                continue;    
            
            for (int j = 0; j < depLinks.length; j++)           // for all links of a given type
            {
                AnchorLink objLink = depLinks[j];
                if (objLink.dependentAnchor != null)
                    topObjs.add(objLink.dependentAnchor);
            }       // end for depLink
        }   // end for rel
        return topObjs;
    }
   
     //--------------------------------------------------------------------------------------------------------/   
   // >>>>> This module is not needed if we are buildingS  ClauseTree <<<<<<<<//  
   public static HashMap  <String, ArrayList<ClausalAnchor>>  setClausalDependencies(
                        VerbAnchor vbr, ArrayList <TextAnchor> anchorSet)
    {
       ArrayList <TextAnnotation> tokenSet = vbr.getTokenSet();
       HashMap  <String, ArrayList<ClausalAnchor>> clausalMap  = new HashMap();
       for (int i = 0; i < VERB_CLAUSAL_DEPENDENCIES.length; i++)
       {
           String relation = VERB_CLAUSAL_DEPENDENCIES[i];
            int[] depIds = vbr.governor.getDependentAnnotationIds(relation);      // dependent
            if (depIds == null || depIds.length == 0)            // no such relation
                continue;
            ArrayList<ClausalAnchor> clausalAnchors = new ArrayList();
            for (int j = 0; j < depIds.length; j++)
            {
                ClausalAnchor canchor = generateClausalAnchor( vbr, depIds[j], relation, anchorSet, tokenSet);
                if (canchor != null)
                    clausalAnchors.add(canchor);
            }
            if (clausalAnchors.size() > 0)
                clausalMap.put(relation, clausalAnchors);
       }
       
        // All pcomp clause, linking through prepc preposition (but also accasionally through "prep")
       TextAnnotation governor = vbr.governor;
       
        int[] prepcIds = governor.getDependentAnnotationIds(PREPC);      // dependent
        ArrayList< TextAnnotation> prepcAnnots = governor.getDependentAnnotations("prepc",  tokenSet);
        if (prepcAnnots  == null ||  prepcAnnots.isEmpty())
             prepcAnnots = governor.getDependentAnnotations("prep",  tokenSet);
        if (prepcAnnots!= null &&  prepcAnnots.size() > 0)           // got prepc links
        {
            ArrayList<ClausalAnchor> pcompAnchors = new ArrayList();
            for (int i = 0; i < prepcAnnots.size(); i++)
            {
                ClausalAnchor panchor = generatePcompAnchor(vbr, prepcAnnots.get(i), anchorSet, tokenSet);
                if (panchor != null)
                    pcompAnchors.add(panchor);
            }
            if (pcompAnchors.size() > 0)
                clausalMap.put("pcomp", pcompAnchors);
        }   
       
       // Add relation for PComp Link - done differently from other ones as: prepc->pcomp
       return clausalMap;
    } 
    /*--------------------------------------------------------------------------------------------------------
    * Generate a ClausalDependent for this Anchor
    * clsId = Annotation Id of the head word (verb) of the clause
    */
    protected static ClausalAnchor  generateClausalAnchor(VerbAnchor vbr, int clsId, 
        String clsRelation,  ArrayList <TextAnchor> anchorSet, ArrayList<TextAnnotation> tokenSet)
    {
        TextAnchor  depAnchor  = vbr.getAnchorByID(anchorSet, clsId);      // the dependent of this link
        if (depAnchor == null)
            return null;
        ClausalAnchor cla = new ClausalAnchor(vbr, clsRelation);
        cla.clsHead = depAnchor;               
        String connectText = "";
        String connectRelation = ClausalAnchor.getConnector(clsRelation);
         if (connectRelation != null && connectRelation.length() > 0 )
         {
             // find the annotation with this relation and get its text
             TextAnnotation connectAnnot = 
                 depAnchor.governor.getDependentAnnotation(connectRelation, tokenSet);
             if (connectAnnot != null)
                 connectText = connectAnnot.text;
         }
         cla.setConnectionText(connectText);
          return cla;
    }
    /*--------------------------------------------------------------------------------------------------------
    * Generate a "pcomp" ClausalDependent for this Anchor
    * prepcId = Annotation Id of the head word (verb) of the clause
    */
    protected static ClausalAnchor  generatePcompAnchor(VerbAnchor vbr, TextAnnotation prepcAnnot,   
        ArrayList <TextAnchor> anchorSet, ArrayList<TextAnnotation> tokenSet)
    {  
        String clsRelation = "pcomp";
        // get the head annotation of the clause  which has a pcomp relation to this prepc
        TextAnnotation pcompAnnot = prepcAnnot.getDependentAnnotation(clsRelation, tokenSet);
        if (pcompAnnot == null)
            return null;             // because we are also checking with "prep" dependencies too
        
        TextAnchor  depAnchor  = vbr.getAnchorByID(anchorSet, pcompAnnot.id);      // the dependent of this link
        if (depAnchor == null)
            return null;
        ClausalAnchor cla = new ClausalAnchor(vbr, clsRelation);
        cla.clsHead = depAnchor;                                   // the pcomp anchor      
        cla.setConnectionText (prepcAnnot.text);         // the actual preposition text
        return cla;
    }

   /*-------------------------------------------------------------------------------------------------------*/ 
    protected TextAnchor[]  getPcompAnchors(VerbAnchor vbr)
    {
       // get the pcomp clausal anchors by doing through prepc->pcomp
       ArrayList <TextAnchor> pcomps = new ArrayList();
       TextAnchor[] panchors= vbr.getDependentAnchors("prepc");
        if (panchors == null ||  panchors.length == 0)
            return null;
        
        for (int i = 0; i< panchors.length; i++)
        {
            TextAnchor[] pc = panchors[i].getDependentAnchors("pcomp");
            if (pc == null || pc.length == 0)
                continue;
            // add the pcomps to the array
            for(int j = 0; j < pc.length; j++)
               pcomps.add(pc[j]);
        }
        if  (pcomps.size() == 0)
            return null;

        TextAnchor[] panch = new TextAnchor[pcomps.size()];
        pcomps.toArray(panch);
         return panch;
    }
    
    public static boolean   isNegativeStatus(String negativeStr, String[]adjectives)
    {
       boolean isNegative = (negativeStr != null && negativeStr.length() > 0);
        // tbd: interrogative
        
        if (!isNegative) 
        {
            for (int i = 0; i < adjectives.length && !isNegative; i++)
            {
                String adjective = adjectives[i].toLowerCase();
                if (adjective != null && adjective.matches(NLPRelations.NEGATIVE_ADJECTIVES))       // TBD: other terms
                    isNegative = true;
            }
        }
        return isNegative;
    }
    
/****************************************************************************************/
// Standard Static "grammar-based" functions
/***************************************************************************************/
    /******************************************************************************************************
 * Verb Tense Classification - Related to Modal Verbs
* Reference:   bartonig   bartonig is offline Senior Member
* Join Date:  Oct 2005, Native language: UK English
*	
 *  In English there are, in a sense, two basic tenses - the present simple and past simple. So, for example, 
 * give / gave, dream / dreamt (or dreamed), say / said, have / had, do / did, am / was, can / could, shall / should, 
 * will / would and so on. 
 * The simple aspect is used to describe complete whole actions such as I gave him a present / I give presents at Christmas.
*
 * Most verbs have a past participle, a passive participle and an active participle - for example, given, given and giving. 
 * Have and be are called auxiliary verbs because they are used with one of the participles to construct 
 * other tenses for describing things in the present or with a sense of the past that cannot be done with just the 
 * present and past simple. For example:
 *        I am giving / I have given / I was given / I had given / I have been given / I have been giving / I am being given and so on.
  * Do is an auxiliary used in constructing questions, expressing negation and emphasizing affirmation in the simple tenses. 
 * For example:
 *         Did you give a present? / You didn't give me a present. / I do give presents!
 * 
 * The modal auxiliary verbs are used with some of these tenses to give an additional sense. 
 * The additional senses include possibility, probability, ability, necessity and so on. 
 *  - 'Will' is used to express certainty - I will give him a present (sometimes called the future tense). 
 * - 'Can' expresses ability - I can afford a present. 
 * - 'Have' to expresses an external obligation - I have to give him a present. My mum said so. 
 * - 'May' expresses permission - I may give him a present. My Mum said so. 
 * These examples are of modal verbs used in their present simple form. Most have a past simple form
 *  which is used to express hypothecation and probability. For example, 
 *      if he was nice, I would give him a present and I might have given him a present.
 *
 *  In some varieties of English two modal verbs are accepted in some combinations. In BrE we can say 
 *     I might have to give him a present.
 * 
*   Of course, there is a lot more to it than I have described. You need to get a grammar - a modern one at about intermediate level. 
 * A good one is English Grammar in Use by Raymond Murphy. 
/********************************************************************************************************************/

     public static int  getVerbTense(VerbAnchor vbr)
     {
           int verbTense = vbr.getVerbTense();
           if (verbTense >=  0)             // already determined
               return verbTense;
           
           
           FeatureMap  features  = vbr.getMainVerb().features;
           String verbCategory  = (String) features.get("category");
           
           // check if this is a stand-alone verb or has relations to any other verb in the sentence  
           TextAnnotation aux = vbr.getAuxilliaryVerb();
           TextAnnotation xcompObj = vbr.getXcompObject();
            
           TextAnnotation pcompObj = null;
           TextAnchor[]  pcomps = vbr.getDependentAnchors("pcomp");
           if (pcomps != null && pcomps.length > 1)
               pcompObj = pcomps[0].getGovernorToken();
           
          // If this is a stand-alone verb, just find its tense
           boolean standAlone = (aux == null && xcompObj == null && pcompObj == null);
           
           if (standAlone)
           {
              verbTense = getVerbTenseFromLists(vbr.getText(), verbCategory);
              return verbTense;
           }
           // Has association with other verbs, determine its tense from one of them
           else if (aux != null)
           {
               String auxCat  = (String) (aux.features.get("category"));
               return getVerbTenseFromLists(aux.text, auxCat);
           }
           
           else if ( xcompObj != null)
           {
               String xvCat  = (String) xcompObj.getFeature("category");
               return getVerbTenseFromLists(xcompObj.text, xvCat);
           }
           else if ( pcompObj != null)
           {
               String pvCat  = (String) pcompObj.getFeature("category");
               return getVerbTenseFromLists(pcompObj.text, pvCat);
           }
           return UNKNOWN_TENSE;        // Could not be figured out
     }
 /****************************************************************************************************/    
     protected static int  getVerbTenseFromLists( String word, String vcat)
     {
           if (Utils.isInList(vcat, PRESENT_TENSE_VERB))
               return PRESENT_TENSE;
            
           else if (Utils.isInList(vcat, PAST_TENSE_VERB) || Utils.isInList(vcat, PAST_PARTICIPLE_VERB))
               return PAST_TENSE;
           
           else if (vcat.equals(MODAL))                 // a Model Verb: MD
           {
               if (Utils.isInList(word.toLowerCase(), PROJECTION_LIST))
                   return PROJECTED_TENSE;
           }  
           return UNKNOWN_TENSE;
     }
}

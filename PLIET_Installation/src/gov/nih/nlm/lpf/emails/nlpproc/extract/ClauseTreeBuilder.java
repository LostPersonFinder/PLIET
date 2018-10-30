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

import gate.creole.ANNIEConstants;


import  gov.nih.nlm.lpf.emails.nlpproc.structure.AnchorSet;
import gov.nih.nlm.lpf.emails.nlpproc.structure.LinkedAnchorMap;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.AnchorLink;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NounAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.AdjectiveAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.ClausalConstants;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ClauseTree;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.Clause;
import gov.nih.nlm.lpf.emails.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;           // retains insertion order
import java.util.Set;
import java.util.Iterator;

import org.apache.log4j.Logger;
/**
 *
 * @author 
 */
public class ClauseTreeBuilder
{
    private  static Logger log = Logger.getLogger(ClauseTreeBuilder.class);
    
   public static boolean debugClause = true;
 
     String documentText;
     TextAnnotation sentence;
     
  /**  TypedAnnotationMap annotMap; **/
     ArrayList<NounAnchor> nouns;
     ArrayList<VerbAnchor> verbs;
     ArrayList<AdjectiveAnchor> adjectives;
     ArrayList <TextAnchor> allAnchors ;
     
     ArrayList<TextAnnotation> dependencies;
     ArrayList<TextAnnotation> tokens;
      ArrayList<TextAnnotation> sentenceAnnots;

     String annotField;         // text field to which the annotations belong


    // TextAnnotation is the "leading" word or Token of the corresponding Anchor
    protected TreeMap<TextAnnotation, VerbAnchor> verbTable;           // stores all verb info
    protected TreeMap<TextAnnotation, NounAnchor> nounTable;         // stores all noun  info
    protected TreeMap<TextAnnotation, AdjectiveAnchor> adjectiveTable;      //stores all adjective info
      
     /*  Constructor:
      * Param: anchorData containing iinfo about all anchors in a sentence
     *                  documentTr
      */
    public ClauseTreeBuilder(AnchorSet anchorData)
    {
        // store data locally
        documentText = anchorData.documentText;
        sentence = anchorData.sentence;
        verbTable = anchorData.verbTable;      //stores all adjective info
        nounTable = anchorData.nounTable;      //stores all adjective info
        adjectiveTable = anchorData.adjectiveTable;     //stores all adjective info
        tokens = anchorData.getAnnotations(ANNIEConstants.TOKEN_ANNOTATION_TYPE);
        sentenceAnnots = anchorData.getAnnotations(ANNIEConstants.SENTENCE_ANNOTATION_TYPE);
            
        nouns = new ArrayList();
        nouns.addAll(nounTable.values());
        
        adjectives = new ArrayList();
       adjectives.addAll(adjectiveTable.values());

        verbs = new ArrayList();
        verbs.addAll(verbTable.values());
        
        allAnchors  = new ArrayList();
        allAnchors.addAll(nouns);
        allAnchors.addAll(adjectives);
        allAnchors.addAll(verbs);
    }
    
    /*****************************************************************************************
     * Extract the set of  LPF relationships found within the given sentence by executing various 
     * dependency related rules.
     * @return clauseTree
     * 
     * Note: This method finds the primary  Verbs in a sentence and build the  
     *            Clausal hierarchy  for each of them.
     *     by descending each primary clause and adding the secondary clauses to the 
     *     primary clause through the SetParentClause() method.
     ****************************************************************************************/
    public  ClauseTree  buildClauseTree()
    { 
        if (verbs == null || verbs.size() == 0)
            return null;                // no verbs in this text
        
       Collections.sort(verbs);
       ClauseTree  clauseTree   = new  ClauseTree(sentence);
     
       boolean interrogative = isInterrogativeSentence(sentence);
       VerbAnchor[]  primaries = getPrimaryVerbsInSentence();
       int np =  primaries.length;      // number of Primaries
       for (int i = 0; i < np; i++)
       {
           VerbAnchor vbr = primaries[i];
           Clause clause =  buildClausalHierarchy(vbr, sentence, interrogative, documentText);
           String conj = ""; // TBD
           clauseTree.addMainClause(clause, conj);
        }
      
       if (np == 0)
            log.warn(">>> No primary Clauses found in sentence");
   
       else if (debugClause)
           System.out.println(clauseTree.toPrint());
        return clauseTree;
     }
    
   /***************************************************************************************
     *  NOTE:  morphological form of the first word for an interrogative sentence
     * does not work for this parser. We cannot also look for a "?"  at the end of a 
     * sentence, as the text may be poorly written.
     ************************************************************************************/     
    public boolean isInterrogativeSentence(TextAnnotation sentence)
    {
        if (sentence  == null || sentence.text.length()  == 0)
            return false;
        // NOTE:  morphological form of the first word for an interrogative sentence does not work for this parser  
       String[] words = sentence.text.split("\\W+");
        boolean   interogative = false;
        String  word = words[0];
       if (Utils.isInList( word, ClausalConstants.interogations) ||
           Utils.isInList( word, ClausalConstants.questions) )
           interogative = true;
       return  interogative;
    }

        
     /****************************************************************************************/   
    // TBD: When there are no primary verbs
    /***************************************************************************************/
    protected VerbAnchor[] getPrimaryVerbsInSentence()
    {
        ArrayList<VerbAnchor>  pv = new ArrayList();
        for (int i = 0; i < verbs.size(); i++)
        {
            VerbAnchor vbr = verbs.get(i);
            if (vbr.isPrimary())
                pv.add(vbr);
        }
        // now order it according to position in sentence
       int n = pv.size();
       if (n > 1) 
            Collections.sort(pv);
        
        VerbAnchor[] primaryVerbs = new VerbAnchor[n];
        pv.toArray(primaryVerbs);
        return primaryVerbs; 
    }    
   /****************************************************************************************/ 
    // Build the Clause Tree based upon the Verb (or Adjective if no verb exists)
    // first fill its subject (note: A subject may itself be a clause (xsubj)  
    // then fill its Objects, which may include other clauses
    /****************************************************************************************/ 
    protected Clause buildClausalHierarchy(TextAnchor vhead, TextAnnotation  sentence, 
                boolean interrogative, String document)
    {
        VerbAnchor vbr = (VerbAnchor)vhead;
        String clauseType = vbr.getClauseType();     //a Verb Anchor or Adjective used as Verb
       
        Clause clause = new Clause(vbr, clauseType, vbr.getClausalGovernor());
        clause.setSentence(sentence, interrogative);
        
        clause.subject = getSubject(vbr, allAnchors);
        clause.verbObjectMap = getPhrasalObjects(vbr);      
        if (clause.subject != null)
            clause.subjObjectMap = getPhrasalObjects(clause.subject);
        
        // Check other verbs to determine its subordinate clauses and descend dow,
        setSubordinates(clause);
        return clause;
    }
    /*------------------------------------------------------------------------------------------------------------*/
    // Set the subordinate clauses for this clause (acting as the parent) 
    // by checking the other  Anchors for which its Verb  is the  governor (mentioning) Anchor,
    // or one of its objects is the  governor Anchor
    // setParentClause() fills the static properties (documentText and sentence) from the parent.
    /*------------------------------------------------------------------------------------------------------------*/
    protected int setSubordinates(Clause pclause)
    {
        TextAnchor  vbr = pclause.clauseHead;
        int ns = 0;         // number of subordinates
        for (int i = 0; i < verbs.size(); i++)
        {
            VerbAnchor sv = verbs.get(i);
            if (sv.equals(vbr))
                continue;
            if (sv.getClausalGovernor() != null && sv.getClausalGovernor().equals(vbr))
            {
                // create the subordinate clause corresponding to this dependent anchor
                Clause sclause = new Clause(sv, sv.getClauseType(), vbr);
                sclause.setParentClause(pclause,  ((VerbAnchor)sv).getClausalMarker());
                sclause.subject = getSubject(sv, allAnchors);
                sclause.verbObjectMap = getPhrasalObjects(sv);      
                if (sclause.subject != null)
                    sclause.subjObjectMap = getPhrasalObjects(sclause.subject);
                setSubordinates(sclause);                // performs recursion
                ns ++;         
            }
        }
        return ns;
    }
   
   /************************************************************************************************/
   // find the subject for a given verb, in the following ways (in order of prioriry)
    // 1) A  direct relation: through nsubj or nsubjpass 
    // 2) A  direct relation: through the head of a Conj relation
    // 3) An indirect relation through "xcomp" dependency with another verb
    // 4) A mentioning relation  whose  "rcmod" or "partmod" relation points to this verb
    // Note: Subjects derived through subjects of other Verbs are performed later
    /************************************************************************************************/
    protected TextAnchor getSubject(VerbAnchor vbr, ArrayList<TextAnchor> allAnchors)
    {
        TextAnchor subj = vbr.getSubject(allAnchors);
         return subj;       
    }
    
    /*--------------------------------------------------------------------------------------------------------
     Get the phrasal objects associated with this clause. This includes objects 
     * associated with the verb (including the copular Object, as well as the subject, 
     * and recurses down the path 
     --------------------------------------------------------------------------------------------------------*/

protected LinkedAnchorMap  getPhrasalObjects(TextAnchor head)      
    {
        if (head == null)
            return null;
        // look for  relations  {"dobj", "iobj", "prep", "copobj"} 
        String[] phObjectRels  = NLPRelations.PHRASAL_OBJECT_RELATIONS;     // word or phrases
        
        LinkedAnchorMap objectMap  = new LinkedAnchorMap();
        
        for (int i = 0; i < phObjectRels.length; i++)
        {
            String rel = phObjectRels[i];
            AnchorLink[] depLinks = head.getDirectDependencyLinks(rel);
            if (depLinks == null || depLinks.length == 0)          // no relations of this type
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
                    log.info("Number of objects of type " + type + " for  verb " + head.getText() + " = " + objectMap.get(type).size() );
            }       // end for depLink
        }    
      
        if (objectMap.size() ==   0)
            return objectMap;
       
      // recurse over the phrasal objects to fill  lower level objects
        Set keySet = objectMap.keySet();
        Iterator it  = keySet.iterator();
        LinkedAnchorMap finalObjectMap = objectMap;
        while (it.hasNext())
        {
            ArrayList <TextAnchor> values = objectMap.get(it.next());
            for (int i = 0; i < values.size(); i++)
            {
                TextAnchor avalue = values.get(i);
                // Avoid circular relation with Copular verbs
                if (avalue == null || avalue  == head)
                    continue;
                
                LinkedAnchorMap lowerMap = getPhrasalObjects(avalue);
                // merge the contents of the two maps
                finalObjectMap =objectMap.mergeContents(lowerMap);
            }
        }   // end for rel
        return finalObjectMap;
    }
 
    
    /*************************************************************************************************/
     /*Find the LPF information in the sentence by applying Rule 2   
     * Rule 2: 
      *     Check an adjective and see if it is connected to a Person and/or a location
     * Note: 
       * Here we assume that ther is no ReportedPerson/Reporter verbs present, but other 
      * statnard verbs may be there. (E..g. John is fine:  note: fine  is an adjective)
      *************************************************************************************************/
/*    protected int applyAdjectiveRule(SentenceRelations lpfRelation)
    {
        LinkedAnchorMap objectMap = new LinkedAnchorMap();

        // get the first adjective with an nsubj
        Iterator<TextAnnotation> it = adjectiveTable.keySet().iterator();
        int status = 0;
        while (it.hasNext())
        {
            TextAnnotation adj = it.next();     // adjective  phrase
            AdjectiveAnchor adjAnchor = adjectiveTable.get(adj);      // the next Adj anchor  `
            if (adjAnchor.isLeaf())
                continue;                          // no links from here to anything
            
            // Check if the adjective anchor contains any status verb
            TextAnchor[] subjects = adjAnchor.getDependentAnchors("nsubj");
            if (subjects == null || subjects.length == 0)
            {
                // get the governor anchor with amod relationship in the nouns
                TextAnchor nounAnchor = adjAnchor.getMentioningAnchor(nouns, "amod");
                if (nounAnchor != null)
                    subjects = new TextAnchor[] {nounAnchor};
            }

            // get the location strings, may be more than one
            TextAnchor[] locationAnchors = null;
            ArrayList<AnchorLink> locationLinks = adjAnchor.getLocationLinks();
            if (locationLinks == null)          // no relations of this type
                continue;
            if (locationLinks.size() > 1)
                log.warn("Number of " + adj.text + " objects for adjective : " + adjAnchor.getText() + "is: " + locationLinks.size());

            ArrayList<AnchorLink> objLinks = locationLinks;
            for (int j = 0; j < objLinks.size(); j++)           // for all links of a given type
            {
                AnchorLink objLink = objLinks.get(j);
                String type = objLink.linkType;
                if (type.equals("prepc"))
                    continue;           // a clausal object, not stored here

                if (type.equals("prep") && objLink.linkSubtype != null)
                    type += "_" + objLink.linkSubtype;           // concatenate the prep word 

                objectMap.addAnchor(type, objLink.dependentAnchor);
                if (objectMap.get(type).size() > 1)         // more than one object for the same link type
                    log.info("Number of objects of type " + type + " for  Adjective " + adjAnchor.getText() + " = " + objectMap.get(type).size() );         
            }
            // Treat it as a Status Verb (e.g. "alive")
            SentenceRelations.LPFTriple triple = lpfRelation.createTriple(LPFVerbs. HEALTH_STATUS_VERB);
            triple.subject = (subjects == null) ? null : subjects[0];
            triple.verb = adjAnchor;
            triple.objects = objectMap;

            lpfRelation.addTriple(triple);
            status = 1;
        }
        return status;
    }
 */   
    /*************************************************************************************************/
     // Find the LPF information in the sentence by applying Rule 3
     /* Rule 3 
      *     Check each  Noun (/Pronoun) and see if it is connected to a Person and/or a location
     * This is invoked when there is no known REPORTER or   REPORTED_PERSON 
     * status verb found, plus  adjective anchors (acting as verbs) are found connected to this noun.
      *************************************************************************************************/
 /*   protected int applyNounRule(SentenceRelations lpfRelations)
     {
         // get the first verb with an nsubj
         Iterator <TextAnnotation > it = nounTable.keySet().iterator();
         while (it.hasNext())
         {
             TextAnnotation nounAnnot= it.next();     // verb phrase
             NounAnchor nounAnchor = nounTable.get(nounAnnot);      // the next noun anchor  `
            
             // check if it is connected to any adjective
             TextAnchor[] adjAnchors = nounAnchor.getDependentAnchors("amod");
             if (adjAnchors == null)
                 continue;
             ArrayList<TextAnchor> objectList = new ArrayList();
            objectList.add(adjAnchors[0]);

            SentenceRelations.LPFTriple  triple = lpfRelations.createTriple(StatusVerbs.UNKNOWN_VERB);

            triple.verb = null;
            triple.subject = nounAnchor;
            triple.objects = new LinkedAnchorMap(); 
            triple.objects.put("amod", objectList  );
            lpfRelations.addTriple(triple);
        }               
        return 1;
    }  
*/      

    //-----------------------------------------------------------------------------------------------//
    public String getDependentString( TextAnchor anchor,  String relation)
    {
        // get the Objects satisfying a given relation and return their string valuej
        String textStr = "";
        TextAnchor[] depAnchors = anchor.getDependentAnchors(relation);

        if (depAnchors == null && depAnchors.length  == 0)
            return "";

         for (int i = 0; i < depAnchors.length; i++)
        {
            textStr  +=  depAnchors[i].getTextWithConjunct();
        }
        return textStr;
    }

    /*********************************************************************************************/
}
     
   
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

/**
 *
 * @author 
 */

import gov.nih.nlm.lpf.emails.nlpproc.TextMessageProcessor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;

import gov.nih.nlm.lpf.emails.nlpproc.structure.ProcessedResults;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.AnchorSet;
import gov.nih.nlm.lpf.emails.nlpproc.structure.TypedAnnotationMap;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ClauseTree;
import gov.nih.nlm.lpf.emails.nlpproc.structure.AnaphoraInfo;


import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;


import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collections;

public class PLNLPExtractor implements TextMessageProcessor
{
      static String[] LPFAnnotTypes = new String[] {
       "Sentence",  "Token", "Lastname", "Person", "Location", "Attribute", 
       "Unknown",  "Lookup", "Address", "Date", "PRN", "WHICH"};
      
     HashMap<String, long[]> fieldExtents;          // email fields extent in the document message 
   
     String   originalText ;
     boolean useClause = true;          // analyze through clause structure
     
     // For the entire message
     //ClauseTree[]   lpfRecords;
   
     
     // The following data structuctures are created during processing
     TreeMap<TextAnnotation, ArrayList <TextAnchor>> anchorMap = new TreeMap();
     
     TypedAnnotationMap[]  annotatedSentenceMaps  = null;      // All annotations
     ArrayList<AnchorSet> anchorSets = null;
     TreeMap<Annotation, Annotation> corefInfo = null;
     TreeMap<Integer, AnaphoraInfo> anaphoraInfo = null;
     
     //--------------------------------------
     
     boolean printTokenDependency =  true;
     boolean testing =  false;
   
     protected ProcessedResults processedResults;
     protected Document  msgDoc;
    
     public PLNLPExtractor(Document gateDoc, ProcessedResults inputResults)
     {
         msgDoc = gateDoc;
         processedResults = inputResults;
     }
     
     //---------------------------------------------------------------------------------------------------------------
     // Extract information  relevant to LPF using NLP processing
     // This includes establishing clausal, anaphoric and coreference relations
     //---------------------------------------------------------------------------------------------------------------
    public  int extractLPFInfo()
     {
          // Extract all annotations of types needed for LPF analysis 
        annotatedSentenceMaps  =  extractMessageAnnotations(msgDoc);
        
        // generate the anchors and relationships from the annotation map of each sentence
        anchorSets = generateTextAnchors(msgDoc);
        
        // Simply extract the anaphora and coref info from the Token features.
        // Do not resolve to actial anchors  (full namrs etc.) here
        anaphoraInfo = extractAnaphoraInfo(msgDoc);
        corefInfo  = extractCorefInfo(msgDoc);
        
        processedResults.anaphoraInfo = anaphoraInfo;
        processedResults.corefInfo = corefInfo;
        processedResults.anchorMap = anchorMap;
         
                  
         useClauseApproach(true);

        if (testing)   // for testing 
            return 0;           // not supported anymore

        // build the clause tree and extablish Clausal Assertions
         ClausalRelationExtractor clsExtractor = 
             new ClausalRelationExtractor(msgDoc, annotatedSentenceMaps,  anchorSets);
         TreeMap<TextAnnotation, ClauseTree> clauseMap  = clsExtractor.extractClausesInSentences();
         
         // now update the processing results info

        processedResults.personList = clsExtractor.personList;
        processedResults.locationList = clsExtractor.locationList;
        processedResults.attributeList = clsExtractor.attributeList;
        processedResults.lpfClauseMap = clauseMap;
        processedResults.fragmentSentences = clsExtractor.getFragmentSentences();

         return 1;
     }
    
    public ProcessedResults getProcessedResults()
    {
        return processedResults;
    }

    /*--------------------------------------------------------------------------------------------------------------*/ 
   
   // extract the required annotations from the Gate annotated document
   protected   TypedAnnotationMap[]  extractMessageAnnotations(Document gateDoc)
   {
          // get the original text
        FeatureMap  docFeatures =   gateDoc.getFeatures();
        originalText  = gateDoc.getContent().toString();
        
        // get all types of desires annotations, ordered according to sentence
        
         TypedAnnotationMap[] annotatedSentenceMaps  = 
                getAnnotationsInSentences(gateDoc);     // Array of all sorted annotation maps for each sentence
        
        // get the offset of each field; NOTE: Annotation names start with Uppercase
        fieldExtents = new HashMap();
        AnnotationSet annotSet = gateDoc.getAnnotations(null);   
        Annotation subjAnnot = annotSet.get("subject").iterator().next();
        long[] subjOffsets = new long[]{subjAnnot.getStartNode().getOffset(), subjAnnot.getEndNode().getOffset()};
        fieldExtents.put("subject", subjOffsets);
        
        Annotation bodyAnnot = annotSet.get("body").iterator().next();
        long[] bodyOffsets = new long[]{bodyAnnot.getStartNode().getOffset(), bodyAnnot.getEndNode().getOffset()};
        fieldExtents.put("body", bodyOffsets);
        
        System.out.println("-- Subject offsets: [" + subjOffsets[0] + "," +subjOffsets[1]+"] ---");
        System.out.println("-- Body offsets: [" + bodyOffsets[0] + "," +bodyOffsets[1]+"] ---");
        return annotatedSentenceMaps;
    }   
    
   //--------------------------------------------------------------------------------------------------------------/
   // extract sentence structure through Clausal hierarchy. Currently, this is the 
   // only supported method.
    public void useClauseApproach( boolean doClause)
    {
        useClause = doClause;
    }
  
  //-------------------------------------------------------------------------------------------------------------   
  // Generate Text (Noun/Verb/Adjective) anchors using the Annotations in 
  //all sentences in the message
//-------------------------------------------------------------------------------------------------------------   
     protected ArrayList<AnchorSet>  generateTextAnchors(Document gateDoc)
     {
         // Generates anchors corresponding to Noun, Verb and Adjective Phrases or words in a sentence  
         ArrayList<AnchorSet> myAnchorSets = new ArrayList();
        for (int i = 0; i < annotatedSentenceMaps.length; i++)
        {
            // All annotations within a sentence as <type, ArrayList>
            TypedAnnotationMap sentenceAnnotations = annotatedSentenceMaps[i];
            TextAnnotation sentenceAnnot = sentenceAnnotations.get("Sentence").iterator().next();
            AnchorGenerator anchorGenerator = new AnchorGenerator(
                        gateDoc.getContent().toString(), sentenceAnnotations, "");
            anchorGenerator.generateAnchors();
            AnchorSet anchorData = anchorGenerator.getAnchorData();
            if (anchorData != null && anchorData.allAchors.size() > 0)
            {
                    myAnchorSets.add(anchorData);
                    anchorMap.put(anchorData.sentence, anchorData.allAchors);
            }
        }
        return (myAnchorSets);
     }

     /************************************************************************************************
     * Retrieve the required annotations, generated by the GATE pipeline.
     * and return them as a Map of Text annotations, with the annotation "type" as key
     * Note: The Email text is processed as a text document in the LPF application (.gapp) 
     * All annotations for this  "text/plain" document are in the "default annotation" list
     * @param gateDoc 
     ***********************************************************************************************/
    protected  TypedAnnotationMap[]  getAnnotationsInSentences(Document gateDoc)
    {
        // Get all required annotations as subsets of the "default" Annotation set
        // Note: An AnnotationSet is a derived class of java.util.set        
        AnnotationSet annotSet = gateDoc.getAnnotations(null);
        AnnotationSet sentenceSet =  annotSet.get("Sentence");
        
        // convert to an ordered list
        ArrayList <Annotation> sentList = new ArrayList ( sentenceSet );
        Collections.sort ( sentList , new gate.util.OffsetComparator ());

         // get all  types of annotations within each sentence 
        // (including the Annotation for the sentence itself)
        int ns = sentList.size();
        TypedAnnotationMap[] allAnnotationMap = new TypedAnnotationMap[ns];
        for (int i = 0; i < ns; i++)
        {
            Annotation sentence = sentList.get(i);
            TypedAnnotationMap annotMap = buildTextAnnotationsForSentence(sentence);
            allAnnotationMap[i] =annotMap;
        }
        return allAnnotationMap;
    }
  /**********************************************************************************************************/        
    // Return all TextAnnotations of specified types  within a sentence, including the sentence itself
    //Note: All anntations are created as TextAnnotations 
    protected  TypedAnnotationMap buildTextAnnotationsForSentence(Annotation sentence)   
    { 
        // get all annotations within this range
        Long startNode = sentence.getStartNode().getOffset();
        Long endNode  = sentence.getEndNode().getOffset();
        AnnotationSet annotSet = msgDoc.getAnnotations(null);     

        AnnotationSet annotsInSentence  = annotSet.getContained(startNode, endNode);  // all annotations within the sentence
        if (printTokenDependency)
            printAnnotations(annotsInSentence);      // for debug
      
        TypedAnnotationMap annotMap = new TypedAnnotationMap();
        // add the following types, in sorted manner,  to the TypedAnnotationMap
        for (int i= 0; i < LPFAnnotTypes.length; i++)
        {  
            String atype = LPFAnnotTypes[i];
            AnnotationSet typedSet  = annotsInSentence.get(atype);

            // convert it to an ArrayList of TextAnnotation in sorted order
             ArrayList <Annotation> typedList = new ArrayList ( typedSet );
            Collections.sort ( typedList , new gate.util.OffsetComparator ());

            // Convert from Annotation to TextAnnotatin (with -+    `````   `   ++  details)  object
            ArrayList <TextAnnotation> typedAnnotList  = new ArrayList();
            for (int j = 0; j < typedList.size(); j++)
            {
                TextAnnotation textAnnot  = new TextAnnotation(typedList.get(j), originalText);
                typedAnnotList.add(textAnnot);
                 //System.out.println("  -  "+textAnnot.toString());
            }
            annotMap.put(atype, typedAnnotList);
        }
        System.out.println("----");
        return   annotMap;
    }    
    
 
     /*****************************************************************************************/
    // Print all annotations of the given type in the specified set
    // Used for debug only
    protected void printAnnotations( AnnotationSet annotations)
    {
        // <<< ----  For debug only, print all annotations within this sentence
        ArrayList <Annotation> annotList = new ArrayList ( annotations );
        Collections.sort(annotList,  new gate.util.OffsetComparator());
        for (int i = 0; i < LPFAnnotTypes.length; i++)
        {
            
            String lpfAnnotType = LPFAnnotTypes[i];
        //String AnnotationsToPrint = "Sentence|Token|Lookup|Person|Location|Address|Date|Unknown|PRN|WHICH";
            for (int  j = 0; j < annotList.size(); j++)
            {
                Annotation annot = annotList.get(j);
                String type = annot.getType();
                if (type.equalsIgnoreCase(lpfAnnotType))
                {
                    TextAnnotation anDet = new TextAnnotation(annot ,  originalText);
                    System.out.println(anDet);
                }
            }
        }
    }

     /*****************************************************************************************/
    String getText(String fullText, Annotation annot)
    {   
        return fullText.substring(
            annot.getStartNode().getOffset().intValue(), annot.getEndNode().getOffset().intValue());
    }
    /*****************************************************************************************/
    protected String getText(Document gateDoc, Annotation annot)
    {
        String documentText = gateDoc.getContent().toString();
        return getText(documentText, annot);
    }
   
     /************************************************************************************
     * Extract each anaphoric term and related information in the document  
     * Save the information as Pass1 data in AnaphoraInfo structure
     **************************************************************************************/
    protected  TreeMap<Integer, AnaphoraInfo>  extractAnaphoraInfo(gate.Document gateDoc)
     {
        // Get all required annotations as subsets of the "default" Annotation set      
        AnaphoraExtractor anphExtractor = new AnaphoraExtractor(gateDoc);
        
        // The Integer key below is the ID of the entry in the pronoun (PRN/WHICH) list
        TreeMap<Integer, AnaphoraInfo> anaphoraMap = anphExtractor.extractAnaphoraInfo();
        return  anaphoraMap;
     }
 
     
    /************************************************************************************
     * Extract each anaphoric term and related information in the document  
     * Save the information as Pass1 data in AnaphoraInfo structure
     **************************************************************************************/
     protected  TreeMap<Annotation, Annotation>  extractCorefInfo(gate.Document gateDoc)
     {
        // Get all required co-references (refering to the same object)  as subsets of the "default" Annotation set      
        CorefExtractor corefExtractor = new CorefExtractor(gateDoc);
        
        TreeMap<Annotation, Annotation> corefMap = corefExtractor.extractCorefInfo();
        return  corefMap;
     }
     
}

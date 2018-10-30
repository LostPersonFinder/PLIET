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

import gov.nih.nlm.lpf.emails.nlpproc.infer.VerbInterpreter;
import gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve.AnaphoraResolver;
import gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve.CorefResolver;
import gov.nih.nlm.lpf.emails.nlpproc.TextMessageProcessor;
import  gov.nih.nlm.lpf.emails.nlpproc.extract.PLNLPExtractor;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;

import gov.nih.nlm.lpf.emails.nlpproc.structure.AnaphoraInfo;
import gov.nih.nlm.lpf.emails.nlpproc.structure.CorefInfo;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ProcessedResults;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ClausalAssertion;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ClauseTree;

import gov.nih.nlm.lpf.emails.nlpproc.structure.SubjectEntity;
import gov.nih.nlm.lpf.emails.nlpproc.structure.PLSearchInfo;

import gov.nih.nlm.lpf.emails.nlpproc.structure.EmailHeaderInfo;

import gate.Document;
import gate.Annotation;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class MessageAnalyzer implements TextMessageProcessor
{
   
    private Logger log = Logger.getLogger(MessageAnalyzer.class);
    
    // input data
    protected PLNLPExtractor nlpExtractor;                            // Object with extracted information
    protected Document msgDoc;
    protected ProcessedResults inputResults;
    
    // input data
    ArrayList <TextAnnotation> personList;
    ArrayList <TextAnnotation> locationList;
     ArrayList <TextAnnotation> fragmentSentences;
    
     // processed results
     protected ProcessedResults processedResults;
    TreeMap<TextAnchor, AnaphoraInfo> anaphoraMap;
    TreeMap<TextAnchor, CorefInfo>  corefMap;
    TreeMap<TextAnchor, SubjectEntity> subjectMap;
    SubjectEntity nullSubject;         // a special structure with assertions with no subject Anchor 
    ArrayList<PLSearchInfo>  fragmentSearchInfo;         // searching text for PL-specific annotations and values
    
    
      VerbInterpreter vbAnalyzer;
    public MessageAnalyzer(Document doc, PLNLPExtractor extractor)
    {
        msgDoc = doc;
        nlpExtractor = extractor;
        
     
        inputResults = extractor.getProcessedResults();
        retrieveInputData();                  // retrieve input data

        // initialize
        processedResults = inputResults;
    }
    
    protected void retrieveInputData()
    {
         personList = inputResults.personList;
        locationList = inputResults.locationList;
        fragmentSentences = inputResults.fragmentSentences;
        
    }
    
    // Analyze the information extracted so far, invoking various analysers/resolvers
    public  int  analyzeInfo()
    {
        anaphoraMap  = setAnaphorAnchors(inputResults.anaphoraInfo, inputResults.anchorMap);
        corefMap = setCorefAnchors(inputResults.corefInfo, inputResults.anchorMap, anaphoraMap, personList);
       
        // A sentence vs. all assertions in that sentence
        TreeMap <TextAnnotation, ArrayList<ClausalAssertion>> clausalAsertions = 
            analyzeClauses( inputResults.lpfClauseMap);
        
        // flatten the TreeMap to a single list of clausal assertions
          ArrayList<ClausalAssertion> msgAssertions =getAllClausalAssertsInMsg (clausalAsertions);
        
        // Analyze the subjects in clausal assrtion (clause)  and combine them to generate
        // a unique mao pd subjects, talink anaphora ans coreference into account
        // The key of the map is the bestmatch anchor fot that subject
         subjectMap = generateSubjectEntityMap(msgAssertions);
         // fragmentSearchInfo = processFragmentSentences();
          return 1;
    }
 
     /***************************************************************************************
      *  Resolve the TextAnchor for each Anaphor and the corresponding Antecedent
     *  in the document (Pass2 data)
     ****************************************************************************/
     protected TreeMap<TextAnchor, AnaphoraInfo> setAnaphorAnchors(
                TreeMap<Integer, AnaphoraInfo> anaphoraInfo,
                TreeMap<TextAnnotation, ArrayList <TextAnchor>> anchorMap)
     {
         AnaphoraResolver resolver = new AnaphoraResolver(anaphoraInfo, anchorMap,  personList);
         int naph = resolver.resolveAnaphora();      // number of anaphor resolved
        TreeMap<TextAnchor, AnaphoraInfo> anaphoraData = resolver.getAnaphoraData();
        return anaphoraData;
     }
     
     /***************************************************************************************
      *  Resolve the TextAnchor for each CorefAnnot  and the corresponding bestMatch 
     *  Anchor  in the document (Pass2 data)
     ****************************************************************************/
     protected TreeMap<TextAnchor, CorefInfo> setCorefAnchors(
                TreeMap<Annotation, Annotation> corefAnnots,
                 TreeMap<TextAnnotation, ArrayList <TextAnchor>> anchorMap,
                 TreeMap<TextAnchor, AnaphoraInfo> anaphoraMap,
                  ArrayList <TextAnnotation> personList)
     {
        CorefResolver corefResolver = new CorefResolver(anchorMap, corefAnnots, anaphoraMap, personList);
        corefResolver.resolveCorefs();
        TreeMap<TextAnchor, CorefInfo> corefData  = corefResolver.getCorefData();
        return corefData;
     }

  /*------------------------------------------------------------------------------------------------------------------*/
   // Analyse the clauses in each sentence and generate  corresponding assertions
   // for each clause. 
   // Each element in the ClausalAssertion Array represents all assertions corresponding to 
   // a main clause in a sentence
  /*------------------------------------------------------------------------------------------------------------------*/
    protected  TreeMap <TextAnnotation, ArrayList<ClausalAssertion>> analyzeClauses( 
                        TreeMap<TextAnnotation, ClauseTree> lpfClauseMap)
    {
       // get the Location Annotations and Person Annotations for this document from the stored set
         TreeMap <TextAnnotation, ArrayList<ClausalAssertion>>clausalAssertsMap = new TreeMap();;

         ArrayList <ClausalAssertion> clausalAssertList  = new ArrayList();       // List of all ClausalAssertion objects
        
         Iterator <TextAnnotation>  it = lpfClauseMap.keySet().iterator();          // top level clauses 
         while (it.hasNext())
         {
             TextAnnotation sentence = it.next();
             ClauseTree clsTree = lpfClauseMap.get(sentence);
             if (clsTree == null)           // no clauses found in this sentence, that is not a grammatical sentence with a verb
                 continue;
             
             ClauseAnalyzer clauseAnalyzer = new ClauseAnalyzer(clsTree, personList, locationList );
                 //locationList, personList, gateDoc);
             
             ArrayList <ClausalAssertion> clausalAssertsSet = clauseAnalyzer.analyzeClauses();          // for each sentences
             clauseAnalyzer.printClauses();
             clausalAssertsMap.put(sentence, clausalAssertsSet);        // sentence vs.  Assertions
         } 
         return clausalAssertsMap;
    }
    
    /****************************************************************************
        // Must be invoked after PersonList and LocationLists are built       
        boolean checkHeader = ctxInfo.checkHeader;
       if (checkHeader)
       {    
           // TBD: Determine the header section from the field extents in the message
            emailHeaderAnnotationMap = annotatedSentenceMaps[0];
            EmailHeaderAnalyzer emailAnalyzer = 
                    new EmailHeaderAnalyzer(emailHeaderAnnotationMap);
            emailHeaderInfo = emailAnalyzer.analyzeSubjectHeader();
       } 
*/
    
        
    /****************************************************************************
     * Analyze the fragment sentences, if any, in the message (including the
     * email subject header and save the information
     *  Note: proceed at Annotation level only - as parsed results may not be correct
     * -----------------------------------------------------------------------------------------------
    protected ArrayList<PLSearchInfo> processFragmentSentences()
    {
       if (fragmentSentences.isEmpty())
       {
           log.info("There are no sentence fragments in the message.");
           return null;
       }
           AnnotationSet fragmentSet = new AnnotationSetImpl(msgDoc, "Fragments");
           //ArrayList<Annotation> fragments = new ArrayList();
           AnnotationSet inputAS = msgDoc.getAnnotations();
           for (int i = 0; i < fragmentSentences.size(); i++)
           {
               int annotId = fragmentSentences.get(i).id; 
               Annotation annot = inputAS.get(annotId);
               fragmentSet.add(annot);
           };
           FragmentAnalyzer  fragmentAnalyzer = new FragmentAnalyzer(fragmentSet, msgDoc);
           fragmentAnalyzer.processFragments();
           ArrayList<PLSearchInfo> plInfo = fragmentAnalyzer.getPersonSearchInfo();
           
           // currently, return only one
           return (plInfo == null || plInfo.isEmpty()) ? null : plInfo;
       }
      
*/
    /*------------------------------------------------------------------------------------------------------------------*/
    // Traverse the ClausalAssertion object for each main clause in the message (all sentences)
    // and add all the assertion for each clause in an ArrayList
    /*------------------------------------------------------------------------------------------------------------------*/
    
    protected ArrayList<ClausalAssertion> getAllClausalAssertsInMsg (
        TreeMap <TextAnnotation, ArrayList<ClausalAssertion>> clausalAssertsMap)
    {
         ArrayList<ClausalAssertion> messageAssertsList = new ArrayList();
        Iterator <TextAnnotation> it = clausalAssertsMap.keySet().iterator();
        while(it.hasNext())
        {
            //TextAnnotation sentence = it.next();
            ArrayList<ClausalAssertion> assertsList = clausalAssertsMap.get(it.next());
            for (int i = 0; i < assertsList.size(); i++)        // ClausalAssertion Hierarchy for each main clause
            {
                ClausalAssertion topClausalAsserts  = assertsList.get(i);
                messageAssertsList.addAll(topClausalAsserts.getAllAssertions());
            }    
        }
        return messageAssertsList;
    }

/*********************************************************************************************/
    // Analyze the subjects in clausal assertion (clause)  and combine them to generate
    // a unique map of subjects, taking  anaphora and coreference into account
    // The key of the map is the best match anchor fot that subject
    protected TreeMap<TextAnchor, SubjectEntity> generateSubjectEntityMap(
           ArrayList<ClausalAssertion> msgAssertions)
    {
        // Rearrange the assertion for each unique "Subject"  in the message
        SubjectAnalyzer subjectAnalyzer = 
            new SubjectAnalyzer(msgAssertions, corefMap, (EmailHeaderInfo)null, personList);  //emailHeaderInfo
        subjectAnalyzer.buildSubjectEntityList();
        TreeMap<TextAnchor, SubjectEntity> subjectMap = subjectAnalyzer.getSubjectMap();
    
     
        //printSubjectMap
        Iterator <TextAnchor> it = subjectMap.keySet().iterator();
        while (it.hasNext())
        {
            TextAnchor key = it.next();
            System.out.println(subjectMap.get(key).toString());
              
        }
        nullSubject = subjectAnalyzer.getNullSubject();
        System.out.println( "-------------------------------------------------------------------------------------------------------\n");
        return subjectMap;
    }

   /***************************************************************************************/    
     // Returned the results of  pipeline processing so far
     //
    public ProcessedResults getProcessedResults()
    {
         processedResults.anaphoraMap = anaphoraMap;
         processedResults.corefMap = corefMap;
         processedResults.subjectMap = subjectMap;
         processedResults.nullSubject = nullSubject;
         
         // add the information trytieved from the Fragment Analyzer
         processedResults.fragmentSearchInfo = fragmentSearchInfo;
         
        return processedResults;
    }


}

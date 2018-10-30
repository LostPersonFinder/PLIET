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
package gov.nih.nlm.lpf.emails.nlpproc.structure;

import  gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexicon;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;


import gate.Document;
import gate.Annotation;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashMap;

import gov.lpf.resolve.GeoLocationInfo;

/**
 *
 * @author 
 */
public class ProcessedResults
{
    // constants
    public PLLexicon plLexicon;
    public ArrayList <HashMap<String, String>>   plEventList;  
    public HashMap<String, GeoLocationInfo> eventLocationList;
    boolean checkHeader;
    
    // Elements  Specific to each message being processed
    public Document messageDoc;
    // Results from Extractor
    public TypedAnnotationMap[]  annotatedSentenceMaps ;      // All annotations
    public TreeMap<TextAnnotation, ArrayList <TextAnchor>> anchorMap;
    
    public TreeMap<Integer, AnaphoraInfo> anaphoraInfo;
    public TreeMap<Annotation, Annotation> corefInfo ;

    public ArrayList <TextAnnotation> locationList;
    public ArrayList <TextAnnotation> personList;
    public ArrayList <TextAnnotation> attributeList;
    public ArrayList <TextAnnotation> fragmentSentences;                // sentences with no verbs: fragments
    public TreeMap<TextAnnotation, ClauseTree> lpfClauseMap;    // all clauses in each sentence
    
   // Results from Analyzer
    public TreeMap<TextAnchor, AnaphoraInfo> anaphoraMap;
    public TreeMap<TextAnchor, CorefInfo> corefMap;
    public TreeMap<TextAnchor, SubjectEntity> subjectMap;
    public SubjectEntity  nullSubject;                                             // Assertions without an explicit subject (null subject)
    public ArrayList<PLSearchInfo>  fragmentSearchInfo;            // Person information by searchig  fragment sentences
    
    // Results from inference generator
    public ArrayList<InferredPerson> inferredPersonList;            // set of entries for reported persons from sentences
    
    // Set of records without using Reg. Exp search throuhj the 
    public ArrayList<ReportedPersonRecord> inferredPersonRecords;   // same with results from fragments
    
    // final Records, to be sent to the Pipeline executor
    public ArrayList<ReportedPersonRecord> plPersonRecords;     // final, reconciled records
}

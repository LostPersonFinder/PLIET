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

/**
 *
 * @author 
 */

import gov.nih.nlm.lpf.emails.nlpproc.ner.VerbRules;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.ClausalConstants;
import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexiconReader;
import gov.nih.nlm.lpf.emails.nlpproc.ner.LPFVerbs;
import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexicon;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbUtils;
import gov.nih.nlm.lpf.emails.nlpproc.structure.PredicateModel;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;


public class VerbInferenceGen implements  LPFVerbs, ClausalConstants
{
    private static Logger log = Logger.getLogger(VerbInferenceGen.class);
    private static VerbInferenceGen VBInferEngine;
    private static PLLexicon lpfLexicon;
    
   /*********************************************************************/          
      public class ReportingType
      {
          String[] reportingVerb;
          String[] complements;
          String[] conditions;
          
          public ReportingType ( String[] repVerbs, String[] complem, String[] conditions)
          {
              this.reportingVerb = repVerbs;
              this.complements = complem;
              this.conditions = conditions;
          }
      }            
    /*********************************************************************/    

    
    protected static  VerbInferenceGen theLookup = null;            // singleton object;
    
    public static VerbInferenceGen getInferenceEngine()
    {
        if  (VBInferEngine == null)
            VBInferEngine = new VerbInferenceGen();

        return VBInferEngine;
    }
    /*********************************************************************/    
    protected  VerbInferenceGen()
    {
        lpfLexicon = PLLexiconReader.getLexicon();
        VerbInterpreter vba = VerbInterpreter.getInterpreter();
    }
 
    
/**************************************************************************************************
     * Determine the LPF Health status category for the input status verb of a reported person.
     *  Consider the type of the clause (genre) in which the status verb occurs:
     *  if it is REASON_CLAUSE or RESULT_CLAUSE etc, it is a statement.
     *  If it is a CONDITION_CLAUSE (if) or a TEMPORAL_CLAUSE, the status has to be
     *  determined accordingly.
     * If it has a negative modifier (such as not never, neither etc.) first determine the 
     * corresponding equivalent  positive word. Similarly, if it is in an interrogative clause,
     * determine the actual meaning conveyed.
     ************************************************************************************************/
    public String getHealthStatusCategory(PredicateModel verbModel, int genre, String negativeStr, 
        String[] adjectives,   boolean inerrogative)
    {
        boolean isNegative = VerbUtils.isNegativeStatus(negativeStr, adjectives);
        // tbd: interrogative
        
        // Check other adjective words (such as doing  fine, well etc. to determine the health status
        if (genre ==  CONDITION_CLAUSE)
            isNegative = (isNegative)? false : true;        // reverse

        String status;
        if (isNegative)
            status = verbModel.negativeCondition;
        else
             status = verbModel.healthStatusCondition;
        
        if (status == null || status.length() == 0)
            status = LPFVerbs.UNKNOWN;
        
        if (genre == TEMPORAL_CLAUSE)
            ;           // TBD: how to interprete other types od clauses
        
        return status;
    }

    
    /**************************************************************************************************
     * Determine the statement category  from  reporting  person's reporting verb.
     * If it has a negative modifier (such as not,  never, neither etc.) first determine the 
     * corresponding equivalent  positive word.
     ************************************************************************************************/
    protected String getReportingCategory(PredicateModel verbModel, String negativeStr, String[] adjectives)
    {
        String statement =  verbModel.verb.getGovernorToken().text;   ;
        boolean isNegative = VerbUtils.isNegativeStatus(negativeStr, adjectives);

        String status;
        if (isNegative)
            status =verbModel.negativeCondition;
        else
             status = verbModel.healthStatusCondition;
          
        if (status == null || status.length() == 0)
            status = LPFVerbs.UNKNOWN;
         return status;
    }

  /********************************************************************************************
   * String interrogation, if not null, indicates the verb in an interrogative clause, and the
   * specified word is the WH interrogative word.
   ********************************************************************************************/
   public   String analyzeVerbsForHealthStatus(PredicateModel verbModel, int genre, 
            String negativeStr, String[] adjectives, boolean interrogative)
   {
           String status = getHealthStatusCategory(verbModel, genre, 
               negativeStr, adjectives, interrogative);
           return status;
   }
    
   /********************************************************************************************/
     public String analyzeVerbsForReporter(PredicateModel verbModel, int genre, 
            String negativeStr, String[] adjectives)
   {
           String status = getReportingCategory(verbModel, negativeStr, adjectives);
           return status;
   }
             
   /********************************************************************************************/
   /*  Analyze the verb to see if it contains information about the personal attribute of a 
   * Person. If so, return the attribute Name and the value
      */
     public HashMap<String, TextAnchor> analyzeVerbsForAttributes(PredicateModel verbModel,
         ArrayList<TextAnnotation>attributeList)
   {
        if (attributeList == null || attributeList.isEmpty())
            return null;
        HashMap<String, TextAnchor> attrInfo = VerbInterpreter.getPersonAttribute(verbModel, attributeList);
        return attrInfo;
   }
     
  /********************************************************************************************/
     // Select the verb, in terns of importance, from a set of verbs for the same Subject (Person")
     // Note: The highest order category has the lowest index
     public PredicateModel selectVerb(ArrayList <PredicateModel> verbModels)
     {
         if (verbModels.isEmpty())      // no verbs found
             return null;                           
         PredicateModel selectedModel = verbModels.get(0);
         int vcat = VerbRules.getCategoryOrder(selectedModel.healthStatusCondition);
         for (int i = 1; i < verbModels.size(); i++)
         {
             PredicateModel nextVerb = verbModels.get(i);
             int cat = VerbRules.getCategoryOrder(nextVerb.healthStatusCondition);
             if (cat < vcat)
             {
                 vcat = cat;
                 selectedModel = nextVerb;
             }
         }
         if (verbModels.size() > 1)
             log.info ("---- Selected Verb: " + selectedModel.verb.getTextWithXcomp() + "----");
         
         return selectedModel;
     }
 
}

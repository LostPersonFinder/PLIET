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
package gov.nih.nlm.lpf.eval;

//import gov.nih.nlm.lpf.eval.AccuracyScore;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 *
 * @author 
 */
public class ScoreEvaluator 
{

    // not used
    private class FieldWeight {
        String name;
        float weight;
    }
    

    
    boolean showDetails;
    HashMap  <String, FieldEntries >referenceData;         // Key -> fieldName,  value -> set of messageIds and fieldValues
    HashMap  <String, FieldEntries>computedData;
    
    AccuracyScore[] fieldScores = null;
    
    
    /** Creates a new instance of Precision score Evaluator */
    // The <K,V> of each HashMap refers to a field (i.g. name) and its value 
    public ScoreEvaluator(HashMap <String, FieldEntries>references, HashMap <String,FieldEntries>computes) 
    {
        referenceData = references;
        computedData = computes;
    }
    
    
    public void showDetails(boolean ok)
    {
        showDetails = ok;
    }
    
    //*******************************************************************
    // Evaluate all Data in the input sets
    //*******************************************************************
    
    public LinkedHashMap<String, AccuracyScore> evaluateAll()
    {
        // use field names in the computed set as the reference set  may have 
        // extra (other than extracted) fields, e.g. processing date
        Set fieldSet  = referenceData.keySet();
        int nf = fieldSet.size();
        String[] fieldNames= new String[nf];
        fieldSet.toArray(fieldNames);
       
        // for each field, get the values in the reference and computed sets
       LinkedHashMap <String, AccuracyScore> accuracyScoreMap = new LinkedHashMap();
       boolean partialMatch = false;
        for (int i = 0; i < fieldNames.length; i++)
        {
            String fieldName = fieldNames[i];
            if (fieldName.equals("name") || fieldName.equals("location"))
                partialMatch = true;        // hard-cpded kludge
           AccuracyScore  fieldScore = evaluateField(fieldName, partialMatch); 
           accuracyScoreMap.put(fieldName, fieldScore);
        }
        return accuracyScoreMap;
    }
        
    //*******************************************************************
    // Evaluate a specific field in the input sets
    //*******************************************************************
    
    public AccuracyScore evaluateField(String field, boolean partialMatch)
    {        
        FieldEntries referenceValues = referenceData.get(field);
        FieldEntries computedValues = computedData.get(field);;
        AccuracyScore fieldScore = evaluate(field, referenceValues, computedValues, partialMatch) ;
        return fieldScore;
    }

    //*******************************************************************
    // Compute the score for a specific field in the input sets
    //******************************************************************* 
    
    public AccuracyScore evaluate(String fieldName, 
            FieldEntries referenceValues,  FieldEntries computedValues, boolean partialMatch)
    {
        AccuracyScore ascore = 
                new AccuracyScore(fieldName, referenceValues, computedValues, partialMatch);
                
        ascore.computeScore();
        return ascore;
    }
}

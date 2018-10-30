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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author 
 *
 * Use the following equations to compute Precision and Recall values
 *   Precision P = (# of elements common to both correct (reference) and retrieved sets) / 
 *                     (# of elements in the retrieved set ) 
 *   Recall R =    (# of elements common to both correct and retrieved sets ) / 
 *                     (# of elements in the correct set )
 *
 *   F1_score = 2*P*R / (P+R),  
 *
 *   Alternately:
 *
 *   F1_score = 2 * (# of elements common to both correct and retrieved sets )/
 *                  (sum of numbers in both correct and retrieved sets)
 *
 *   Validity = (# of elements common to both correct and retrieved sets )/
 *              (# of elements in the correct set + extra elements in the retrieved set)
 *      or,     (intersection / union of correct and retrieved sets)
 *
 *   Note: Validity is not a regular information retrieval concept, but added here
 *   specific to our AME 
 *
 * Reference:
 *    http://search.cpan.org/~kwilliams/Statistics-Contingency-0.06/Contingency.pm
 * 
 *>>> NOTE:  An element is asserted by its messageId and value combination
 */
public class AccuracyScore 
{
    String fieldName;             // number of elements in field value
    boolean partialMatch = false;
    HashSet truth = null;
    String[] missing = null;
    String[] extras = null;
    
    float F1_score = -1.0f; 
    float validity = 0.0f;
    float precision = 0.0f;
    float recall = 0.0f;
    
    
    // HashMap <K, V> => <messageId, fieldValue> all referring to the same field
    public AccuracyScore(String fieldName, HashMap referenceValues, 
            HashMap  retrievedValues, boolean acceptPartial)
            
    {
        this.fieldName = fieldName;  
        partialMatch = acceptPartial;
        init(referenceValues, retrievedValues);
    } 
    
    /************************************************************************
     * Initialize - by computing the intersection union etc of the two sets
     ************************************************************************/
    
    protected void init( HashMap referenceValues, HashMap retrievedValues) 
    {
       // truth = new HashSet(referenceValues.values());
        
        HashSet referenceSet = createUniqueValueSet(referenceValues);  
        HashSet retrievedSet = createUniqueValueSet(retrievedValues);
        truth = new HashSet(referenceSet);
        
        ArrayList missingList = new ArrayList();
        ArrayList extrasList = new ArrayList();
        
        // compare the two sets
        if (retrievedSet.equals(referenceSet))
        {
            ;
        }
        // Target set does not fully match with reference set
        else
        {
            // find elements missing in the retrieved set
            Iterator rit = referenceSet.iterator();
            while (rit.hasNext())
            {
                String val = (String)rit.next();
                if (!retrievedSet.contains(val)  && !hasPartialMatch(retrievedSet, val))
                {
                    // try to match substrings until we clean up better 
                    missingList.add(val);   
                }
            }
             
            // Find extra elements in the retrieved set
            Iterator it = retrievedSet.iterator();
            while (it.hasNext())
            {
                String val = (String)it.next();
                if (!referenceSet.contains(val))
                    extrasList.add(val);       
            }
            
            if (missingList.size() >0)
            {
                this.missing = new String[missingList.size()];
                missingList.toArray(this.missing);
            }
            if (extrasList.size() > 0)
            {
                this.extras = new String[extrasList.size()];
                extrasList.toArray(this.extras);
            }
        }
    }
    
    /*----------------------------------------------------------------------------------------------------
     // Create a unique vallue for each member of the set, by concatenating the 
     key and the value together
     -----------------------------------------------------------------------------------------------------*/
    protected  HashSet <String> createUniqueValueSet(HashMap <String, String> valueMap)
    {   
        HashSet  uniqueValueSet = new HashSet();
        if (valueMap  != null)
        {
            Iterator<String> it = valueMap.keySet().iterator();
            while (it.hasNext())
            {
                String prefix  = it.next();         // message Id
                String value = valueMap.get(prefix).trim().toLowerCase();
                String uniqueValue = prefix+"_"+ value;
                uniqueValueSet.add(uniqueValue);
            }
        }
        return uniqueValueSet;
    }
    /*-----------------------------------------------------------------------------------------------------*/
    // Check if val is a substring
    protected boolean hasPartialMatch(HashSet retrievedSet, String refVal)
    {
        // check omly for the name field
        if (!partialMatch)
            return false;
        String[] refs = refVal.split("_");
        String rv = refs[0];
        ArrayList <String> retValues = new ArrayList(retrievedSet);
        for (int i = 0; i < retValues.size(); i++)
        {
            String[] comp = retValues.get(i).split("_");      // remove the prefox
            String val = comp[1];
            if (rv.contains(val) || val.contains(rv))
                return true;
        }
        return false;
    };
        
        
                    // try to match substrings until we clean up better 
    
   /*************************************************************************
    * Compute the F1-measure and accuracy score  based upon the 
    * reference and retrieved sets  
    ***********************************************************************/      
 
    public void computeScore() 
    {
        if (F1_score >=  0.0)
            return;
        int nCorrect = (truth == null) ? 0 : truth.size();
        int nMissing = (missing == null) ? 0 : missing.length;
        int nExtra = (extras == null) ? 0 : extras.length;
        int nRetrv = nCorrect - nMissing + nExtra;
        
        // intersection of reference and target sets
        int nIntersect = nCorrect - nMissing;   
        int nUnion = nCorrect + nExtra;
        
        precision = (nRetrv == 0) ? 0.0f : (float)nIntersect/nRetrv;
        recall = (nRetrv == 0) ? 0.0f : (float)nIntersect/nCorrect;
        
        //score = (2 * precision * recall)/(precision + recall); // 2a/(2a+b+c). 
        F1_score = 2* (float)nIntersect/(nCorrect+nRetrv);
        validity = (float)nIntersect/nUnion;
    }
    
    public float getScore()
    {
        if (F1_score < 0.0)
            computeScore();
        return F1_score;
    }
    
    public float getValidity()
    {
        if (F1_score < 0.0)
            computeScore();
        return validity;
    }
    
    public float getPrecision()
    {
        if (F1_score < 0.0)
            computeScore();
        return precision;
    }
    
    public float getRecall()
    {
        if (F1_score < 0.0)
            computeScore();
        return recall;
    }
    
    public void printResult()
    {
        System.out.println("field name: " + fieldName);
        String missingValues = "  missing values: ";
        if (missing != null)
        {
            for (int i = 0; i < missing.length; i++)
                missingValues += missing[i]+", ";
            System.out.println(missingValues);
        }
        String extraValues = "  extra values: ";
        if (extras != null)
        {
            for (int i = 0; i < extras.length; i++)
                extraValues += extras[i]+", ";
            System.out.println(extraValues);
        }
        
        System.out.println( "Precision="
                + precision + ", Recall=" + recall + "  F1_score=" + F1_score );
        System.out.println("--------");
    }
}

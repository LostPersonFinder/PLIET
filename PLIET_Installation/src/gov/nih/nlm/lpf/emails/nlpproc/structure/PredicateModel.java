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

/**
 * @author 
 */

import  gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.Clause;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;

public class PredicateModel
{
     // Attributes of a LPF verb, some attributes may be null, or not filled (if type = unknown)
    // As determined from NLP precessing
    public VerbAnchor verb;
    public String[] modifiers;                     // an adjectival phrase or clause
    public String fullVerb;
    public int  verbTense;
    public Clause verbClause;             // clause where occurred, or none
    public boolean isPrimary;                           // type of the primary/dependent clause
    public boolean isNegative;
    public boolean isInterrogative;       // is in an interrogative sentence
    public boolean isActiveVoice;
    
    // as determined by LPF lookups
    public int lpfVerbCategory;                            // Health Status, Reporting or unknown
    public String  healthStatusCondition;          //  derived health condition
    public String  negativeCondition;
    
    public PredicateModel(VerbAnchor verb, Clause clause)
    {
        this.verb = verb;
        this.verbClause = clause;
      
        // set initial properties pulled from the verb anchor
        modifiers = verb.getAdjectives();
       isPrimary = (verb.getClausalVerbType() == 1);
       verbTense = verb.getVerbTense();   // past, present, future etc., to be set later
       isNegative = (verb.getNegativeWord() != null && verb.getNegativeWord().length() > 0);
       isActiveVoice = verb.isActive();

       isInterrogative = (verbClause == null) ? false : verbClause.isInterrogative;    
       isNegative = verb.isNegative();
       if (!isNegative)
           isNegative =  isNegativeStatus( verb.getAdjectives());
   

       lpfVerbCategory = -1;            // not set
       healthStatusCondition  = "";
    }
    /*----------------------------------------------------------------------------------*/
    // Determine the status from the list of adjectives
     public static  boolean   isNegativeStatus(String[]adjectives)
    {
        if (adjectives == null || adjectives.length == 0)
            return false;           // no negative adjectives
   
        boolean neg = false;
        for (int i = 0; i < adjectives.length && !neg; i++)
        {
            String adjective = adjectives[i].toLowerCase();
            if (adjective != null && adjective.matches(NLPRelations.NEGATIVE_ADJECTIVES))       // TBD: other terms
                neg = true;
        }
        return neg;
    }
    /*----------------------------------------------------------------------------------*/
    public int getLpfVerbCategory()
    {
        return lpfVerbCategory; 
    }
}
  /********************************************************************************************/   


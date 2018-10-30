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
package gov.nih.nlm.lpf.emails.rule;

import gov.nih.nlm.lpf.emails.nlpproc.ner.LPFVerbs;
import   gov.nih.nlm.lpf.emails.nlpproc.structure.*;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.*;
import gov.nih.nlm.lpf.emails.util.Utils;

import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 * @author 
 */
public class PersonRanker
{
    private static Logger log = Logger.getLogger(PersonRanker.class);  

    static  int nameWeight = 5;
    static int attrWeight = 5;
    static  int reporterWeight = 2;
    static  int assertionWeight = 5;           // since a fraction, max = 1
    static int statusWeight = 5;
    static int locationWeight = 5;
    
     static int maxNameWeight = 6;
     static int maxAttrWeight = 4;
     
     static int maxTotalWeight =  ( nameWeight*maxNameWeight)  + (attrWeight*maxAttrWeight) +
                                                           locationWeight+ reporterWeight + assertionWeight  + statusWeight;
    
    // Featre count for each category
    int nname = 0;
    int nrpt =  0;
    int nattr =  0;
    int nloc = 0;
    double fassert = 0.0;
    double  fsts = 0.0;
    
   
    
    InferredPerson person;
     
    public PersonRanker (InferredPerson aperson)
    {
        person = aperson;
    }
    
    // Rank a person according to whether  a set of features associated with it or not
    public  int rankPerson(LPFEntity lpfEntity, int  nMsgAssertions)
    {
        if (lpfEntity == null)
        {
            log.error("No matching entity found for thr person " + person.person.getCoveringText());
            return -1;
        }
        LPFEntity  personEntity  = lpfEntity;             // entity corresponding to this reported person
        nname = getNameFeatureCount(personEntity);
        nattr =  getAttributeFeatureCount(personEntity);
        nrpt =   getReporterFeatureCount(person);
        nloc = getLocationCount(person);
        fassert = getAssertionFeatureWeight(personEntity, nMsgAssertions);
        fsts =  getStatusFeatureWeight(personEntity);
        // buildLocationFeatures();
        System.out.println("--- Weights: nname: " + nname+", nattr: " + nattr + ", nrpt: " + nrpt 
            + ", nloc: " + nloc +", fassert: " + fassert + ", fsts: " + fsts);
            
        int rank =  rankUsingAllFeatures();
        
        return rank; 
    }
 /*-----------------------------------------------------------------------------------------------------------*/
    //  Assign weight for  the name feature according to the following characteristics
    // A Proper Noun, full Name 
    // A Proper Noun, partial Name
    // A pronoun
    // A  NonPersonNoun
    protected int getNameFeatureCount(LPFEntity personEntity)
    {
        String pname = person.getName();
        String[]  nameSegs = NameRule.getNameComponents(pname);     // three parts
        String word = nameSegs[0];
        if (word.length() == 0)
          return 0;

       if ( NameRule.isTextANonPerson(word))
           return 1;
        if (NameRule.isTextAPronoun(word)  ||  NameRule.isTextARelative(word)) 
            return 2;
        
        // check if it is a proper name, starting with 1 as mininum
        int nr = 2;
        if (nameSegs[0].matches("^[A-Z].*"))  nr++;
        
        if (nameSegs[2].length() > 0)       // last name exists
        {
            nr++;
             if (nameSegs[2].matches("^[A-Z].*"))  nr++;
        }
        return nr;
    }

    //-------------------------------------------------------------------------------------------------//
    // Check if there is an explicit reporter (including self) for this Person.
    // TBD: Should it be based upon health status?
      protected int buildReporterFeatures(LPFEntity personEntity)
      {
          int nr = 0;
         ArrayList<SubjectEntity>  reporters = personEntity.reporters;
         if (reporters != null && !reporters.isEmpty())
             nr = 1;
         return nr;
      }
//-------------------------------------------------------------------------------------------------//
// Check if there is an explicit reporter (including self) for this Person.
// TBD: Should it be based upon health status?
      protected int getAttributeFeatureCount(LPFEntity personEntity)
      {
          int na = 0;
          PersonAttributes  attributes = personEntity.pattr;
          if (attributes == null)
              return 0;
          // Increament the counter for each explicitly specofied attribute
          if (attributes.age != null)
              na++;
          if (attributes.name != null)
              na++;
          if (attributes.gender != null)
              na++;
          if (attributes.location != null)
              na++;
          return na;
      }
//-----------------------------------------------------------------------------------------------------
    // Assign weight depending upon if this person has a reporter
    protected int getReporterFeatureCount(InferredPerson person)
    {
        int reporterRanking = 0;
        if (person.reporter != null)            // someone reported about this person
            reporterRanking = 1;
        return reporterRanking;
    }
    //-----------------------------------------------------------------------------------------------------
    // get the ratio of assertions for this object vs. total number of assertions in the message
    protected double getAssertionFeatureWeight(LPFEntity entity,  int  nMsgAssertions)
    {
        int numAssertions = entity.getNumAssertions();
        int totalAssertions = nMsgAssertions;
        double assertWeight = (double)numAssertions/(double)totalAssertions;
        return assertWeight;
    }
    
    //-----------------------------------------------------------------------------------------------------
    // Determine how many assertions out of the set are related to health status
    protected double  getStatusFeatureWeight(LPFEntity entity)
    {
        int nhverbs = 0;         // number of health status related verbs
        ArrayList< PredicateModel> predicates = entity.getAllPredicates();  
        int nverbs = predicates.size();
        for (int i = 0; i < nverbs; i++)
        {
             PredicateModel predicate =predicates.get(i);
             // set the person category (reporter.reported/unknown)
            int verbType = predicate.getLpfVerbCategory();
            // if used as a subject only of a reporting verb
            if (verbType == LPFVerbs.REPORTING_VERB  || verbType == LPFVerbs.HEALTH_STATUS_VERB)
                nhverbs++;
        }
        double statusWeight = (double)nhverbs/(double)nverbs;
        return statusWeight;
    }
    
    // Return 1 or zero if a location is included (could be the same as in the attribute list)
    protected int getLocationCount(InferredPerson person)
    {
        if (person.locations == null || person.locations.size() == 0)
            return 0;
        return 1;
    }
    
    //-------------------------------------------------------------------------------------------------------//
    // Rank the given reported person using normalized  weighted value of all features
    // Noet: Diferent types of features have different normalization weight
    protected int rankUsingAllFeatures()
    {
        double ranking =  nname*nameWeight + nattr*attrWeight 
                                + nrpt*reporterWeight + nloc*locationWeight +
                                 + fassert*assertionWeight  + fsts*statusWeight;
       // normalize to 100
        int normalizedRanking = (int) (ranking*100/maxTotalWeight);
        return normalizedRanking;   
    }
}

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

import  gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;
import  gov.nih.nlm.lpf.emails.nlpproc.ner.LPFVerbs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author 
 */

public class SubjectEntity
{
    public TextAnchor subject;                                                              // Person, or informaion (for a Person)
    public ArrayList<ClausalAssertion> assertions;       // all assertions related to the "Subject"
    public ArrayList <PredicateModel> predicates;          // "Simple predicates" ( Verbs)  from all assertions
    
    // Instantiate a new Object. Note the subject may be a null Anchor
    public SubjectEntity(TextAnchor aSubject)
     {
         this.subject = aSubject;
        assertions  = new ArrayList();
        predicates = new ArrayList();
     }
    
     public SubjectEntity(TextAnchor aSubject, ClausalAssertion assertion)
     {
         this(aSubject);
         assertions.add(assertion);
     }
    
    
    public  void addAssertion(ClausalAssertion assertion)
    {
        assertions.add(assertion);
    } 
    
        
    public  void addAssertions(ArrayList <ClausalAssertion> newAssertions)
    {
        assertions.addAll(newAssertions);
    } 
    
      public  void addPredicateModels(ArrayList <PredicateModel>  predicateModels)
    {
        predicates.addAll(predicateModels);
    } 
    
    public  void addPredicateModel(PredicateModel  predicate)
    {
        predicates.add(predicate);
    } 

     public ArrayList<ClausalAssertion> getAssertions()
     {
         return assertions;
     }
     
     public ArrayList<PredicateModel> getPredicateModels()
     {
         return predicates;
     }
     
      public TextAnchor  getSubject()
     {
         return subject;
     }
      
       /******************************************************************************************/
      // Get the assertion correspomding to the givenPredicate
      //-------------------------------------------------------------------------------------------------------------/
      public  ClausalAssertion getAssertionForPredicate(PredicateModel predicate)
      {
          return  getAssertionForVerb(predicate.verb);
    }
     /******************************************************************************************/
      // Get the assertion correspomding to the given Verb
      //-------------------------------------------------------------------------------------------------------------/
      public  ClausalAssertion getAssertionForVerb(VerbAnchor verb)
      {
          for (int j = 0; j < assertions.size(); j++)
          {
             ClausalAssertion assertion = assertions.get(j);
             if (assertion.verb == verb)
                 return assertion;
          }
          return null;
    }
     
      // Check if any of the Predicates indicate a reporting Verb 
      // If so, it is a Reporter
      public boolean isReporterSubject()
      {
           for (int i = 0; i <predicates.size(); i++)
          {
              PredicateModel predicate =  predicates.get(i);
              if (predicate.lpfVerbCategory == LPFVerbs.REPORTING_VERB)
                  return true;
          }
           return false;
      }
      
     // Check if any of the Predicates indicate a reported person Verb 
      // If so, it is a Reported person
      public boolean isReportedSubject()
      {
           for (int i = 0; i <predicates.size(); i++)
          {
              PredicateModel predicate =  predicates.get(i);
              if (predicate.lpfVerbCategory == LPFVerbs.HEALTH_STATUS_VERB)
                  return true;
          }
           return false;
      }
    
    
      /******************************************************************************************/
      // Get the instances  of type Person which are referred to as Objects for this Subject
      //-------------------------------------------------------------------------------------------------------------/
      public ArrayList<TextAnchor> getPersonObjects()
      {
          ArrayList<TextAnchor> personObjects = new ArrayList();
          for (int i = 0; i <predicates.size(); i++)
          {
              PredicateModel predicate =  predicates.get(i);
              ClausalAssertion assertion = getAssertionForPredicate(predicate);
               if ( assertion.persons != null)
                    personObjects.addAll(assertions.get(i).persons);
          }
          return (personObjects.size() == 0 ) ? null :  personObjects;  
    }
      
    /******************************************************************************************/   
    // Get all Locations contained in all assertions for this Subject
    //
    public ArrayList<TextAnchor> getLocationObjects()
    {
        ArrayList<TextAnchor> locations = new ArrayList();
        // check for location in other assertions
        for (int i = 0; i < assertions.size(); i++)
        {
            ArrayList <TextAnchor> newLocations = assertions.get(i).locations;
           if (newLocations != null)
           {
               for (int j = 0; j < newLocations.size(); j++)
               {
                    if (! locations.contains(newLocations.get(j)))
                        locations.add(newLocations.get(j));
               }
           }
        }
        return locations;
    }

    /******************************************************************************************/    
     public String toString()
     {
         String str = "-------------------------------------------------------------------------------------------------------\n Subject: " ;
         str += (subject == null) ? "NULL" : subject.getTextWithConjunct();
         for (int i = 0; i < assertions .size(); i++)
         {
             str += "\n"+assertions.get(i).toString();
         }
         return str;
     }

     /******************************************************************************************/
       public String getObjectString(HashMap<String, ArrayList<TextAnchor>> objects)
       {
           String str = "";
           Iterator <String> it = objects.keySet().iterator();
           while (it.hasNext())
           {
               String type = it.next();
               ArrayList <TextAnchor> list = objects.get(type);
              str +=  ( " type: "+ type +", " + list.toString()+"\n");
           }     
           return str;
       }
             
}

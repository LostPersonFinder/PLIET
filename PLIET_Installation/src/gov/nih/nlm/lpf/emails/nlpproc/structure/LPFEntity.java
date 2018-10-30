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
import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import java.util.ArrayList;

/**
 *
 * @author 
 */
public class LPFEntity extends SubjectEntity  implements NERConstants
{
  
    /*--------------------------------------------------------------------------------------*/
    
    public int entityClass;                   // information class (1 through 4 above)
    public int  entityType;                     // a clausal subject or an object
    public int  personCategory;         // reported, reporter etc. 
    public PersonAttributes pattr;
    public ArrayList<SubjectEntity> reporters = null;              // If an object, reporter = subject of the clause, else null
    
    // List of assertions and Predicates containing this Entity  as an object
    public ArrayList<ClausalAssertion>objectAssertions;
    public ArrayList<PredicateModel>objectPredicates;
    
    public LPFEntity(TextAnchor subject, int type)
    {
        super(subject);
        entityType = type;
        entityClass = UNKNOWN_TYPE_ENTITY;                // unknown at this time
        objectAssertions = new ArrayList();
        objectPredicates = new ArrayList();
        pattr = null;
    }
    
    //--------------------------------------------------------------------------------
    // Create an LPFEntiry  instance from a SubjectEntity
    //---------------------------------------------------------------------------------
    public LPFEntity(SubjectEntity lpfSubject)
    {
        this(lpfSubject.getSubject(), CLAUSAL_SUBJECT);
        assertions = lpfSubject.getAssertions();
        predicates = lpfSubject.getPredicateModels();  
        reporters = null;
        pattr = null;
    }
    
    // If a reporter specified, the person is objects for that reporter
    // Note: we only consider Person type objects 
    public void setReporter(SubjectEntity rept)
    {
       if (reporters == null)
           reporters = new ArrayList();
       reporters.add(rept);
       entityType = entityType | CLAUSAL_OBJECT;
    }
     public  void addObjectAssertion(ClausalAssertion newAssertion)
    {
        objectAssertions.add(newAssertion);
    } 
    
      public  void addObjectPredicateModel(PredicateModel predicateModel)
    {
        objectPredicates.add(predicateModel);
    } 
      
      /******************************************************************************************/
      // Get the assertion correspomding to the givenPredicate
      //-------------------------------------------------------------------------------------------------------------/
      public  ClausalAssertion getAssertionForPredicate(PredicateModel predicate)
      {
          return  getAssertionForVerb(predicate.verb);
    }
     /******************************************************************************************/
      // Get the assertion corresponding to the given Verb
      //-------------------------------------------------------------------------------------------------------------/
      public  ClausalAssertion getAssertionForVerb(VerbAnchor verb)
      {
          // first check for subject assertions
          ClausalAssertion assertion = super.getAssertionForVerb(verb);
          if (assertion != null)
              return assertion;

          // Check for object assertions
          for (int j = 0; j < objectAssertions.size(); j++)
          {
             assertion = objectAssertions.get(j);
             if (assertion.verb == verb)
                 return assertion;
          }
          return null;
    }
   //-------------------------------------------------------------------------------------------------------------/
      // Merge information about another entity with self, the other entiry is not affected
      public void mergeEntity(LPFEntity otherEntity)
      {
            this.mergeAssertions(otherEntity);
            this.mergeAttributes(otherEntity);
            this.entityType = (otherEntity.entityType |  otherEntity.entityType);
            this.entityClass =  otherEntity.entityClass;      // Person, nonPerson etc.
      }
      
      //-------------------------------------------------------------------------------------------------------------/
      // merge the information contained in another instance of the same Entity with self
      public void mergeAssertions(LPFEntity other)
      {
          if (other.assertions!= null)
          {
            assertions.addAll(other.getAssertions());
            predicates.addAll(other.getPredicateModels());  
          }
           if (other.objectAssertions!= null)
          {
               objectAssertions.addAll(other.objectAssertions);
               objectPredicates.addAll(other.objectPredicates);
          }
           if (other.reporters != null)
           {    
               if (reporters == null )
                    reporters = other.reporters;
               else
                   reporters.addAll(other.reporters);
           }
          return;
      }
      
      public int getNumAssertions()
      {
          return assertions.size()+objectAssertions.size();
      }
 
      public ArrayList<PredicateModel>getAllPredicates()
     {
         ArrayList<PredicateModel>allPredicates = new ArrayList();
         allPredicates.addAll(predicates);
         allPredicates.addAll(objectPredicates);
          return allPredicates;
     }
      
       public ArrayList<ClausalAssertion>getAllAssertions()
     {
         ArrayList<ClausalAssertion>allAssertions = new ArrayList();
         allAssertions.addAll(assertions);
         allAssertions.addAll(objectAssertions);
          return allAssertions;
     }
    //----------------------------------------------------------------------------------------//
      // get the locations directly linked to this person in a clause
      public   ArrayList <TextAnchor> getDirectLocations()
      {
          TextAnchor person = subject;
          TextAnchor[] locations = person.getLocationDependentAnchors();
          if (locations ==  null)
              return null;
          ArrayList <TextAnchor> locationList = new ArrayList();
          for (int i = 0; i < locations.length; i++)
              locationList.add(locations[i]);
          return locationList;  
      }
      
      
      //----------------------------------------------------------------------------------------//
      // get the locations for which this entity  is the subject
      public   ArrayList <TextAnchor> getSubjectLocations()
      {
          if (assertions == null || assertions.size() == 0)
              return null;
          
          ArrayList <TextAnchor>locationList = new ArrayList();
          for (int i = 0; i < assertions.size(); i++)
          { 
              if(assertions.get(i).locations != null)
              locationList.addAll(assertions.get(i).locations);
          }
          return locationList;  
      }
  
       
      //----------------------------------------------------------------------------------------//
      // get the locations for which this entity  is the Object
      public   ArrayList <TextAnchor> getObjectLocations()
      {
          if (objectAssertions == null || objectAssertions.size() == 0)
              return null;
          
          ArrayList <TextAnchor>locationList = new ArrayList();
          for (int i = 0; i < objectAssertions.size(); i++)
          { 
              if(objectAssertions.get(i).locations != null)
              locationList.addAll(objectAssertions.get(i).locations);
          }
          return locationList;  
      }
      
 //-------------------------------------------------------------------------------------------------------------/
  // merge the attributes contained in another instance of  Entity with self
  //-------------------------------------------------------------------------------------------------------------/
      public void mergeAttributes(LPFEntity other)
      {
          if (other.pattr == null)
              return;
          
          else if (this.pattr == null)
          {
              pattr =  other.pattr;
          }
          else if (this.pattr != null )
          {
              pattr.mergeAttributes(other.pattr);
          }
          return;
      }
      //-------------------------------------------------------------------------------------------------------------/
}

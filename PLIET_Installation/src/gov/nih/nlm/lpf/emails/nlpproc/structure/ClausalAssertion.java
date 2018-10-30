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

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.Clause;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.ClausalConstants;
import gov.nih.nlm.lpf.emails.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class ClausalAssertion implements ClausalConstants
{
    private static Logger log = Logger.getLogger(ClausalAssertion.class);
    
     // determine the clause's use depending upon the text connecting it to the parent
    static String[][] ClauseIntroText  = {TemporalIntro, LocationIntro, PurposeInfo, ReasonIntro , 
                                         ConditonIntro, ResultIntro, ResultPrecede, ComparisionIntro , 
                                         SuppositionIntro};
    static int[] ClauseGenre = {TEMPORAL_CLAUSE, LOCATION_CLAUSE,  PURPOSE_CLAUSE, REASON_CLAUSE ,
                                       CONDITION_CLAUSE, RESULT_CLAUSE, RESULT_PRECEDE_CLAUSE,
                                       COMPARISION_CLAUSE, SUPPOSITION_CLAUSE };

 /***********************************************************************************************************/   
        
    public Clause clause;           // clause from which the data is derived
    public int clauseGenre;
    
    public TextAnchor subject;
    public TextAnchor verb;                     // main verb related to a subject
    public int verbType;                            // verb classification, PRIMARY, CCOMP, ADVCL  etc
    public Clause objectClause;           // a clause, if any,  that is used as the object of this clause, or null
    public Clause parentClause;         // parent clause, if any, of which this is a subordinate, or null
    
    // filler later
    public ArrayList <TextAnchor> locations;   // associated locations
    public ArrayList <TextAnchor> persons;     // associated persons as direct objects
    public HashMap<String, TextAnchor> attributes;        // as in He is 10 years old: AGE attribute = 10 years
    public ArrayList <TextAnchor> otherObjects;     // other  direct objects
    public ArrayList<ClausalAssertion> subClauseAssertions;           // info for all lower level subclauses

    
    public ClausalAssertion(Clause clause, TextAnchor defaultSubject)
    {
        this.clause = clause;
        clauseGenre = setClauseGenre();
        subject = clause.subject;
        if (subject == null)
            subject = defaultSubject;
        
        verb = clause.clauseHead;
        verbType =  verb.getVerbType();
        objectClause = clause.getObjectClause();
        parentClause = clause.parentClause;
        subClauseAssertions  = null;         // to be set later
       
    }
    /*-------------------------------------------------------------------------------------------------------*/
   // Set the genre of the clause to indicate its purpose
    // Note The clause is used an equal (for xcomp relations) if there is no Intro text
    // or it is "to" as in "I want to find"
    protected  int  setClauseGenre()
    {
        int genre = -1;
        String introText = clause.introText.toLowerCase();
        if (introText.length() == 0  || introText.equals("to"))
                return -1;         // may be an xcomp, or with  with no marker 
        
        for (int i = 0; i < ClauseIntroText.length; i++)
        {
            if (Utils.isInList(introText, ClauseIntroText[i]))
            {
                genre = ClauseGenre[i];
                break;
            }
        }
        if (genre == -1)
            log.error ("No genre found for clause intro text " + introText);
        else
            System.out.println("Clause enre for introductory text: " + introText + " is "  + genre);
        return genre;
    }
    
    public void setLocations(ArrayList<TextAnchor> loc)
    {
       locations = loc;
    }
     public void setPersons(ArrayList<TextAnchor>persn)
    {
       persons = persn;
    }
    
    public void setAttributes(HashMap<String, TextAnchor> attrs)
    {
        attributes = attrs;
    }
/*-------------------------------------------------------------------------------------------------------*/
    public boolean isMainClause()
    {
        return (clause.clauseLevel == 1);
    }
    
    public int getClauseLevel()
    {
        return clause.clauseLevel;
    }
     public int getClauseGenre()
     {
         return clauseGenre;
     }
     
     // is it a conditional Assertion
     public boolean isConditional()
     {
         return (clauseGenre == CONDITION_CLAUSE);
     };  
  
     /*---------------------------------------------------------------------------------------------------*/
     // Get all assertions for a given main clause, starting with the top
     // and descending down
     /*---------------------------------------------------------------------------------------------------*/
     public ArrayList <ClausalAssertion> getAllAssertions()
     {
         ArrayList <ClausalAssertion> casserts = new ArrayList();
         casserts.add(this);

         if (subClauseAssertions == null || subClauseAssertions.isEmpty())        // a leaf clause
             return casserts;
         
         for (int i = 0; i < subClauseAssertions.size(); i++)
         {
             ArrayList <ClausalAssertion> mysubAssertions =  subClauseAssertions.get(i).getAllAssertions();
             casserts.addAll(mysubAssertions);
         }
         return casserts;
     }
     
     public ClausalAssertion  updateSubject(TextAnchor newSubject)
     {
         ClausalAssertion newAssertion = this.clone();
         newAssertion.subject = newSubject;
         return newAssertion;
     }
  
     /**********************************************************************************************/
     public ClausalAssertion  updatePerson(TextAnchor person, TextAnchor newPerson)
     {
         ClausalAssertion newAssertion = this.clone();
         for (int i = 0; i < persons.size(); i++)
         {
             if (person.equals(persons.get(i)))
             {
                 persons.add(i, newPerson);
             }
         }     
         return newAssertion;
     }
 /**********************************************************************************************/       
     public ClausalAssertion  updateLocation(TextAnchor loc, TextAnchor newLoc)
     {
         ClausalAssertion newAssertion = this.clone();
         for (int i = 0; i < locations.size(); i++)
         {
             if (loc.equals(locations.get(i)))
             {
                locations.add(i, newLoc);
             }
         }     
         return newAssertion;
     }
 /**********************************************************************************************/  
     protected ClausalAssertion clone()
     {
         ClausalAssertion newAssertion = new ClausalAssertion(this.clause, null);
          newAssertion.locations = locations;   // associated locations
          newAssertion.persons = persons;     // associated persons as direct objects
          newAssertion.attributes = attributes;
          newAssertion. subClauseAssertions = subClauseAssertions;          
          return newAssertion;
     }
/**********************************************************************************************/ 
    // Return the string representation of self 
     public String toString()
     {
         String adjectStr = "";
         String[] adjectives = verb.getAdjectives();
         if (adjectives != null && adjectives.length > 0)
         {
             for (int i = 0; i < adjectives.length; i++)
                 adjectStr += adjectives[i] +", ";
         }
         String str = "Clause Genre: " + clauseGenre + ", Verb type: " + verbType;
          str+=    ", ParentClause: " + (parentClause == null ? "NULL" : clause.clausalGovernor.getText())
                     +", No. of subAssertions: " + (subClauseAssertions == null ? "0" : subClauseAssertions.size());
         str += "\nSubject: " + (subject == null ? "NULL" : subject.getTextWithConjunct());
         str += "\nVerb: " + this.verb.getText() + ", IsNegative? " + (verb.isNegative() ? "yes" : "no");
         str += ", Modifier: " + adjectStr ;
        str += ", HasLocations: " + (locations == null ? "None" :  locations.toString());
        str += ", HasPersons: " + (persons == null ? "None" :  persons.toString());
        str += ", HasAttrubute: " + (attributes == null ? "None" :  attributes.values());
        return str;
     }
}

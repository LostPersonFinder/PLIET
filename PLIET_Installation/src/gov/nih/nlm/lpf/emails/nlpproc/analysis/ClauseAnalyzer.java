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

import gov.nih.nlm.lpf.emails.nlpproc.structure.ClauseTree;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.Clause;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ClausalAssertion;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;



/**
 *
 * @author 
 */
public class ClauseAnalyzer
{
    public static int PERSON_TYPE = 1;
    public static int LOCATION_TYPE = 2;
    
 

    /******************************************************************************/
    // information about each clause tree
    ClauseTree clauseTree;
    ArrayList <ClausalAssertion> level1ClauseInfo ;             // level 1 clauses in order
        
    // List of Person and Location Annotations already found in the  message (by GATE)
    ArrayList<TextAnnotation> AnnotatedPersonList;    
    ArrayList <TextAnnotation> AnnotatedLocationList;
    
    LocationAnalyzer locationAnalyzer;
    PersonAnalyzer personAnalyzer;
    
    /**********************************************************************************/
    public ClauseAnalyzer (  ClauseTree clauseTree, 
        ArrayList<TextAnnotation> persons, ArrayList<TextAnnotation> locations)
    {
        this.clauseTree = clauseTree;
        AnnotatedPersonList  = persons;
        AnnotatedLocationList  = locations;
        personAnalyzer = new PersonAnalyzer( AnnotatedPersonList);
        locationAnalyzer = new LocationAnalyzer( AnnotatedLocationList, AnnotatedPersonList);
    }

  /**********************************************************************************/
    public ArrayList <ClausalAssertion> analyzeClauses()
    {
        level1ClauseInfo = new ArrayList();
        TextAnchor defaultSubject = null;
        for (int i = 0; i < clauseTree.mainClauses.size(); i++)
        {
            ClausalAssertion info = analyzeClause( clauseTree.mainClauses.get(i), defaultSubject);
            defaultSubject = info.subject;
            level1ClauseInfo.add(info);
        }
        return level1ClauseInfo;
    }
    
    public void printClauses()
    {
        for (int i = 0; i < level1ClauseInfo.size(); i++)
        {
            System.out.println(level1ClauseInfo.get(i).toString());
        }
    }
            
  /********************************************************************************************/  
    // Use a default subject for primary clauses as that of the preceding primary clause
    protected ClausalAssertion analyzeClause(Clause clause, TextAnchor defaultSubject)
    {
        ArrayList <TextAnchor> locations = locationAnalyzer.getLocationObjects(clause);  
        ArrayList <TextAnchor> persons = personAnalyzer.getPersonObjects(clause);
        HashMap<String, TextAnchor> attributes = AttributeAnalyzer.getAttributeObjects((VerbAnchor)clause.clauseHead);

        ClausalAssertion clsAsserts  = new ClausalAssertion(clause, defaultSubject);
        clsAsserts.setLocations(locations);     // locations as object of this clause
        clsAsserts.setPersons(persons);                //Persons  as object of this clause
        clsAsserts.setAttributes(attributes);
        
       ArrayList <ClausalAssertion> subClauseAssertions = getSubClauseAssertions(clause.getSubordinates());
       
       // Merge the xcomp (also pcomp??) assertions from subclause with the parent assertion
        clsAsserts.subClauseAssertions =  mergeXcompAssertions(clsAsserts,  subClauseAssertions);
         return   clsAsserts; 
    } 
    /************************************************************************************************/
    // Add the subclause info for the subclauses hierarchically into the Array
      /************************************************************************************************/
    protected ArrayList<ClausalAssertion>getSubClauseAssertions( ArrayList <Clause> subClauses)
    {
        if (subClauses == null || subClauses.isEmpty())
            return null;
       
       ArrayList <ClausalAssertion> subClauseAsserts = new ArrayList();
       TextAnchor defaultAnchor = null;
        for (int i = 0; i < subClauses.size(); i++)
        {
             ClausalAssertion lowerAssertion = analyzeClause(subClauses.get(i), defaultAnchor);
            subClauseAsserts.add(analyzeClause(subClauses.get(i), defaultAnchor));
        }     
        return subClauseAsserts;
    }
    //----------------------------------------------------------------------------------------------------------------------
    // Get the subject of this assertion. If it is null, go to its governor clause etc. to determine 
    // the actual subject
    //----------------------------------------------------------------------------------------------------------------------
    protected TextAnchor  getClauseSubject(Clause clause)
    {
        if (clause.subject !=  null)
            return clause.subject;
        
        if (clause.clauseType.equals("xcomp")  || clause.clauseType.equals("pcomp"))
            return clause.parentClause.subject;
        return null;
    }
    
   // Merge the assrtion of a parent clause with that of Xcomp subclause, since they
    // refer to a single subject/object (e.g. I want to read a book  => want and read are complements))
  protected ArrayList <ClausalAssertion> mergeXcompAssertions ( 
       ClausalAssertion parentAssertion, ArrayList <ClausalAssertion> subAssertions)
  {
        if (subAssertions == null || subAssertions.isEmpty())
            return subAssertions;

        Iterator<ClausalAssertion> it =  subAssertions.iterator();
        while (it.hasNext())
        {
            ClausalAssertion sub =it.next();
            if (!sub.clause.clauseType.matches("xcomp|pcomp"))
                continue;
            // since a match, get objects from sub and add to parent
            if (sub.subClauseAssertions != null)
            {
                if ( parentAssertion.subClauseAssertions == null)
                     parentAssertion.subClauseAssertions = new ArrayList();
                 parentAssertion.subClauseAssertions.addAll(sub.subClauseAssertions);
            }
            parentAssertion.persons = addToList(parentAssertion.persons, sub.persons);
            parentAssertion.locations = addToList( parentAssertion.locations, sub.locations);
            parentAssertion.otherObjects = addToList( parentAssertion.otherObjects, sub.otherObjects);
            it.remove();
        }
    return subAssertions;
  }
 
    // Add elements fron the secondary list to the primary list, changing the primary
    protected  ArrayList<TextAnchor>  addToList(ArrayList <TextAnchor>primary,  ArrayList <TextAnchor>secondary)
    {
        if (secondary == null || secondary.isEmpty())
            return primary;

        if (primary == null)
            primary = new ArrayList();
        
        primary.addAll(secondary);
        return primary;
    }
}
  
  
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

/**
 *
 * @author 
 */

//import gov.nih.nlm.ceb.lpf.emails.nlp.VerbAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve.NameResolver;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NounAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.AnchorLink;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.Clause;
import gov.nih.nlm.lpf.emails.nlpproc.structure.LinkedAnchorMap;

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;

import gov.nih.nlm.lpf.emails.util.Utils;

//import gov.nih.nlm.ceb.lpf.emails.util.Utils;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class PersonAnalyzer 
{
    private static Logger log = Logger.getLogger(PersonAnalyzer.class);
    
    protected static String PersonObjectRelations="dobj|iobj|copobj";
    
    public static int FIRST_PERSON = 1;
    public static int SECOND_PERSON = 2;
    public static int THIRD_PERSON = 3;
    
    ArrayList<TextAnnotation> PersonList;     // input list of known persons from external source
    HashMap<TextAnnotation, TextAnnotation> PersonFullMap;           // List of  Persons with full names

    
       public PersonAnalyzer (ArrayList<TextAnnotation>persons)
    {
        PersonList = persons;
        PersonFullMap = new HashMap();
        
        for (int i = 0; i < PersonList.size(); i++)
        {
            TextAnnotation person = PersonList.get(i);
             int curId = person.id;
            String rule1 = (String) person.getFeature("rule1");     // PersonFull or other things
            ArrayList <Integer> matchingIds = (ArrayList <Integer>)person.getFeature("matches");
            
            if (rule1 == null || matchingIds == null)       // no further information
            {
                PersonFullMap.put(person, person);
                continue;
            }
            else if (matchingIds.size() == 1)
            {
                TextAnnotation person1 = TextAnnotation.getAnnotationById(persons, matchingIds.get(0));
                PersonFullMap.put(person, person);
                continue;
            }
            
            // Matches with more than one Persons
            for (int j = 0; j < matchingIds.size(); j++)
            {
                int matchingId = matchingIds.get(j);
                TextAnnotation person1 = TextAnnotation.getAnnotationById(persons, matchingId);
                String jrule1 = (String) person1.getFeature("rule1");     // PersonFull or other things
                if (jrule1 != null && jrule1.equals("PersonFull"))      // no other better match
                {
                    PersonFullMap.put(person, person1);
                    break;
                }
            }
        }
    }

  /*---------------------------------------------------------------------------------------------------------*/  
    // Get the persons from the objects  in a Clause  through phrasal Links
    // by descending the phrasal hierarchy, but not the lower level clauses
    // We match direct objects persons against the given known list of persons. 
   // For prepositional Links, we retrieve them if it is a person_type preposition, even if
   // that is not in the list.
  /*---------------------------------------------------------------------------------------------------------*/  
    public  ArrayList <TextAnchor>  getPersonObjects(Clause clause)
    {
        ArrayList <TextAnchor> personObjects = new ArrayList();     
 
        // Relationship to other phrases  in the sentence
        personObjects.addAll( getPersonObjects(clause.verbObjectMap));
        personObjects.addAll( getPersonObjects(clause.subjObjectMap));

       if (personObjects.isEmpty())
           return null;
      
       else if (personObjects.size() > 1)
           log.warn("More than one person found " +":  " + personObjects); 
        return personObjects;
    }
    /*---------------------------------------------------------------------------------------------------------*/    
    protected   ArrayList <TextAnchor>  getPersonObjects(LinkedAnchorMap objectMap)
    {   
        ArrayList <TextAnchor> personObjects = new ArrayList();     
        if (objectMap == null)
            return personObjects;         // empty list
         
        Iterator <String> it = objectMap.keySet().iterator();
        while (it.hasNext())
        {
            String link = it.next();
            TextAnnotation matchingPerson = null;
            if (link.matches(PersonObjectRelations))        // note "copobj" is a synthetic link
            {
                TextAnchor anchor = objectMap.getAnchor(link);
                if (anchor == null)
                {
                    log.warn ("No anchor found for link:  " + link);
                    continue;
                }
                
                 TextAnnotation gov = anchor.getGovernorToken();
                 matchingPerson = getOverlappingPersonAnnotation(PersonList, anchor);
                
                 if (isValidPerson (matchingPerson))
                     personObjects.add(anchor);
                     
                else if (anchor.getType() == TextAnchor.NOUN_ANCHOR)
                {
                   NounAnchor nounAnchor = (NounAnchor)anchor;
                   
                    // check if the noun word as an "apposite modifier" referring to a person
                    TextAnchor apposAnchor = nounAnchor.getApposAnchor();
                    if (apposAnchor != null)
                    {
                        matchingPerson = getOverlappingPersonAnnotation(PersonList, apposAnchor);
                        if (isValidPerson (matchingPerson))
                                    personObjects.add(anchor);
                    }
                    
                    // Check if the anchor's possessive words refer to a person
                    ArrayList<TextAnnotation> possessiveWords = nounAnchor.getPossessiveWords();
                    if (possessiveWords != null )          
                    {
                        for (int i = 0; i < possessiveWords.size(); i++)
                        {   
                             matchingPerson = TextAnnotation.getContainingAnnot(PersonList, possessiveWords.get(i));
                              if (isValidPerson (matchingPerson))
                              {
                                    personObjects.add(anchor);
                                    break;
                              }
                        }
                    }   // end if "anchor"
                } // end if  "link"
               // also add descendant Persons through prepositional links of lower level  objects
                personObjects.addAll( getDescendantPersons(anchor,  PersonList));
            }
            else if (link. startsWith("prep_"))
            {
                TextAnchor personObject  = getDirectPerson(link, objectMap);  
                if (personObject != null && isValidPerson(personObject.getGovernorToken()))
                    personObjects.add(personObject);
            }
        }
        return personObjects;         // empty list
   }

        /*-------------------------------------------------------------------------------------------------------------*/
    protected boolean isValidPerson(TextAnnotation annot)
    {
        if (annot == null)
            return false;
        if (Utils.isInList(annot.text.toLowerCase(), NERConstants.ENQUIRY_OBJECTS)
                    || Utils.isInList(annot.text, NERConstants.ASSISTANCE_OBJECTS)
            || Utils.isInList(annot.text, NERConstants.ENVIRONMENTAL_OBJECTS))
            return false;
        return true;
        
    }
    
    /*-------------------------------------------------------------------------------------------------------------
     * Get a person by tracing through Prepositional Links recursively
     * If it is not a  person type preposition but in a common list for either person or person, 
     * we assume it is a person
     *-----------------------------------------------------------------------------------------------------------*/
    protected  TextAnchor   getDirectPerson(String plink, LinkedAnchorMap objectMap)
    {
            TextAnchor  person = null;
            ArrayList<TextAnchor> objAnchors = objectMap.get(plink);
            
            for (int i = 0; i < objAnchors.size() && person == null; i++)
            {
                TextAnchor objAnchor = objAnchors.get(i);
                if (AnchorLink.isPersonPreposition(plink))     // prep_for, prep_with, prep_of   etc
                {
                    String anchorText = objAnchor.getPhraseText();
                    if(!Utils.isInList(anchorText, NERConstants.NONPERSON_NOUNS))
                        person =  objAnchor;
                }
                else  if (AnchorLink.isCommonPreposition(plink))     // either Location or Person: check Gate annotations 
                {
                    TextAnnotation matchingPerson  = getOverlappingPersonAnnotation(PersonList, objAnchor);
                    if (matchingPerson != null)
                        person = objAnchor;
                }
            }   
            return person;
    }
 /**********************************************************************************************/
 // Get the Lower level Annotations attached to this TextAnchor that represent persons  
 // Note: We return a String rather than a TextAnnotation, since the Person annotations
// are not direct part of any TextAnchor (only Token annotations are so.)
  /**********************************************************************************************/
    public ArrayList <TextAnchor> getDescendantPersons(TextAnchor anchor, 
        ArrayList<TextAnnotation> personList)
    {
         ArrayList <TextAnchor> persons = new ArrayList();
         AnchorLink[]  plinks = anchor.getDirectDependencyLinks("prep");     // not clausal ones
         if (plinks == null || plinks.length == 0)
             return persons;          // empty list
         
         for (int i = 0; i < plinks.length; i++)
         {
             AnchorLink plink = plinks[i];
             String prepos = plink.getLinkName();           // full name  e.g. "prep_in"
             TextAnchor prepAnchor = plink.dependentAnchor;
             if (AnchorLink.isPersonPreposition(prepos))     // in, near , for etc
             { 
                   persons.add( prepAnchor);
            } 
            else  if (AnchorLink.isCommonPreposition(prepos))    
            {
                 // either Person or Person: check Gate Person Annotations 
                TextAnnotation matchingPerson = TextAnnotation.getContainingAnnot(
                            personList, prepAnchor.getGovernorToken());
                if (matchingPerson != null)  persons.add(prepAnchor);
            }
             
            // now add the lower level prepositions through recuession
             ArrayList <TextAnchor> depPersons = getDescendantPersons( prepAnchor, personList);
            persons.addAll(depPersons);
         }
         return persons;
    }
  
   /**********************************************************************************************/
    // Check if the given anchor is a person
    public static boolean isInPersonList( ArrayList<TextAnnotation> persons, TextAnchor  anchor)
    {
           TextAnnotation matchingPerson  =  getPersonInList( persons,  anchor);
           if (matchingPerson != null)
               return true;
           // check if the anchor has an appostional modifier which is in person list
           // for example " my friend, John Smith"
           if (anchor.getType()  != TextAnchor.NOUN_ANCHOR)
               return false;
           TextAnchor apposAnchor = ((NounAnchor)anchor).getApposAnchor();
           if (apposAnchor == null)
               return false;
           return  isInPersonList(persons, apposAnchor);   
    }
    
    /**********************************************************************************************/   
    // Check if the anchor, or its "Appos"  is in the person list. Return the Anchor (self or appos) 
    //  that is in the list.
   public static TextAnchor getMatchingPersonAnchor (
            ArrayList<TextAnnotation> persons, TextAnchor  anchor)
     {
         // check if the anchor has an appostional modifier which is in person list
         // for example " my friend, John Smith"
         TextAnnotation gov = anchor.getGovernorToken();
         if (NameResolver.getMatchingName(gov, persons) != null)
             return anchor;         // self is in list
         
         if (anchor.getType() == TextAnchor.NOUN_ANCHOR)
         {
                 TextAnchor apposAnchor = ((NounAnchor)anchor).getApposAnchor();
                 if (apposAnchor != null)
                 {
                     gov = apposAnchor.getGovernorToken();
                     if (NameResolver.getMatchingName(gov, persons) != null)
                         return apposAnchor;
                 }
         }
         return null;
     }
   
    /**********************************************************************************************/   
    // Check if the person in the anchor has a bestMatch person in the  list. If so, return the person
   public  TextAnnotation getOverlappingPersonAnnotation( 
                     ArrayList <TextAnnotation> persons, TextAnchor  anchor)
   {
       TextAnchor  personAnchor = getMatchingPersonAnchor ( persons,  anchor);
       if (personAnchor == null)
           return null;             // not in the PersonList
       
       TextAnnotation[] annots = personAnchor.getWordSet();
       for (int i = 0; i < persons.size(); i++)
       {
           TextAnnotation person = persons.get(i);
           if (person.overlapsWith(annots))
           {
               TextAnnotation matchingPerson = PersonFullMap.get(person);
               return matchingPerson;
           }
       }
       log.error("Could not find matching annotation for " + anchor.getText() + "in Person List");
       return null;         //
   }
    /**********************************************************************************************/    
    public TextAnnotation getBestMatchPersonInList(TextAnchor  anchor)
    {
         return getOverlappingPersonAnnotation( PersonList,  anchor);
    }
   
    /**********************************************************************************************/
     // Get the Person annotation corresponding to the given TextAnchor
    public static TextAnnotation getPersonInList( ArrayList<TextAnnotation> persons, TextAnchor  anchor)
    {
        if (anchor == null)
            return null;

        TextAnnotation gov = anchor.getGovernorToken();
        TextAnnotation matchingPerson  =  NameResolver.getMatchingName(anchor.getText(), persons);

        if (matchingPerson == null && anchor.getType() == TextAnchor.NOUN_ANCHOR)
        {
             TextAnchor apposAnchor = ((NounAnchor)anchor).getApposAnchor();
             if (apposAnchor != null) 
                matchingPerson = getPersonInList(persons, apposAnchor);   
        }      
        return matchingPerson;
    }
    
/**********************************************************************************************/    
     public static int getPersonCategory( TextAnnotation person)
     {
        String personCategory  = (String) person.getFeature("person");
        if (personCategory == null)
           return -1;
        
        if (personCategory.equals("first"))
           return FIRST_PERSON;
        else  if (personCategory.equals("second"))
           return SECOND_PERSON;
        else if (personCategory.equals("third"))
           return THIRD_PERSON;
        else 
           return -1;
     }      
         
         
}

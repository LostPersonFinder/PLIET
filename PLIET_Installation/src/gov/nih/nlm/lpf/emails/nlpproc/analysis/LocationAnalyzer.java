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
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.AnchorLink;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.Clause;
import gov.nih.nlm.lpf.emails.nlpproc.structure.LinkedAnchorMap;

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import gov.nih.nlm.lpf.emails.util.Utils;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class LocationAnalyzer 
{
    private static Logger log = Logger.getLogger(LocationAnalyzer.class);
    
    protected static String LocationObjectRelations="dobj|iobj|copobj";
    
    ArrayList<TextAnnotation> LocationList;     // input list of known locations from external source
     ArrayList<TextAnnotation> PersonList;

    
       public LocationAnalyzer (ArrayList<TextAnnotation>locations, ArrayList<TextAnnotation>persons)
    {
        LocationList = locations;
        PersonList = persons;
    }

  /*---------------------------------------------------------------------------------------------------------*/  
    // Get the locations from the objects  in a Clause  through phrasal Links
    // by descending the phrasal hierarchy, but not the lower level clauses
    // We match direct objrvts locations against the given known list of locations. 
   // For prepositional Links, we retrieve them if it is a location_type preposition, even if
   // that is not in the objectMap
    
  /*---------------------------------------------------------------------------------------------------------*/  
    public  ArrayList <TextAnchor>  getLocationObjects(Clause clause)
    {
        ArrayList <TextAnchor> locationObjects = new ArrayList();     
       
        // Relationship to other phrases  in the sentence
        locationObjects.addAll( getLocationObjects(clause.clauseHead, clause.verbObjectMap));
        if (clause.subject != null)
        {
            ArrayList  <TextAnchor>subjectLocs = getLocationObjects(clause.subject, clause.subjObjectMap);
            addNewObjectsToList(locationObjects, subjectLocs);
        }
        if (locationObjects.isEmpty())
            return null;
       else if (locationObjects.size() > 1)
           log.warn("More than one location found " +":  " + locationObjects); 
        return locationObjects;
        
    }    
 
    /**********************************************************************************/
    protected   ArrayList <TextAnchor>  getLocationObjects(TextAnchor parent, LinkedAnchorMap objectMap)
    {
        ArrayList <TextAnchor> locationObjects = new ArrayList();     
        // Relationship to other phrases  in the sentence
        if (objectMap == null)
            return locationObjects;         // empty list
        
        Iterator <String> it = objectMap.keySet().iterator();
        
        // repeat for each link type
        while (it.hasNext())
        {
            String link = it.next();
            TextAnnotation matchingLoc = null;
            if (link.matches(LocationObjectRelations))        // note "copobj" is a synthetic link
            {
                TextAnchor anchor = objectMap.getAnchor(link);
                if (anchor == null)     // may have no real anchor with a link
                {
                    log.warn ("No anchor found for link:  " + link);
                    continue;
                }
                // find if this object refers to a Location Annotation in the given list
                matchingLoc = getOverlappingLocationAnnotation(LocationList, PersonList, anchor); 
                if (matchingLoc != null)
                {
                    locationObjects.add(anchor);
                    
                    // also add descendant Locations through prepositional links of lower level  objects
                    ArrayList <TextAnchor> descLocations = getDescendantLocations(anchor,  LocationList);
                    if (descLocations != null && !descLocations.isEmpty())    
                        locationObjects.addAll(descLocations);   
                }
            }   // end link: dobj/iobj/copobj
                
            else if (link. startsWith("prep_"))
            {
                  ArrayList <TextAnchor> locs  = getDirectLocation(link, objectMap);  
                if (locs != null  && locs.size() > 0)
                    addNewObjectsToList(locationObjects, locs);
                   
                else 
                {
                    TextAnchor anchor = objectMap.getAnchor(link);
                    if (anchor != null)
                    {
                         ArrayList <TextAnchor> descLocations = getDescendantLocations(anchor, LocationList);
                        if (descLocations != null && !descLocations.isEmpty())    
                            addNewObjectsToList(locationObjects, descLocations);   
                    }
                }
            }
        }   // end while
        
        return locationObjects;
   }
  
    // Add objects from a second list to the original list if not already in the original list)
    protected void addNewObjectsToList(ArrayList currentObjectList, ArrayList newObjectList)
    {
        if (newObjectList == null || newObjectList.size() == 0)
            return;
        
          for (int i = 0;  i < newObjectList.size(); i++)
            {
                 if (currentObjectList.contains(newObjectList.get(i)))
                     continue;              // same one, don't add
                currentObjectList.add(newObjectList.get(i));
            }
          return;
    }
    /*-------------------------------------------------------------------------------------------------------------
     * Get a location by tracing through Prepositional Links recursively
     * If it is not a  location type preposition but in a common list for either person or location, 
     * we assume it is a person
     *-----------------------------------------------------------------------------------------------------------*/
    protected ArrayList<TextAnchor>  getDirectLocation( String plink,  LinkedAnchorMap objectMap)
    {
            ArrayList<TextAnchor> locAnchors  = new ArrayList();
            ArrayList<TextAnchor> objAnchors = objectMap.getAnchorList(plink);
            
            if (AnchorLink.isLocationPreposition(plink))     // check for direct link prep_in, prep_near  etc
            {
               // make sure it is not like "I am in stable condition" with preposition "IN"
                if (objAnchors != null)
                {
                    for (int i = 0; i < objAnchors.size(); i++)
                    {
                        String str = objAnchors.get(i).getCoveringText();
                        if (!isHealthStatusInfo(str) && !inExcludeList(str))
                        {
                            locAnchors.add (objAnchors.get(i));
                        }
                    }
                    return locAnchors;
                }
            }
            
           // check if the linked object  is a known  Person or Location: check Gate annotations 
           if (AnchorLink.isCommonPreposition(plink))     // either Person or Location: check Gate annotations 
            {
                 for (int i = 0; i < objAnchors.size(); i++)
                 {
                        TextAnnotation matchingLoc = getOverlappingLocationAnnotation(
                            LocationList, PersonList, objAnchors.get(i));
                        if (matchingLoc != null)
                            locAnchors.add(objAnchors.get(i));
                 }
            }
            return locAnchors;
    }
 /**********************************************************************************************/
 // Get the Lower level Annotations attached to this TextAnchor that represent locations  
 // Note: We return a String rather than a TextAnnotation, since the Location annotations
// are not direct part of any TextAnchor (onlt Token annotations are so.)
  /**********************************************************************************************/
    public ArrayList <TextAnchor> getDescendantLocations(TextAnchor anchor, 
        ArrayList<TextAnnotation> locationList)
    {
         ArrayList <TextAnchor> locations = new ArrayList();
         AnchorLink[]  plinks = anchor.getDirectDependencyLinks("prep");     // not clausal ones
         if (plinks == null || plinks.length == 0)
             return locations;          // empty list
         
         for (int i = 0; i < plinks.length; i++)
         {
             AnchorLink plink = plinks[i];
             String prepos = plink.getLinkName();           // full name  e.g. "prep_in"
             TextAnchor prepAnchor = plink.dependentAnchor;
             if (AnchorLink.isLocationPreposition(prepos))     // in, near , for etc
             { 
                   locations.add( prepAnchor);
            } 
            else  if (AnchorLink.isCommonPreposition(prepos))    
            {
                 // either Person or Location: check Gate Location Annotations 
                TextAnnotation matchingLoc = getOverlappingLocationAnnotation(
                    LocationList, PersonList, prepAnchor); 
                if (matchingLoc != null)  locations.add(prepAnchor);
            }
             
            // now add the lower level prepositions through recuession
             ArrayList <TextAnchor> depLocations = getDescendantLocations( prepAnchor, locationList);
            locations.addAll(depLocations);
         }
         return locations;
    }
    
        /**********************************************************************************************/   
    // Check if the anchor overlaps an entry  in the person list. If so, return the person
   public static TextAnnotation getOverlappingLocationAnnotation( 
            ArrayList<TextAnnotation> locations, ArrayList<TextAnnotation> persons, TextAnchor  anchor)
   {
       TextAnnotation[] annots = anchor.getWordSet();
       for (int i = 0; i < locations.size(); i++)
       {
           if (locations.get(i).overlapsWith(annots))
           {
            // make sure it does not overlap with a person
               for (int j = 0; j < persons.size(); j++)
               {
                if (persons.get(j).overlapsWith(annots))
                    return null;
               }
               return locations.get(i);
           }
       }
       return null;         
   }
  //----------------------------------------------------------------------------------------------//
   // Check if the given text String contains a health status term such as health, condition, ...
   protected boolean isHealthStatusInfo(String textStr)
   {
       String[] words = textStr.split("\\W+");
       String[] statusWords  = NERConstants.STATUS_DATA;
       for (int i = 0; i < words.length; i++)
       {
           if (Utils.isInList(words[i], statusWords))
               return true;
       }
       return false;
   }
   //-------------------------------------------------------------------------------------------//
   // Check if the  given text String contains a personal attribiue term such 
   // as age or a body part such as "In head"
   protected boolean inExcludeList(String textStr)
   {
       String[] words = textStr.split("\\W+");
       String[] excludeWords  = {"age", "years", "pain", 
           "head", "body",  "hand", "hands", "leg", "legs", "face", "chest", "back"};
       for (int i = 0; i < words.length; i++)
       {
           if (Utils.isInList(words[i], excludeWords))
               return true;
       }
       return false;
   }
       
    
    /*---------------------------------------------------------------------------------------------------------------/
    *  Resolve the location by analyzing importance
     * AnnotatedLocationList is the list of "LOCATION" annotation from GATE pipeline
    *--------------------------------------------------------------------------------------------------------------*/
    public static String resolveLocation( ArrayList<TextAnnotation> AnnotatedLocationList, String[] locations)
    {
     if (locations == null || locations.length == 0)
         return "";
     if (locations.length == 1)
        return  locations[0];

     // more than two locations - check if one is in Location List
     int n = locations.length;
     boolean [] locInList = new boolean[n];

     for (int i = 0; i < n; i++)
     {
         locInList[i] = false;      // default
        for (int j = 0; j < AnnotatedLocationList.size(); j++)
        {
            if ( (AnnotatedLocationList.get(j).text).equalsIgnoreCase(locations[0]))
            {
                locInList[j] = true;
                break;
            }
        }
     }
     // check priority etc.
     if (n == 2)
     {
         if  (locInList[0] && !locInList[1])
             return locations[0];
         else if (locInList[1] && locInList[0])
             return locations[1];
         else if (!locInList[0] && !locInList[1])     // neither in list
             return  locations[1];
         else   // both in list: concatenate
            return (locations[0] +", " + locations[1]);
        }
         //TBD:
         return locations[n-1];         // return the last one
    }
    
    /**************************************************************************/
    public String  getAddresses(TextAnchor location)
    {
        return location.getTextWithAppos();         // location components separated by commas    
    }
    
}

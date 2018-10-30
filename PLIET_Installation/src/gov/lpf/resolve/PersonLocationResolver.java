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
package gov.lpf.resolve;

//import gov.lpf.location.GeocoderClient;

import  gate.stanford.DependencyRelation;

import gate.creole.*;
import gate.creole.metadata.*;
import gate.ProcessingResource;

import gate.AnnotationSet;
import gate.annotation.AnnotationSetImpl;
import gate.Factory;
import gate.FeatureMap;
import gate.Annotation;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.Serializable;

import java.util.Collections;
import org.apache.log4j.Logger;


/**
 * To mark Locations and persons in a message, we follow preposition links in a sentence:
 * 
 *  Check the ANNIE annotations: Location, Person, Lookup, DisasterTerms, Conditions and Tokens 
 *  in the Input Annotation Set
 * 
 * indicating a location/Person
 * Create candidate Person and Annotation sets
 * If it is already annotated as such (by ANNIE), 
 *      add to set
 * Check lookup table 
 *  If in person or location category in the Lookup list
 *      Add to corresponding candidate set
 *  If common to both sets - put in ambiguous list
 *      Check corresponding preposition, 
 *      if resolved add to the corresponding set
 * if unresolved 
 *      Check the dependencies of the head token to determine  type
 *      If not resolved, check through Google Geocoder service
 *  If  in UNKNOWN list,
 *      Check as in ambiguous set, including Geocoder service
 *  If fails, leave as unknown.
 * *----------------------------------------------------------------------------------------------------------
 * @author 
 */
@CreoleResource(name = "PersonLocationResolver",
helpURL = "",
comment = "Plugin that determines Persons and Locations from Lookups and Unknowns, checking with GeoCoder ")
public class PersonLocationResolver extends AbstractLanguageAnalyser 
        implements   VocabularyTerms,  ProcessingResource, Serializable 
{
    /** A logger to use instead of sending messages to Out or Err **/
    protected static final Logger log = Logger.getLogger(PersonLocationResolver.class);
    
    
   // A placeholder  class to hold information for creating a new annotation of a given type
   // from an existing annotation, but with different featuremap 
    protected class CandidateAnnotation
    {
        Annotation originalAnnotation;      // from which created
        String originalType;
        String annotType;                   // new annotation type
        FeatureMap features;           // Features for the new annotation          
        Long[] offsets;                        // offsets of new annotation
         
    
        protected CandidateAnnotation(Annotation originalAnnot, 
                String originalType,   String annotType, FeatureMap  features, Long[] annotOffsets)
        {
            originalAnnotation = originalAnnot;
            this.originalType = originalType;
            this.annotType = annotType;                        
            this. features = features;   

            if (annotOffsets == null)
                offsets =new  Long[]{originalAnnotation.getStartNode().getOffset(), 
                                        originalAnnotation.getEndNode().getOffset()};
            else
               offsets = annotOffsets;
        }
       
        // create a candidate annotation using the same offsets as the original one
      protected CandidateAnnotation(Annotation originalAnnot, 
            String originalType,   String annotType, FeatureMap  features)
        {
           this( originalAnnot, originalType, annotType,  features, null);
        }        
        
    }   
    /*-------------------------------------------------------------------------------------------*/
    
    // majorType in Annotation featureMap indicating location
     private static String LocationMajorType  = "location|loc_key|facility\\*|address|org_base";
     private static String PersonMajorType  = "person_first|person_last|person_relative|title|jobtitle";
     
    /*-------------------------------------------------------------------------------------------*/
    private String inputASName;     //  Input AnnotationSet name
    private String outputASName;    // Output AnnotationSet set name

    AnnotationSet inputAS; 
    AnnotationSet outputAS; 
    AnnotationSet tokenSet;
    
    AnnotationSet candidatePersonAS;
    AnnotationSet candidateLocationAS;
     
// All  locations and Person Annotation set
    ArrayList<Annotation>  allLocationList;
    ArrayList<Annotation> allPersonList;

   ArrayList <Annotation> lastNameList;             // List of annoations marked as "Lastname"
   ArrayList <Annotation> unknownList;               // List of annoations marked as "Unknown"
   ArrayList <Annotation> lookupList;                   // List of annoations marked as "Lookup"
   ArrayList <Annotation> lpfStatusList;               // List of annoations marked as Condition (lpfStatus)
   ArrayList <Annotation> disasterWordList;      // List of annoations marked as Disaster
   
    // candidate sets
   //ArrayList<Annotation>candidatePersons;
   //ArrayList<Annotation>candidateLocations;
   ArrayList<Annotation[]> ambiguousList;

   boolean personListChanged = false;
   boolean locationListChanged = false;
   
   boolean debug = true;
   String docContent;
    
    public PersonLocationResolver()  
    {
       super();
    }
    
   // Read the Firstname/Lastname dictionaries for matching person names, if necessary
 

   @Override
   public void execute() //throws ExecutionException
    //public void test(String inputASName, String outputASName, Document gateDoc)
    {

        inputAS = (inputASName == null || inputASName.trim().length() == 0)
            ? document.getAnnotations() : document.getAnnotations(inputASName);
        outputAS = (outputASName == null || outputASName.trim().length() == 0)
            ? document.getAnnotations() : document.getAnnotations(outputASName);

        //copy the results to the output set if they are different
        if (inputAS != outputAS)
            outputAS.addAll(inputAS);

        // Document content
        docContent = document.getContent().toString();
        
        // initialize 
        initSets();

        boolean testing = false;
        if (testing)
            return;
        
        printAnnotations(inputAS, true);
        try
        {
           // create the candidate person and location sets from the marked annotations
            candidatePersonAS = initCandidatePersonList();
            candidateLocationAS = initCandidateLocationList();

           // add entries from Lookup list to various bins
            ambiguousList = addEntriesFromLookupList(candidatePersonAS, candidateLocationAS);

           if (unknownList == null && ambiguousList == null  && (!personListChanged && !locationListChanged))
               return;          // no updates 

           // First go over the ambiguous  list and see how they could be resolved
           ArrayList<CandidateAnnotation>  resolvedAmbigAnnotations = 
                    resolveAmbiguousAnnotations(ambiguousList);
           // Now go over the Unknown list and see how they could be resolved
           ArrayList<CandidateAnnotation>  resolvedUnknownAnnotations = 
                    resolveUnknownAnnotations(unknownList);

           AnnotationSet  newPersonAS = 
                    addResolvedAnnotations("Person", candidatePersonAS, 
                    resolvedAmbigAnnotations,  resolvedUnknownAnnotations);
          AnnotationSet   newLocationAS =
                    addResolvedAnnotations( "Location", candidateLocationAS, 
                    resolvedAmbigAnnotations, resolvedUnknownAnnotations);
         newPersonAS = addFullPersons(newPersonAS);
         newLocationAS  = removeLocationsFromPersons(newLocationAS, newPersonAS);

           // Now update the outputAnnotationSet by adding Annotations for the new Person/Location list
           // and deleting the corresponding ones fron original Annotation sets
           // TBD: New unknown list
           updateOutputAnnotationSet( newPersonAS, newLocationAS);
 
        }
        catch (Exception e)
        {
            log.error("Got exception", e);
        }
   
       /* processCandidatePersons(candidatePersons);
        cleanup();
        fireProcessFinished();*/
         printAnnotations(outputAS, false);
        return;
    }
    
      //-------------------------------------------------------------------------------------------------
    // initialize the three  Annotation sets we are concerned with
    //-----------------------------------------------------------------------------------------------/
    protected void initSets()
    {
        allLocationList = retrieveAnnotationSet("Location");
        allPersonList = retrieveAnnotationSet("Person");
      
        lookupList =  retrieveAnnotationSet("Lookup");
        lpfStatusList =  retrieveAnnotationSet("Condition");
        disasterWordList=  retrieveAnnotationSet("DisasterTerm");
        lastNameList=  retrieveAnnotationSet("Lastname");
        unknownList =  retrieveAnnotationSet("Unknown");
        unknownList =  removeLastnamesFromList(unknownList, lastNameList);

           
        tokenSet = inputAS.get("Token");  
    }
    
    /*----------------------------------------------------------------------------------------------------
     * Retrieved the named annotation set from the inputAS and delete it from the
     * outputAS set.
     * Return the retrieved set in a sorted manner
     *-------------------------------------------------------------------------------------------------------*/
    protected ArrayList<Annotation> retrieveAnnotationSet(String setName)
    {
        candidatePersonAS = new AnnotationSetImpl(document, "CandidatePerson");
        AnnotationSet annotSet =  inputAS.get(setName);  
        ArrayList<Annotation> annotList = new ArrayList();
        if (annotSet.size() > 0)
        {
            //outputAS.removeAll(annotSet);
             annotList = new ArrayList(annotSet);
            Collections.sort(annotList, new gate.util.OffsetComparator());
        }
        
       if (debug) 
       {
           String str = setName + " - " ;
           Iterator <Annotation> it = annotSet.iterator();
           while (it.hasNext())
              str += getText(it.next()) +", ";
            System.out.println(str);    
       }
        
       return annotList;
    }
   /*-------------------------------------------------------------------------------------*/ 
    protected String getText(Annotation annot)
    {
        int   start = annot.getStartNode().getOffset().intValue();
        int end = annot.getEndNode().getOffset().intValue();
        return (docContent.substring(start, end));
    }
   
   //---------------------------------------------------------------------------------------------------------*/
    // Initialize the candidate Location Annotations from annotations in the inputAS  
   //---------------------------------------------------------------------------------------------------------*/
   protected  AnnotationSet  initCandidateLocationList()
   {
      AnnotationSet  locationSet = new AnnotationSetImpl(document, "CandidateLocation");
       for (int i = 0; i < allLocationList.size(); i++)
       {
            addCandidateAnnotation(locationSet, allLocationList.get(i),  "Location", "Location", null, null);
       }
       
        //Collections.sort(annotList, new gate.util.OffsetComparator());
      return locationSet;
   }
    
    //---------------------------------------------------------------------------------------------------------*/
    // Initialize the candidate Person Annotations from annotations in the inputAS  
   // Then add all Lastname annotations that is not part of any Person name
   //---------------------------------------------------------------------------------------------------------*/
   
   protected  AnnotationSet  initCandidatePersonList()
   {
       AnnotationSet personSet = new AnnotationSetImpl(document, "CandidatePerson");
       ArrayList<Annotation> annotList = new ArrayList();
       for (int i = 0; i < allPersonList.size(); i++)
       {
            addCandidateAnnotation(personSet, allPersonList.get(i),  "Person", "Person", null, null);
       }
       
       // Consider lastnames only if it starts with upper case
       for (int i = 0; i < lastNameList.size(); i++)
       {
           Annotation lastNameAnnot = lastNameList.get(i);
           String lname = getText(lastNameAnnot) ;
           if (!lname.substring(0,1).matches("[A-Z]"))
               continue;
           boolean contained = false;
           for (int j = 0; j < allPersonList.size() && !contained; j++)
           {
               if (lastNameAnnot.withinSpanOf(allPersonList.get(j)))
                   contained = true;
           }
           // Check if it is part of a full location (as in 123 Washington Street)
          for (int j = 0; j < allLocationList.size() && !contained; j++)
           {
               String location = getText(allLocationList.get(j));
               if (lastNameAnnot.withinSpanOf(allLocationList.get(j)) &&
                   location.length() > lname.length())
                   contained = true;
           }
           
           if (!contained)
           {
                addCandidateAnnotation(personSet, lastNameAnnot,  "Lastname", "Person", null, null);
                System.out.println("Added Lastname " + getText(lastNameAnnot) + " to CandidatePerson List");
           }
       }
       return personSet;
   } 
   
   //---------------------------------------------------------------------------------------------------------*/
   //Add an entry to the candidateAnnotation set by copying features from another Annotation
   
   protected Annotation addCandidateAnnotation( AnnotationSet annotSet,  Annotation originalAnnot,
       String originalType, String newType, FeatureMap features, Long[] offsets)
   {
       if (features == null)
            features = originalAnnot.getFeatures();
       if (!originalType.equals(newType))
            features.put("original_type", originalType);
       Long start , end;
       
       if (offsets == null)
       {
           start = originalAnnot.getStartNode().getOffset();
           end =  originalAnnot.getEndNode().getOffset();
       }
       else
       {
           start = offsets[0];
           end = offsets[1];
       }
       try
       {
            Integer annotId = annotSet.add(start, end, newType, features);
            Annotation annot = annotSet.get(annotId);
            return annot;
       }
       catch (Exception e)
       {
           log.error("Could not add new annotation", e);
           return null;
       }
   }
   
   //---------------------------------------------------------------------------------------------------------*/
    protected ArrayList<Annotation>removeLastnamesFromList(
        ArrayList<Annotation>unknownList, ArrayList<Annotation>lastNameList)
    {
        //for (int i = 0; i < unknownList.size(); i++)
        Iterator <Annotation>it = unknownList.iterator();
        while (it.hasNext())
        {
            Annotation unknown = it.next();
            for (int i = 0; i < lastNameList.size(); i++)
           {
              if (unknown.coextensive(lastNameList.get(i)))
                  it.remove();          // remove the annotation from Unknown list
           }     
        }
        return unknownList;
    }
   
   //---------------------------------------------------------------------------------------------------------*/
   // Add entries from the Lookup list to either the Person or Location list, depending 
   // upon the features
   // If features don't match, add to ambiguous list
   protected  ArrayList <Annotation[]>  addEntriesFromLookupList(
      AnnotationSet personAS,  AnnotationSet locationAS)
   {
        // Add proper annotations from lookup list
        ArrayList <Annotation[]> ambigList = new ArrayList();
        for (int i = 0; i < lookupList.size(); i++)
        {
           Annotation lookupAnnot = lookupList.get(i);
           FeatureMap features = lookupAnnot.getFeatures();
           String majorType = (String)features.get("majorType");
           if (majorType != null && majorType.matches(LocationMajorType))
           {
               // check if a corresponding Person also exists
               Annotation matchingLookup = getCoextensiveAnnotation(lookupAnnot, lookupList, "Person");
               if (matchingLookup ==null)                       // No match in Person List
               {
                    int matchType = getMatchType(lookupAnnot, locationAS);
                    boolean updated = updateAnnotationList(matchType, locationAS, 
                        lookupAnnot, "Location", "Lookup");       // if not already included
                    if (updated)
                        locationListChanged = true;
               }
               else         // add to ambiguous list as [locationLookup, personLookup]
                     ambigList.add(new Annotation[]{lookupAnnot, matchingLookup});
           }
        }

        // repeat the same procedure withPersons
        for (int i = 0; i < lookupList.size(); i++)
        {
           Annotation lookupAnnot = lookupList.get(i);
           FeatureMap features = lookupAnnot.getFeatures();
           String majorType = (String)features.get("majorType");
           if (majorType != null && majorType.matches(PersonMajorType))
           {
               // check if a corresponding Person also exists
               Annotation matchingLookup = getCoextensiveAnnotation(lookupAnnot, lookupList, "Location");
               if (matchingLookup ==null)                       // No match in Person List
               {
                    int matchType = getMatchType(lookupAnnot, personAS);
                   boolean updated = updateAnnotationList(matchType, personAS, 
                        lookupAnnot, "Person", "Lookup");       // if not already included
                    if (updated)
                        personListChanged = true;
               }
               else         // add to amboguous list as [locationLookup, personLookup]
                     ambigList.add(new Annotation[]{lookupAnnot, matchingLookup});
           }
        }

        return(ambigList.size() > 0) ? null : ambigList;
   } 
   //---------------------------------------------------------------------------------------------------------------------
   // Get the annotation that is coextensive with another annotation of  a specific type
   protected Annotation getCoextensiveAnnotation(Annotation annot,
        ArrayList <Annotation> annotationList, String type)
   {
      for (int i = 0; i < annotationList.size(); i++)
      {
          if (annot.getType().equals(type) && annot.coextensive(annotationList.get(i)))
              return annotationList.get(i);
      }
      return null;
   }
 
   //----------------------------------------------------------------------------------------------------------------------*/
   // Check if the given annotation is a new one, exactly matches another annotation or
   // contained by/contains  another annotation 
   // 0 => no match, 1 => full match; 2 => contained by, 3 => contains
   // 1: The new one is added to the list, 3: replaces the existing annot
   // Returns true if List is updated
   //----------------------------------------------------------------------------------------------------------------------
   protected int getMatchType( Annotation newAnnot, AnnotationSet annotSet)
   {
        int match = 0;          // no match
        int index = -1;
         // TBD: overlap
        Iterator<Annotation> iter = annotSet.iterator();
        while(iter.hasNext() && match == 0)
        {
            Annotation annot = iter.next();
           if (newAnnot.coextensive(annot))
              match =  1;                                               // exact match in terms of offsets
           else if (newAnnot.withinSpanOf(annot))     // newAnnot smaller, no change required
               match = 2;
           else if (annot.withinSpanOf(newAnnot))     // annot larger, should replace cannot
               match = 3;
        }
        return match;
   }

   //----------------------------------------------------------------------------------------------------------------------*/
   // Update the annotation list, if necessary with Featuremap of the given annotation.
   // in another list, depending upon the match type
   // 0 => new annotation, 1 => full match; 2 or 3 => an exact or better match exists in the current set
   // 3 => Given annotation is a better match, should replace the existing annotation in the set
   // Returns true if List is updated
   //----------------------------------------------------------------------------------------------------------------------
   protected boolean   updateAnnotationList( int matchType, AnnotationSet annotAS,
                        Annotation inputAnnot, String newType, String originalType)
   {
        Annotation newAnnot = null;
        boolean updated = false;
        if (matchType == 0)         // no current match
        {
           newAnnot = addCandidateAnnotation(annotAS, inputAnnot, originalType, newType, null, null);     
        }
       else if (matchType == 1 || matchType == 2)   // exact match or better match exists, do nothing
          ;        
       
       else if (matchType == 3)     // should replace an existing annotations of smaller length from the set
       {
           Long start = inputAnnot.getStartNode().getOffset();
           Long end = inputAnnot.getEndNode().getOffset();
           AnnotationSet  oldAnnots = annotAS.getContained(start, end);
           annotAS.removeAll(oldAnnots);
           newAnnot =  addCandidateAnnotation(annotAS, inputAnnot, originalType, newType, null, null);
       } 
        if (debug)
        {
            if (matchType == 0)
               log.info(getText(newAnnot) + " added to candidate " +  newType + " list" );
            else if (matchType == 3)
              log.info(getText(newAnnot) + "    replaced  annotation(s) in " + newType + " list");
        }
       return (newAnnot != null);
   }
   
//-----------------------------------------------------------------------------------------------------*/
// Add the resolved annotatons of the given type to the current set
//-----------------------------------------------------------------------------------------------------*/
   protected    AnnotationSet   addResolvedAnnotations( String listType,
           AnnotationSet  initialSet,
           ArrayList<CandidateAnnotation> resolvedAmbigs,
           ArrayList<CandidateAnnotation>  resolvedUnknowns)
   {
       AnnotationSet  newSet  = initialSet;
       
       try
       {
           // add appropriate entries from the resolved ambiguous list
           if (resolvedAmbigs != null && resolvedAmbigs.size() > 0)
           {
               for (int i = 0; i < resolvedAmbigs.size(); i++)
               {
                   CandidateAnnotation cannot = resolvedAmbigs.get(i);;
                   String ctype = cannot.annotType;
                   if (ctype.equalsIgnoreCase(listType))
                   {
                       Long[] offsets = cannot.offsets;
                       newSet.add(offsets[0], offsets[1], ctype, cannot.features);
                    }
               }
           }
             // add appropriate entries from the resolved unknown list
             if (resolvedUnknowns != null && resolvedUnknowns.size() > 0)
             {
               for (int i = 0; i < resolvedUnknowns.size(); i++)
               {
                   CandidateAnnotation cannot = resolvedUnknowns.get(i);
                   String ctype = cannot.annotType;
                   if (ctype.equalsIgnoreCase(listType))
                   {
                       Long[] offsets = cannot.offsets;
                       int id = newSet.add(offsets[0], offsets[1], ctype, cannot.features);
                       Annotation annot = newSet.get(id);
                   }
               }
            }   // end if
       }
       catch (Exception e)
       {
           log.error(e);
       }
       return newSet;        
   }
 
 //-----------------------------------------------------------------------------------------------------*/
    protected ArrayList<CandidateAnnotation> resolveUnknownAnnotations
        ( ArrayList<Annotation> unknownList)
    {
        ArrayList<CandidateAnnotation> resolvedList = new ArrayList();
        ArrayList <Annotation> unresolvedPersons = new ArrayList();
        
        for (int i = 0; i < unknownList.size(); i++)
        {
            boolean resolved = false;
            Annotation unknown = unknownList.get(i);
            // Is is relate to a known location through an appos relation, as in "Rockville, MD)
            boolean  relatedLoc = isARelatedLocation (unknown);
            if (relatedLoc)
            {
                 CandidateAnnotation locationAnnot = annotateAsLocation(unknown);
                 resolvedList.add(locationAnnot);
                 resolved = true;
            }
            else 
            {   
                // check if it is parsed with a Prepositional Relation as a  location (Object of a Preposition)
                // Then verify that it is location 
                if  (isParsedAsLocation(unknown))
                {
                    CandidateAnnotation googleAnnot = annotateAsGoogleLocation(unknown);
                    if (googleAnnot != null)
                    {
                      resolvedList.add(googleAnnot);
                      resolved = true;
                    };
                }   // end if
            }   // end else
            if (!resolved)
            {
                // neither a matching location or a google location, a possible Person
                unresolvedPersons.add(unknown);
            }
        } 
        
        // now process the unresolved Persons to try to resolve them
        ArrayList<CandidateAnnotation> resolvedPersonList = 
            resolveCandidatePersons(unresolvedPersons);
        if (resolvedPersonList.size() > 0)
            resolvedList.addAll(resolvedPersonList);
        return resolvedList;
    }
    /*---------------------------------------------------------------------------------------------------------------*/
   // create a new Annotation if the string is recognized as aLocation by checking its dependency
    // replacinig the "Unknown" annotation
    // Return true or false depending upon if it was a Location
    /*-------------------------------------------------------------------------------------------------------------*/
    protected boolean isARelatedLocation(Annotation unknown)
    { 
        //AnnotationSet unknownTokenSet  = inputAS.get("Token", 
         //       unknown.getStartNode().getOffset(), unknown.getEndNode().getOffset());
        AnnotationSet tokens = getAllTokensInSentence(unknown);

        // get the token which matches the UNKNOWN annotation
        AnnotationSet tokensubset = tokens.get("Token", 
                    unknown.getStartNode().getOffset().longValue(), 
                    unknown.getEndNode().getOffset().longValue());
        
        // find the head token in that set
        Annotation token = tokensubset.iterator().next();
        Annotation head = getHeadToken(token, tokens);

        // first check if it is is related to another  known location through an "appos" relation
        Annotation apposGov = getGovernorToken(head, tokens, "appos");
        while (apposGov != null)
        {
            Iterator <Annotation> it = allLocationList.iterator();
            {
                if (it.hasNext() && apposGov.withinSpanOf(it.next()))
                    return true;
            }  
            apposGov = getGovernorToken(apposGov, tokens, "appos");         // recurse
        }
        
        // Not an appos that is a Location: Check if it has a follow up "appos"  token  which is a know location
        Annotation newHead = head;
        while (newHead != null)
        {
            int apposId =  getTargetTokenId(newHead, "appos");
            if (apposId == -1)
                return false;           // has no appos
        
             Annotation apposAnnot =  inputAS.get(apposId);
             Iterator <Annotation> it = allLocationList.iterator();
             while (it.hasNext())
            {
                
                if (apposAnnot.withinSpanOf(it.next()))
                    return true;
            }  
             // not known location, recurse for next appos 
            newHead = apposAnnot;         
         }
        
         return false;
    }
   
   /*---------------------------------------------------------------------------------------------------------------*/
   // create a new Annotation if the string is recognized as a Location by checking its dependency
    // replacinig the "Unknown" annotation
    // Return true or false depending upon if it was a Location
    /*-------------------------------------------------------------------------------------------------------------*/
    protected boolean isParsedAsLocation(Annotation unknown)
    { 
        AnnotationSet tokens = getAllTokensInSentence(unknown);

        // get the token which matches the UNKNOWN token
        AnnotationSet tokensubset = tokens.get(unknown.getStartNode().getOffset().longValue(), 
                    unknown.getEndNode().getOffset().longValue());
        Annotation token = tokensubset.iterator().next();
        Annotation head = getHeadToken(token, tokens);
       
   
        // check if this token is  related to a Preposition  relation (IN)
        boolean isLoc = false;            // is it a candidate for location
        Iterator<Annotation> it = tokens.iterator();
        while (it.hasNext())
        {
            Annotation ptoken = it.next();
            String category = (String) ptoken.getFeatures().get("category");
            String type = (String) ptoken.getFeatures().get("morph");
            if (!category.equalsIgnoreCase("IN") || isInList(type, PERSON_PREPS))
                continue;
            
            int  pobjId = getTargetTokenId(ptoken, "pobj");
             if (isInList(type, LOCATION_PREPS) || isInList(type, COMMON_PREPS))
            {
                if ( pobjId  == head.getId())
                {
                    isLoc = true;
                     break;
                }
            }    
        }
        return isLoc;
    }   
     
    /*---------------------------------------------------------------------------------------------------------------*/
    protected boolean isInList(String str, String[] alist)
    {
        for (int i = 0; i < alist.length; i++)
        {
            if (str.equals(alist[i]))
                return true;
        }
        return false;
    }
        
  //----------------------------------------------------------------------------------------------------------------
    protected Annotation getHeadToken(Annotation token, AnnotationSet tokens)
    {
        // get the head token for an "nn" relationship
        
        Annotation head = token;
        Annotation oldHead = head;
        while (true)
        {
          Annotation newHead = getGovernorToken(head, tokens, "nn");
          if (newHead == null)
              break;
          head  = newHead;
        }
        if (head.getId() != oldHead.getId())
            log.info("New head token for " +token.getFeatures().get("string") + " is : "  + head.getFeatures().get("string"));    
        
        return head;
    }          
        
    //*------------------------------------------------------------------------------------------------
    //Get all annotations in a sentence where the specified annotation  exists
    /*-----------------------------------------------------------------------------------------------*/
    protected AnnotationSet getAllTokensInSentence(Annotation annot)
    {
        AnnotationSet sentenceAnnots  = inputAS.getCovering(
                    ANNIEConstants.SENTENCE_ANNOTATION_TYPE,
                    annot.getStartNode().getOffset().longValue(), 
                    annot.getEndNode().getOffset().longValue());
        if (sentenceAnnots == null || sentenceAnnots.isEmpty())       // should not happen
            return null;
        
        Annotation sentence = sentenceAnnots.iterator().next();       // only one covering sentence
       
        // get All tokens within this sentence
        AnnotationSet tokens = inputAS.get( 
                   ANNIEConstants.TOKEN_ANNOTATION_TYPE,
                    sentence.getStartNode().getOffset().longValue(), 
                    sentence.getEndNode().getOffset().longValue());
        return tokens;   
    }
/*-----------------------------------------------------------------------------------------------*/
    // Get a token which is the governor of a relationship to a  specified  one
    /*-----------------------------------------------------------------------------------------------*/
    protected Annotation getGovernorToken(Annotation token,   
                AnnotationSet tokens, String relationType)
    {
        int id = token.getId();
        Iterator<Annotation> it = tokens.iterator();
        
        while (it.hasNext())
        {
            Annotation atoken = it.next();
            ArrayList<DependencyRelation> dependencies = (ArrayList) atoken.getFeatures().get("dependencies");
            if (dependencies == null)
                continue;
            
            for (int i = 0; i < dependencies.size(); i++)
            {
                if (dependencies.get(i).getType().equalsIgnoreCase(relationType))
                {
                    if (token.getId() == dependencies.get(i).getTargetId())
                        return atoken;              // governor 
                }
            }
        }
        return null;
    }    

/*-----------------------------------------------------------------------------------------------*/
    // Get ID of the token related to the given token through a specific relation
    // Assume there is only one such relation
    /*-----------------------------------------------------------------------------------------------*/
    protected int  getTargetTokenId(Annotation token,   String relationType)
    {
        int[]  targetIds = getTargetTokenIds( token,  relationType);
        if (targetIds == null)
            return -1;
        return targetIds[0];
    }    
    
    /*-----------------------------------------------------------------------------------------------*/
    // Get ID of the token related to the given token through a specific relation
    /*-----------------------------------------------------------------------------------------------*/
    protected int[]  getTargetTokenIds(Annotation token,   String relationType)
    {
        int id = token.getId();
      
        ArrayList<DependencyRelation> dependencies = (
            ArrayList) token.getFeatures().get("dependencies");
        if (dependencies == null)
            return null;
       
        ArrayList <Integer> targetIds = new ArrayList();
        for (int i = 0; i < dependencies.size(); i++)
        {
            if (dependencies.get(i).getType().equalsIgnoreCase(relationType))
                targetIds.add(dependencies.get(i).getTargetId());   
        }
        
       if (targetIds.isEmpty()) 
           return null;
       
       int[]  tIds = new int[targetIds.size()];
       for (int i = 0; i < targetIds.size(); i++)
           tIds[i] = targetIds.get(i).intValue();
       
       Arrays.sort(tIds);
       return tIds;
    }    
/*----------------------------------------------------------------------------------------------------------------*/
    // Change the annotation of the given annotation from UNKNOWN to Location,
    // extending over the  tokens with an "nn" relation
    //------------------------------------------------------------------------------------------------------------------*/
    
    protected CandidateAnnotation  annotateAsLocation(Annotation unknown)
    {
        AnnotationSet tokens = getAllTokensInSentence(unknown);
        
        AnnotationSet tokensInRange = inputAS.get(ANNIEConstants.TOKEN_ANNOTATION_TYPE,
                    unknown.getStartNode().getOffset().longValue(), 
                    unknown.getEndNode().getOffset().longValue());
       
        Annotation leftAnnot = tokensInRange.iterator().next();         // get the first one
        Annotation headToken = getHeadToken(leftAnnot,  tokens);
   
        int[] compAnnots =  getTargetTokenIds(headToken, "nn");         // preceeding ones in set 
        if (compAnnots != null)
            leftAnnot = inputAS.get(compAnnots[0]);
        
        String locStr =  docContent.substring(leftAnnot.getStartNode().getOffset().intValue(), 
                                                                           headToken.getEndNode().getOffset().intValue());
        try
        {
            FeatureMap origFeatures = unknown.getFeatures();

            // add new features 
            FeatureMap features = Factory.newFeatureMap();
            features.put("rule", "LPFLocation");
            features.put("kind", (String)origFeatures.get("kind"));
            features.put("rule1", "relatedloc");
            features.put("text", locStr);
     
            log.info(">> Changed annotation \"" + locStr + "\" " +  " from Unknown to Location .");
            CandidateAnnotation cannot = new CandidateAnnotation( unknown, "Unknown", "Location", features);
            return cannot;
        }
        catch (Exception e)
        {
            log.error("Error changing UNKNOWN annotation to Location", e);
        }
       return   null;              
    }
 
 /*------------------------------------------------------------------------------------*/
    // create a new Annotation if the string is recognized as a location by the Google Geocoder
    // to replace the "Unknown" annotation
    // Return null  if it was not a valid Geocoder Location
    protected CandidateAnnotation annotateAsGoogleLocation(Annotation unknown)
    {
        //log.info("Trying to find Location for UNKNOWN term: " + unknown.toString());    
        GeocoderClient geovalidation = new GeocoderClient();    
        String locStr = getText(unknown);

        if (geovalidation.validateLocation(locStr))       // a known Geo location    
        {
            // add new features 
            FeatureMap features = Factory.newFeatureMap();
            features.put("rule", "geocoder");
            features.put("geo_location", geovalidation.getGeoLocation());
            features.put("latLng", geovalidation.getLatLng());
            features.put("text", locStr);
            
            log.info(locStr  + " recognized as a location through Geovalidation");
            CandidateAnnotation cannot = new CandidateAnnotation (unknown, "Unknown", "Location", features);
            return cannot;
        }     
         return   null;            //  not a known Geo location    
    }

    /*-------------------------------------------------------------------------------------------------*/
      // Check if unknown and determine if it is a new Person, a part of another 
      // Person or  not a person. Update the annotations accordingly
      //-------------------------------------------------------------------------------------------------*/
    protected ArrayList<CandidateAnnotation> resolveCandidatePersons
        (ArrayList<Annotation> candidates)
    {
        ArrayList<CandidateAnnotation> resolvedPersonList = new ArrayList();
        for (int i = 0; i < candidates.size(); i++)
        {
            Annotation cannot = candidates.get(i);
            if (!isCompatibleAsPerson(cannot) )                 // if features don't match, don't proceed 
                continue;
            
            // get the Token which matches it 
            AnnotationSet tokens = getAllTokensInSentence(cannot);
            AnnotationSet tokensubset = tokens.get(cannot.getStartNode().getOffset().longValue(), 
                        cannot.getEndNode().getOffset().longValue());
            Annotation token = tokensubset.iterator().next();
            Annotation head = getHeadToken(token, tokens);
            
      
           // Create a new FeatureMap and add new features 
            // annotation;  remove the old one and add the new one
            FeatureMap features = createUpdatedFeatures(cannot,"LPFPerson",  "resolved_person"); 
            if (features.get("gender") == null)
                features.put("gender", "unknown");
            CandidateAnnotation resolvedPerson = new CandidateAnnotation(
                cannot, "Unknown", "Person", features);
            resolvedPersonList.add(resolvedPerson);
        }  
        return resolvedPersonList;
      /*      // Check if it overlaps with a previous Person annotation. If so, remove the 
           // shorter one and keep the longer one.
           AnnotationSet  existing = outputAS.get("Person", startOffset, endOffset);
           boolean addAnnot = true;
           if (existing.size() > 0)
           {
               String newText = docContent.substring(startOffset.intValue(), endOffset.intValue());
               Iterator <Annotation> it = existing.iterator();
               while (it.hasNext())
               {
                   Annotation curAnnot = it.next();
                   String curText = getText(curAnnot);
                   if (curText.equalsIgnoreCase(newText) || curText.contains(newText))
                   {
                       addAnnot = false;
                       break;
                   }
                   else if (newText.contains(curText))          // a subset: remove the smaler one
                        outputAS.remove(curAnnot);      // remove the original "unknown" annotation   
               } // end-while
           }
           if (!addAnnot)
               continue;

          
              // create a new Annotation and add to output list
            try
            {
                // log.info("Removing UNKNOWN annotiation with ID: " + annotId);
                outputAS.remove(cannot);      // remove the original "unknown" annotation       
                outputAS.add(startOffset, endOffset,  "Person", features);       
                //og.info(">> Changed annotation \"" + textStr +"\" with  ID " + annotId + " from Unknown to Person");    
               //reinitSets();
            }
            catch (Exception e)
            {
                log.error("Error changing UNKNOWN annotation to Person", e);
               e.printStackTrace();
            }
        }   
    }*/
   }
 /*-------------------------------------------------------------------------------------------------*/
// Check if the given annotation may be classified as a person
// Exclude if it is alreadyin a list of terms to be excludes
 //-------------------------------------------------------------------------------------------------*/
    protected boolean isCompatibleAsPerson(Annotation candidate)  
    {
         AnnotationSet tokens = getAllTokensInSentence(candidate);
        // get the token which matches the UNKNOWN token
        AnnotationSet tokensubset = tokens.get(candidate.getStartNode().getOffset().longValue(), 
                    candidate.getEndNode().getOffset().longValue());
        Annotation token = tokensubset.iterator().next();
        
        // Note: This test is not necessary now, since such terms are already annotated as
        // DisasterTerms or Conditions through ANNIE and so not in the "UNKNOWN" list
        String textStr  = ((String)token.getFeatures().get("string")).toLowerCase();
         for (int i = 0; i < NonPersonNouns.length; i++)
        {
            if (textStr.equals(NonPersonNouns[i]))
                return false;
        }
        for (int i = 0; i < AdjecttiveNVerbs.length; i++)
        {
            if (textStr.equals(AdjecttiveNVerbs[i]))
                return false;
        }
        
        String category = (String)token.getFeatures().get("category");
        if (!category.matches("NN|NNP|NNS|NNPS"))
         {
             log.error("Unknown Category for token UNKNOWN: " + candidate.toString());
             return false;
         }
       return true; 
    }
    
    /*----------------------------------------------------------------------------------------------------*/
    // Resolve each entry in the given Lookup set to either a Person or a Location
   // Each entry has the form  [locationLookup, personLookup]
   /*----------------------------------------------------------------------------------------------------*/
   protected  ArrayList<CandidateAnnotation>  resolveAmbiguousAnnotations(
        ArrayList<Annotation[]> ambiguousList)
   {
       if (ambiguousList == null || ambiguousList.size() == 0)
           return null;

       ArrayList <CandidateAnnotation> resolvedAnnotations = new ArrayList();
       for (int i = 0; i < ambiguousList.size(); i++)
       {
           Annotation personAnnot = (ambiguousList.get(i))[0];
           Annotation locationAnnot = (ambiguousList.get(i))[1];
           
          boolean possibleLoc  = this.isParsedAsLocation(locationAnnot);
          boolean possiblePerson  = this.isCompatibleAsPerson(personAnnot);
          
          if (possibleLoc == false && possiblePerson == false)
               continue;
          
          // resolved fully as a location
          else  if (possibleLoc == true && possiblePerson == false)
          {
            FeatureMap newFeatures = createUpdatedFeatures(locationAnnot,"LPFLocation",  "resolved_location");
            CandidateAnnotation  resolvedLocation = new CandidateAnnotation(
                locationAnnot,  "Lookup", "Location", newFeatures);
            resolvedAnnotations.add(resolvedLocation);
          }
          
          // resolved fully as a person
          else if (possibleLoc == false && possiblePerson == true)
          {
            FeatureMap newFeatures = createUpdatedFeatures(personAnnot,"LPFPerson",  "resolved_person");
            CandidateAnnotation resolvedPerson = new CandidateAnnotation(
                personAnnot,  "Lookup", "Person", newFeatures);
            resolvedAnnotations.add(resolvedPerson);
          }
       }
       return resolvedAnnotations;
   }
       
  /*----------------------------------------------------------------------------------------------------*/
    protected FeatureMap createUpdatedFeatures(Annotation originalAnnot, 
        String rule, String rule1)
    {
        FeatureMap origFeatures = originalAnnot.getFeatures();
        FeatureMap features = Factory.newFeatureMap();
        features.put("rule",  rule);    
        features.put("rule1", rule1);    
        features.put("kind", (String)origFeatures.get("kind"));
        return features;
    }

  /*----------------------------------------------------------------------------------------------------*/
       // Check if it overlaps with a previous Person annotation. If so, remove the 
       // shorter one and keep the longer one.
 /*  protected Annotation getLongestMatch(Long startOffset,  Long endOffset)
    {
           AnnotationSet  existing = outputAS.get("Person", startOffset, endOffset);
           boolean addAnnot = true;
           if (existing.size() > 0)
           {
               String newText = docContent.substring(startOffset.intValue(), endOffset.intValue());
               Iterator <Annotation> it = existing.iterator();
               while (it.hasNext())
               {
                   Annotation curAnnot = it.next();
                   String curText = getText(curAnnot);
                   if (curText.equalsIgnoreCase(newText) || curText.contains(newText))
                   {
                       addAnnot = false;
                       break;
                   }
                   else if (newText.contains(curText))          // a subset: remove the smaler one
                        outputAS.remove(curAnnot);      // remove the original "unknown" annotation   
               } // end-while
           }
    }
*/
    /*--------------------------------------------------------------------------------------------------------*/
    // If Person Annoation has components, but not the full name, in the Person List, 
    // create a combined person
    /*--------------------------------------------------------------------------------------------------------*/
    protected AnnotationSet addFullPersons( AnnotationSet personSet)
    {
        
        AnnotationSet fullPersonSet  = new AnnotationSetImpl(document, "temp");
        Iterator <Annotation> it = personSet.iterator();

        while (it.hasNext())
        {
            try
            {
                Annotation person = it.next();
                FeatureMap featureMap = person.getFeatures();
                String rule = (String)featureMap.get("rule");
                String rule1 = (String)featureMap.get("rule1");
                if (rule == null && rule1 == null)
                    continue;
                else if  (!rule.equals("LPFPerson" ) || !rule1.equals("resolved_person"))
                    continue;
                // if they are resolved through dependency tracing , get the data 
                // Find the offsets of this annotation (from Candidate Annotation list)
                AnnotationSet personTokenSet  = inputAS.get("Token", 
                    person.getStartNode().getOffset(), person.getEndNode().getOffset());
               if (personTokenSet == null || personTokenSet.isEmpty())            // TBD: error message, something wrong
               {
                   log.error("Could not find matching token for person, ID = " + person.getId());
                    continue;
               }
               
               // should be only one
               Annotation personToken = personTokenSet.iterator().next();
               int[] nnIds = getTargetTokenIds(personToken, "nn");        // "nn"s as targets to get the full set
               if (nnIds == null || nnIds.length == 0)         // has no leading - itself is the leading or first name
                    continue;

               // get the offsets of the first and last words and combine 
               Long   startOffset = inputAS.get(nnIds[0]).getStartNode().getOffset();
               Long endOffset = personToken.getEndNode().getOffset();
               FeatureMap pf = person.getFeatures();
                // add new features 
                FeatureMap features = Factory.newFeatureMap();
                features.putAll(pf);
                if (features.get("matches") != null) features.remove("matches");
                // update 
                
                int pid  = fullPersonSet.add(startOffset, endOffset, "Person", features);
                System.out.println("-- added full person, ID = " + pid);
            }
            catch (Exception e)         // ignore this object
            {
                log.error("Error creating full person", e);
            }
        }
         // Add the fullPerson to the Person annotations
        Iterator <Annotation> it1 = fullPersonSet.iterator();
        while (it1.hasNext())
            personSet.add(it1.next());
        
        return personSet;
    }
    /*------------------------------------------------------------------------------------------------------*/
    // If a Location annotation is in Person fullname list, keep it as person, remove 
    // from Location list, since often a person's name is also used as a location
    //-------------------------------------------------------------------------------------------------------*/
    protected AnnotationSet removeLocationsFromPersons(AnnotationSet locationSet, 
        AnnotationSet personSet)
    {
        Iterator <Annotation> it = locationSet.iterator();
        while (it.hasNext())
        {
            Annotation  location = it.next();
            Iterator <Annotation>  it1 =  personSet.iterator();
            while (it1.hasNext())
            {
                Annotation person = it1.next();
                if (location.withinSpanOf(person))
                {
                    System.out.println("-- removed duplicate Annotation " + location.getId() + "from Location List");
                     it.remove();
                }
            }
        }
        return locationSet;
    }
        protected void printAnnotations(AnnotationSet as, boolean all)
    {
      /*  ArrayList <Annotation>[] annoationLists = new ArrayList [] {
            allPersonList, allLocationList, lastNameList, lookupList};
        for (int li = 0; li < annoationLists.length; li++)
        {
            ArrayList <Annotation> annotations = annoationLists[li];
            if (annotations == null)
                continue;*/
        
        String excludeTypes = "SyntaxTreeNode|SpaceToken|Dependency";
        String[] matchTypes = { "Token", "FirstPerson", "Lastname",  "Person",  "Location", 
                                                   "Lookup", "Unknown","Condition", "Attribute"};
        
        if (all)
        {
            Iterator <Annotation> it = as.iterator();
            while (it.hasNext() )  // for (int i = 0; i < annotations.size(); i++)
            {
                  Annotation annot = it.next();  //annotations.get(i);
                  String type = annot.getType();
                  if (type.matches(excludeTypes))
                      continue;
                  String text = getText(annot);
                  int offset0 = annot.getStartNode().getOffset().intValue();
                  int offset1 = annot.getEndNode().getOffset().intValue();

                  String str = new String("Type: " +type +  ", Id: " + annot.getId() + ", Offsets: [" + offset0+","+offset1
                     +"] , Text: \"" + text+ "\", " + ", Features: " + annot.getFeatures().toString()); 

                System.out.println(str);
            }
            return;
        }
        
        for (int i = 0; i < matchTypes.length; i++)
        {
            Iterator <Annotation> it = as.iterator();
            while (it.hasNext() )  // for (int i = 0; i < annotations.size(); i++)
            {
                  Annotation annot = it.next();  //annotations.get(i);
                  String type = annot.getType();
                  if (!type.equals(matchTypes[i]))
                      continue;

                  String text = getText(annot);
                  int offset0 = annot.getStartNode().getOffset().intValue();
                  int offset1 = annot.getEndNode().getOffset().intValue();

                  String str = new String("Type: " +type +  ", Id: " + annot.getId() + ", Offsets: [" + offset0+","+offset1
                     +"] , Text: \"" + text+ "\", " + ", Features: " + annot.getFeatures().toString()); 

                System.out.println(str);
            }
        }
    }

      /*----------------------------------------------------------------------------------------------------*/
     protected void   updateOutputAnnotationSet(AnnotationSet newPersonAS, 
           AnnotationSet  newLocationAS)
    {
        // Fist remove all annotations with type Location person Person
         AnnotationSet personAnnotSet =  outputAS.get("Person");
         outputAS.removeAll(personAnnotSet);
         AnnotationSet locAnnotSet =  outputAS.get("Location");
         outputAS.removeAll(locAnnotSet);
         AnnotationSet unknownAnnotSet =  outputAS.get("Unknown");
         outputAS.removeAll(unknownAnnotSet);
         
         // Add new Annotations from the two lists
         String[] types = {"Person", "Location"};
         AnnotationSet[] asSets = {newPersonAS, newLocationAS};
         
         for (int i = 0; i < types.length; i++)
         {
             Iterator<Annotation> iter = asSets[i].iterator();
             while (iter.hasNext())
             {
                 try
                 {
                     Annotation cannot = iter.next();
                    Long start = cannot.getStartNode().getOffset();
                    Long end = cannot.getEndNode().getOffset();
                    outputAS.add(start, end, types[i], cannot.getFeatures());
                 }
                 catch(Exception e)
                 {
                     log.error("Error updating outputAS", e);
                 }
             }  
         } // end for
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "Input Annotation Set Name")
    public void setInputASName(String inputASName) {
        this.inputASName = inputASName;
    }

    public String getInputASName() {
        return inputASName;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "Output Annotation Set Name")
    public void setOutputASName(String outputASName) {
        this.outputASName = outputASName;
    }

    public String getOutputASName() {
        return outputASName;
    }
}

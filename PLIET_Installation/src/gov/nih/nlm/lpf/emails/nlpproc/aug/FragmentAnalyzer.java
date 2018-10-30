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
package gov.nih.nlm.lpf.emails.nlpproc.aug;


import  gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import  gov.nih.nlm.lpf.emails.nlpproc.ner.VerbRules;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;

import  gov.nih.nlm.lpf.emails.regex.PatternMatcher;
import  gov.nih.nlm.lpf.emails.regex.PatternMatcher.Result;
import  gov.nih.nlm.lpf.emails.regex.TextMatch;
import  gov.nih.nlm.lpf.emails.util.NumericWords;
import gov.nih.nlm.lpf.emails.nlpproc.structure.PLSearchInfo;

import gate.AnnotationSet;
import gate.Annotation;
import gate.FeatureMap;

import gate.Document;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class FragmentAnalyzer implements NERConstants
{    
    private static Logger log = Logger.getLogger(FragmentAnalyzer.class);
    
    protected static String pronouns = NLPRelations.pronounSet;

    Document  messageDoc;
    String messageContents;
    AnnotationSet  sentenceAnnots;
    
    ArrayList <Annotation> personAnnots;
    ArrayList <Annotation> locationAnnots;
    ArrayList <Annotation>  attributeAnnots;  
    ArrayList <Annotation> conditionAnnots;  
    

    ArrayList<PLSearchInfo> searchInfo = new ArrayList();

    // constructor
    public FragmentAnalyzer( AnnotationSet fragmentSentences,
        Document messageDoc)
    { 

       messageContents  = messageDoc.getContent().toString();
        
       this.messageDoc = messageDoc;
       this.sentenceAnnots = fragmentSentences;

        this.personAnnots = getAnnotationList(sentenceAnnots, "Person");
        this.locationAnnots = getAnnotationList( sentenceAnnots, "Location");
        this.attributeAnnots = getAnnotationList( sentenceAnnots, "Attribute");    
        this.conditionAnnots = getAnnotationList( sentenceAnnots, "Condition");    
        return;
    }
    
    /*-------------------------------------------------------------------------------------*/
      // constructor for testing - process all sentences in document
    public FragmentAnalyzer( Document messageDoc)
    { 
          this(messageDoc.getAnnotations().get("Sentence"), messageDoc);
    }
    
    /*--------------------------------------------------------------------------------------------------------------------------*/
    protected ArrayList<Annotation> getAnnotationList(AnnotationSet sentenceAnnots, String type)
    {
        ArrayList<Annotation> annotList = new ArrayList();
        AnnotationSet  inputAnnotations = messageDoc.getAnnotations();
        
        Iterator <Annotation> it = sentenceAnnots.iterator();
        while (it.hasNext())
        {
            Annotation sentence = it.next();
            Long start = sentence.getStartNode().getOffset();
            Long end = sentence.getEndNode().getOffset();
            AnnotationSet  myAnnots =inputAnnotations.get(type, start, end);
            if (!myAnnots.isEmpty())
                annotList.addAll(myAnnots);
        }
        return annotList;
    }

    /*-------------------------------------------------------------------------------------*/
      public void processFragments()
      {
         System.out.println("------------------------------------------------------------------------------------------");
         printAnnotations();
      
      log.info("----------------------------------------------------------\n>>> Message " + messageContents);

         ArrayList <Annotation> namedPersons = getNamedPersons(personAnnots);
         if (namedPersons.size() == 1)
         {
             PLSearchInfo pinfo = new PLSearchInfo(namedPersons.get(0));
             fillInfoForSinglePerson(pinfo);
             searchInfo.add(pinfo);
         }
         else if (namedPersons.size() > 1)
         {
             System.out.println("----  selecting person " +namedPersons.get(0) + " from multiple  persons");
             PLSearchInfo pinfo = new PLSearchInfo(namedPersons.get(0));
             fillInfoForSinglePerson(pinfo);
             searchInfo.add(pinfo);
             // personInfo.add(checkInfoForMultiplePerson());
         }
          System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++");
  }
    
  /*--------------------------------------------------------------------------------------------*/
     protected  ArrayList<Annotation> getNamedPersons(ArrayList <Annotation> personAnnots)
     {
         ArrayList<Annotation> namedPersons = new ArrayList();
         for (int i = 0; i < personAnnots.size(); i++)
         {
             Annotation pannot = personAnnots.get(i);
        
           /* The followingfeature "number"  is not relaiable, so don't test for it
              String number = getStringFeature(pannot, "number"); // singular or plural 
            if (!number.equals("singular"))
                continue; 
            */
             
            // TBS: check for an antecedent in feature list
            String personText = getAnnotationText(pannot).toLowerCase();
            if (personText.matches(pronouns))
                    continue;
             
            String rule =getStringFeature( pannot, "rule");   //ule=PersonFinal   
            if (rule != null)
            {  
                if (rule.equals("PersonFinal"))
                {
                      //String rule1 =getStringFeature( pannot, "rule1");
                    String role = getStringFeature( pannot, "role");            // brother, sister, man etc.
                    if (role != null)    // skip this if it is a family relation
                        continue;
                    else
                    {
                        namedPersons.add(pannot);
                        log.info("Found person " + getAnnotationText(pannot));
                    }
                }
            }
            else 
            {
                String majorType  = getStringFeature( pannot, "majorType");
                if (majorType != null && majorType.equals("person_first"))
                {
                    namedPersons.add (pannot);
                }
            }
         }  
         // Check if they refer to the same person (in the matches list. then remove the shorter/duplicate one
         if (namedPersons.size() == 1)
             return namedPersons;
         
          ArrayList<Annotation>  uniquePersons = removeDuplicateNames(namedPersons);
          Collections.sort (uniquePersons , new gate.util.OffsetComparator ());
          // arrange sequentially in order of offset
          
         return uniquePersons;
     }
      /*----------------------------------------------------------------------------------------------------------*/
     protected ArrayList<Annotation>  removeDuplicateNames(ArrayList<Annotation> persons)
     {
          ArrayList<Annotation> uniquePersons = new ArrayList();
          int n = persons.size();
          int[] ids = new int[n];
          int[] lengths = new int[n];
          
          for (int i = 0; i < persons.size(); i++)
          {
              Annotation p = persons.get(i);
              ids[i] = p.getId();
              lengths[i] = p.getEndNode().getOffset().intValue() - p.getStartNode().getOffset().intValue();
          }
          for (int i = 0; i < persons.size(); i++)
          {
              Annotation p = persons.get(i);
              ArrayList<Integer> matches = ( ArrayList)  p.getFeatures().get("matches");
              if (matches == null || matches.size() == 0)
                  uniquePersons.add(p);
              else
              {
                  // find the id of the longest match for this annotation
                   int  index  = getLongestmatch(matches, ids, lengths);
                   Annotation p1 = persons.get(index);
                   if (!uniquePersons.contains(p1))
                       uniquePersons.add(p1);
              }    
          }
         return uniquePersons;         
     }
   /*----------------------------------------------------------------------------------------------------------*/   
    protected int   getLongestmatch(ArrayList<Integer> matches, int[] ids, int[] lengths)
    {
        int length = 0;
        int index = 0;
        for (int i = 0; i < matches.size(); i++)
        {
            int matchedId = matches.get(i).intValue();
            for (int j = 0; j < ids.length; j++)
            {
                if (matchedId != ids[j])
                  continue;
                // matching id - including self
                int newLength = lengths[j];
                if (newLength > length)
                {
                    length = newLength;
                    index = j;
                }
            }
        }
        return index;
    }

     
     /*----------------------------------------------------------------------------------------------------------*/
      // If we get a sinple Person Annotation, assume it to be the reported person 
      //  and get whichever attributes are present
      /*----------------------------------------------------------------------------------------------------------*/
      protected PLSearchInfo fillInfoForSinglePerson(PLSearchInfo pinfo)
      {
          try
          {  
              System.out.println("    ----   Name: "+ getAnnotationText(pinfo.nameAnnot));
              String gender = getStringFeature(pinfo.nameAnnot,  "gender");
              if (gender == null)
              {
                   // look for: original_type=Lookup, minorType=male,
                  String type = getStringFeature(pinfo.nameAnnot,  "original_type");
                  if (type != null && type.equals("Lookup"))
                      gender  = getStringFeature(pinfo.nameAnnot,  "minorType");
              }   
   
              pinfo.gender = (gender == null) ? "" : gender;
              System.out.println("    ----   Gender: "+ gender);

              // check for age attributes 
              if (attributeAnnots != null &&  attributeAnnots.size() > 0)
              {
                  for (int i = 0; i < attributeAnnots.size(); i++)
                  {
                      Annotation attr = attributeAnnots.get(i);
                      String attrType = getStringFeature(attr, "type");
                      if (attrType.equals("age"))
                      {
                        String ageValue = searchForAgeValue(attr);
                        pinfo.age = (ageValue == null) ? "" : ageValue;
                        System.out.println("    ----   Age: "+ ageValue);
                        break;
                      }
                  } 
              }
                // Now check for location
                if (locationAnnots != null && locationAnnots.size() > 0)
                {
                       String location = finalizeLocation(locationAnnots);
                       pinfo.location = location;
                       Collections.sort(locationAnnots, new gate.util.OffsetComparator());
                        pinfo.locations = locationAnnots;  // TBD: resolve to one 
                        String locStr = getAnnotationText(locationAnnots.get(0));
                        for (int i = 1; i < locationAnnots.size(); i++)
                            locStr  +=  " " + getAnnotationText(pinfo.locations.get(i));
                        System.out.println("    ----   Location: " + locStr);
                 } 
                
                // and Condition
                  // Now check for location
                if (conditionAnnots != null && conditionAnnots.size() > 0)
                {
                       String condition = finalizeCondition(conditionAnnots);
                       pinfo.condition = condition;
                       pinfo.conditions = conditionAnnots;  // TBD: resolve to one 
                       System.out.println("    ----   condition: "+ condition);
                 } 
          }
        catch (Exception e)
        {
            log.error("Could not find Person's annotations from message", e);   
        }
        return pinfo;           // only partially build in case of Exception
      }
  /*-----------------------------------------------------------------------------------------------*
    * Search for the age of a person. We search for it within the same sentence.
    * If the attribute text is "age" search forward, as in
    *     (His age is 10 or he is aged 10), but not he is 10 years of age)
    * If the attribute text is "years old" search backwards (He is 10 years old)
    *********************************************************************************/
    protected String searchForAgeValue (Annotation ageAnnot)
    {
        // get the Sentence containing this age annotation
        Annotation sentence = sentenceAnnots.getCovering("Sentence",
        ageAnnot.getStartNode().getOffset(), ageAnnot.getEndNode().getOffset()).iterator().next();

        int sentenceStart =  sentence.getStartNode().getOffset().intValue();
        int  sentenceEnd = sentence.getEndNode().getOffset().intValue();
        String sentenceText = messageContents.substring(sentenceStart, sentenceEnd);

        int ageStart  =  ageAnnot.getStartNode().getOffset().intValue();
        int ageEnd = ageAnnot.getEndNode().getOffset().intValue();
        String age = messageContents.substring(ageStart, ageEnd);

   /*     boolean forwardSearch;
        if (age.toLowerCase().contains("year"))          // as in 10 years old
            forwardSearch = false;
        else 
            forwardSearch = true;      // TBD: check for backward cases too

          String searchStr = "";
          if (forwardSearch) 
              searchStr = sentenceText.substring(ageEnd - sentenceStart);       // relative to sentence start
          else
               searchStr = sentenceText.substring(0, ageStart-sentenceStart);

          //System.out.println ("Searching string:  "+ searchStr + " - for age attribute");
*/
         // search for a number - 1 to 3 digits
        
          String searchStr = sentenceText;
          boolean found = false;
         Result  searchResult = PatternMatcher.getPatternMatches(searchStr,  "[0-9]{1,3}\\W*");
         if (searchResult !=  null)
         {
             // if forward match, get the first one; if backward match, get the last one.
             TextMatch[] matches = searchResult.values;
             if (matches.length > 0)
             {
                 // just get the first one
                 TextMatch match = matches[0];  //  forwardSearch ? matches[0] : matches[matches.length -1];
                 String ageValue = match.text;
                 return ageValue;
             }
         }
         
         //  search each word and convert it to a number. if okay, the value is the age
         String[] words = searchStr.split("\\W+");
         for (int i = 0; i < words.length; i++)
         {
             if ( isInteger(words[i]))          // a numeric value such as  22
                return words[i];
         }  
         
         // Check for a String representation such as: Twenty two
         String ageStr = "";
         for (int i = 0; i < words.length; i++)
         {
             if (words[i].matches("and"))
                 continue;
             
             if (NumericWords.isNumericWord(words[i]))
                 ageStr = ageStr.concat(words[i]).concat(" ");
             else
             {
                 if (ageStr.length() > 0)           // has ended
                    break;
             }
         }
         Integer ageNum = NumericWords.extractNumberInString(ageStr);
         if (ageNum == null)
             return "";         // should not happen
         System.out.println("-- Age value " + ageNum);
         return ageNum.toString();
    }              
                 
   /*------------------------------------------------------------------------------------------------------*
     * Determie the full location from the location annotation
     *----------------------------------------------------------------------------------------------------*/
    protected String finalizeLocation( ArrayList<Annotation>locationList)
    {
        if (locationList.size() == 1)
            return getAnnotationText(locationList.get(0));
        
        int nl = locationList.size();
        int [] ids = new int[nl];
        Long[] startOffsets = new Long[nl];
        Long[] endOffsets = new Long[nl];
       
        for (int i = 0; i < nl; i++)
        {
            ids[i] = locationList.get(i).getId();
            startOffsets[i] = locationList.get(i).getStartNode().getOffset();
            endOffsets[i] = locationList.get(i).getEndNode().getOffset();
        }
        // if IDs are consecutive, combine them after sorting
        boolean consec = false;
         for (int i = 1; i < nl; i++)
         {
             consec =  (ids[i] == ids[i-1]+1) ? true : false;
        }
         if (consec)
         {
             Arrays.sort(startOffsets);
             Arrays.sort(endOffsets);
             String text = messageContents.substring(startOffsets[0].intValue(), endOffsets[nl-1].intValue());
             return text;
         }
         
         // check for street, city, country etc.
         return getAnnotationText(locationList.get(0));
    }   
    
    /*------------------------------------------------------------------------------------------------------*
     * Determie the full location from the location annotation
     *----------------------------------------------------------------------------------------------------*/
    protected String  finalizeCondition( ArrayList<Annotation>conditionList)
    {
      int nc = conditionList.size();
      ArrayList <String> statusValues = new  ArrayList();
       // check for  a negative prior tothe status word
      String negatives = "not|never|last|unable";
 
      for (int i = 0; i < nc;  i++)
      {
          Annotation condAnnot = conditionList.get(i);
          String status = (String)condAnnot.getFeatures().get("status");
          if (status == null) continue;
          
           Annotation sentence = sentenceAnnots.getCovering("Sentence",
                condAnnot.getStartNode().getOffset(), condAnnot.getEndNode().getOffset()).iterator().next();

            int sentenceStart =  sentence.getStartNode().getOffset().intValue();
            int  sentenceEnd = sentence.getEndNode().getOffset().intValue();
            String sentenceText = messageContents.substring(sentenceStart, sentenceEnd);

            int condStart  =  condAnnot.getStartNode().getOffset().intValue();
     
             String searchStr = sentenceText.substring(0, condStart-sentenceStart).toLowerCase();
             Result  searchResult = PatternMatcher.getPatternMatches(searchStr,  "("+negatives+")\\W+");
             boolean negativeCondition = (searchResult != null && searchResult.values.length > 0);
          /*   if ( negativeCondition)   //TBD:  if a single word, fond the matching token and check for nefative
             {
                 TextAnnotation condToken = ???
                 String negStatus = (String)condTokent.getFeatures().get("neg") ;
                 if (negStatus  != null ) status = negStatus;
             } */
             if (negativeCondition) status = searchResult.getMatch(0).text + " " +status;
             statusValues.add(status);
         }
         if (statusValues.isEmpty())
             return null;
         String finalStatus = getHighestPriorityStatus(statusValues);
         return finalStatus;
    } 
 /*--------------------------------------------------------------------------------------------------*
     * The "status" strings here are the health status "category" strings encoded in 
     * in the feature map of the Condition annotations
     */
    protected String getHighestPriorityStatus(ArrayList<String>statusValues)
    {
        String selectedStatus = statusValues.get(0);
        int vcat = VerbRules.getCategoryOrder(selectedStatus);  // highest priority so far
        
         for (int i = 1; i < statusValues.size(); i++)
         {
             String status = statusValues.get(i);
             int cat = VerbRules.getCategoryOrder(status);
             if (cat < vcat)
             {
                 vcat = cat;
                 selectedStatus = status;
             }
         }
         return selectedStatus;
     }

 /*----------------------------------------------------------------------------------------------------------*/ 
  // get the original Gate Annotation for which the given TextAnnotation is the warapper
  /*----------------------------------------------------------------------------------------------------------*/
  protected Annotation getGateAnnotation(TextAnnotation textAnnot, Document gateDoc)
  {
      AnnotationSet inputAS = gateDoc.getAnnotations("InputAS");
      if (inputAS == null)
          return null;           // TBD: error
     int annotId = textAnnot.id;
     Annotation gannot = inputAS.get(annotId);
     return gannot;
  }
  
  
  
      /*--------------------------------------------------------------------------------------------*/         
        public String  getStringFeature(Annotation annot, String feature)
        {
           FeatureMap features = annot.getFeatures();
           if (features == null)
               return null;
           return (String)features.get(feature);
        }
      
     /*--------------------------------------------------------------------------------------------*/   
      protected  String  getAnnotationText(Annotation annot)
      {
          return getAnnotationText(annot, messageContents);
      }
        
      /*--------------------------------------------------------------------------------------------*/         
        protected  String  getAnnotationText(Annotation annot, String docString)
        {
            int start = annot.getStartNode().getOffset().intValue();
            int end = annot.getEndNode().getOffset().intValue();
            
            if (start < 0 || end > docString.length())      // note: offsets are 1-based
                return "";
            return docString.substring(start, end);
        }
  /*----------------------------------------------------------------------------------------------------------*/
    public static boolean isInteger(String s)
    {
        try 
        { 
            Integer.parseInt(s); 
        } 
        catch(NumberFormatException e) 
        { 
            return false; 
        }
        // only got here if we didn't return false
        return true;
    }
    
    /*--------------------------------------------------------------------------------------------------------*/
    protected void printAnnotations()
    {
        ArrayList <Annotation>[] annoationLists = new ArrayList [] {personAnnots, locationAnnots, attributeAnnots, conditionAnnots};
        for (int li = 0; li < annoationLists.length; li++)
        {
            ArrayList <Annotation> annotations = annoationLists[li];
            if (annotations == null)
                continue;
            for (int i = 0; i < annotations.size(); i++)
            {
                  Annotation annot = annotations.get(i);
                  String type = annot.getType();
                  String text = getAnnotationText(annot, messageContents);
                  int offset0 = annot.getStartNode().getOffset().intValue();
                  int offset1 = annot.getEndNode().getOffset().intValue();

                  String str = new String("Type: " +type +  ", Id: " + annot.getId() + ", Offsets: [" + offset0+","+offset1
                     +"] , Text: \"" + text+ "\", " + ", Features: " + annot.getFeatures().toString()); 

                System.out.println(str);
            }
        }
    }
 /*----------------------------------------------------------------------------------------------------------*/ 
      public ArrayList <PLSearchInfo> getPersonSearchInfo()
      {
          return searchInfo;
      }
}

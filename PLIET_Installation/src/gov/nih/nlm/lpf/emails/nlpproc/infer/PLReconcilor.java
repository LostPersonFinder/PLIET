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
package gov.nih.nlm.lpf.emails.nlpproc.infer;

import gov.nih.nlm.lpf.emails.nlpproc.TextMessageProcessor;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ProcessedResults;
import gov.nih.nlm.lpf.emails.nlpproc.structure.InferredPerson;
import gov.nih.nlm.lpf.emails.nlpproc.structure.PLSearchInfo;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ReportedPersonRecord;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
//import gov.nih.nlm.lpf.emails.nlpproc.nlp.NounAnchor;

//import  gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve.NameResolver;
import gov.nih.nlm.lpf.emails.nlpproc.ner.PersonRelatives;
import gov.nih.nlm.lpf.emails.nlpproc.aug.TextAnalyzer;
import  gov.nih.nlm.lpf.emails.rule.NameRule;


import gov.lpf.resolve.GeocoderClient;
import gov.lpf.resolve.GeoLocationInfo;

import gate.Annotation;
import gate.FeatureMap;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class PLReconcilor  implements TextMessageProcessor
{
    private Logger log = Logger.getLogger(PLReconcilor.class);
    
    public static String pronouns = "^i(I|we|you||he|she|they)";

   // input data
    protected ProcessedResults processedResults;
    protected String message;
    PLSearchInfo  fragmentRecord;                            // information from fragmented sentences
    PLSearchInfo  messageSearchRecord;
    ArrayList <InferredPerson> inferredPersons;
    InferredPerson selectedPerson;
    
    ArrayList<PLSearchInfo> extraRecords;
        
    public PLReconcilor(ProcessedResults inputResults)
    {
        message = inputResults.messageDoc.getContent().toString();
        message = message.replaceAll("\n\n", ". ");
        processedResults = inputResults;
        inferredPersons = processedResults.inferredPersonList;  
         selectedPerson = selectInferredPerson(inferredPersons);        // select based upon ranking
        
        ArrayList <PLSearchInfo> fragmentInfo = processedResults.fragmentSearchInfo;
            fragmentRecord = (fragmentInfo == null || fragmentInfo.size() == 0) ?
            null : fragmentInfo.get(0);         // TBD : Assume only one for now
        messageSearchRecord =  createMessageSearchRecord();
    }
    
    protected PLSearchInfo createMessageSearchRecord()
    {
        TextAnalyzer  textAnalyzer = new TextAnalyzer( processedResults.messageDoc);
        textAnalyzer.processText();
        ArrayList<PLSearchInfo> plInfo = textAnalyzer.getPersonSearchInfo();
        if (plInfo == null || plInfo.size() == 0)
            return null;
         return plInfo.get(0);  
    }
   
    /************************************************************************************************/
    // select an InferredPerson based ypon their ranking as established by  the PersonRanker
    protected InferredPerson selectInferredPerson(ArrayList <InferredPerson> inferredPersons)
    {
        if (inferredPersons == null || (inferredPersons.isEmpty()))
            return null;
        else if (inferredPersons.size() == 1)
            return  inferredPersons.get(0);
        else
        {
            InferredPerson selectedPerson = inferredPersons.get(0);
            for (int i = 1; i < inferredPersons.size(); i++)
            {
                  InferredPerson nextPerson = inferredPersons.get(i);
                  if (nextPerson.confidenceRanking > selectedPerson.confidenceRanking)
                      selectedPerson = nextPerson;
            }
            return selectedPerson;
        }
    }           
            
 /************************************************************************************************   
    // Reconcile the results of processing through NLP and Annotation search to generate the 
    // final ReportedPerson records and send them to the server.
    // Fill in the event related data from the event list also
  *************************************************************************************************/
    public  void reconcileResults()
    {
         ArrayList <ReportedPersonRecord>  plRecords = new ArrayList();
         ArrayList <ReportedPersonRecord>  inferredPLRecords = new ArrayList(); 
        // First combine results from Full and fragment sentences
         
         boolean hasInferredPerson = (selectedPerson != null );
         boolean hasFragmentInfo = (fragmentRecord != null); 
         
         // case 1: No InferredPersons and no fragment data - simply create one record from message search
         if (!hasInferredPerson &&  !hasFragmentInfo)
         {
             if (messageSearchRecord == null)       // no meaningful data in message
                 ;
             else
             {
                 ReportedPersonRecord plRecord = createPLRecord(messageSearchRecord);
                 plRecords.add(plRecord);
                 inferredPLRecords = null;
             }
        }
         else if (!hasInferredPerson && hasFragmentInfo)
         {
            // create a plRecord from fragment info and combine with messageSearch data
            ReportedPersonRecord plRecord = createPLRecord(fragmentRecord);
            inferredPLRecords.add(plRecord);
        }
         // case 3: inferredPerson records exists (from full sentence processing), but no fragments
        else if (hasInferredPerson &&  !hasFragmentInfo)
        {
            for (int i = 0;   i < inferredPersons.size(); i++)
            {
                ReportedPersonRecord inferredRecord = createPLRecordFromInferredData (inferredPersons.get(i));
                 inferredPLRecords.add(inferredRecord);
            }
        }
        // case 4: most general case, both senttence and fragment analysis resuts exist
        else        // (hasInferredPerson &&  hasFragmentInfo)
        {
             int numIP = inferredPersons.size();
             for (int i = 0; i <numIP; i++)
             {
                  ReportedPersonRecord plRecord = reconcilePerson(numIP, inferredPersons.get(i),  fragmentRecord);
                  inferredPLRecords.add(plRecord);
             }  
         }
         // compare and add missing info from messageSearch info
       
         if (inferredPLRecords != null)
              plRecords  = mergeWithSentenceSearch( inferredPLRecords,  messageSearchRecord);
         
        for (int i =0;   plRecords != null && i < plRecords.size(); i++)
        {
            postProcessRecord(plRecords.get(i));
        }
 
        processedResults.inferredPersonRecords = inferredPLRecords;
        processedResults.plPersonRecords = plRecords;
    }
    
    
    /************************************************************************************************
    *  create a ReportedPersonRecord from search info PL Records
    * ************************************************************************************************/
    protected   ReportedPersonRecord createPLRecord(PLSearchInfo searchRecord)
    {
        ReportedPersonRecord plRecord = new ReportedPersonRecord();
        String name = "";
        if (searchRecord.nameAnnot != null)
            name = getText(searchRecord.nameAnnot);
        plRecord.personName = name;

        plRecord.age = searchRecord.age;
        plRecord.gender = searchRecord.gender;
        plRecord.reportedStatus = searchRecord.condition;
        plRecord.location = searchRecord.location;
        return plRecord;
    }
    
    /************************************************************************************************
    * Combine information from both full and fragment sentences to build the PL Records
    * ************************************************************************************************/
/*    ArrayList <ReportedPersonRecord>  combineAllSentenceInfo(
         ArrayList <InferredPerson> inferredPersons, PLSearchInfo fragmentRecord)
    {      
        ArrayList <ReportedPersonRecord>  plRecords = new ArrayList();
        int numIP =  inferredPersons.size();            // total number of inferred persons
        for (int i = 0; i <numIP; i++)
        {
            ReportedPersonRecord plRecord;  
            if (fragmentRecord != null)
                // same logic as for full message search
                 plRecord = reconcilePerson(numIP, inferredPersons.get(i),  fragmentRecord);
            else
                plRecord = createPLRecordFromInferredData(inferredPersons.get(i));
            if (plRecord != null)
                plRecords.add(plRecord);
        }
        return plRecords;
    }*/
    
 /************************************************************************************************
     * Reconcile analyzed results of "full sentences" for  a person with that of Pattern Search of text
     * This is invoked when there are no fragment sentences
     ***********************************************************************************************/ 
  /* protected ArrayList <ReportedPersonRecord> reconcileFromSentenceSearch
       ( ArrayList <InferredPerson>inferredPLRecords,  PLSearchInfo messageSearchRecord)
   {
       ArrayList <ReportedPersonRecord> plRecords = new ArrayList();
       
       int numIP =  inferredPersons.size();
       for (int i = 0; i < numIP; i++)
       {
           ReportedPersonRecord plRecord = reconcilePerson(numIP, 
               inferredPersons.get(i),  messageSearchRecord);
           plRecords.add(plRecord);
       }
       return plRecords;
   }    
  */
    /************************************************************************************************
     * Reconcile analyzed results of "full sentences" for  a person with that of Pattern Search of text
     * This is invoked after all merging with fragment info is complete.
     * NOTE: 
     ***********************************************************************************************/ 
   protected ArrayList <ReportedPersonRecord> mergeWithSentenceSearch
       ( ArrayList <ReportedPersonRecord>inferredPLRecords,  PLSearchInfo messageSearchRecord)
   {
       ArrayList <ReportedPersonRecord> plRecords = new ArrayList();
       int numIP =  inferredPLRecords.size();
       for (int i = 0; i < numIP; i++)
       {
           ReportedPersonRecord plRecord =   mergePersonInfo( inferredPLRecords.get(i),  
                    messageSearchRecord);
           plRecords.add(plRecord);
       }
       return plRecords;
   }   
        
  /*----------------------------------------------------------------------------------------------------*/  
   // Combine the informations from an InferredPersonRecord (from "full" sentence
   // analysis  with that of  Pattern search records (either fragments ot full message)
   /*----------------------------------------------------------------------------------------------------*/
    protected   ReportedPersonRecord  reconcilePerson(int numIP, 
            InferredPerson inferredPerson,  PLSearchInfo searchRecord)
    {
       ReportedPersonRecord plRecord = new ReportedPersonRecord();
       plRecord.confidenceFactor = String.valueOf( inferredPerson.confidenceRanking);
       
       String inferredName = inferredPerson.getName();
       if (isValidData(inferredName, "name") && (!inferredName.matches(pronouns)
          &&  inferredPerson.person.getType() == TextAnchor.NOUN_ANCHOR))
       {
             plRecord.personName = inferredName;
       }
       else
       {
           // set the person name
            String searchName = (searchRecord.nameAnnot == null) ? "" : getText(searchRecord.nameAnnot);
            if (searchName != null)
            {
              searchRecord.name = searchName;
              plRecord.personName = searchName; 
            }
        }

          // set the age
        String age = inferredPerson.getAge();
        if (searchRecord != null)
        {
            if (!isValidData(age, "age") &&  searchRecord.age != null)
                    age = searchRecord.age;
        }
        if (age == null || age == "0")
            age = "unknown";
         plRecord.age = age;

        // set the gender 
        String gender = inferredPerson.getGender();
        if (searchRecord != null)
        {
            if (!isValidData(gender, "gender") && searchRecord.gender.matches("male|female"))
                    gender = searchRecord.gender;
        }
          plRecord.gender = gender;

        String healthStatus = inferredPerson.getStatus();
        if (searchRecord != null)
        {
            if (!isValidData(healthStatus, "healthStatus"))
                healthStatus = searchRecord.condition;
        }
        plRecord.reportedStatus = healthStatus;

        String[] locations  = inferredPerson.getLocations();
        if (locations == null && searchRecord != null)
        {
            String loc = searchRecord.location;
            if (loc != null && loc.length() > 0)
            {
                locations = new String[1];
                locations[0] = loc;
            }
        }
        if (locations != null)
        {
            String location = locations[0];
            for (int i = 1; i < locations.length; i++)  
                location += "; " + locations[i];
             location = location.replaceAll("^(the\\W+)", "");
        }
       
       return plRecord;
    }  
    

/*------------------------------------------------------------------------------------*/
    protected boolean isValidData(String val, String field)
    {
        if (val == null || val.length() == 0)
        return false;
        if (val.toLowerCase().equals("unknown"))
            return false;
        // Check for the field "name:)
        if (field.equals("name"))
        {
            if (PersonRelatives.isRelative(val))
                return false;
        }
         if (field.equals("gender"))
        {
            if (val.matches("either"))
                return false;
        }
        return true;
    }

     /*-----------------------------------------------------------------------------*/  
    protected   ReportedPersonRecord createPLRecordFromInferredData
        (InferredPerson inferredPerson)
    {
        ReportedPersonRecord plRecord = new ReportedPersonRecord();
        String name = inferredPerson.getName();
        plRecord.personName = name;
     
        plRecord.age =  inferredPerson.getAge();
        plRecord.gender = inferredPerson.getGender();
        plRecord.reportedStatus = inferredPerson.getStatus();
        plRecord.confidenceFactor = String.valueOf( inferredPerson.confidenceRanking);
       String[] locations = inferredPerson.getLocations();
         if (locations != null)
        {
            String location = locations[0];
            for (int i = 1; i < locations.length; i++)  
                location += "; " + locations[i];
            plRecord.location = location;
        }
        return plRecord;
    }

   /*----------------------------------------------------------------------------------------------------*/  
   // Combine the informations from an InferredPersonRecord (from f"ull" sentence
   // analysis  with that of  Pattern search records (either fragments ot full message)
    // TBD: Should combine with reconcilePerson
   /*----------------------------------------------------------------------------------------------------*/
    protected   ReportedPersonRecord  mergePersonInfo(
            ReportedPersonRecord reportedPerson,  PLSearchInfo searchRecord)
    {
        if (searchRecord == null)
            return reportedPerson;          // nothing to merge
        
       ReportedPersonRecord plRecord = new ReportedPersonRecord();
       String inferredName = reportedPerson.personName;
        plRecord.confidenceFactor = String.valueOf(reportedPerson.confidenceFactor);
       if (isValidData(inferredName, "name") && (!inferredName.matches(pronouns)))
       {
             plRecord.personName = inferredName;
       }
       else
       {
           // set the person name
            String searchName = (searchRecord.nameAnnot == null) ? "" : getText(searchRecord.nameAnnot);
            if (searchName != null)
            {
              searchRecord.name = searchName;
              plRecord.personName = searchName; 
            }
        }

          // set the age
        String age = reportedPerson.age;
        if (searchRecord != null)
        {
            if (!isValidData(age, "age") &&  searchRecord.age != null)
                    age = searchRecord.age;
        }
        if (age == null || age == "0")
            age = "unknown";
         plRecord.age = age;

        // set the gender 
        String gender = reportedPerson.gender;
        if (searchRecord != null)
        {
            if (!isValidData(gender, "gender") && searchRecord.gender.matches("male|female"))
                    gender = searchRecord.gender;
        }
          plRecord.gender = gender;

        String healthStatus = reportedPerson.reportedStatus;
        if (searchRecord != null)
        {
            if (!isValidData(healthStatus, "healthStatus"))
                healthStatus = searchRecord.condition;
        }
        plRecord.reportedStatus = healthStatus;

        String location  = reportedPerson.location;
        if (location == null && searchRecord != null)
        {
            String loc = searchRecord.location;
            location = loc;
        }
        plRecord.location = location;
        return plRecord;
    }
    
       
 /*-----------------------------------------------------------------------------*/      
    protected String getText(Annotation annot)
    {
        if (annot == null)
            return "";
        return message.substring(annot.getStartNode().getOffset().intValue() , 
            annot.getEndNode().getOffset().intValue());
    }   
/*-------------------------------------------------------------------------------------------------*/
     protected void postProcessRecord(ReportedPersonRecord plRecord)
     {
       decomposeLocation(plRecord);
       postProcessPersonName(plRecord);
       plRecord.emailData = message;
     }
   /*-------------------------------------------------------------------------------------------------*/ 
    protected void postProcessPersonName(ReportedPersonRecord record)
    {
        if (record.personName != null)
        {
             // discard salutations
             record.personName = record.personName.replaceAll("^((?i)(mr|mrs|miss|ms|dr|sri|shri|smt)\\W+)", "");
             String name = record.personName.trim();   
             
             String[] nameSegs = NameRule.getNameComponents(name);      // returns three elements
             record.firstName = nameSegs[0];
             record.lastName = nameSegs[2];     // ignore middle name, if any
        }    
             /* earlier way:
            String[]  nparts = name.split("\\W+");
            String word1 = nparts[0];
            
            // discard the first word of name if it is like son David, ...
            if (PersonRelatives.isRelative(word1))
            {
                int index = word1.length();
                name = name.substring(index);
                name = name.replaceAll("^(\\W+)", "");
                nparts = name.split("\\W+");
            }
            
            int n = nparts.length;
            if (n  == 1)
                record.firstName = name;
            else if (n == 2)
            {
                record.firstName = nparts[0];
                record.lastName = nparts[1];
            }
            else if (n >= 3)
            {
                record.lastName  = nparts[n-1];
                String fn = nparts[0];
               // if (fn.matches("(?i)(mr|mrs|miss|ms|dr|sri|shri|smt)"))       // discard salutations
               //     fn = nparts[1];
                record.firstName = fn;
            } 
        } */  
   }
    protected void decomposeLocation(ReportedPersonRecord  plRecord)
    {
        
        String locstr = plRecord.location;
        if (locstr == null || locstr.length() == 0)
            return; 
        locstr = locstr.replaceAll("^(the\\W+)", ""); 
        plRecord.location = locstr;     // clean up
        String[]  locs = locstr.split(";\\W*");
        int n = locs.length;
        GeocoderClient geovalidator = new GeocoderClient();    
        
        // start from the last location
       ArrayList <GeoLocationInfo> glInfo = new ArrayList();
       String addrString  = "";         // a possible address or an institute, etc.
        for (int i = 0; i < locs.length; i++)
        {
            if (geovalidator.validateLocation(locs[i])  )       // a known Geo location    
                glInfo.add(geovalidator.getGeoLocationInfo());
            else 
                addrString = validateAddress(locs[i]);
        }
        if (addrString != null)
           plRecord.address = addrString;
        // check the other information and combine as necessary
        plRecord.country = ""; plRecord.region = ""; plRecord.city = "";
        plRecord.lat = ""; plRecord.lng = "";
        
        //first check for the city
       for (int i = 0; i < glInfo.size(); i++)
        {
             GeoLocationInfo ginfo = glInfo.get(i);
             String city = ginfo.city;
             String locality = ginfo.locality;
            if (city  == null ||  city.length() == 0)
                city = locality;
             if (city  != null && city.length() > 0)
            {
                plRecord.city = city;
                plRecord.lat = ginfo.lat;
                plRecord.lng = ginfo.lng;
                plRecord.region = ginfo.region;
                plRecord.country = ginfo.country;
                break;
            }
        }
       // next match the region
        if (plRecord.region == "")
        {
            for (int i = 0; i < glInfo.size(); i++)
            {
                GeoLocationInfo ginfo = glInfo.get(i);
                String region = ginfo.region;
                if (region  != null && region.length() > 0)
                {
                    plRecord.region = region;
                    if (plRecord.lat == "")
                    {
                        plRecord.lat = ginfo.lat;
                        plRecord.lng = ginfo.lng;
                    }
                }
                break;
            }
        }
         if (plRecord.country == "")
        {
            for (int i = 0; i < glInfo.size(); i++)
            {
                GeoLocationInfo ginfo = glInfo.get(i);
                String country = ginfo.country;
                if (country  != null && country.length() > 0)
                {
                    plRecord.country = country;
                    if (plRecord.lat == "")
                    {
                        plRecord.lat = ginfo.lat;
                        plRecord.lng = ginfo.lng;
                    }
                }
                break;
            }
        }  
          System.out.println("-->Geovalidation  city: " + plRecord.city + ", region " 
              + plRecord.region + ", country " + plRecord.country + ", lat : "+ plRecord.lat + ", long = " + plRecord.lng);
        }
                    
           /*      if (locality != null)
                 {
                     if (plRecord.region != null)
                         plRecord.region= locality;
                     int index = locality.indexOf("City of");
                     if (index >= 0)
                         plRecord.city = locality.substring(index+ "City of".length());
                     else
                         plRecord.city = locality;
                 }    */ 
   /*              if ( plRecord.country == null && country != null)
                     plRecord.country = country;
                 
                 if (plRecord.country != null &&  plRecord.city != null)
                     break;
            }
            if (plRecord.country == null &&  plRecord.city == null && locstr != null)
                plRecord.address = locstr;
        }*/
    
    protected String validateAddress(String addr)
    {
        // find the matching Annotation
        ArrayList<TextAnnotation> locations = processedResults.locationList;
        for (int i = 0; i < locations.size(); i++)
        {
            TextAnnotation loc = locations.get(i);
            if (loc.text.equalsIgnoreCase(addr) || loc.text.contains(addr))
            {
                // check the features
                FeatureMap features = loc.features;
                if (features.get("rule1") != null && ((String)features.get("rule1")).contains("Street"))
                    return loc.text;
                else if (features.get("original_type") != null && ((String)features.get("original_type")).equals("Lookup"))
                {
                    if (features.get("majorType") != null && ((String)features.get("majorType")).equals("loc_key"))
                        return loc.text;
                }
            }   
        }
        // no known location, simply check if starts with a number
        String[] words = addr.split("\\W+");
        if (words.length > 0 && words[0].matches("[0-9]+"))
                return addr;
        return "";
    }
        
    /***************************************************************************************/        
    public ProcessedResults getProcessedResults()
    {
        return processedResults;
    }
}

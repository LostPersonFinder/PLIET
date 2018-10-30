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


import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 *
 * @author 
 */
public class ReportedPersonRecord
{
    public static String[] reportingProperties = {
        "hasName", "hasLocation", "hasAddress", "hasGender",
        "hasAgeGroup", "hasReporter", "hasNote"};

    public String firstName;
    public String lastName;
    //
    public String reportedStatus;
    public String gender;
    public String age;
    //
    public String address;
    public String city;
    public String region;
    public String country;
    public String lat;
    public String lng;
    public String emailData;             // email text and headers

    // parsed fields
    public String personName;
    public String location;
    public String[] statusVerbs;     // time order list of status verbs from which the final status was deriived
    public String reportingPerson;
    public String eventShortname;          // short name of PL event
    public String confidenceFactor;

    public ReportedPersonRecord()
    {
        confidenceFactor = "Unknown";
    }
    
    
    public LinkedHashMap <String, String>  geFieldMap()
    {
             // Now send the record  to the server, along with comments.
           ReportedPersonRecord record = this;
            LinkedHashMap<String, String> personInfo = new LinkedHashMap();
            personInfo.put  ("firstName", record.firstName);
            personInfo.put("lastName", record.lastName );
            personInfo.put("gender", record.gender );
            personInfo.put("age", record.age );
            personInfo.put("address", record.address);
            personInfo.put("city", record.city); 
            personInfo.put("region", record.region);
            personInfo.put("country", record.country);
            personInfo.put("lat", record.lat);
            personInfo.put("lng", record.lng);
            personInfo.put("reportedStatus", record.reportedStatus);
            personInfo.put("emailData", record.emailData);
            personInfo.put("eventShortname", record.eventShortname);
            personInfo.put(">>NLP ConfidenceFactor", record.confidenceFactor+" out of 100");

           return personInfo;
    }
    
    // get the name of the event for wfich this person should be reported
    public String getEventName()
    {
        return eventShortname;
    }

    public boolean hasMultiplePersons()
    {
        return false;       // to be implemented: check for conjunctions in reported name
    }
    public String toString()
    {
        String s = "";
        LinkedHashMap<String, String> personInfo = this.geFieldMap();
        Iterator <String> it = personInfo.keySet().iterator();
        while (it.hasNext())
        {
            String key = it.next();
            String value = personInfo.get(key);
            if (value == null) value = "";
             s += (key+": "+value+"\n");
        }
        return s;
    }


}

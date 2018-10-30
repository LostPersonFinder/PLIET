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
package gov.nih.nlm.lpf.emails.nlpproc.ner;

/**
 *
 * @author 
 */
public  interface  LPFVerbs
{
    
     public static   int REPORTING_VERB = 1;
     public static   int HEALTH_STATUS_VERB = 2;
     public static   int  AMBIGUOUS_VERB = 3;      // unresolved, could be either 1 or 2
     public static   int UNKNOWN_VERB = 4;

    // Tense of a Verb
    public static int PAST_TENSE = 1;
    public static int PRESENT_TENSE = 2;
    public static int PROJECTED_TENSE = 3;         // not yet known definitely
    public static int UNKNOWN_TENSE = 0;
    
    public static String[] VerbTense = { "UNKNOWN", "PAST", "PRESENT", "PROJECTED"};

     // Verb classification according to tense: Reference: PennTreebank Parser
     public static String[] BASE_VERB={"VB"};
     public static String[] PRESENT_TENSE_VERB  =  { "VBP", "VBZ", "VBG"}; 
     public static String[] PAST_TENSE_VERB  = {"VBD"};
     public static String[] PAST_PARTICIPLE_VERB  = { "VBN" };  // should  have an auxillary
     
      //---------------------------------------------------------------------------------------------/ 
     // indicates a projected happening such as a possibility, probability, ability, necessity 
     public static String[] PROJECTION_LIST = {"will", "would", "may", "might", "shall", "should", "can", "could"};
     
     public static String MODAL = "MD"; 
     //---------------------------------------------------------------------------------------------/  
     
         //------------------------ constants ---------------------------------------

       public static String[]  VERB_CATEGORY_NAMES = {
             "REPORTING_VERB",  "HEALTH_STATUS_VERB", 
                 "AMBIGUOUS_VERB", "UNKNOWN_VERB"
       };
  
     
    public static String ALIVE = "Alive and Well";
    public static String DECEASED = "Deceased";
    public static String FOUND = "Found";
    public static String INJURED = "Injured";
    public static String MISSING = "Missing";
    public static String UNKNOWN = "Unknown";
    public static String INDETERMINED = "Indetermined";
    public static String ENQUIRY = "Enquiry";       // for Reporting - not used
    
    // TBD: Find what is the real order
 /*   public String[] StatusWeightOrder =  {ENQUIRY,
        DECEASED, ALIVE, INJURED, FOUND, MISSING, UNKNOWN, INDETERMINED
    };*/
        public String[] StatusWeightOrder =  {
        DECEASED,  INJURED, MISSING, ALIVE, FOUND, UNKNOWN, ENQUIRY,INDETERMINED
    };
    
    // Person's Health status condition implied by the Lexicon entry
    // Store constant High level String values from LPFVerb
    public static String[][] HEALTH_CONDITIONS = {
        {"ALIVE", ALIVE},
        {"DECEASED",  DECEASED},
        {"FOUND",  FOUND},
        {"INJURED",  INJURED},
        {"MISSING",  MISSING},
        {"UNKNOWN", UNKNOWN},
        {"INDETERMINED",  INDETERMINED},
        {"ENQUIRY", ENQUIRY}
    };
      //---------------------------------------------------------------------------------------------/     
/*
    public static String[] AliveAndWellStatus = {
        "alive",  "fine",  "live", "living", "okay", "ok", "o.k.", "safe",  "well", "uninjured", 
        "unhurt", "recovered", "make", "released",
       };      //alive and well
    
      public static String[] FoundStatus = {
        "discovered", "located", "staying", "waiting", "conveyed", "rescued",  "reached", 
        "survived",  "concious","driving", "going", "speak"};      //alive and well
   
    public static String[]MissingStatus = {
        "missing", "lost", "disappear" };      // missing
    
  public static String[] InjuredStatus = {
        "injure", "break", "hurt", "trapped",  "stuck", "wounded",  "unable",  "disabled",   "unconcious",
        "treated", "taken"};  
        
  public static String[] DeceasedStatus = { "died", "dead", "die", "death", "killed", "breathless", "lifeless", 
      "perish", "crush", "decapacitated", "swept", "washed"};
  
  public static String[] UnknownStatus = {
       "unknown",  "seen", "identified" };    //[whereabouts]
  
    // All health status words (including the ones in common lists
    public String[][] HealthStatusWords = new String[][] {
      AliveAndWellStatus, FoundStatus, MissingStatus, InjuredStatus, 
      DeceasedStatus, UnknownStatus
    };
  
  // adjectives indicating health status of a person
  public static String[] AdjecttiveStatusWords = {
      "alive", "well", "fine", "good", "okay", "ok", "o.k.",
      "safe", "able", "capable", "strong", "uninjured", "unhurt", "concious", "unconcious",
      "breathless", "lifeless"
  };  
   
 // Verbs common to both reporter and  reported person, but with
    public static String[] CommonWords = {"has", "have", "be",                 // be=> morpho for is, am, are, ...
     "ask", "find", "seen",  "waiting","contacted", "tell", "need",  "call", "want", "wait", 
      "informed", "said",  "write", "get", "discover", "reply", "speak"
  };
    
   
    // Verbs representing reporting for a PL person
   /* public  static String[] ReportingWords= {
    "look",   "ask",  "search",  "seek",  // for
    "concerned", "worried", "worry", "distressed",  //  [about]
    "know", "heard",   "hope",  "believe",  "pray",   // [to]
    "reported",  "said",  "confirmed", "sure",
     "recieved", "reply"       
};*/

 /*--------------------------------------------------------------------------------------------------
 // Verb/adjective words from reporter indicating various status of  repored person
 // 
 public static String[]  FoundReports = {
           "find", "locate",  "know",  "reach",  "recieve", "talk", "inform", "try", "rescue", "help"};
  public static String[]  MissingReports = {
            "miss", "look", "search"};
  public static String[] AliveAndWellReport = { };
  public static String[] DeceasedReport = { };        
  public static String[] UnknownReport = {
            "unknown",  "look", "search", "ask", "hope",   "pray",   "need",   "concern", "want", "seek",
                "worry", "distress", "know", "want", "try", "wait"};

  // The following words require additional data for resolving the reporter intention
 public static String[] GeneralReports = {
     "believe", "sure", "recieved", "heard", "confirmed", "reply"};
 //public static String[] IndeterminedReports = {"*"};
 
 
 public  static String[][] ReportingWords = {
     FoundReports, MissingReports, AliveAndWellReport, 
     DeceasedReport, UnknownReport, GeneralReports};
     
  /*--------------------------------------------------------------------------------------------------

 // Common words that represent "Health status", depending upon the voice active/passive
 // active and passive voice respectively. (e.g. "We found" vs. "he was found")
 // passive voice for the reported person
    public static String[] PassiveStatusWords = {
      "find", "see", "discover"};

    // Active voice for the reported person 
    public static String[] ActiveStatusWords = {
       "made contact", "contacted", "in touch",  
      "said",  "told", "informed", "write"
    };

  

    // Things a reporter  may be "waiting" for  or "needs"
    public static String[] ReporterObjectWords = {
     "information", "news", "status"};

    // Things a reported  person may be "waiting" for or "needs"
    public static String[] StatusObjectWords = {
    };
    */
};
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



import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexicon.StatusEntry;
import gov.nih.nlm.lpf.emails.nlpproc.ner.PLLexicon.ResolutionEntry;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class PLLexiconReader
{
    private static Logger log = Logger.getLogger(PLLexiconReader.class);
    
    protected static String  Category = "Verb Category";
    protected static String StatusCategory= "HealthStatus Category";
     protected static String UniqueStatusVerb = "Unique_Status_Verbs";
    protected static String AmbiguityResolution = "Ambiguity_Resolution";
    
    protected static String lexiconFileName;
    protected static PLLexicon theLexicon = null;
    

    //-------------------------------------------------------------------------/
    
    public PLLexiconReader(String filePath)
    {
         if (theLexicon != null)
            return;
        //lexiconFileName = filePath;
        initialize(filePath);
    }
    
    protected int initialize(String filePath)
    {
        File file = new File(filePath);
        if (file.exists() && file.canRead())
        {
             lexiconFileName =  filePath;
            return 1;
        }
       else
        {
            log.error("PL  lexicon " +  filePath + " does not exist or not readable");
            return 0;
        }
    }
    
    /*-------------------------------------------------------------------------------------------------*/
    // TBD: We should really compare for filenames to see if it is a new lexicon
    //----------------------------------------------------------------------------------------------------
    public  int readLexicon()
    {
        if (theLexicon != null)
            return 1;
        
         try
        {
            theLexicon = new PLLexicon(lexiconFileName); 
            
            String currentSet = "";     // type of dataset currently being processed
             BufferedReader inputReader = new BufferedReader(new FileReader(lexiconFileName));
            String  lexLine = getNextInputLine(inputReader);
            while (lexLine != null) 
            {
                currentSet = processLexiconLine(currentSet, lexLine);
                lexLine = getNextInputLine(inputReader );
            }
            inputReader.close();
        }
        catch (Exception e)
        {
            log.error("Could not open/read input file: " + lexiconFileName, e);
            return 0;
        }
         log.info("PL  Lexicon created fron file " + lexiconFileName);
        return 1;
    }
   /*------------------------------------------------------------------------------------------------------------*/
    public static PLLexicon getLexicon()
    {
        return theLexicon;
    }
     /*---------------------------------------------------------------------------*/
    // read the next line from the file, ignoring blank lines and comment lines
     protected String getNextInputLine(BufferedReader inputReader  )
     {
         try
         {
             while (true)
             {
                String line =  inputReader.readLine();
                if (line == null)
                    return null;
                else if (line.trim().length() == 0 || line.startsWith( "//") )        // an empty  or comment line
                    continue;       // read the next line
                return line;
             }
         }
         catch (Exception e)
         {
             log.error("Could not read data from inut file", e);
             return null;
         }
     }
            

     /*------------------------------------------------------------------------------------**/
     // Parse and process each line, according to its content
     // Example:   
     //
     protected String  processLexiconLine (String dataType, String textLine)
    {
         String  setType  = dataType;
         
         // ignore comments
         int nc = textLine.indexOf("//") ;
         if (nc >= 0)
             textLine  = textLine.substring(0, nc);
         
         textLine = textLine.replaceAll("^(\\s+)", "").replaceAll("(\\s+)$", "");  // remove leading/traling blanks
         if (textLine.length() == 0)        // an empty or fully comment line
             return setType;
         
         if (textLine.startsWith("*"))              // introducing a different  set of entries
         {
               setType  = getEntryType(textLine);
               return  setType;
         }
         int status = 0;
         if (setType.equals(UniqueStatusVerb))
            status = createLexiconEntry( textLine);
        else if (setType.equals(AmbiguityResolution))   
           status = storeAmbigResolutionInfo(textLine);
        if (status == 1)
          ;        
        else
        {
           log.error(">>> ERROR in PLLexicon, must be corrected for the above line for proper processing <<<");
        }
        return setType;          // error in input data
    }     
/*------------------------------------------------------------------------------------**/
    private String getEntryType(String inputLine)
    {
        String type = inputLine.replaceAll("\\*","");
        return type;
    }
    
 
 /*------------------------------------------------------------------------------------------
     * Create an entry in the lexicon by parsing the given input line.
     * The seven parsed fields, separated by ":"  are: 
     *      Word:POSTag:Morpho:AlternateForms:Category:HealthCondition:NegativeCondition
     * where the trailing fields may be omitted and a field value could be empty.
     * Note each field is validated against static data in the lexicon
    -------------------------------------------------------------------------------------------*/   
    private int createLexiconEntry( String textLine)
    {
        String[] fields = textLine.split(":");
        int n = fields.length;
        int nerr = 0;
        if (n < 5)
        {
            log.error("Incomplete input - " + textLine);
            return 0;
        }
        for (int i = 0; i < n; i++)
            fields[i] = fields[i].replaceAll("^(\\s+)", "").replaceAll("(\\s+)$", "");  // leading/traling blanks
        
         StatusEntry entry  = theLexicon.createStatusEntry();
         entry.entryWord = fields[0].toLowerCase();
         entry.posTag = fields[1];
         if (!entry.posTag.matches("Adjective|Noun|Verb"))
         {
             log.error ("Pos tag must be one of Adjective, Noun, or Verb in line " + textLine);
             nerr++;
         }
         String morphWord = fields[2].toLowerCase();
         if (morphWord.length() == 0)
             morphWord = entry.entryWord;
         entry.morphWord = morphWord;
         entry.alternateForms = fields[3].toLowerCase();
         
         String category = fields[4];
         int catNum = PLLexicon.getCategoryByNumber(category);
         if (catNum == -1)
          {
             log.error ("invalid Verb category in : " + textLine);
             nerr++;
         }
         else
              entry.category = catNum;
         if (n > 5)
         {
             String condition = fields[5];
             if (PLLexicon.getHealthStatusType(condition).equals(""))     // invalid one
             {
                 log.error ("invalid Health condition  category in : " + textLine);
                 nerr++;
             }
             else
                 entry.healthCondition = condition;
         }
          if (n > 6)
         {
             String negCondition = fields[6];
             if (PLLexicon.getHealthStatusType(negCondition).equals(""))     // invalid one
             {
                 log.error ("invalid Negative of Health condition category in : " + textLine);
                 nerr++;
             }
             else
                 entry.negativeCondition = negCondition;
         }
          if (nerr == 0)
              theLexicon.addStatusEntry(entry);
          return (nerr == 0 ? 1 : -1);
    }
    
     //--------------------------------------------------------------------------------------
     // Store the info to resolve ambiguity of a verb with contextual data
    // There are 5 or 6  fields in each entry: 
    //  field 1  -  Entry Identifier: ignored
    //  field 2 - list of '|' separted words for which ambiguation is being resolved
    //  field 3 - criteria being used to resolve ambiguity
    //  field 4 - list of words to match the criteria
    // field 5 - Resolved Verb category
    // field 6 - Resolved health condition (optional)
    // field 7 - condition of word in negative
     //---------------------------------------------------------------------------------------*/
    private int storeAmbigResolutionInfo(String textLine)
    {
        String[] fields = textLine.split(":");
        int n = fields.length;
        int nerr = 0;
        if (n < 5)
        {
            log.error("Incomplete input - " + textLine);
            return 0;
        }
        for (int i = 0; i < n; i++)
            fields[i] = fields[i].replaceAll("^(\\W+)", "").replaceAll("(\\W+)$", "");  // leading/traling blanks
        
         ResolutionEntry  resEntry  = theLexicon.createResolutionEntry();
          resEntry.words = fields[1].split("\\|");
          resEntry.resCriteria = fields[2];
          String[] depList = fields[3].split("\\|");              // depending factor
          
          // now intrerprete the rest of the fields based upon this criteria
          if (resEntry.resCriteria.equalsIgnoreCase("VOICE"))
          {
              resEntry.voice = depList[0].toLowerCase();        // only active or passive
          }
           if (resEntry.resCriteria.equalsIgnoreCase("TENSE"))
          {
              resEntry.tense = depList[0].toLowerCase();        // only past or present
          }
          else if (resEntry.resCriteria.equalsIgnoreCase("OBJECT"))
          {
              resEntry.objects = depList;
          }
           else if (resEntry.resCriteria.equalsIgnoreCase("OBJECT_TYPE"))
          {
              resEntry.objects = depList;
          }
          else  if (resEntry.resCriteria.equalsIgnoreCase("TO_COMPLEMENT"))
          {
              resEntry.complements = depList;
          }
            else  if (resEntry.resCriteria.equalsIgnoreCase("DEFAULT"))
          {
              ;
          }
          // fill in the health category etc.
          int catNum = PLLexicon.getCategoryByNumber(fields[4]);
         if (catNum == -1)
          {
             log.error ("invalid Verb category in : " + textLine);
             nerr++;
         }
         else
             resEntry.resolvedCategory = catNum;
         if (n > 5)
         {
             String condition = fields[5];
             if (PLLexicon.getHealthStatusType(condition).equals(""))     // invalid one
             {
                 log.error ("invalid Health condition  category in : " + textLine);
                 nerr++;
             }
             else
                 resEntry.healthCondition  = condition;
         }
         
          if (n > 6)
         {
             String negCondition = fields[6];
             if (PLLexicon.getHealthStatusType(negCondition).equals(""))     // invalid one
             {
                 log.error ("invalid Negative of Health condition category in : " + textLine);
                 nerr++;
             }
             else
                 resEntry.negativeCondition = negCondition;
         }
          if (nerr ==  0)
              theLexicon.addResolutionEntry(resEntry);
          return (nerr == 0 ? 1 : -1);
    }      
 
}

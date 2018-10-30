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
package gov.nih.nlm.lpf.eval;

//import gov.nih.nlm.lpf.eval.AccuracyScore;
import  gov.nih.nlm.lpf.emails.util.MessageSetIO;
import java.util.Properties;

import java.io.InputStream;
import java.io.FileInputStream;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Iterator;


/**
 *
 * @author 
 *
 * Main class to evaluate the accuracy of PL Email Information Extraction by comparing
 * the extractied results with ground truth data
 * It uses lower level classes, e.g. ScoreEvaluator and AccuracyScore to compute
 * the actual scores for individual cases and fields.
 *
 */
public class PLIETEvaluator
{
    
    protected class PLRecord  extends HashMap <String, String>
    {
        public PLRecord()
        { super();}
        
        
        // convert to PLRecord form
        public PLRecord(HashMap record)
        {
            super();
            this.putAll(record);
        }   
    }
        
    // mapping of field names from external metadata format to internal AME format
    final static String[]  fieldNames = {"name", "healthStatus", "location", "gender"};       // no  "gender", "age" for now

    
    // configurable parameters in the config file
    String referencePath;       // Path with reference results (groung truth)
    String dataStorePath;        // Path with extraction results are stored
    String[] setNames;            // name of files with testSets (without extension of .xnl)
 
    
    //-------------------------------------------------------------------------------------------------*/
    /** Creates a new instance of  PLIET evaluator */
     public PLIETEvaluator (String refPath, String resultsPath, String[] sets)
     {
         referencePath = refPath;
         dataStorePath = resultsPath;
         setNames = sets;
     }
     
     //-------------------------------------------------------------------------------------------------*/
     
     public PLIETEvaluator (String configFile)        
    {
        Properties testProperties = installConfigProperties(configFile);
    }   
  
    protected Properties installConfigProperties(String configFile)
    {
        Properties configProperties = new Properties();
        try
        {
            InputStream is = new FileInputStream(configFile);
            if (is == null)
            {
                System.out.println("Cannot find specified file " + configFile);
                return null;
            }

            // load the properties from the file
            configProperties.load(is);
            is.close();
            
            // retrieve various parameters from this file
            referencePath = (String)configProperties.getProperty("referencePath");
            dataStorePath = (String)configProperties.getProperty("resultsPath");
            setNames = getTestSetNames(configProperties);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            configProperties = null;
        }
        return configProperties;
    }
   /***********************************************************************
    * Get data on all the batches to be processed here
    ************************************************************************/ 
    protected  String[] getTestSetNames(Properties properties)
    {
        String testSets = (String)properties.getProperty("testSets");
        String[] setNames = testSets.split(",");

        return setNames;
    }
    
    
 
    /*********************************************************************
    * Evaluate the results of information  extraction by comparing with the
    * values in the reference set.
    **********************************************************************/
    public void evaluateResultForSet()
    {
        int numSets = setNames.length;
        // In the evaResults: => Key is FieldName, accuracyScore is the score for each field
        LinkedHashMap <String, AccuracyScore> evalResults = new LinkedHashMap();     // preserves item order
        for (int i = 0; i < numSets; i++)
        {  
             String fileName = setNames[i].endsWith(".xml") ?  setNames[i] : setNames[i]+".xml";
            LinkedHashMap<String, PLRecord> referenceRecords = 
                readRecordsFromFile(referencePath, fileName);                   // <K, V> => message Id vs. all values for that message

            String resultsName =  fileName.replace(".xml", "_results.xml"); 
            LinkedHashMap<String, PLRecord> computedRecords = 
                readRecordsFromFile(dataStorePath, resultsName);

          // Returned as FieldName vs. accuracy
            LinkedHashMap <String, AccuracyScore> scoreSet = 
                evaluateResultForSet(referenceRecords, computedRecords);

            Iterator <String> it = scoreSet.keySet().iterator();
            while ( it.hasNext())
            {
                String fieldName = it.next();
                AccuracyScore scores = scoreSet.get(fieldName);      // score for each field in item 
                evalResults.put(fieldName, scores);
            }
        } 
        publishResults(evalResults);
    }
    
  
    /*-----------------------------------------------------------------------------------------------
     * Read the set of messages K =>  message Id, 
     */
     protected   LinkedHashMap<String, PLRecord> readRecordsFromFile(
         String dirPath, String fileName)
     {
         String fileFullPath = dirPath+"/"+fileName;
         
          LinkedHashMap<String, PLRecord> messageSet = new LinkedHashMap();
         // read the  given fields for each message in the file 
         boolean readOnly = true;
         MessageSetIO msgIO = new MessageSetIO(fileFullPath, readOnly);
         while (true)
         {
             String msgId = msgIO.readNextMessage();            // read with ground truth
             if (msgId == null)
                 break;
             HashMap <String, String>  plRecord  = msgIO.getMessageData(fieldNames);
             messageSet.put(msgId, new PLRecord(plRecord));
         }
         return messageSet;
     }
    
   /*********************************************************************
    
    **********************************************************************/
    public  LinkedHashMap <String, AccuracyScore>  evaluateResultForSet(
        LinkedHashMap<String, PLRecord> referenceRecords,
        LinkedHashMap<String, PLRecord> computedRecords)
    {
        HashMap<String, FieldEntries> referenceEntries = 
           arrangeDataByField(referenceRecords);

        HashMap<String, FieldEntries> computedEntries =
            arrangeDataByField(computedRecords);

        ScoreEvaluator evaluator = new ScoreEvaluator(referenceEntries, computedEntries);
        LinkedHashMap<String, AccuracyScore> accuracyScores = evaluator.evaluateAll();

        return accuracyScores;
    }
   /*--------------------------------------------------------------------------------------------------*/
    protected HashMap <String, FieldEntries> arrangeDataByField(
        HashMap<String, PLRecord> records)
    {
        
        HashMap <String, FieldEntries> fieldValueMap = new HashMap();
        ArrayList <String> msgIds = new ArrayList(records.keySet());
        for (int i = 0; i < msgIds.size(); i++)
        {
            String msgId = msgIds.get(i);
            PLRecord plRecord = records.get(msgId);
            // Iterate over fieldnames to get values
            Iterator <String> fieldIt = plRecord.keySet().iterator();
            while (fieldIt.hasNext())
            {
                String fieldName = fieldIt.next();
                String fieldValue = plRecord.get(fieldName);
                if (fieldValue == null || fieldValue.length() == 0)
                    continue;
                FieldEntries entries = fieldValueMap.get(fieldName);
                if (entries == null)        // for the first one
                {
                    entries = new FieldEntries();
                    fieldValueMap.put(fieldName, entries);
                }
                entries.put(msgId, fieldValue);
            }
        }
        return fieldValueMap;
    }
        
 
/*************************************************************************
 * Publish the evaluated scores
 * Skip the fields with the perfect score
 ***************************************************************************/    
     protected void publishResults(HashMap <String, AccuracyScore> evaluationResults)
     {
        Iterator <String> fieldItr =  evaluationResults.keySet().iterator();
        while (fieldItr.hasNext())
        {
            String fieldName = fieldItr.next();
            System.out.println("\n********** Field: "+ fieldName+ " ************");
            AccuracyScore score =  evaluationResults.get(fieldName);
            score.printResult();
            System.out.println("-----------------------------------");
        }
     }    
        
 
    /*************************************************************************/
    public static void main(String[] args)
    {
     /*    if (args.length < 2)
            System.out.println("Please specify the parameter file name, and format");
        
        String configFile = args[0];
        //boolean externalFormat = args[1].equalsIgnoreCase("external");
   */     
        String refPath = "C:/DevWork/LPF/EmailProc/testData/groundTruth/sets";
        String resultsPath = "C:/DevWork/LPF/EmailProc/testData/groundTruth/sets/results";
        String[]  testSets =  {"AttributeTest_tagged"};  // { "AllMessages_Set1"};
        
        PLIETEvaluator  evaluator = 
                new PLIETEvaluator(refPath, resultsPath, testSets);
         evaluator.evaluateResultForSet();       
    }
}

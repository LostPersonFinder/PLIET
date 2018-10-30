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
package gov.nih.nlm.lpf.emails.regex;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

// For tesing only
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 *
 * @author 
 *
 *******************************************************************************
 * A PatternMatcher works with three components:
 *
 * 1 - A text string on which the text is to be performed
 * 2 - A set of cue words which act as keywords (must occur in the text)
 *     for the search to occur. These anchoe words may or may not be 
 *     a part of the matches we are looking for
 * 3 - A set of one or more patterns (regular expressions) to find in the text
 *     The return results value shows the actual String that matched a pattern
 *      corresponding to an anchor. (After matches are found for one anchor, 
 *      it is no more searched with a different anchor.)
 *
 * Component 2 is optional, no cue words may be specified for a match.
 *
 ************************************************************************/
public class PatternMatcher 
{
       /** log4j category */
    private static Logger log = Logger.getLogger(PatternMatcher.class);
    
    
   public static int TERMP_IN_TEXT = 1;
    public static int TERMP_CULLS_TEXT = 2;
    public static int TERMP_FOLLOWS_TEXT = 3;
    
    // Terminator limits the extent of search string, useful for multiple values
    // such as Dates - To be implemented
    public static int TERMP_CULLS_SEARCH = 4;   
    
    
    //holds results of all matches corresponding to a pattern (and cue word, if any)
    public static class Result
    {
        public String keyword;  // cue word that was found in the String
        public TextMatch[] values;  // value of each match
        
        public Result(TextMatch[] val)
        { keyword = null;  values = val;};
        
         public Result(String kw, TextMatch[] val)
        { keyword=kw; values = val;};
        
        public TextMatch getMatch(int index)
        {
            if (index < 0 || index > values.length)
                return null;
            return values[index];
        }
    }
    
    // instance variables for non-static invocations
    protected Pattern p = null;
    protected Matcher m = null;
    protected String str = "";
    protected String[] keywords = null;
    protected String[] terminators = null;
    protected PatternMatcher apm = null;   // for matching anchor words
    
    
    /**************************************************************************
     * Create a PatternMatcher with a given pattern that is used repeatedly
     * The Regular Expression pattern string is compiled for later use
     *************************************************************************/
    
    public PatternMatcher(String stringPattern) 
    {
        try
        {
            p = Pattern.compile(stringPattern);
            m = p.matcher("");
        }
        catch (PatternSyntaxException e)
        {   
            log.error(getSyntaxError(e) +" in pattern:\n  "+ stringPattern );         
            p = null;
        }
    }
    
    // check if the input pattern is a valid one
    public boolean isValidPattern()
    {
        return (p != null);
    }
    
    /************************************************************* 
     * Search for a given RegEx pattern in the input String
     * @arg str => text string to search for the pattern
     * @arg index -> Index within the string to search from
     ************************************************************/
  
    public Result searchPattern(String aStr, int index)
    {
        if (p == null || aStr == null)
            return null;
        
        str = aStr;
        return findMatches(p, str, index, null, -1);
    }
    
    public Result searchPattern(String aStr)
    {
        return searchPattern(aStr, 0);
    }
    
    /****************************************************************************
     * Perform pattern searches if an "anchor" word from a specified set is present
     * Note: this is better than doing an independent pattern match for an anchor word
     * as then we lose the connection between the anchor and the matched string
     * Note: anchor strings are separated by '|' character
     * Returns 1 if valid RegEx anchor string, 0 otherwise
     ****************************************************************************/ 
    
    public int setKeywords(String keywordstr)
    {
        keywords = keywordstr.split("\\|");  // split on |
        apm = new PatternMatcher(keywordstr);
        return (apm.isValidPattern() ? 1 : 0);
    }
    

    /*************************************************************************
     * Search for matching pattern corresponding to a specified anchor
     * @arg firstOnly  indicates if we want only the results for the first anchor 
     * or all specified keywords 
     * @arg past = String to match following the index if the anchor
     * @arg firstOnly - Return after matches found with any one anchor
     ************************************************************************/
    public Result[] searchPatternWithKeywords(String aStr, boolean pastAnchor, 
            boolean firstOnly)
    {
        if (apm == null || aStr == null)
            return null;              // no valid cue words or test string specified
        
        //System.out.println("Test String: " + aStr);
        Result anchorMatch = apm.searchPattern(aStr);
        if (anchorMatch == null)
            return null;                // anchor (cue word) not found
 
        ArrayList v = new ArrayList();
        int numMatches = anchorMatch.values.length;
        for (int i = 0; i < numMatches; i++)
        {
            TextMatch amatch = (anchorMatch.values)[i];
            int sIndex = 0;
            if (pastAnchor)    
                sIndex = amatch.endIndex;
            else
                sIndex = amatch.startIndex;
             
           // now perform the real search for the given pattern
            Result patMatch = searchPattern(aStr, sIndex);
            if (patMatch == null)
                continue;
            
            //for (int j = 0; j < patMatch.numMatches; j++)
            //    System.out.println("Matched text: "+patMatch.getMatch(j).text);
            
            // set the anchor in the result
            patMatch.keyword = amatch.text;
            v.add(patMatch);
            if (firstOnly)
                break;
        }
        
        // check that the same text is not picked up twice (due to two different keywords
        
        if ( v.size() == 0)
            return null;
        
        v = removeDuplicateMatches(v);
        Result[] ra = new Result[v.size()];
        v.toArray(ra);
        return ra;
    }
  
    // Remove duplicate matches in matched results due to matching be several keywords
    public ArrayList removeDuplicateMatches(ArrayList results)
    {
        int ns = results.size();
        if ( ns == 1)
            return results;     // all matches in a result are unique 
       
        // check all matches in a Result with matches in subsequent Result's matches
        for (int i = 0; i < ns; i++)
        {
            Result r1 = (Result) results.get(i);
            for (int j = i+1; j < ns; j++)
            {
                int numMatches = r1.values.length;
               for (int k = 0; k < numMatches; k++)  
               {
                   TextMatch amatch = r1.getMatch(k);
                   Result r2 = removeDuplicate(amatch, (Result)(results.get(j)));
                   results.add( j, r2);
                   if (r2 == null)
                       break;
               }
            }
        }
        // if any of the Result elements become an empty field, delete it
        ArrayList newResults = new ArrayList();
        for (int i = 0; i < ns; i++)
        {
            if (results.get(i) != null)
                newResults.add(results.get(i));
        }
        return newResults;   
    }
    
    // A match is regardes as duplicate if it is equals to or a part
    // of another (previous) match
    protected Result  removeDuplicate(TextMatch amatch, Result results)
    {
        int nm = results.values.length;
        for (int i = 0; i < nm; i++)
        {
            TextMatch tm = results.values[i];
            if (amatch.isEqual(tm) || amatch.text.indexOf(tm.text)>= 0)
            {
                results.values[i] = null;
                nm--;
            }
        }
        if (nm == results.values.length)
            return results;
        
        // create a new result object
        TextMatch[] matches = new TextMatch[nm];
        for (int i = 0, j = 0; i < nm; i++, j++)
        {
            while(results.values[j] == null) j++;
            matches[i] = results.values[j];
        }
        Result newResults = new Result(results.keyword, matches);
        return newResults;
    }
    
    
  /**************************************************************************
   * Written the matching string segments corresponding to the first 
   * anchor that matched in the given string
   *************************************************************************/
   public Result[] searchPatternWithAnchor(String aStr)
   {
       return searchPatternWithKeywords(aStr, false, true);
   }         
          
 
      
/* <<<<<<<<<<<<<<<<<<<<<<<<< Static Methods  >>>>>>>>>>>>>>>>>>>>>>>>>>*/
 //
    /***************************************************************
     * Find matches of a Regular expression  within a String
     * Useful if the pattern is being matched once. Otherwise
     * use a non-static  constructor
     ******************************************************************/
    public static Result getPatternMatches(String textStr, String aPattern)
    {
        Result result = null;
        try
        {        
            Pattern p = Pattern.compile(aPattern);
            result = findMatches(p, textStr, 0, null, 0);
        }
        catch (PatternSyntaxException e)
        {   
            log.error("Pattern matching syntax error", e);
        }
        return result;
    }
    
     /***************************************************************
     * Find matches of a compiled pattern  within a String
     * Useful if the caller uses the same pattern for repeated matching
     ******************************************************************/
    public static Result getPatternMatches(String testStr, Pattern cp)
    {
        Result result = null;
        try
        {        
            result = findMatches(cp, testStr, 0, null, 0);
        }
        catch (PatternSyntaxException e)
        {   
            log.error("Pattern matching sysntax error", e);
        }
        return result;
    }
    
    
   /***************************************************************************
    * Find the index to a pattern in the given string
    * returns -1 if pattern not found
    *************************************************************************/      
    public static int findPatternIndex(String textStr, String aPattern)
    {
        Pattern p = Pattern.compile(aPattern);
        Matcher m = p.matcher(textStr);
        int startIndex = -1;
        if (m.find())           // got at least one match
        {
            startIndex = m.start();
        }
        return startIndex;
    }
    
    
   /*--------------------------------------------------------*/    
    protected static Result findMatches(Pattern p, String aStr)
    {
        return findMatches(p, aStr, 0, null, 0);
    }
 
    
    /******************************************************************************/
    protected static Result findMatches(Pattern p, String inStr, 
            int startIndex, String[] termPatterns, int termType)
    {
        String str = inStr;
        if (startIndex < 0)
            return null;
        
        if (startIndex != 0)
            str = inStr.substring(startIndex);
        
        
        Matcher m = p.matcher(str);
        ArrayList v = new ArrayList();
        while (m.find())
        {
            TextMatch aMatch = new TextMatch(m.group(), m.start()+startIndex, m.end()+startIndex);
            // check if matching strings should be terminated at certain patterns
            if (termPatterns != null && termPatterns[0] != null)
                aMatch = getMatchedSegment(aMatch, termPatterns, termType, inStr);
            
            if (aMatch != null && !isDuplicateMatch(aMatch, v))
            {
                v.add(aMatch);
            }
        }
        if (v.size() == 0)
            return null;                // no match found
        TextMatch[] values = new TextMatch[v.size()];
        v.toArray(values);
        Result result = new Result(values);
        return result;
    }
    
    // check if a the specified match is contained in a ArrayList of Match objects
    protected static boolean isDuplicateMatch(TextMatch amatch, ArrayList oldMatches)
    {
        for (int i = 0; i < oldMatches.size(); i++)
        {
            TextMatch pmatch = (TextMatch)(oldMatches.get(i));
            if ( amatch.isEqual(pmatch))
            {
                System.out.println("Text " + amatch.text +" is a duplicate "
                        + " with match number " + i + " indexes: " 
                        + amatch.startIndex +", " + amatch.endIndex +", " 
                        + pmatch.startIndex +", " + pmatch.endIndex );
                return true;
            }
        }
        return false;
    }
    
    /*---------------------------------------------------------------------*/
    // Modify the match by picking the first segment terminated by termPatterns
    // Note that termPattern may or may not be a part of search Pattern.
    //   If in search Pattern, we split the matched String
    //  Otherwise, we look into the following segment of string for the pattern
    // If no such segment found, return null
    /*----------------------------------------------------------------------*/
    
    public static TextMatch getMatchedSegment(TextMatch aMatch, String[] termPatterns,
                             int termType, String inputText)
    {
       if (termPatterns == null  || termPatterns[0] == null ||
                            termPatterns[0].length() == 0)
                    return aMatch;

       
       if (termType == TERMP_FOLLOWS_TEXT)   // terminating string to follow search pattern
       {
           if (aMatch.endIndex > inputText.length())
           {
               System.out.println("Stop at: " + aMatch.text + ", indexes: "
                       + aMatch.startIndex + ", "+ aMatch.endIndex);
           }
            String remainder = inputText.substring(aMatch.endIndex);
            for (int i = 0; i < termPatterns.length; i++)
            { 
                String regex = "^("+termPatterns[i]+").+";
                if (remainder.matches(regex))
               /* Pattern p = Pattern.compile("^("+termPatterns[i]+")");
                Matcher m = p.matcher(remainder);
                if (m.find())*/
                    return aMatch;
            }
       }
       else if (termType == TERMP_IN_TEXT)   // terminating string delimits search pattern
       { 
            for (int i = 0; i < termPatterns.length; i++)
            {
                //Pattern p = Pattern.compile("("+termPatterns[i]+")$");  // ends in pattern        
                //Matcher m = p.matcher(aMatch.text);
                String regex = ".*+(termPatterns[i])$";
                if (aMatch.text.matches(regex))
                    return aMatch;
            }
       }
       else if (termType == TERMP_CULLS_TEXT)      // terminator to end the matching text
       {
           String textStr = aMatch.text;
           // add sentence boundary detection here, only find the Matches within the current sentence
           // Do not use BreakIterator, does not work....dm 6/17/2008
           //textStr = PatternMatcher.getASentence(textStr);
            
           for (int i = 0; i < termPatterns.length; i++)
           {
               // split the String using the terminating pattern
               // discard starting empty strings
               
               // If more than one segment found, use the first one
               String[] matchStrs = textStr.split(termPatterns[i]);
               if (matchStrs.length == 0)
                   continue;        // how could it happen??
               int start = 0;
               int len = matchStrs.length;
               while(matchStrs[start].length()== 0 && (start < len-1) )
                   start++;
               
                TextMatch newMatch = new TextMatch(matchStrs[start], 
                      aMatch.startIndex, aMatch.startIndex+matchStrs[start].length());
                return newMatch;
           }
       }   
       return null;
    }

    /****************************************************************
    // Get Error w.r.t. Pattern syntax
    *****************************************************************/
    public static String getSyntaxError(PatternSyntaxException e)
    {
        String errorStr = "Regex syntax error: " + e.getMessage ()  +
            "\nError description: " + e.getDescription() +
            "\n index: " + e.getIndex () + 
            "\n Erroneous pattern: " + e.getPattern();
        return errorStr;
    }
    
    //* >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/
     /****************************************************************************
   * main.
   * @param args the command line arguments
   **************************************************************************/
   
    public static void main(String[] args) 
    {
        FileReader inFile;
        BufferedReader in = null;
        try
        {
            inFile = new FileReader(args[0]);
            if (inFile == null)
            {
                System.out.println("Cannot find specified input file " + args[0]);
                System.exit(-1);
            }
            in = new BufferedReader(inFile);       
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        try
        {
            String testType = in.readLine();
            if (testType.equals("1"))   
                performSimpleTest(in);
            else    
                performAnchorTest(in);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
   
    /******************************************************************
     * Perform simple pattern matching tests
     ******************************************************************/
    public static void performSimpleTest(BufferedReader in) throws Exception
    {  
        // read data from the input file and match patterns
        PatternMatcher pm = null;
        
        while(true)
        {
            String pattern = in.readLine();
            if (pattern == null)       // no more patterns
                break;
            if (pattern.startsWith("#"))        // a comment line, skip
                continue;

            pm = new PatternMatcher(pattern);                
            String ns = in.readLine();          // number of test strings for pattern matching
            int numStr =  Integer.parseInt(ns);

            System.out.println("------------------------------\nPattern: "+ pattern);
            for (int i = 0; i < numStr; i++)
            {
                String testStr = in.readLine();
                if (testStr == null)
                    break;
                if (testStr.startsWith("#"))        // a comment line, skip
                    continue;
                int status = matchPattern(pm, testStr);
                if (status == -1)
                    break;
                System.out.println("\n----------------");
            }
        }
    }
    
    protected static int matchPattern(PatternMatcher pm, String testStr)                               
    {
        if (testStr == null)
            return -1;              // no string specified
        System.out.println("Test String: " + testStr);
        try
        {
            Result result = pm.searchPattern(testStr);
            if (result == null)
            {
                System.out.println("No match found");
                return 0;
            }
            
            for (int i = 0; i < result.values.length; i++)
            {
                TextMatch match = result.getMatch(i);
                System.out.println("Matches: " + match.text + "\noffset: " +
                     match.startIndex + " to " + match.endIndex);   
            }
            return 1;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -1;
        }
    }
    
       /******************************************************************
     * Perform simple pattern matching tests
     ******************************************************************/
    
    public static void performAnchorTest(BufferedReader in) throws Exception
    {
       // read data from the input file and match patterns
        PatternMatcher pm = null;
        
        while(true)
        {
            String pattern = in.readLine();
            if (pattern == null)       // no more patterns
                break;
            if (pattern.startsWith("#"))        // a comment line, skip
                continue;
            System.out.println("------------------------------\nPattern: "+ pattern);
            
            // get the begin cue words
            String keywordstr = in.readLine();
            String[] keywords = keywordstr.split("\\|");  // split on |
            String outStr = "";
            for (int i = 0; i < keywords.length; i++)
            {
                outStr += "\""+keywords[i]+"\", ";
            }
            

            System.out.println("keywords: "+ outStr);        
                    
            pm = new PatternMatcher(pattern);                
            String ns = in.readLine();          // number of test strings for pattern matching
            int numStr =  Integer.parseInt(ns);
            if (numStr == -1)  numStr = 1000;        // till the end of file
         
            for (int i = 0; i < numStr ; i++)
            {
                String testStr = in.readLine();
                if (testStr == null)
                    break;
                if (testStr.startsWith("#"))        // a comment line, skip
                    continue;
                int status = matchPatternWithkeywords(pm, keywordstr, testStr);
                if (status == -1)
                    break;
               System.out.println("\n----------------");
            }
        }
    }
 
    /***********************************************************************/
    
      protected static int matchPatternWithkeywords(PatternMatcher pm, 
              String keywordstr, String testStr)  
    {
        if (testStr == null)
            return -1;              // no string specified
        System.out.println("Test String: " + testStr);
                
        try
        {
            int status = pm.setKeywords(keywordstr);
            if (status != 1)
            {
                log.error("Invalid Anchor String");
                return -1;
            }
            Result[] results = pm.searchPatternWithKeywords(testStr, false, true);
            if (results == null)
            {
                System.out.println("No match found");
                return 0;
            }
            
            // print all matches
            for (int i = 0; i < results.length; i++)
            {
                Result result = results[i];
                for (int j = 0; j < result.values.length; j++)
                {
                    TextMatch match = result.getMatch(j);
                    System.out.println("Anchor: " + result.keyword+
                        ", Matches: " + match.text + "\noffset: " +
                        match.startIndex + " to " + match.endIndex);  
                }
                System.out.println("Matched segment: " + testStr.substring(
                        result.getMatch(0).startIndex,
                        result.getMatch(result.values.length-1).endIndex));
            }
            return 1;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -1;
        }
    }
}


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
package gov.nih.nlm.lpf.emails.util;

import java.util.ArrayList;
/**
 *
 * @author 
 */
public class NumericWords
{
    protected static String[]  singleNumbers= {
      "zero", "one", "two", "three", "four" ,"five" ,"six", "seven", "eight", "nine", "ten",
       "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
   
    protected  static String[] decades = {
        "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
    
    public static String[]  higherNumbers = {"hundred", "thousand", "million", "billion"};
    protected  static int[] unitValues = {100, 1000, 1000000, 1000000000};
    
    public static int convertWordToNumber(String word)
    {
        String lword = word.toLowerCase();
        String pword = word.concat("s").toLowerCase();           // in plural form
        for (int i = 0; i < singleNumbers.length; i++)
        {
            if (lword.equals(singleNumbers[i]))
                return i;
        }
        
         for (int i = 0; i < decades.length; i++)
        {
            if (lword.equals(decades[i]))
                return ((i+2)*10);
        }  
         
         // check for hundre
         for (int i = 0; i < higherNumbers.length; i++)
         {
             if (lword.equals(higherNumbers[i]) || pword.equals(higherNumbers[i]))
                 return unitValues[i];
         }
          return -1;
    }
    
    public static boolean isNumericWord(String word)
    {
         int  num = convertWordToNumber(word);
         return (num >= 0) ? true : false;
    }
    
    public static boolean isNumberUnit(int num)
    {
        for (int i = 0; i < unitValues.length; i++)
        {
            if (unitValues[i] == num)
                return true;
        }
        return false;
    }
   
    /*---------------------------------------------------------------------------------------*/
    //  Return the number contained in this string  - starting with the number
    // deals with positive numbers only
    // NOTE:  Works only up to 20999. (one digit before a unit)
    /*---------------------------------------------------------------------------------------*/
    public  static Integer extractNumberInString(String wordString)
    {
        wordString = wordString.replaceAll(" and", " ");
        wordString =  wordString.replaceAll("^(\\W+)", "").trim();
        String[] words = wordString.toLowerCase().split("\\W+");
        ArrayList <Integer> numbers = new ArrayList();
        for (int i = 0; i < words.length; i++)
        {
            int nv = convertWordToNumber(words[i]);
            if (nv == -1)
                break;
            numbers.add(new Integer(nv));
        }
        if (numbers.isEmpty())
            return null;

        int nw  = numbers.size();
        int nvalue = 0;
        int unit = 1;
        for (int i = 0; i < nw; i++)
        {
            int value  =  numbers.get(i);
            if (i < nw-1)
            {
                int nu  =numbers.get(i+1);
                if (isNumberUnit(nu))
                {
                    value = value*nu;
                    i++;
                }      
            }
            nvalue += value;
        }
        return new Integer(nvalue);
    }  

    
    /*---------------------------------------------------------------------------------------------*/
  public static void main(String[] args)
  {
      
      String[] numberStrings = {
          "hundred years",
          "ninety nine",
          "one hundred",
          " five hundred",
          "one hundred and seventy five",
          "Three thousand five hundred ninety years",
          "Twenty  thousand fifty  years",
          "Thirty Three Thousand five hundred ninety years",
          "fifteen",
          "Twenty five years old",
          "One thousand and five hundred",
          "hundred and thirty five",   
          " my name is"};
      
         for (int i = 0; i <  numberStrings.length; i++)
         {
             String s = numberStrings[i];
             Integer nval = NumericWords.extractNumberInString(s);
             if (nval == null)
                 System.out.println ("String: " + s +  "- Invalid number");
             else
                System.out.println ("String: " + s +  "- Number: " + nval.intValue()); 
         }
         System.exit(0);
  }

}

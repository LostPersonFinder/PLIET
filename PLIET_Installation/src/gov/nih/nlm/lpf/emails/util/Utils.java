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
import java.util.Properties;

/**
 *
 * @author 
 */
public class Utils
{
        
 // utility functions   

  //-------------------------------------------------------------------------------------------------
    public static ArrayList<String> createArrayList(String[] words)
    {
        ArrayList<String> alist = new ArrayList();
        for (int i = 0; i <words.length; i++)
        {
            alist.add(words[i]);
        }
        return alist;
    }
    
     //-------------------------------------------------------------------------------------------------
    // Add all words from a set of arrays into a single list
    public static ArrayList<String> createArrayList(String[][] wordSets)
    {
        ArrayList<String> wlist = new ArrayList();
        for (int i = 0; i < wordSets.length; i++)
        {
            wlist.addAll(createArrayList(wordSets[i]));
        }
        return wlist;
    }
          
          
      //-----------------------------------------------------------------------------------------------

     public static  boolean isInList(String word, String[] alist)
     {
         if (alist == null || alist.length == 0)
             return false;
         for (int i = 0; i < alist.length; i++)
         {
             if (alist[i].equals(word))
                 return true;
         }
         return false;
     }    
    /****************************************************************************************************/             
    // Add two integer arrays and return result as an  array
     // which may be the same as one input array if the other one is null or empty
    public static  int[] addIntArrays(int[] a, int[] b)
    {
         if (b == null || b.length == 0)
            return a;
         else if (a == null || a.length == 0)
            return b;

        // both a and b have elements
        int n = a.length + b.length;
        int[] c = new int[n];
        for (int i = 0; i < a.length; i++)
            c[i] = a[i];
        
        int na = a.length;
        for (int j = 0; j < b.length; j++)
            c[na+j] = b[j];
        return c;    
    }  
 /****************************************************************************************************/    
// Add two String arrays and return result as an  array
// which may be the same as one input array if the other one is null or empty
    public static  String[] addStringArrays(String[] sa,  String[] sb)
    {
         if (sb == null || sb.length == 0)
            return sa;
         else if (sa == null || sa.length == 0)
            return sb;

        // both a and b have elements
        int n = sa.length + sb.length;
        String[] sc = new String[n];
        for (int i = 0; i < sa.length; i++)
            sc[i] = sa[i];
        
        int na = sa.length;
        for (int j = 0; j < sb.length; j++)
             sc[na+j] = sb[j];
        return sc;    
    }  
     /*---------------------------------------------------------------------------------------------------------------------------    
    // Dereference a property in the property file that may refer to another environmental type symbol
    // that should also be contained in the same Properies object
    // For example:   configFile = $APP_HOME/config.txt. APPHOME must be also known here
    // The separator is assumed to ne "/"
    *----------------------------------------------------------------------------------------------------------------------*/
    public  static String getDereferencedProperty( Properties properties, String namedProperty)
    {
        String prop = properties.getProperty(namedProperty);
        if (prop == null)
            return null;
        if (!prop.startsWith("$"))
            return prop;
       
        // resolve reference to the environmental type symbol
        int pindex = prop.indexOf("/");
        if (pindex == -1)
           pindex = prop.length();
        String env = prop.substring(1, pindex);
        String envStr = properties.getProperty(env);
        if (env == null)
            return prop;            // could not be resolved; may not be a referenced string
        else            // dereference
        {
            prop = prop.replace("$"+env, envStr);
            System.out.println("Util: Dereferenced " + namedProperty + " as: " + prop );
            return prop;
        }
    }
}

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
package gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author 
 */
public class NameResolver
{
    // Get the anchor from a list which matches with the current anchor
    public static TextAnchor getMatchingName(TextAnchor myAnchor, 
        ArrayList <TextAnchor> anchorSet)
    {
        if (anchorSet == null || myAnchor == null || myAnchor.getType() != TextAnchor.NOUN_ANCHOR)
            return null;
        
        Iterator <TextAnchor> it = anchorSet.iterator();
        while (it.hasNext())
        {
            TextAnchor nextAnchor = it.next();
            if (nextAnchor.equals(myAnchor))            // skip shelf
                continue;
            if (nextAnchor.getType() != TextAnchor.NOUN_ANCHOR)
                continue;
            // compare only the phrase without adjectives and pronouns
            if (  isEquivalentName(myAnchor.getPhraseText(), nextAnchor.getPhraseText()))
                return nextAnchor;
        }
        return null;
    }
     /*-----------------------------------------------------------------------------------------------------------*/    
   // Get the longer of the two anchors - the specified anchor and its  match from a list
    // If there is no match, return the specified anchor
    public static TextAnchor getLongerMatchingName(TextAnchor myAnchor, 
        ArrayList <TextAnchor> anchorSet)
    {
        TextAnchor matchingAnchor = getMatchingName(myAnchor, anchorSet);
        if (matchingAnchor == null)      
            return myAnchor;
       return getLongerName(myAnchor, matchingAnchor);
    }
   /*-----------------------------------------------------------------------------------------------------------*/  
    // Get the longer of the two anchors - the specified anchor and its  match from a list
    // If they are of the same length, return the first refrence (smaller anchor Id)
    /*-----------------------------------------------------------------------------------------------------------*/
    
    public static TextAnchor getLongerName(TextAnchor myAnchor, 
        TextAnchor otherAnchor)
    {
        int len1 = myAnchor.getText().length();
        int len2 = otherAnchor.getText().length();
        if (len1 == len2)
        {
            return (  myAnchor.getAnchorId() < otherAnchor.getAnchorId() ?
                myAnchor : otherAnchor);
        }   
        else
            return (len2 > len1 ) ? otherAnchor : myAnchor;
    }
    
  /*-----------------------------------------------------------------------------------------------------------*/  
    public static boolean isEquivalentName(String name1, String name2)
    {
        name1 = name1.toLowerCase();
        name2 = name2.toLowerCase();
        if (name1.equals(name2))
           return true;
        
       if (isFirstPersonMatch(name1, name2))
                 return true;
       
       // check for matching of all words
       String[]  wset1 = name1.split("\\W+");
       String[]  wset2 = name2.split("\\W+");
       if (intersect(wset1, wset2) == null)      // empty set 
            return true;
       else if (isPartialNameMatch(name1, name2))
           return true;
       
       return false;
    }
 
      /*-----------------------------------------------------------------------------------------------------------*/  
    // get the intersection of two sets of words
    // returns null if all words in one set are in the other and vice versa
    protected static ArrayList<String> intersect (String[] ws1, String[] ws2)
    {
        ArrayList <String> diff = new ArrayList();
        for (int i = 0; i < ws1.length; i++)
        {
            String w1 = ws1[i];
            boolean matching = false;
            for (int j = 0; j < ws2.length; j++)
            {
                String w2 = ws2[j];
                if (w2 != null && w1.equals(w2) )      // a match found
                {
                    //remove w2 from list and go to the next word
                    ws2[j]= null;          // don't check this element again
                    matching = true;     
                    break;
                }
            }
            if (!matching) diff.add(w1);      // for each word in set1
        }
        // add the extra words from set2
          for ( int i = 0; i < ws2.length; i++)
          {
              if (ws2[i] != null)
                  diff.add(ws2[i]);
          }
          return (diff.isEmpty() ? null : diff);
      }
   /*-----------------------------------------------------------------------------------------------------------*/     
    // If  one name is the  first or last name of the other 
    
    protected static boolean  isPartialNameMatch(String name1, String name2)
    {
      // if (name1.contains(name2) || name2.contains(name1))
      //    return true;
           
        // We cannot discard titles, since one may be Mr. and other Mrs.
        String[] titles = {"mr.", "mrs.", "Sr.", "Jr.", "Miss"};
        
       String[]  nw1 = name1.toLowerCase().split("\\W+");
       String[]  nw2 = name2.toLowerCase().split("\\W+");
       if (nw1.length == 0 || nw2.length == 0)
       {
           System.out.println("---name1: " + name1+", name2: " + name2);
           return false;
       }
       
      // TBD: Check for titles, exclude if don't collide
       
       String fw1 = nw1[0], fw2 = nw2[0];
       String lw1 = nw1[nw1.length-1],  lw2 = nw2[nw2.length-1];       // last words
       
       if (fw1.equals(fw2)  && lw1.equals(lw2))            // first and last words match
           return true;
      
       // one name may simply be a first )or last name)
       if (fw1.equals(lw2) || fw2.equals(lw1))
           return true;
       return false;
    }
  
       /*-----------------------------------------------------------------------------------------------------------*/     
     public static TextAnnotation  getMatchingName(TextAnnotation myAnnot, 
         ArrayList <TextAnnotation> annotSet)
    {
        if (annotSet == null)
            return null;
        
        Iterator <TextAnnotation> it = annotSet.iterator();
        while (it.hasNext())
        {
            TextAnnotation nextAnnot = it.next();
            if ( isEquivalentName(myAnnot.text, nextAnnot.text))
                return nextAnnot;
        }
        return null;
    }
     
     /*-----------------------------------------------------------------------------------------------------------*/     
     public static TextAnnotation  getMatchingName(String myName, 
         ArrayList <TextAnnotation> annotSet)
    {
        if (annotSet == null)
            return null;
        
        Iterator <TextAnnotation> it = annotSet.iterator();
        while (it.hasNext())
        {
            TextAnnotation nextAnnot = it.next();
            if ( isEquivalentName(myName, nextAnnot.text))
                return nextAnnot;
        }
        return null;
    }
    
     /*-----------------------------------------------------------------------------------------------------------*/
  // Get the anchor from a list whih matches with the current anchor through an exact match
  // Except for white spaces and case
    public static TextAnchor findExactMatchingName(TextAnchor myAnchor, 
        ArrayList <TextAnchor> anchorSet)
    {
        if (anchorSet == null || myAnchor == null || myAnchor.getType() != TextAnchor.NOUN_ANCHOR)
            return null;
        
        
        // check for matching of all words
        String name1 = myAnchor.getText();
        String[]  wset1 = name1.toLowerCase().split("\\W+");
        Iterator <TextAnchor> it = anchorSet.iterator();
        while (it.hasNext())
        {
            TextAnchor nextAnchor = it.next();
            if (nextAnchor.getType() != TextAnchor.NOUN_ANCHOR)
                continue;
            String name2 = nextAnchor.getText();
           String[]  wset2 = name2.toLowerCase().split("\\W+");
           if (intersect(wset1, wset2) == null)      // empty set 
                return nextAnchor;
        }
        return null;
    }
 /*-----------------------------------------------------------------------------------------------------------*/   
    public static boolean isFirstPersonMatch(String name1, String name2)
    {
        String firstPersonSingular  = "i|me|mine|myself";
        String firstPersonPlural = "we|us|our|ours|ourselves";

        if (name1.matches(firstPersonSingular) && name2.matches(firstPersonSingular) )
            return true;
        else if(name1.matches(firstPersonPlural) && name2.matches(firstPersonPlural) )
            return true;
        return false;
    }
}

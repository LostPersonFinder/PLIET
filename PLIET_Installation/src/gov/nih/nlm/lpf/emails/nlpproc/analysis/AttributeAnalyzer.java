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
package gov.nih.nlm.lpf.emails.nlpproc.analysis;

/**
 *
 * @author 
 */

//import gov.nih.nlm.ceb.lpf.emails.nlp.VerbAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;


//import gov.nih.nlm.ceb.lpf.emails.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class AttributeAnalyzer implements NERConstants
{
    private static Logger log = Logger.getLogger(AttributeAnalyzer.class);
    
     public static String[] AttrKeywords = {
            NAME_ID,  LOCATION_ID, AGE_ID,  MARKS_ID, STATUS_ID};
     public static String[] AttrTypes = {
            NAME_ATTR,  LOCATION_ATTR, AGE_ATTR,  MARKS_ATTR, STATUS_ATTR 
     };
    
     /*---------------------------------------------------------------------------------------------------*/
     
    protected ArrayList<TextAnnotation> attributeList;     // input list of known persons grom external source

    
    
    public AttributeAnalyzer (ArrayList<TextAnnotation>attributes)
    {
        attributeList = attributes;
    }

    /*-------------------------------------------------------------------------------------------------------------
     * get the type of attribute (Name, location, status, etc) represented by this attribute Anchor
     *-----------------------------------------------------------------------------------------------------------*/
    public String    getAttributeType( TextAnchor anchor )
    {
        if (attributeList == null || attributeList.size() == 0)
            return null;              // nor a Person's attribute

        String textStr = anchor.getPhraseText();
        String[] words = textStr.split("\\W+");
        
       String attrWord = "";
        for (int i = 0; i < words.length; i++)
        {
           for (int j = 0; j < attributeList.size() && attrWord.length()  == 0; j++)
           {
               if (words[i].equalsIgnoreCase(attributeList.get(j).text))
                   attrWord = attributeList.get(j).text;
           }
        }
        if (attrWord.length() == 0)
            return  null;          // no match
        
        return getAttributeType(attrWord);
    }
    
    /*--------------------------------------------------------------------------------------------------------------*/
    public  String  getAttributeType(String attribute)
    {
        attribute = attribute.toLowerCase();
        for (int i = 0; i < AttrKeywords.length; i++)
        {
             if (attribute.matches(AttrKeywords[i]))
                 return AttrTypes[i];
        }
        return null;
    }
    /*--------------------------------------------------------------------------------------------------------------*/  
    // Check if a copula object of this anchor corresponds to a personal attribute such as "age"
    // This is usyally obtained through the copular object s of the verb
    // Note: Currently: onlt Age attribute is retrieved this way.
     /*--------------------------------------------------------------------------------------------------------------*/  
    public static  HashMap<String, TextAnchor> getAttributeObjects(VerbAnchor verb )
    {
        TextAnchor[] copula = verb.getCopularObjects() ;
       if ( copula == null)
           return null;
       
       HashMap <String, TextAnchor> attrAnchors = new HashMap();
       for (int i = 0; i < copula.length; i++)
       {
           if (TextAnchor.isCardinal(copula[i].getGovernorToken()))  // He is 10.
               attrAnchors.put( AGE_ATTR, copula[i]);
           else         // check for its adjective: He is 10 years old
           {
              TextAnchor[] npAdject = copula[i].getAdvmodAnchors();
              if (npAdject == null)
                  continue;
              for (int j = 0; j < npAdject.length; j++)
              {
                  if (npAdject[i].getText().toLowerCase().contains(AGE_COMPLEMENT))   // "year";
                      attrAnchors.put(AGE_ATTR, npAdject[i]);
              }
           }
       }  
       return (attrAnchors.isEmpty() ? null : attrAnchors);
    }
 }

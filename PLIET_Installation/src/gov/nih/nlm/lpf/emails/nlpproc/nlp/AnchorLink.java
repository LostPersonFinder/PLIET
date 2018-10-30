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
package gov.nih.nlm.lpf.emails.nlpproc.nlp;

import gov.nih.nlm.lpf.emails.util.Utils;
import java.util.ArrayList;

/**
 *
 * @author 
 */
public class AnchorLink 
{
        // Prepositional Words refering to a location such as town, city or a site
        public static String[] locationKeywords = NLPRelations.LOCATION_PREPS;
        public static String[] personKeywords = NLPRelations.PERSON_PREPS;
        public static String[] commonKeywords = NLPRelations.COMMON_PREPS;

        public String linkType;                     // as per Stanford dependency Parser
        public int governerId;
        public int dependentId;
        public String linkSubtype;               // subtype text of the connecting link the and, but, with, in  etc.
        public String connectType;             // the connection name for indirect targets e.g. prep->pcomp or prep->pobj
        public  TextAnchor dependentAnchor;           // null if no dependents (a leaf Anchor)
        boolean isClausalLink;                  // True if link type denotes a clausal link (ccomp, pcomp, advcl etc.

     
      public AnchorLink(int gid, int did, String type, String subtype) 
     {
        this(gid, did,  type, subtype, "");           // default  same as link type
     }
         
     public AnchorLink(int gid, int did, String type, String subtype, String connection)
     {
         governerId = gid;
         dependentId = did;
         linkType = type;
         linkSubtype = subtype;
         connectType = connection;          // for indirect (two step) links
         isClausalLink  =  isInClausalCategory(linkType);
         
        dependentAnchor = null;     // to be set later
     }
     
     public String getLinkName()
     {
         String name = linkType;
         if (linkSubtype != null)
             name += "_"+linkSubtype;
         return name;
     }
     public String getLinkText()
     {
         return linkSubtype;
     }
     
     public String getDependentText()
     {
            return  dependentAnchor.getTextWithConjunct();
     }
     //--------------------------------------------------------------------------------------------------//
     public boolean isLocationPrep()
     {
         return isMatchingLink(locationKeywords);
     }
        //--------------------------------------------------------------------------------------------------//
     public boolean isPersonPrep()
     {
         return isMatchingLink(personKeywords);
     }
      //--------------------------------------------------------------------------------------------------//
     public boolean isCommonPrep()
     {
         return isMatchingLink(commonKeywords);
     }
     //--------------------------------------------------------------------------------------------------//      
     public boolean isMatchingLink(String[] linkWords)
     {
            if (!linkType.equalsIgnoreCase("prep")  || linkSubtype == null  || linkSubtype.length() == 0)
                return false;
            for (int i = 0; i < linkWords.length; i++)
            {
                if (linkSubtype.equals(linkWords[i]))
                    return true;
            }
            return false;
     }
     //--------------------------------------------------------------------------------------------------//    
     // Get Ids of all annotation comprising this link
     // This recurses over the TextAnchors that constitute the dependents of a link
     public ArrayList <Integer> getCoveringAnnotations()
     {
         ArrayList annots = new ArrayList();
         annots.add(this.governerId);
         annots.add(this.dependentAnchor.getCoveringAnnotations());
         return annots;
     }
     
     /******************************************************************************/
     //                   STATIC  METHODS
     /*****************************************************************************/
    // Determine if a link type implies  a phrasal link (to a word or pharse)
    // or a clausal link (link to the head of a clause)
    // Note: A clause is a set of words containing a subject and a verb as its predicate
  /*****************************************************************************/
     public static boolean  isInClausalCategory(String link)
     {
         if (Utils.isInList(link, NLPRelations.VERB_CLAUSAL_DEPENDENCIES))
             return true;
         
         else if (Utils.isInList(link, NLPRelations.NOUN_CLAUSAL_DEPENDENCIES))
             return true;
         
         else if (Utils.isInList(link, NLPRelations.ADJECT_CLAUSAL_DEPENDENCIES))
             return true;
        
         else if (Utils.isInList(link, NLPRelations.PREP_CLAUSAL_DEPENDENCIES))
             return true;
         
         return false;
     }  
     /*****************************************************************************/
     public static boolean  isPersonPreposition(String linkStr)
     {
        String[] types = linkStr.split("_");
        if (types.length != 2 || !(types[0].equals("prep")))
           return false;
          for (int i = 0; i < personKeywords.length; i++)
           {
                if (types[1].equals(personKeywords[i])) // points to a person
                    return true;
            }
          return false;
     }
   /*****************************************************************************/
     public static boolean  isLocationPreposition(String linkStr)
     {
        String[] types = linkStr.split("_");
        if (types.length != 2 || !(types[0].equals("prep")))
           return false;
          for (int i = 0; i < locationKeywords.length; i++)
           {
                if (types[1].equals(locationKeywords[i]))
                    return true;
            }
          return false;
     }  
     /*****************************************************************************/
     public static boolean  isCommonPreposition(String linkStr)
     {
        String[] types = linkStr.split("_");
        if (types.length != 2 || !(types[0].equals("prep")))
           return false;
          for (int i = 0; i < commonKeywords.length; i++)
           {
                if (types[1].equals(commonKeywords[i]))
                    return true;
            }
          return false;
     }   

 
}
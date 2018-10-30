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

/**
 * @author 
 */

import gov.nih.nlm.lpf.emails.util.Utils;
import java.util.ArrayList;

public class AdjectiveAnchor extends TextAnchor
{  
        protected static String[]  directRelations = null; 

  /****************************************************************************************/
   // local members   
     int tokenId;
     protected  ArrayList<VerbAnchor> xcomps;           //  An ADJP may also have xcomp verbs

    public AdjectiveAnchor( TextAnnotation sentence, TextAnnotation annot, ArrayList <TextAnnotation> tokens)
    {
        super (sentence, annot, ADJECTIVE_ANCHOR);
        setComplementWords( tokens);
        governor = annot;
        xcomps = new ArrayList();
    }
   /****************************************************************************************/   
   protected String[] getDirectRelationTypes()
   {
       return  ADJECT_PHRASAL_DEPENDENCIES; 
   }  
   
   //--------------------------------------------------------------------------------------------------
    // Set the words that are part of a multi-word phrase
    // this is a no-op for adjectives
    //----------------------------------------------------------------------------------------------------
     protected void setComplementWords(ArrayList <TextAnnotation> tokens)
    {
            phraseWords = new TextAnnotation[1];
            phraseWords[0] = leading;      // last word
            governor = phraseWords[0];
        //}
        return;
    }
     
  /**********************************************************************************************/
 // Get the anchor which represents a dependent "location"of this given anchor
  // Algorithm:  It may be a direct link, or indirect one through the conjunction
    /**********************************************************************************************/
    public ArrayList <AnchorLink> getLocationLinks()
    {
        ArrayList <AnchorLink>  locationLinks = super.getLocationLinks();
        if (locationLinks != null)
            return locationLinks;

        // check for location 
        if (conjunctionLinks == null ||  conjunctionLinks.size() == 0)
            return null;
        
        locationLinks = new ArrayList();
        for (int i = 0; i < conjunctionLinks.size(); i++)
        {
            TextAnchor anchor = conjunctionLinks.get(i).dependentAnchor;   
            if (anchor != null &&  anchor.getLocationLinks() != null) 
                locationLinks.addAll(anchor.getLocationLinks());         
         }
        return (locationLinks.size() == 0 ? null : locationLinks);
    }    
   
    /******************************************************************************/
        public void setOtherProperties(ArrayList <TextAnchor> anchors)
    { 
        // Determine if there is an xcomp verb corresponding to this  anchor
       
        TextAnchor[] xcompAnchors = this.getDependentAnchors("xcomp");
        if (xcompAnchors != null && xcompAnchors.length > 0)
        {
            for (int i =0; i < xcompAnchors.length; i++)
            {
                if ( xcompAnchors[i].type == VERB_ANCHOR)
                {
                    xcomps.add( (VerbAnchor) xcompAnchors[i]);
                    System.out.println ("-- Xcomp for " + this.getText() + " is: " + xcompAnchors[i].getText());
                }
            }
        }
    }

}

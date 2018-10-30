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

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class TextAnchor implements NLPRelations, Comparable <TextAnchor>
{
    private static Logger log = Logger.getLogger(TextAnchor.class);
    
    public static int NOUN_ANCHOR = 1;
    public static int VERB_ANCHOR = 2;
    public static int ADJECTIVE_ANCHOR = 3;
    public static int NUMERAL_ANCHOR = 4;
    public static int INVALID_ANCHOR = -1;
    
    public static String cc = CC;       // link to conjunction
    public static String conjunct = CONJ;    // link to Object (direct)
    
   // public static String[] prepLinks = {PREP, PREPC};       // link to prepositions
 
    // common modifiers for all phrase types
    public static String advmod = "advmod";        // an adverbial phrase: e.g. "alive and well"
     public static String  negative  = NEG;
     public static String  temporal = TEMP_MOD;
     public static String prep = "prep";            // 
     public static String poss = "poss";
     public static String determinor = "det";           // "a"
    
    /********************************************************************************/

    protected int type;                                // type of anchor
    protected TextAnnotation sentence;
    protected TextAnnotation leading;     // Annotion for the leading  token, that is first token in a multi-token phrase
    protected TextAnnotation[]  phraseWords;   // Annotation of each word (token) comprising this phrase  
    protected TextAnnotation governor;  // Governing token for various relationships
    protected int anchorId;                         // same as the id of the governor token
    protected String anchorText;              // Expansion of words in text
    
    protected ArrayList< ConjunctionLink> conjunctionLinks = null;
    protected ArrayList <AnchorLink> prepositionLinks;                              // could be more than one
    
    protected AnchorLink negativeLink;    // link showing the nefative relation
    protected AnchorLink tempralLink;     // link showing a time marker
    
    protected HashMap<String, AnchorLink[]> dependencyLinkMap;          // for all other Anchor-specific links
    protected ArrayList<TextAnnotation> tokenSet = null;                               // set of annotation for the entire sentence
    
    protected boolean singular;                                           // contains a single  entiry or multiples (with AND, BUT, OR)    
    protected TextAnnotation negativeAnnot  = null;
    protected TextAnnotation detAnnot  = null;
    /********************************************************************************/

    public TextAnchor(TextAnnotation sentenceAnnot, TextAnnotation leadingAnnot, int anchorType)
    {  
        sentence = sentenceAnnot;
         leading = leadingAnnot;            // leading word of annotation
         type = anchorType;
         
         // for convenince
         governor = leading;          // default
         anchorId = governor.id;
         dependencyLinkMap = new HashMap();
         singular = true;

         //text = annot.text;
    }    
         
   /**************************************************************/
    // static functions 
    //--------------------------------------------------------------------------------/
    // Is a given token a noun? Note: also includes pronouns
    //
      public static  boolean isNoun(TextAnnotation token)
      {
          String cat = (String)token.getFeature("category");
          if (Utils.isInList(cat, NOUN_CATEGORIES))
                  return true;
          return false;
      }
      
      public static  boolean isPersonNoun(TextAnnotation token)
      {
          String cat = (String)token.getFeature("category");
          if (Utils.isInList(cat, PERSON_NOUN_CATEGORIES))
                  return true;
          return false;
      }
      
     //--------------------------------------------------------------------------------/
     // Is a given token a pronoun? Specifically tests for pronouns
      public static  boolean isPronoun(TextAnnotation token)
      {
          String cat = (String)token.getFeature("category");
          for (int i = 0; i < PRONOUN_CATEGORIES.length; i++)
          {
              if (PRONOUN_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
      //--------------------------------------------------------------------------------/     
     // Is a given token a Cardinal number?  Specifically tests for CD
      public static  boolean isNumber(TextAnnotation token)
      {
          String cat = (String)token.getFeature("category");
          for (int i = 0; i < CARDINAL_CATEGORIES.length; i++)
          {
              if (CARDINAL_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
      //--------------------------------------------------------------------------------/     
    
    
    // Is a given token a verb?
      public static  boolean isVerb(TextAnnotation token)
      {
          String cat = (String)token.getFeature("category");
          for (int i = 0; i < VERB_CATEGORIES.length; i++)
          {
              if (VERB_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
      //--------------------------------------------------------------------------------/     
         // Is a given token an adjective?
      public static  boolean isAdjective(TextAnnotation token)
      {
          String cat = (String)token.getFeature("category");
          for (int i = 0; i < ADJECTIVE_CATEGORIES.length; i++)
          {
              if (ADJECTIVE_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
    //--------------------------------------------------------------------------------/     
     // Is a given token  represents a Cardinal number?
      // Yes: only if it is connected to other numbers
      //------------------------------------------------------------------------------/
      public static  boolean isCardinal(TextAnnotation token)
      {
          String cat = (String)token.getFeature("category");
          for (int i = 0; i < CARDINAL_CATEGORIES.length; i++)
          {
              if (CARDINAL_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
//-------------------------------------------------------------------------------//
// Return the anchor type of self
// Note: Prepositional and cardinal words are not built as anchors.
//-------------------------------------------------------------------------------//
    public static int getAnchorType(TextAnnotation token)
    {
        if (isNoun(token))
            return NOUN_ANCHOR;
        else if (isVerb(token))
            return VERB_ANCHOR;
        else if (isAdjective(token))
            return ADJECTIVE_ANCHOR;
        // A NUMERAlL anchor must possess more than one numeric term
       // else if (isCardinal(token))
           // return NUMERAL_ANCHOR;
        else
            return INVALID_ANCHOR;          // not a defined type
    }
    
//-------------------------------------------------------------------------------//
// Return the main (govrning)  token of this anchor
    public TextAnnotation getGovernorToken()
    {
        return governor;
    }

//-------------------------------------------------------------------------------------------------------//
// initialize the Links of other words to this anchor. Consists of  four parts 
// Anchor-specific links are deligated to the derived classes
// >>> NOTE: This function Must be called after the achor is instantiated.
// 1) Get  own auxiliary connections
// 2) Set the Conjunction Links
// 3) Set Preposition Links
// 4) Set other Links
//--------------------------------------------------------------------------------------------------------//
public int  initLinks(ArrayList <TextAnnotation> tokens)
{
    tokenSet = tokens;
    setConjunctionLinks(tokens);
    setPrepositionLinks(tokens);
    
    
    // also set the negativity of the anchor  (such as "not there", "not found", "not a person
      negativeAnnot  = governor.getDependentAnnotation("neg", tokens);
      detAnnot = governor.getDependentAnnotation("det", tokens);
      
     setOtherDependencyLinks(governor, tokens);                  // Anchor type specific
     setCoveringText();
     anchorId = governor.id;
      return 1;
}

//-----------------------------------------------------------------------------------------------
// Find the "words or phrase"  which are connected to this word with a conjuction link
// such as AND, OR, BUT
// A conjuction may start from the leading word (e.g. Evan and Mary Aquino => Evan)
//    or the governer word (Evan Miller and Mary Aquino => Miller)
// The conjunction link may point to another word with a "conj" link from the "cc" link
// or to an independent "advmod link", which we don't consider here
//
/* A conunction dependency  simply adds the series of such  tokens by their  relation type: 
 * a) CC with no CONJ (alive but injured)
 * b) CC, CONJ (John and Alice)
 * c) CONJ, CC, CONJ (Mark, John and Alice)
 * d) CC, CONJ, CC, CONJ (Mark and John and Alice)
 */
    
//--------------------------------------------------------------------------------------------------
    protected void setConjunctionLinks(ArrayList <TextAnnotation> tokens)
    {
        TextAnnotation conjWord = null;
        TextAnnotation conjHead = null;
        ArrayList<TextAnnotation> conjTokens  = governor.getDependentAnnotations(conjunct, tokens);
        
       // conjHead  is the head word (phrasal govornor) to which the conjunctions are attached 
        if (conjTokens != null)     
            conjHead = governor;
        else if (leading != governor)          // check if the leading word is linked to the conjunction as in  Evan and Mary Aquino => Evan
        {
            conjTokens  = leading.getDependentAnnotations(conjunct, tokens);
           if (conjTokens != null)     
                     conjHead = leading;
        }  
        if (conjHead == null)
            return;
        
        // Find the words to which the conj head is linked by a "cc"  relations: could be more than one
        conjunctionLinks = new ArrayList();
        ArrayList<TextAnnotation> ccTokens  = conjHead.getDependentAnnotations(cc, tokens);
        // AND, OR, BUT type conjuctions
        if (ccTokens != null)
        {
            Collections.sort(conjTokens);      
            Collections.sort(ccTokens);

            int mcl = conjTokens.size() -  ccTokens.size();          // number of conjunctiions with missing link types
            String missingLinkType = mcl > 0 ? ccTokens.get(0).text : "";
            for (int i = 0; i < mcl; i++)
            {
                conjWord = conjTokens.get(i);
                ConjunctionLink clink = new ConjunctionLink(anchorId, conjWord.id, cc, missingLinkType);
                conjunctionLinks.add(clink);
            }
            for (int i = 0; i < ccTokens.size(); i++)
            {
                conjWord = conjTokens.get(mcl+i);
                ConjunctionLink clink = new ConjunctionLink(anchorId, conjWord.id, ccTokens.get(i).type,  ccTokens.get(i).text);
                conjunctionLinks.add(clink);
            }  
        }
        else            // not a cc type conjunction
        {
           // if it is an adjective, the relation may be in "advmod"
            conjWord = conjHead.getDependentAnnotation(advmod, tokens);
            if (conjWord != null)
            {
                ConjunctionLink clink = new ConjunctionLink(anchorId, conjWord.id, advmod,  conjWord.text);
                conjunctionLinks.add(clink);
            }
        }
         if (conjunctionLinks.size() > 0)
                singular = false;
         else
             conjunctionLinks = null;           // reset it
         return;
    }
    
 //-----------------------------------------------------------------------------------------------
// Find the words which are connected to this word with a Preposition link
// such as FOR, WITH, ABOUT, IN, NEAR...
// Note: Here we don't follw the link recursively, but find independent links
//
// PREP consists of both prep and prepc, which are stored as prep here,
// since the objects always point to the governor token in  "prepc"   
// 
// >> Note: a Prep link may point to a pobj or a the head of a clause called  "pcomp"
//--------------------------------------------------------------------------------------------------
    protected void setPrepositionLinks(ArrayList <TextAnnotation> tokens)
    {
        ArrayList<AnchorLink> prepositions  = new ArrayList();
        
        int[] pplinkIds = governor.getDependentAnnotationIds(PREP);
        if (pplinkIds != null && pplinkIds.length > 0)
            addPrepositionLinks( prepositions, tokens, pplinkIds, PREP, POBJ); 
          
          prepositionLinks = prepositions;          // store explicitly
         // convert to array             
            AnchorLink[] links = new AnchorLink[prepositions.size()];
            prepositions.toArray(links);
            dependencyLinkMap.put(prep, links);
    }
    
 
    //--------------------------------------------------------------------------------------------------
    // Add the pobj or pcomp objects for this prepositional link "prep" or "prepc"
    //--------------------------------------------------------------------------------------------------
    protected void  addPrepositionLinks(  ArrayList <AnchorLink> prepositions,
                ArrayList <TextAnnotation> tokens, int[] plinkIds, String linkType, String objectType)
    {
        for (int i = 0; i < plinkIds.length; i++)
        {
            TextAnnotation plink = TextAnnotation.getAnnotationById(tokens, plinkIds[i]);
            TextAnnotation prepobj = plink.getDependentAnnotation(objectType, tokens);
            if (prepobj != null)               // possible there is no associated pobject 
            {
                AnchorLink prepLink = new AnchorLink(anchorId, prepobj.id, linkType,  plink.text, objectType);
                prepositions.add(prepLink);
            }
        }   
        return;
    }
    
//--------------------------------------------------------------------------------------------------------//
// return the type if this anchor
//--------------------------------------------------------------------------------------------------------//
public int getType()
{
    return type;
}
//-------------------------------------------------------------------------------//
// Return the id of the anchor ( annotation id of the leading term)
    public int getAnchorId()
    {
        anchorId = governor.id;
        return (anchorId);
    }


//--------------------------------------------------------------------------------------------------------//
// in derived class
protected  String[] getDirectRelationTypes()
{
    return null;
}

 /***************************************************************************************************/
// Get other type of dependent relations associated with this Anchor
  /***************************************************************************************************/
protected void setOtherDependencyLinks(TextAnnotation gov , ArrayList <TextAnnotation> tokens)
    {
         // Id of various tokens related to the governor token, -1 means not found
        //TextAnnotation gov = mainVerb;
        
        String[]  directRelations = getDirectRelationTypes();
         for (int i = 0; i < directRelations.length; i++)
         {
             AnchorLink[] plinks = createAnchorLinks(directRelations[i], gov);
             if (plinks != null)
                dependencyLinkMap.put(directRelations[i], plinks);
         }
           // also add the  temporal links (though usually for Verbs, may also be for nouns/adjectives)
        String[] otherRelations = {temporal};
         for (int i = 0; i < otherRelations.length; i++)
         {
            AnchorLink[] plinks = createAnchorLinks(otherRelations[i], gov);
            if (plinks != null)
                dependencyLinkMap.put(otherRelations[i], plinks);
         }
          return;
    }
   /***************************************************************************************************
   * Create and return the links connecting a governor token to its specified dependents
 * ****************************************************************************************************/
    protected AnchorLink[]  createAnchorLinks(String relation, TextAnnotation governor)
    {
        int[] depIds = governor.getDependentAnnotationIds(relation);      // dependent
        if (depIds == null || depIds.length == 0)            // no such relation
            return null;
        AnchorLink[]  links  = new AnchorLink[depIds.length];
         for (int j = 0; j < depIds.length; j++)
         {
             links[j] = new AnchorLink(governor.id, depIds[j],  relation, null);
         }
         return links;
    }


    //---------------------------------------------------------------------------------------------------------//  
    // Check if an annotation is already included in this anchor, so that we do not
    // create another Anchor for that one.
   //---------------------------------------------------------------------------------------------------------//  
    public boolean containsAnnot(TextAnnotation annot)
    {
        for (int i = 0; i < phraseWords.length; i++)
        {
            if (annot.id == phraseWords[i].id)
                return true;
        }
        // check conjunctions
        
        return false;
    }
    
    
    
   /****************************************************************************************************/         
   public AnchorLink[] getDirectDependencyLinks(String relation)
   {
        return dependencyLinkMap.get(relation);
   }
   
    /***************************************************************************************************/
   public TextAnchor  getConjunct()
   {
       if ( conjunctionLinks == null || conjunctionLinks.size() == 0)
           return null;
       else   
           return conjunctionLinks.get(0).dependentAnchor; 
   }
   
   
 /***************************************************************************************************/
   public String getTextWithConjunct()
   {
       String str = getText();
       if ( conjunctionLinks != null && conjunctionLinks.size() > 0)
       {
           for (int i =0; i < conjunctionLinks.size(); i++)
           {
               ConjunctionLink clink = conjunctionLinks.get(i);
               if (clink.dependentAnchor != null)
                    str += " "+clink.linkSubtype + " " + clink.dependentAnchor.getText(); 
           }
       }
       return str;
   }


/***************************************************************************************************
 * get the full set of annotations (Tokens) creating this anchor
  **************************************************************************************************/
   public TextAnnotation[] getWordSet()
   {
       return phraseWords;
   }

      /******************************************************************************/ 
   // check if a given word is contained within its list of words or conjunctions
   public boolean containsWord(int annotId)
   {
       for (int i = 0; i < phraseWords.length; i++)
       {
           if (phraseWords[i].id == annotId)
               return true;
       }
       if (detAnnot != null && detAnnot.id== annotId)
           return true;
      
       // check conjunction
       //  if (conjunctionLink != null && conjunctionLink.dependentId == annotId)
      return false;  
   }
       
    /******************************************************************************/
    // check if it is a leaf Anchor
   // An anchor is "leaf" if no other Tokens depend upon it; that is
   // it is not the "Governor" of any link
   //------------------------------------------------------------------------------------------------/
   public boolean isLeaf()
   {
       return governor.isLeaf();
   }
   
        
//*******************************************************************************************/
     public boolean isNegative()
    {
        return (negativeAnnot != null);
    }
       public String getNegativeWord()
    {
        return (negativeAnnot != null) ? negativeAnnot.text : null;
    }
       
       
     // Get the list of tokens in the sentence relevant fot getting info for  this anchor
    public ArrayList<TextAnnotation> getTokenSet()
    {
        return tokenSet;
    };
//*******************************************************************************************/
    //--------------------------------------------------------------------------------------------------------
    // This is performed in the second pass where  the target anchors in each Link 
    // are filled, after all Anchors are constructed by the invoker. 
    //----------------------------------------------------------------------------------------------------------
    public void  fillLinkTargetAnchors(ArrayList <TextAnchor> allAnchors)
    {   
       if (isLeaf())           // no links to it
            return;

       // System.out.println("--- Filling target anchors for \"" + this.getText()+"\"");
        // fill for the conjunction Link
        if (conjunctionLinks != null)
        {
            for (int i = 0; i < conjunctionLinks.size(); i++)
            {
                ConjunctionLink conjunctionLink = conjunctionLinks.get(i);
                int depId =conjunctionLink.dependentId;
                for (int j = 0; j < allAnchors.size(); j++)
                {
                    if (allAnchors.get(j).containsWord(depId))          // contains this dependent
                    {
                       conjunctionLink.dependentAnchor = allAnchors.get(j);
                        break;
                    }
                }
                if  (conjunctionLink.dependentAnchor == null)
                {
                    System.out.println("<< Error: Could not find anchor for Conjuct  "+ conjunctionLink.linkType);
                }
            }
        }    

        // Check all  dependency links
        int nd = dependencyLinkMap.size(); 
        if (nd == 0)
            return;
        
      Iterator<String> it = dependencyLinkMap.keySet().iterator();
      while (it.hasNext())
        {
            AnchorLink[] plinks =  dependencyLinkMap.get(it.next());
            if (plinks == null || plinks.length == 0)
                continue;
            // get the dependent anchor for each link of  this link  type
            for (int  i = 0; i < plinks.length; i++)
            {
                int depId =plinks[i].dependentId;
                TextAnchor depAnchor;
                /*if (plinks[i].linkType.equals(COPULA_LINK))
                    depAnchor = getAnchorByPhraseID(allAnchors, depId);     // match the leading word
                 * else */
                    depAnchor = getAnchorByID(allAnchors, depId);
                 if (depAnchor  != null)
                        plinks[i].dependentAnchor = depAnchor;
                 else
                        log.error(" No Anchors found for the Annotation with id  = " + depId +
                     " Link type: " +  plinks[i].getLinkName());

             } // for each link of this type
            
          } // each dependency link type
    }
  
 /*********************************************************************************/  
    public String[]  getAdjectives()
    {
        ArrayList<TextAnnotation> adj = getAdjectiveAnnots();
        if (adj == null || adj.size() == 0)    
           return new String[] {};      // Array of length 0

        String[] adjectives = new String[ adj.size()];
        for (int i = 0; i < adj.size(); i++)
           adjectives[i] = adj.get(i).text;
        
        return adjectives;
    }
     
     /*********************************************************************************/
       public ArrayList<TextAnnotation> getAdjectiveAnnots()
     {
        ArrayList<TextAnnotation> adjectives = new ArrayList();
        TextAnnotation adject = null;
        for (int i = 0; i < adjectiveDeps.length; i++)
        {
            adject = governor.getDependentAnnotation(adjectiveDeps[i], tokenSet);
            if (adject != null)
                adjectives.add(adject);
        } 
        
        int n = adjectives.size();          // do only for the original set
        for (int i = 0; i < n; i ++)
        {
            for (int j = 0; j < adjectiveDeps.length; j++)
            {
                adject = adjectives.get(i).getDependentAnnotation(adjectiveDeps[j], tokenSet);
                if (adject != null)
                    adjectives.add(adject);
            } 
        }

        return adjectives;
     }
       
     
/*********************************************************************************/
    // This is performed in the second phase of Anchor building, after all anchors are 
    // created for a sentence. The relations may refer to other Anchors.
    // To be implemented in the derived class for Anchor-type specific properties
   public void setOtherProperties(ArrayList <TextAnchor> anchors)
   {
       return;
   }
   /*********************************************************************************/
    
    public String toFullString()
    {
        String[] typeText = {"Noun", "Verb", "Adjective"};
        String str = "";
        if (negativeAnnot != null) str = "["+getNegativeWord()+"] ";
        
        str += getTextWithConjunct() +"  Type: " + (type == -1 ? "INVALID "  : typeText[type-1]);
        if (dependencyLinkMap != null)
        {
            Iterator <String> it = dependencyLinkMap.keySet().iterator();
            while (it.hasNext())
            {
                String type = it.next();
                AnchorLink[] plinks = dependencyLinkMap.get(type);
                for (int i = 0; i < plinks.length; i++)
                {
                    str += "\n  Link: " + plinks[i].linkType 
                        +  (plinks[i].linkSubtype == null ? "" :  ", subtype: " + plinks[i].linkSubtype) + ", " 
                        +   (plinks[i].dependentAnchor == null ? "" : plinks[i].dependentAnchor.getFullTextWithConjunct());
                }
            }
        }
        return str;
    }   
    
    /******************************************************************************/
    // Get the dependentAnchor for a specific relation
    // Note: The dependent Anchor may be null if the link points to a Leaf Anchor
    public TextAnchor[] getDependentAnchors(String linkType)
    {
        AnchorLink[]  depLinks = dependencyLinkMap.get(linkType);
         if (depLinks == null ||depLinks.length == 0)
            return null;
         ArrayList <TextAnchor> depAnchorList = new ArrayList();

        for (int i = 0; i < depLinks.length; i++)
        {
            TextAnchor danchor = depLinks[i].dependentAnchor;
            if (danchor != null)
                depAnchorList.add(danchor);
        }
        
        if (depAnchorList.size() == 0)      // no dependent anchor
            return null;
        
        TextAnchor[]  depAnchors = new TextAnchor[depAnchorList.size()] ;
        depAnchorList.toArray(depAnchors) ;
        return depAnchors;
    }
    
     /******************************************************************************/
    // Get the dependentAnchor for a specific relation
    public TextAnchor[] getTypedDependentAnchors(String linkType, String subType)
    {
            AnchorLink[]  depLinks = dependencyLinkMap.get(linkType);
             if (depLinks == null ||depLinks.length == 0)
                return null;
             ArrayList <TextAnchor> subAnchors = new ArrayList();;
             
           
            for (int i = 0; i < depLinks.length; i++)
            {
                if (depLinks[i].linkSubtype.equals(subType))
                    subAnchors.add(depLinks[i].dependentAnchor);
            }
            
            TextAnchor[] subArray = new TextAnchor[subAnchors.size()]; 
            return (subAnchors.toArray(subArray));
    }
    
   //-----------------------------------------------------------------------------------------------//
    public String getDependentString(String linkType)
    {
        TextAnchor[] depAnchors =  getDependentAnchors(linkType);
        if (depAnchors == null || depAnchors.length == 0)
                return "";
        
        String textStr = "";
         for (int i = 0; i < depAnchors.length; i++)
        {
            textStr  +=  depAnchors[i].getTextWithConjunct();
        }
        return textStr;
    }
    
      /******************************************************************************/
    // Get the text of the dependent anchor along with the full link name - for printing
    public String[] getDependentsWithLink(String linkType)
    {
            AnchorLink[]  depLinks = dependencyLinkMap.get(linkType);
             if (depLinks == null ||depLinks.length == 0)
                return null;
             int nl = depLinks.length;
           TextAnchor[]  depAnchors = new TextAnchor[nl] ;
           String[] fullRelations = new String[nl];
            for (int i = 0; i < nl; i++)
            {
                depAnchors[i] = depLinks[i].dependentAnchor;
                 fullRelations[i]  = "\"(\"" + depLinks[i].getLinkText() + "\")\"" +depAnchors[1].getTextWithConjunct();
            }
            return fullRelations;
    }
    /***************************************************************************************/
    // Find the Mentioning (referencing)  Anchor for a specific relation to this anchor
    // We check here for phrasal relations only
    /****************************************************************************************/
       public TextAnchor getMentioningAnchor(ArrayList anchors, String relation)
     {
         for (int i = 0; i < anchors.size(); i++)
         {
             TextAnchor govAnchor = (TextAnchor) anchors.get(i);
             TextAnchor[] depAnchors =  govAnchor.getDependentAnchors(relation);
             if (depAnchors == null || depAnchors.length == 0)
                 continue;
             for (int j = 0; j < depAnchors.length; j++)
             {
                 if (depAnchors[j].anchorId == this.anchorId)
                     return govAnchor;
             }
         }
         return null;
     }
       

 /**********************************************************************************************/
 // Get the anchor which represents a dependent "location"of this given anchor
  // Algorithm:  In all prepositions, find a 
    /**********************************************************************************************/
    public ArrayList <AnchorLink> getLocationLinks()
    {
        AnchorLink[]  depLinks = dependencyLinkMap.get(prep);
         if (depLinks == null ||depLinks.length == 0)
             return null;
        
         ArrayList <AnchorLink> locationLinks = new ArrayList();;
         for (int i = 0; i < depLinks.length; i++)
         {
             AnchorLink plink = depLinks[i];
              if (plink.dependentAnchor == null)
                 continue;
              
             if (plink.isLocationPrep())
                    locationLinks.add(depLinks[i]);

             // trace through to see if it has any chains for location, even if it itself is not the one
              ArrayList <AnchorLink> locationChainLinks = plink.dependentAnchor.getLocationLinks();
              if (locationChainLinks != null)
                  locationLinks.addAll(locationChainLinks);
         }
         if (locationLinks.isEmpty())
             return null;
         return locationLinks;
    }
//------------------------------------------------------------------------------------------------------------------------- 
    // Get the Anchor containing a given annotation - (most likely as the head)
    public  static TextAnchor  getAnchorByID(ArrayList <TextAnchor> anchorSet, int annotId)
    {
         for (int i = 0; i < anchorSet.size(); i++)
        {
            if (anchorSet.get(i ).containsWord(annotId))          // contains this dependent
                return anchorSet.get(i );
        }
         return  null;
    }
    
    //------------------------------------------------------------------------------------------------------------------------- 
    // Get the Anchor  with the given annotation as the leading annotation
    // This is used to distinguish copular adjectives (is old and old) from verbs
    public  static TextAnchor  getAnchorByLeadingID(ArrayList <TextAnchor> anchorSet, int annotId)
    {
         for (int i = 0; i < anchorSet.size(); i++)
        {
            if (anchorSet.get(i).leading.id == annotId)        // anchor id 
                return anchorSet.get(i );
        }
         return  null;
    }
      
//------------------------------------------------------------------------------------------------------------------------- //
   public TextAnnotation[]  getPhrasewords()
   {
       return phraseWords;
   }
   
    //------------------------------------------------------------------------------------------------------------------------- 
    // Get the Anchor spanning a given range of offsets
    public  static TextAnchor  getAnchorByOffsets(ArrayList <TextAnchor> anchorSet, int[]  textOffsets)
    {
        for (int i = 0; i < anchorSet.size(); i++)
        {
            TextAnchor anchor = anchorSet.get(i);
            TextAnnotation[] phraseWords = anchor.getWordSet();
            int n = phraseWords.length;
            if (phraseWords[0].offsets[0] <= textOffsets[0] && phraseWords[n-1].offsets[1] >= textOffsets[1])
                   return anchor;
        }
         return  null;
    }
 //------------------------------------------------------------------------------------------------------------------------- 
    
     public  TextAnchor[]  getLocationDependentAnchors()
     {
         ArrayList <AnchorLink> locationLinks = getLocationLinks();
         if (locationLinks == null || locationLinks.isEmpty())
             return null;
         
         int n = locationLinks.size();
         TextAnchor[] locationAnchors = new TextAnchor[n];
         for (int i = 0; i < n; i++)
             locationAnchors[i] = locationLinks.get(i).dependentAnchor;
         return locationAnchors;
     }
 
/*--------------------------------------------------------------------------------------------*/
 // Applicable only to anchors that  use a noun phrase as an adjective
     
 public TextAnchor[]  getAdvmodAnchors()
 {
     TextAnchor[] advmodAnchors = getDependentAnchors("npadvmod");
      return advmodAnchors;
 }  

 
/****************************************************************************************/
    
     // Check if this anchor id the same as another
     public boolean equals(TextAnchor otherAnchor)
     {
         if (otherAnchor == null)
             return false;
         return (otherAnchor.getAnchorId() == this.anchorId);
     }
     
     // Test if we should consider two anchors to be equivalent
     public boolean isEquivalentTo(TextAnchor otherAnchor)
     {
         if (otherAnchor == null || otherAnchor.type != this.type)
             return false;                                  // should be of same type
         // compare text for exact match (ignoring case)
         String otherText = otherAnchor.getFullTextWithConjunct();
         String thisText = this.getFullTextWithConjunct();
         if (thisText.equalsIgnoreCase(otherText))
             return true;
         // TBD: Do a  regular expression comparison ignoring punctuations
         return false;
     }    
 /*********************************************************************************************/
  // Compare two TextAnchors   based upon theit offsets in the document text.
  // Note: TextAnchors  are non-overlapping
 /*******************************************************************************************/
     public int compareTo(TextAnchor  otherAnchor)
     {
         if (anchorId == otherAnchor.anchorId)
             return 0;          // equal
         // check offsets of the leading token in the anchor
         int offset1 = leading.offsets[0];
         int offset2 = otherAnchor.leading.offsets[0];
         return  (offset1 < offset2) ? -1 : 1;
     }
     
      public int getVerbType()
      {
          return -1;
      }
     

     public String toString()
     {
         return this.getFullTextWithConjunct();
     }
     
     protected void setCoveringText()
     {
         anchorText = getFullTextWithConjunct();
     }
  /* ------------------------------------------------------------------------------*/       
   // Get the text string covered by this anchor
   // Default methid: to br overidden in the derived class     
    public String getCoveringText()
    {
       return anchorText;
    }
    
    // Get the text of the phrase words for this anchor, without adjectives, possessives etc.
    public String getPhraseText()
    {
       String str = phraseWords[0].text;
       for (int i = 1; i < phraseWords.length; i++)
       {
           if (!phraseWords[i-1].text.endsWith("-"))        // do not add " " for xxx-yyy
               str += " ";
           str +=  phraseWords[i].text;
       }
       return str; 
    }
          
 /**********************************************************************************/
   // default, override in derived classes
   public String getFullTextWithConjunct()
   {
       String str = getTextWithConjunct();
       return str;
   }
  /*--------------------------------------------------------------------------------
    * Get the text forr  this anchor.
    * overrides in derived class
    * ------------------------------------------------------------------------------*/

   public String getText()
   {
       String det = (detAnnot == null) ? "" : detAnnot.text + " ";
      return det+getPhraseText();
   }
 /* ------------------------------------------------------------------------------*/     
   public  String getTextWithAppos()   
   {
       return getText();
   }
 /* -------------------------------------------------------------------------------------*/   
   // Get all annotation IDs covered by this TextAnchor, including
   // its dependent links
    public ArrayList <TextAnnotation> getCoveringAnnotations()
     {
         ArrayList annots = getAnchorAnnotations();
         annots.addAll(getDependentLinkAnnotations());
         return annots;
      }
    
//---------------------------------------------------------------------------------------/
 // Overridden in derived classes
 //---------------------------------------------------------------------------------------/
    protected  ArrayList <TextAnnotation> getAnchorAnnotations()
     {
         ArrayList annotIds = new ArrayList();
         for (int i = 0; i < phraseWords.length; i++)
                annotIds.add(phraseWords[i]);
         annotIds.addAll(getDependentLinkAnnotations());
         return annotIds;
      }
    /* ------------------------------------------------------------------------------*/     
   // Get the annotations covered by the dependent links 
    protected  ArrayList <TextAnnotation> getDependentLinkAnnotations()
    {
         ArrayList depAnnots = new ArrayList();
        Iterator iter = dependencyLinkMap.keySet().iterator();
         while (iter.hasNext())
         {
             AnchorLink[] links = dependencyLinkMap.get(iter.next());
             if (links == null)
                 continue;
             for (int i = 0; i < links.length; i++)
                depAnnots.addAll(links[i].getCoveringAnnotations());
         }
         return depAnnots;
     }
}

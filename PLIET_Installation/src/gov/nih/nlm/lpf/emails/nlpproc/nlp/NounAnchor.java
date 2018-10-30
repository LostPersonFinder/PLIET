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
 *
 * @author 
 */

import gov.nih.nlm.lpf.emails.util.Utils;
import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;
import java.util.ArrayList;
import java.util.Collections;

public class NounAnchor extends TextAnchor
{  
    // Relations of a Noun Phrase to other words
    static String[] NPRelations =  null;                    // filled at instantiation
    public static String determinor = "det";            // for an NP: a,  no, some, the etc. We are interested in "no"
    public static String number = "num";
    public boolean isPronoun;                               // true if a pronoun - used later for "anaphora" resolution

   protected TextAnnotation determiner;                 // Sometimes neg as det as in  "no information"
   protected TextAnnotation preceedingNumber;    // as in: 10 years
   protected TextAnchor depNoun = null;   
   protected TextAnchor apposAnchor = null;         // appositional modifier:  e.g.  "Sam, my brother -> appos(Sam, brother)"
   //protected TextAnnotation copularObj;         
   
    // Type of Noun Phrase - To be filled up later
    public boolean isSubject;
    public boolean isObject;
    public boolean isPerson;                                                                                                    
    public boolean isLocation;
    public boolean hasPossessive;               // Does it have possession terms
    
   public TextAnnotation  possessiveAnnot  = null;
   boolean isPronounPoss  = false;
   TextAnnotation  possessionSymbol  = null;  // (apostrophe )
   TextAnchor possessiveAnchor = null;
   
  /****************************************************************************************/
 // Note: Check the treatment of Nouns that have copular verbs, and ither Nouns as  subjects
 // as in "My  name is Michel."
 /****************************************************************************************/
    public NounAnchor(TextAnnotation sentence,TextAnnotation annot, ArrayList <TextAnnotation> tokens)
    {
        super (sentence, annot, NOUN_ANCHOR);
        
        // if it is a possive word, find the possessor
       /* TextAnnotation posessiveGov = leading.getGovernorAnnotation(tokens, POSS);
        if (posessiveGov != null)
            leading = posessiveGov;             // is possessed by another word; start with that word
        */
       governor  = findGovernorToken(leading, tokens);   
       setComplementWords( tokens);
       setPossessiveWords(tokens);
       setPreceedingNumber(tokens);

       leading = phraseWords[0];            // first word of a Noun Phrase
       isPronoun = TextAnchor.isPronoun(governor);
       anchorId = governor.id;
    } 
    
   //--------------------------------------------------------------------------------------------------------------------------//  
   //  Set the  token, if any,  which mentions(governs) the given token 
   // through an "nn" (NOUN_COMP) relationship
   //---------------------------------------------------------------------------------------------------------------------------//
    protected TextAnnotation findGovernorToken(TextAnnotation word, ArrayList <TextAnnotation> tokens)
    {
        TextAnnotation gov = null;
        while ( word != null)
        {
            gov = word;
            word = gov.getGovernorAnnotation(tokens, NOUN_COMP);  //"nn"
        }  
        return gov;
    }

     //--------------------------------------------------------------------------------------------------
    // Set the words that are part of a multi-word phrase  ending with the Governor,
    // where the words are connected by the "nn" compound modifier
    // Example: "my brother Michael Smith" where  starting word is "brother"
    //----------------------------------------------------------------------------------------------------
     protected void setComplementWords(ArrayList <TextAnnotation> tokens)
    {
        // Build the noun phrase; that is: find ALL  the tokens  to which the noun  is linked 
        // by an "nn" relation; in ascending order ( in annotation id)
        ArrayList<TextAnnotation> complements = new ArrayList();
        // find the set (one or more "nn" relations of this governer, going backwards
        int[] nnIds = governor.getDependentAnnotationIds(NOUN_COMP);        // "nn"
        int np = (nnIds == null) ? 1 : nnIds.length+1;      // length of phraseWords
        
        phraseWords = new TextAnnotation[np];
        for (int i = 0; i < np-1; i++)
            phraseWords[i] = TextAnnotation.getAnnotationById(tokens, nnIds[i]);
        
        phraseWords[np-1] = governor;
        return;
    }
 
    //--------------------------------------------------------------------------------------------------
    // Check if this governor is qualified by wither a possessive pronoun (mt, his...)
    // or a possive Anchor (my brother's son : (son, my brother)
    // We distinguisg betwwen them as  the Possessed TextAnchor may be referred to 
    // as a subject elsewhere.
    
     protected void setPossessiveWords(ArrayList <TextAnnotation> tokens)
    {
        int possId = governor.getDependentAnnotationId(POSS);        // "poss":  id for "brother"
        hasPossessive  = (possId > 0);
        if (! hasPossessive)
            return;
        TextAnnotation pannot = TextAnnotation.getAnnotationById(tokens, possId);
        possessiveAnnot = pannot;
        isPronounPoss = isPronoun(pannot);
        
         if (! isPronounPoss)
         {
            // get the possissive symbol
            possessionSymbol = pannot.getDependentAnnotation(POSSESSIVE, tokens);
         } 
        return;
    }
  /*-----------------------------------------------------------------------------------------------------------*/
  // Set the value of the number, if any, that preceeds this anchor (as in an address)
  protected void  setPreceedingNumber(ArrayList <TextAnnotation> tokens)
  {
      TextAnnotation numAnnot = governor.getDependentAnnotation("num",  tokens);
       preceedingNumber  = (numAnnot == null) ? null : numAnnot;
  }
      
   /****************************************************************************************
     * Set other properties  for this NounAnchor. This method should be invoked
     * after all links and relations to other Anchors in a sentence are resolved.
     * Parameters anchors is the collection of all anchors in the enclosing sentence
     *************************************************************************************/
    public void setOtherProperties(ArrayList <TextAnchor> anchors)
    {
        ArrayList<TextAnnotation> tokens =  tokenSet ;
        // Check if it has an appositional mofifier via "appos" dependemcy
        TextAnchor[] da =  this.getDependentAnchors("appos");
        if (da != null && da.length > 0)
            apposAnchor  = da[0];
        
        // Set the possesive anchor for this anchor
        if (possessiveAnnot != null && !isPronounPoss)
        {
            for (int i = 0; i < anchors.size() && possessiveAnchor == null ; i++)
            {
                if (anchors.get(i).containsAnnot(possessiveAnnot))
                  possessiveAnchor = anchors.get(i);
            }         
        }
        
         
        // if the main term  is related to a verb as "subj" or "subjpass" it is a person
        isSubject =  (governor.getGovernorAnnotation ( tokens, "nsubj") != null)
                                || (governor.getGovernorAnnotation(tokens, "nsubjpass") != null);
       
        // 
        isObject =  (governor.getGovernorAnnotation ( tokens, "dobj") != null)
                               || (governor.getGovernorAnnotation(tokens, "iobj") != null);  
         
        // also check if it is a copularObject to another Noun (as in:
        //. My name is Michael ( Michael :copula ; cop -> is,  nsubj-> name)
        if  (governor.getDependentAnnotationId("cop") >= 0) 
        {
            if ( governor.getDependentAnnotationId ("nsubj") > 0 ||
                 governor. getDependentAnnotationId ("nsubjpass") >= 0) 
                isObject = true;
        }
       
       boolean candidatePerson = !Utils.isInList(governor.text, NERConstants.NONPERSON_NOUNS);
       isPerson =   (isSubject || isObject) && candidatePerson;
       isLocation = false;
       if (!isPerson)
       {
           for (int i = 0; i < anchors.size(); i++)
           {
           // It is a  location if it is "pobj" of a verb
               TextAnchor[] locAnchors = (anchors.get(i)).getLocationDependentAnchors();
                if (locAnchors == null)
                    continue;
                for (int j = 0; j <  locAnchors.length; j++)
                {
                    if (locAnchors[j].getAnchorId() == this.getAnchorId())
                    {
                        isLocation = true;
                        break;
                    }
                }
           }
       }  // end: !isPerson

       // Store the anchor which is related to it through a "dep" relationship
       // such as "my brother" => dep: "Michael Sterling" in  "my brother Michael Sterling"
     TextAnchor[] deps = getDependentAnchors("dep");
     if (deps != null && deps.length > 0)
         depNoun = deps[0];     // really only one
    }
    //--------------------------------------------------------------------------------------------------
    // return if it is a pronoun or a real noun
    public boolean isPronounType()
    {
        return isPronoun;
    }

   //--------------------------------------------------------------------------------------------------
    // return if it is a pronoun or a real noun
    public boolean hasPossessiveWords()
    {
          return hasPossessive;
    }
    //--------------------------------------------------------------------------------------------------   
    // return if it is a pronoun Anchor /Annotation associated with this Anchor
    public TextAnchor getPossessiveAnchor()
    {
        return possessiveAnchor;
    }
   //--------------------------------------------------------------------------------------------------
    public  ArrayList <TextAnnotation>  getPossessiveWords()
    {
        if (!hasPossessive)
        
            return null;
        ArrayList<TextAnnotation> possessiveWords = new ArrayList();
        if (isPronounPoss)
        {
            possessiveWords.add(possessiveAnnot);
        }
        else if (possessiveAnchor != null)
        {
            for (int i = 0; i <  possessiveAnchor.phraseWords.length; i++)
                possessiveWords.add(possessiveAnchor.phraseWords[i]);
            if (possessionSymbol != null)
                 possessiveWords.add(possessionSymbol);
        }       
         return  possessiveWords;
    }
    
    /*********************************************************************************/
    // Overides method in superclass
    public String getText()
   {
       String str = super.getText();
       if (depNoun != null)
           str += " " + depNoun.getText();
          
       if (preceedingNumber != null )
       {
           // check if the nunber preceed (as in house number) or follows (as in zipcode) the text
           int pstart = preceedingNumber.offsets[0];
           int gstart = governor.offsets[0];
           String number = this.getPreceedingNumber();      
           str = (pstart < gstart) ? (number + " " + str) : (str + number);
       }
       if (hasPossessive)
           str = getPossessiveText() + " " + str;
       return str;
   }
   /*-------------------------------------------------------------------------------*/
    // Return the string that is possessibe for this anchor
    /*-------------------------------------------------------------------------------*/
   public String getPossessiveText()
   {

       if (!hasPossessive)
           return "";
       
       String str = "";
       if (isPronounPoss)
           str =  possessiveAnnot.text;
       else if (possessiveAnchor != null)
       {
           str = possessiveAnchor.getTextWithConjunct();
           if (possessionSymbol != null)
               str += " " + possessionSymbol.text;
       }
       return str;
   }
           
    

 /***************************************************************************************************/
   public String getFullTextWithConjunct()
   {
       String str = this.getTextWithConjunct();
       return str;
   }
   
   /****************************************************************************************/   
  // relationships for a Noun Anchor that we are interested in
  public  TextAnchor getApposAnchor()
   {
       return apposAnchor;           
   }  
     
     /****************************************************************************************/   
  // relationships for a Noun Anchor that we are interested in
  public  String getTextWithAppos()   
   {
       String text = getText();
       TextAnchor apposAnchor = getApposAnchor();
       while(apposAnchor != null)
       {
           text += ", " + apposAnchor.getTextWithConjunct();
           if (apposAnchor.getType() == NOUN_ANCHOR )
                apposAnchor = ((NounAnchor) apposAnchor).getApposAnchor();      //could be recursive
       }  
       return text;     
   }
  
  /****************************************************************************************/
  // Get the set of TextAnnotations  covered by this Noun anchor
  // must consider precceding number, adjectives, complements, conjunctions, appos and deps
    public ArrayList<TextAnnotation>  getCoveringAnnotationSet()
    {
        ArrayList <TextAnnotation> annotSet = new ArrayList();
        ArrayList<TextAnnotation> adjAnnots = getAdjectiveAnnots();
        if ( adjAnnots  != null)
            annotSet.addAll(adjAnnots);
        if (determiner != null)
            annotSet.add(determiner);
        if ( negativeAnnot  != null)
            annotSet.add(determiner);
        if ( preceedingNumber   != null)
            annotSet.add(preceedingNumber);
            
        for (int i = 0; i < phraseWords.length; i++)
            annotSet.add(phraseWords[i]);

        // Add conjunctions and appos Anchors too
        addCoveringAnnotations(depNoun, annotSet);
        TextAnchor  conjAnchor = getConjunct();
        addCoveringAnnotations(conjAnchor, annotSet);
        addCoveringAnnotations(apposAnchor, annotSet);
        Collections.sort(annotSet);
        return annotSet;
    }
        
      /****************************************************************************************/
  // Get the set of TextAnnotations  covered by this Noun anchor
  // must consider precceding number, adjectives, complements, conjunctions, appos and deps
    public TextAnnotation[]  getCoveringAnnotationArray()
    {
        ArrayList <TextAnnotation> annotSet = getCoveringAnnotationSet();
         // convert it to a sequential array
         int alen = annotSet.size();
         TextAnnotation[] coveringAnnots = new TextAnnotation[alen];
         annotSet.toArray(coveringAnnots);
         return coveringAnnots;
    }   

    /*****************************************************************************
     * Add the annotation of a related anchor to the given annotation set
     *****************************************************************************/
    protected void  addCoveringAnnotations(
                TextAnchor relAnchor, ArrayList<TextAnnotation> myAnnots)
    {
        if (relAnchor == null || relAnchor.type != NOUN_ANCHOR)
            return;

          TextAnnotation[] relAnnotations = ((NounAnchor) relAnchor).getCoveringAnnotationArray();
          if (relAnnotations.length != 0)
          {
              for (int i = 0; i < relAnnotations.length; i++)
                    myAnnots.add(relAnnotations[i]);
          }
          return;
    }
   
    //----------------------------------------------------------------------------------------------------//
    // Overrides the method in TextAnchor
    protected  void setCoveringText()
    {
        String sentenceText = sentence.text;
        int[] sentenceOffsets = sentence.offsets;
        int sentenceStart = sentenceOffsets[0];
        
        TextAnnotation[] coveringAnnots = getCoveringAnnotationArray();
        int n = coveringAnnots.length;
        // get offsets into the sentence
        int startOffset = coveringAnnots[0].offsets[0]-sentenceStart;
        int endOffset = coveringAnnots[n-1].offsets[1]-sentenceStart;
        
        anchorText = sentenceText.substring(startOffset, endOffset);
    }
    /****************************************************************************************/      
        protected  ArrayList <TextAnnotation> getAnchorAnnotations()
     {
        return getCoveringAnnotationSet();
      }

   /****************************************************************************************/   
  // Get the value of the number, if any, that preceeds this anchor (as in an address)
  public String getPreceedingNumber()
  {
      return (preceedingNumber == null ? "" : preceedingNumber.text);
  }
  
 /****************************************************************************************/   
  // relationships for a Noun Anchor that we are interested in
   protected String[] getDirectRelationTypes()
   {
       return  NOUN_PHRASAL_DEPENDENCIES;            
   }  
}

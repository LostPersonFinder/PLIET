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

// IMP: Determine if a (cc, conj)  is another Verb or a separate Anchor/Clause

import gov.nih.nlm.lpf.emails.util.Utils;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class VerbAnchor extends TextAnchor
{
    
    private Logger log = Logger.getLogger(VerbAnchor.class);
   
    /*************************************************************************************/   
     // Relations of a Verb Phrase to other words
    static String[]  phrasalRelations  = VERB_PHRASAL_DEPENDENCIES;
     
    //static String[] objectLinkTypes = {"dobj", "iobj"};
    
    public static boolean isPrimaryVerb(int type)
    {
        return(type ==PRIMARY_TYPE);
    }
    
    // Dependency of a clause with its parent clause
    // Note: CSubj and CSubjpass clauses act as Subjects of the Governor clause
    // It does not appy to partmod and rcmod type clauses that are related to 
    // nouns/adjectives, but not Verbs
    public static  class DependentClause
    {
        public int type;                                  // numeric type
        //int[]  governorTypes;                             // Verb, noun or Adjective
        public String clauseType;                      // Dependency type of the clause to main verb, or noun or adjective
        public String connectorRelation;          // Dependency relation of word  introducing the clause word (connector )
        
        private DependentClause(int ctype,  String crel, String irel)
        {
            type = ctype; clauseType =crel; connectorRelation= irel;
        }
    }
    
    public static DependentClause[]  DependentClauseDefs = 
    {
        new DependentClause(ADVCL_TYPE,   "advcl", "mark"),
        new DependentClause(CCOMP_TYPE,   "ccomp", "complm"),
        new DependentClause(CONJ_TYPE, "conj", "cc"),   
        new DependentClause(CSUBJ_TYPE ,"csubj", ""),
        new DependentClause(CSUBJPASS_TYPE, "csubjpass", ""),
        new DependentClause(DEP_TYPE, "dep", "aux"),
        new DependentClause(PCOMP_TYPE,  "pcomp", ""),
        new DependentClause(PREPC_TYPE,  "prepc", ""),
        new DependentClause( XCOMP_TYPE, "xcomp", "aux"),
        new DependentClause(CC_XCOMP_TYPE, "xcomp", "cc"),   
       new DependentClause(TO_XCOMP_TYPE, "xcomp", "to"),   
    };

   
    
 // NOTE:
 // We also check for a "conj" relation because te parser occasionally fails to mark it as xcomp

   /**************************************************************************************/
    // Instance variables
    //
    protected TextAnnotation mainVerb;                        // in multi-word phrases
    protected TextAnnotation auxilliaryVerb = null;       // if there is an auxilliary to the main verb
    protected boolean active = true;                                // active or passive voice

    protected int tense = -1;                                             // initialized to UNKNOWN    
    boolean isAuxiliaryGov = false;                                // is it the governor for an axiliary verb
    boolean isCopularGov = false;                                // is it the verb  for a copular relation
    TextAnnotation copulaGov = null;                            // Governor token, if this is a copular verb
    
    VerbAnchor  conjunctionHead = null;                      // if it is a conjuction, the head Verb of the relation
    protected int  clausalType;                                        // type of clause containing this verb
   
    // the xcomp complement is a  verb connected with the main verb with the "TO" word 
    TextAnnotation toXcompAnnot;                                // its xcomp verb preceeded with TO 
    // for non-primary verbs
    protected TextAnnotation clausalGovAnnot;
    protected TextAnchor clausalGovernor;                   // Through which this verb is referenced
    protected String clausalMarkerText;                          // text through it is linked to the clausalGovernor        
    
   /**************************************************************************************/
    
    
     public VerbAnchor(TextAnnotation sentence, TextAnnotation annot, ArrayList <TextAnnotation> tokens)
    {
        super (sentence, annot, VERB_ANCHOR);
        mainVerb = findMainVerb(leading,  tokens);      // one to which tokens are related      
        governor  = getGovernorToken(leading, tokens);     
        anchorId = governor.id;
         
        setComplementWords( tokens);
        setXcompWord(tokens);

        // set direct  attributes
         active = true;     // default
         if (mainVerb.getDependentAnnotationId("nsubjpass") >= 0  ||  
                            mainVerb.getDependentAnnotationId("auxpass") >= 0) 
              active = false;
         
         // check if it is a partmod of a noun, as in liikong for David, last seen in Bethesda
         else if (mainVerb.getGovernorAnnotation(tokens, "partmod") != null)
             active = false;
          auxilliaryVerb = getAuxiliary(mainVerb,  tokens);
          
                    // must be set prior to calling verb tense
         tokenSet = tokens;
         tense = VerbUtils.getVerbTense(this);
         setVerbType(mainVerb, tokens);     // whether a primary or a clausal verb   
    }
     
    /****************************************************************************************************
    * Determine if a verb is a primary verb or a  dependent clausal verb
    * Note: A conjunction Anchor has the same VerbType as its conjunctionHead, and is not
     * evaluated separately
     ****************************************************************************************************/
     protected void setVerbType(TextAnnotation verbAnnot, ArrayList<TextAnnotation> tokens)
     {
         clausalGovAnnot  = null;       //default;
         clausalMarkerText = "";
         DependentClause depc = null;
         for (int i = 0; i < DependentClauseDefs.length; i++)
         {
             depc = DependentClauseDefs[i];
              // Check if the verb ir related to another (parent)  through clausal dependency
             String ctype  =  depc.clauseType;
             TextAnnotation annot = verbAnnot.getGovernorAnnotation(tokens, ctype);  
              if  (annot == null) 
                  continue;

              // if it is a "pcomp" clause with annot being a preposition, go to the source annotation 
              //with that prepositional link, since prepositions are a two step link
              if  (ctype.equals( "pcomp"))
              {
                  annot = annot.getGovernorAnnotation(tokens, "prep");
                  if (annot == null)            // an error
                      break;
              }

              // find the connection or marker text
              clausalGovAnnot = annot;
              clausalMarkerText = "";
              String intro = depc.connectorRelation;
              if (intro.length() > 0)
              {
                    TextAnnotation linkAnnot = verbAnnot.getDependentAnnotation(intro,  tokens);
                    if (linkAnnot != null)
                    {
                        clausalMarkerText = linkAnnot.text;
                        break;
                    }
              }
               if (clausalMarkerText.length() == 0)
               {
                   // Look for a modifier
                   int  modAnnotId  =  verbAnnot.getDependentAnnotationId("advmod");
                   if (modAnnotId  > 0)
                        clausalMarkerText = (TextAnnotation.getAnnotationById(tokens, modAnnotId)).text;
                   break;               // TBD: other tests  
              } // end if clausalMarkerText
         } // end of for

         // If there is no other Verb governing this verb (e.g. rcmod dependencies with Nouns as the governor)
         if (clausalGovAnnot == null)
         {
             clausalType = PRIMARY_TYPE;
             return;
         }
        String category = (String)clausalGovAnnot.getFeature("category");
        if (category.equals("JJ"))
             this.clausalType = depc.type;          // for certain XComp verbs: as in  " I am unable to find"
        else  if ( !Utils.isInList(category,  VERB_CATEGORIES))              // no  other" "verb"  governing it
            clausalType = PRIMARY_TYPE;
         else
             this.clausalType = depc.type;
         return;

     } 
     
      public int getClausalVerbType()
      {
          return clausalType;
      }
      
      public String getClausalMarker()
      {
          return (clausalMarkerText == null) ? "" : clausalMarkerText;      // words like:  if, to, that, etc.
      }

     
   //--------------------------------------------------------------------------------------------------------------------------//  
   //  Set the  token, if any,  which mentions(governs) the given token 
  // Note: sometimes "aux" may also be a "to" as in "to be rescued" , which does not come here
   //---------------------------------------------------------------------------------------------------------------------------//
    protected TextAnnotation findMainVerb(TextAnnotation word, ArrayList <TextAnnotation> tokens)
    {
        TextAnnotation mainVb = null;
        for (int i = 0; i < tokens.size() && mainVb == null; i++)
        {
            TextAnnotation token =  tokens.get(i);
            int auxId  = token.getDependentAnnotationId("aux");     // governor to which it is the auxiliary: active voice
            int auxPassId  = token.getDependentAnnotationId("auxpass");    // governor to which it is the auxiliary: passsive  voice
            if  (auxId == word.id || auxPassId == word.id)
               mainVb = token;    // governor of an auxiliary
        }
        
        if (mainVb == null)
            mainVb = word;            // itself is the governor
        else 
            isAuxiliaryGov  = true;
        return mainVb;
    }
    //--------------------------------------------------------------------------------------------------------------------------//
    // Get the governor token for determinig the relationships.
    // If a copula relation exists,  get the governor of that relation
    // for example: "is alive"  where alive is the governor of the "cop" relation
    // Note: the return token is usually an Adjective
    //--------------------------------------------------------------------------------------------------------------------------//
    protected TextAnnotation getGovernorToken(TextAnnotation word, ArrayList <TextAnnotation> tokens)
    {
        // Does not have an   auxiliary dependency; check for adjectival copular token
        //  (e.g. is alive => copula of "alive"  is the Copular Governor for "is", or I am John)
        copulaGov = getCopulaGovernor(word,  tokens);
        isCopularGov = (copulaGov != null);
        if ( copulaGov == null) 
            return mainVerb;
        return
            copulaGov;
    }


    //--------------------------------------------------------------------------------------------------
    // Set the words that are part of a multi-word verb phrase, ending with the Governor
    // e.g.: "is alive", "has been found", "to be rescued"
    //----------------------------------------------------------------------------------------------------
    protected void setComplementWords(ArrayList <TextAnnotation> tokens)
    {
        //Find the Verb  tokens  which mentions this verb (linked directly or indirectly)
        // as auxiliaty or copular link
        ArrayList<TextAnnotation> complements = new ArrayList();
        if (isAuxiliaryGov)
        {
            // find the aux and auxpass relations for the governer, ypu may have one or both
            TextAnnotation auxAnnot   = governor.getDependentAnnotation("aux", tokens);
            if  (auxAnnot != null ) 
                complements.add(auxAnnot);                  

            TextAnnotation auxPassAnnot   = governor.getDependentAnnotation("auxpass", tokens);
            if  (auxPassAnnot != null ) 
                complements.add(auxPassAnnot);
        }
        else
                complements.add( leading);
         if (leading != mainVerb)
                complements.add( mainVerb);

        // If the governer is a copular noun,  we don't add it to the phrase words (for text)
         // otherwise we have already added it as the main Verb
        if ( mainVerb != governor && isAdjective(governor))
            complements.add(governor);          // the last word in verb phrase

        // convert the series into an array 
        phraseWords = new TextAnnotation[complements.size()];
        for (int i = 0; i < complements.size(); i++)
            phraseWords[i] = complements.get(i); 
        return;
    }
   //-----------------------------------------------------------------------------------------------------
   // An xcomp annotation  for a maon verb follows it with a "TO" text, as in 
    // "want to know". In this case, the objects and other relations follow the xcomp
    //-----------------------------------------------------------------------------------------------------
    protected void setXcompWord(ArrayList <TextAnnotation> tokens)
    {
        this.toXcompAnnot = null;             // default
        TextAnnotation xcAnnot   = governor.getDependentAnnotation("xcomp", tokens);
        if  (xcAnnot != null ) 
        {
            // get the "aux" word and get its category
            TextAnnotation xcompAux = xcAnnot.getDependentAnnotation("aux", tokens);
            if (xcompAux != null)
            {
                String category = (String) xcompAux.getFeature("category");
                if (category.equals("TO") && xcompAux.text.equalsIgnoreCase("to")) 
                toXcompAnnot = xcAnnot;
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------//
    // get the Token for which this is the Copular verb, 
    // for example: "is alive"  where alive is the governor of the "cop" relation
    // Note: the return token is usually an Adjective
    //
    public TextAnnotation getCopulaGovernor(TextAnnotation word, ArrayList <TextAnnotation> tokens)
    {  
        copulaGov = governor.getGovernorAnnotation(tokens, "cop");
        return copulaGov;    // null if no copular relation
    }
    
 //-----------------------------------------------------------------------------------------------------//
   // Direct relations are with other words or phrases, but not clauses 
   protected String[] getDirectRelationTypes()
   {
       return   VERB_PHRASAL_DEPENDENCIES;
   }
    
   //------------------------------------------------------------------------------------------------------------//
   // A verb is not auxilary if  it is not the target of an auxilliary link of another token
   // Example:  "is looking"  - looking ->aux = is;  where is->aux = null; so 'is' is auxiliary
   // In a VerbAnchor, it is the last word of the verbgroup which is "not" auxiliary, the rest are. 
   //---------------------------------------------------------------------------------------------------------
    protected boolean isAuxiliaryVerb(TextAnnotation word)
    {
        TextAnnotation lastWord = phraseWords[phraseWords.length-1];
         if (word.id != lastWord.id)
             return true;
         return false;      // the last word is the governor
    }
    
    public TextAnnotation getMainVerb()
    {
        return mainVerb;
    }
    
    // get the auxilliary verb to this main verb
    public TextAnnotation getAuxilliaryVerb()
    {
        return auxilliaryVerb;
    }
    
    // return the anchor which is the governot of this verb in a clausal relationship
    public TextAnchor getClausalGovernor()
    {
        return  clausalGovernor;
    }
   
    /*------------------------------------------------------------------------------------------------------------*/
    // Get other type of dependent relations associated with this Anchor
     // Add the copular depency for this Noun
    //e.g. My name is Michael ( Michael : nsubj-> name, copula ; cop -> is)
    protected void setOtherDependencyLinks(TextAnnotation gov , ArrayList <TextAnnotation> tokens)
        // set the Copula Object for this Noun, if any:. The object must be a noun/pronoun
    {
        super.setOtherDependencyLinks(gov, tokens);
        if (copulaGov == null)
            return;
        
        // Make sure that  the link is not a Verb, but a noun /adjective/number 
        // - links to the governor of copula (a noun)
        if (isNoun(copulaGov) || isPronoun(copulaGov) || isAdjective(copulaGov) || isCardinal(copulaGov))
        {
            String relation = "copobj";         // create a new synthetic relation
            AnchorLink plink = new AnchorLink(mainVerb.id, copulaGov.id, relation, null);
           dependencyLinkMap.put(relation, new AnchorLink[]{plink});
        }
     } 

 
    //---------------------------------------------------------------------------------------------------------
    // Get the other verbs connected to this verb which should be treated in equal footing
    // with this verb.
    // Those are conj, xcomp, and prep->pcomp
    // Returns null if nothing found
    //---------------------------------------------------------------------------------------------------------
    public  TextAnnotation getGovernorVerb(String relation)
    {
        // determine which verb to be the governor of a relationship
        TextAnnotation verbAnnot =  this.isAdjectivalCopula() ?  this.getCopularAdjective() :  mainVerb;
        TextAnnotation govAnnot = verbAnnot.getGovernorAnnotation(tokenSet, relation);
         return govAnnot ;
    }   
    
     public TextAnnotation getAuxiliary(TextAnnotation word, ArrayList <TextAnnotation> tokens)
     {
         return VerbUtils.getAuxiliary(word, tokens);
     }
    /*****************************************************************************/
    public boolean isActive()
    {
        return active;
    }
    
   /*****************************************************************************/
    // Is this  verbAnchor  is the verb of a primary clause of the sentence
    //***************************************************************************/
    public boolean isPrimary()
    {
        return (clausalType == PRIMARY_TYPE);
    }
    
    /*-----------------------------------------------------------------------------------------------------------
     * Set other properties  for this VerbAnchor. This method should be invoked
     * after all links and relations to other Anchors in a sentence are resolved.
     * Parametrs anchors is the collection of all anchors in the enclosing sentence
     * Here we set the xcomp verbs for this verb.
     *************************************************************************************/
    public void setOtherProperties(ArrayList <TextAnchor> anchorSet)
    { 
        // check if it is a Conjunction, if so: save the head verb
        conjunctionHead = setConjunctionHead(anchorSet);
        
        // if it is not a primary verb, set the anchor to which refers to it 
        clausalGovernor = setReferenceAnchor(anchorSet);  
    }
    
   /*************************************************************************************/
    // Find the VerbAnchor, if any,  for which this is a Conjunction
    //--------------------------------------------------------------------------------------------------------/
    protected VerbAnchor setConjunctionHead(ArrayList <TextAnchor> anchorSet)
    {
        for (int i = 0; i < anchorSet.size(); i++)
        {
           TextAnchor anchor = anchorSet.get(i);
            if  ((anchor.type != VERB_ANCHOR) || (anchor.anchorId == this.anchorId))        // exclude self
                continue;
            if (anchor.containsAnnot(governor))
            {
                return (VerbAnchor)anchor;         
            }
        }
        return null;
    }
 
  //--------------------------------------------------------------------------------------------------------/   
    protected TextAnchor setReferenceAnchor( ArrayList <TextAnchor> anchorSet)
    {
        if (clausalType == PRIMARY_TYPE)
            return null;
          for (int i = 0; i < anchorSet.size(); i++)
        {
           TextAnchor anchor = anchorSet.get(i);
           if (anchor.containsAnnot(clausalGovAnnot))
               return anchor;
        }  
          log.error("No reference anchor found for verb " + this.getText() + ", refAnnot: " + clausalGovAnnot.text);
          return null;
    }
    
    public String getTextWithXcomp()
    { 
        if (toXcompAnnot == null)
            return getText();
        else
            return (getText() + " to " + toXcompAnnot.text);
    }
  //-------------------------------------------------------------------------------------------------/     
   public String getClauseType()
   {
       return VerbUtils.getClauseType(this);
   }
   
//-------------------------------------------------------------------------------------------------/   
// Get the Anchor which is the direct or indirect subject of this verb
// We only consider Noun anchors as subject, because adjectives and
// other types may also be marked as subjects, . For example, in
// ." Last seen in Bethesda" => "Last" is marked as Subject for "seen"
//--------------------------------------------------------------------------------------------------/
   public TextAnchor  getSubject(ArrayList <TextAnchor> allAnchors)
   {
       TextAnchor anchor = VerbUtils.getSubjectForVerb(this, allAnchors);
       if (anchor != null && anchor.getType() == NOUN_ANCHOR)
           return anchor;
       
       // Check if the verb is being used as an adjective; if so, find the corresponding anchor
       if (this.isAdjectival())
       {
           for (int i = 0; i < adjectiveDeps.length && anchor != null; i++)
           {
                anchor = getMentioningAnchor(allAnchors, adjectiveDeps[i]);
           }
       }
       return anchor;
   }

/*********************************************************************************/          
    public  int getVerbTense()
    {
       return this.tense;
    }
               
//-------------------------------------------------------------------------------------------------/   
// Get the xcomp annotation used as an object (with TO for this verb)
//
   public TextAnnotation  getXcompObject()
   {
       return this.toXcompAnnot;
   }
       
    /*********************************************************************************/   
    // overridden in derived class
    public VerbAnchor  PrimaryVerb(int type)
    {
        return this;
    }

    /*********************************************************************************/     
    public VerbAnchor  getConjunctionHead()
    {
        return conjunctionHead;             // null if it is not a conjunction
    }
    
    public boolean isConjunction()
    {
        return (conjunctionHead != null);
    }
  /*********************************************************************************/  
    // Check  if this  verb is being used as an adjective to a Noun phrase
    public boolean isAdjectival()
    {
        for (int i = 0; i < adjectiveDeps.length; i++)
        {
            TextAnnotation adectivalGov = 
                this.governor.getGovernorAnnotation(tokenSet, adjectiveDeps[i]);
            if (adectivalGov != null)
                return true;
        }
        return false;
    }
   /*********************************************************************************/
    // Copular verbs
       public boolean isCopularVerb()
    {
        return (copulaGov != null);
    }
    
    public boolean isAdjectivalCopula()
    {
        return (copulaGov != null && isAdjective(copulaGov));
    }

      /*********************************************************************************/  
    // get the Copular adjective, if any, for this verb
     public  TextAnnotation getCopularAdjective()
     {
         if (isAdjectivalCopula() )
             return copulaGov;
         return null;
     }
     /*********************************************************************************/
     // TBD: Return only the Adjectival Anchor for the if the copula is an adjective     
     public TextAnchor[]  getCopularObjects()
     {
         TextAnchor[] copularObjects =  getDependentAnchors("copobj");
         return copularObjects;
     }
         
}
    


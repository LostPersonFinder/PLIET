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

import gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;
import gate.FeatureMap;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import  gate.stanford.DependencyRelation;

import java.util.ArrayList;

/**
 *
 * @author 
 */
public class TextAnnotation1  implements NLPRelations,  Comparable
{
         public String type;
         public String text;
         public int id;
         public int[] offsets;               // offsets in text, returned as Node IDs by GATE
         public FeatureMap features;   
     //--------------------------------------------------------------------------------------------//
     // Get a TextAnnotation by   its  Id from a list of such annotations
     public static TextAnnotation1 getAnnotationById(ArrayList<TextAnnotation1> annotations, int tid)
     {
         for (int i = 0; i < annotations.size(); i++)
         {
             if (annotations.get(i).id  == tid)    
                 return annotations.get(i);
         }
         return null;
     }
     
     /*--------------------------------------------------------------------------------------------
     *  Instance methods       
     *--------------------------------------------------------------------------------------------*/  
         public  TextAnnotation1(String aType, String aValue,  int start, int end, int ID,FeatureMap fmap)
         {
             type = aType;
             text = aValue;
             id = ID;
             features = fmap; 
             offsets = new int[]{start, end};
         }
        /*--------------------------------------------------------------------------------------------*/  
         // convert from an Annotation to an TextAnnotation object (with the associated text value)
         public  TextAnnotation1(Annotation annot, String fullText)
         {
             offsets = new int[2];
             type = annot.getType();
             offsets[0] = annot.getStartNode().getOffset().intValue();
             offsets[1] = annot.getEndNode().getOffset().intValue();        // end+1
             id = annot.getId().intValue();
             features = annot.getFeatures();
             text = (String)features.get("string");     // present only for tokens
             if (text == null)
                 text = fullText.substring(offsets[0], offsets[1]);
         }
        /*--------------------------------------------------------------------------------------------*/  
           public  String toString()
         {
             return ("Type: " +type +  ", Id: " + id + ", Offsets: [" + offsets[0]+","+offsets[1]
                 +"] , Text: \"" + text+ "\", " + ", Features: " + features.toString());
         }
           
    /*--------------------------------------------------------------------------------------------*/         
        public Object  getFeature(String feature)
        {
           if (features == null)
               return null;
           return (features.get(feature));
        }
    
    /*--------------------------------------------------------------------------------------------*/          
    // Check if this annotation's text fully contains a given annotation
       public boolean contains(TextAnnotation1  aAnnot)
       {
           int annotStart = aAnnot.offsets[0];
           int annotEnd =  aAnnot.offsets[1];
           return contains(annotStart, annotEnd);
       }
     /*--------------------------------------------------------------------------------------------*/  
       public boolean contains (int annotStart, int annotEnd)
       {
           if ( this.offsets[1] >= annotEnd &&  this.offsets[0] <= annotEnd)
               return true;
           return false;
       }
   /*--------------------------------------------------------------------------------------------*/  
    /**  Check if this annotation's text range is 
   * fully contained within the text annotated by aAnnot
   * annotation. 
   * @param aAnnot a  TextAnnotation1_old.
   * @return <code>true</code> if this annotation is fully contained in the 
   * other one.
   */
     public boolean withinSpanOf(TextAnnotation1 aAnnot)
     {
       if ( ( aAnnot.offsets[1] >= this.offsets[1]) &&
            ( aAnnot.offsets[0] <= this.offsets[0]))
         return true;
       else 
         return false;
     }    
     
     // check if this annotation is within the given offset range
     public boolean withinRange(int start, int end)
     {
       if ( ( start <= this.offsets[0]) && ( end >= this.offsets[1]))
         return true;
       else 
         return false;
     }    

   //**************************************************************************************************/
    // Check if a specified annotation  overlaps with any one annotation in ascending an array 
    // useful for comparing Annotations of different types refering to same words in Text
     //
    public boolean  overlapsWith(TextAnnotation1[] annots)
     {
         int start = annots[0].offsets[0];
         int end = annots[annots.length-1].offsets[1];
        if (this.contains(start, end) ||   this.withinRange(start, end))
            return true;
        else 
            return false;
     }     

   //**************************************************************************************************
     // Get the set of Text annotations contained within this annotation. (Similar to the Gate function)
     //
     public ArrayList<TextAnnotation1> getContainedAnnotations( ArrayList<TextAnnotation1>  annots)
     {
         ArrayList<TextAnnotation1> containedSet = new ArrayList();
         for (int i = 0; i < annots.size(); i++)
         {
             if (annots.get(i).withinSpanOf(this))
                 containedSet.add(annots.get(i));
         }
         return (containedSet.size() == 0 ? null : containedSet);
     }
     
   /***************************************************************************************************************/
    // check if  this token is a leaf token, that is it has no dependencies
   // which means it is not the Governor of any relationship
   //------------------------------------------------------------------------------------------------/

       public boolean isLeaf()
       {
           ArrayList<DependencyRelation>  dependencies = 
                        (ArrayList)getFeature("dependencies");
           return (dependencies == null );
       }
    /*--------------------------------------------------------------------------------------------------*/
     // get a dependent annotations  of a given type where self is the Governor
     // Note: Assumes single object or the first one in a set
     /*--------------------------------------------------------------------------------------------------*/
       public int getDependentAnnotationId (String relation)
       {
           ArrayList<DependencyRelation>  dependencies = 
                        (ArrayList)getFeature("dependencies");
           if (dependencies == null )
               return -1;
          for (int i = 0; i < dependencies.size(); i++)
          {
              if (dependencies.get(i).getType().equalsIgnoreCase(relation))
                  return dependencies.get(i).getTargetId();
          }
          return -1;        // no such dependency
          
       }  
       
     /*--------------------------------------------------------------------------------------------------*/
     // get a dependent annotations  of a given type where self is the Governor
    // Covers multiple annotations with the same relationship
     /*--------------------------------------------------------------------------------------------------*/
       public int[]  getDependentAnnotationIds (String relation)
       {
           ArrayList<DependencyRelation>  dependencies = 
                        (ArrayList)getFeature("dependencies");
           if (dependencies == null )
               return null;
           ArrayList<Integer> rels = new ArrayList();
          for (int i = 0; i < dependencies.size(); i++)
          {
              if (dependencies.get(i).getType().equalsIgnoreCase(relation))
                 rels.add(dependencies.get(i).getTargetId());
          }
          if (rels.isEmpty())
              return null;
          
          int n = rels.size();
          int[] relIds = new int[n];
          for (int i = 0; i < n; i++)
              relIds[i] = rels.get(i).intValue();
          return  relIds;        // no such dependency      
       }  
       /*--------------------------------------------------------------------------------------------------*/
       // get a dependent annotations  of a given type where self is the Governor
       //
       public TextAnnotation1  getDependentAnnotation (
                    String relation, ArrayList <TextAnnotation1> tokens)
       {
          int annotId = getDependentAnnotationId (relation);
          if (annotId == -1)
              return null;
          return getAnnotationById(tokens, annotId);
       }    
   /*--------------------------------------------------------------------------------------------------*/          
    // get all dependent annotations of a given type where self is the Governor
    // Possiblity of more than one object
     /*-------------------------------------------------------------------------------------------------  
        public int[] getDependentAnnotationIds(String relationship)
       {
           ArrayList<DependencyRelation>  
               dependencies = (ArrayList) getFeature("dependencies");
           if (dependencies == null )
               return null;  // no  dependency

           ArrayList <DependencyRelation> adep = new ArrayList();
          for (int i = 0; i < dependencies.size(); i++)
          {
              if (dependencies.get(i).getType().equalsIgnoreCase(relationship))
                  adep.add(dependencies.get(i));
          }
          if (adep.size() == 0)
              return null;       // no such dependency
          int[] ids = new int[adep.size()];
          for (int i = 0; i < ids.length; i++)
          {
              ids[i] = adep.get(i).getTargetId();
          }
          return ids;      
       }     
   */     
    /*--------------------------------------------------------------------------------------------------*/          
    // get all dependent annotations of a given type where self is the Governor
    // Possiblity of more than one object
     /*--------------------------------------------------------------------------------------------------*/       
        public ArrayList<TextAnnotation1> getDependentAnnotations(String relationship,  
                    ArrayList <TextAnnotation1> tokens)
       {
           ArrayList<DependencyRelation>  
               dependencies = (ArrayList) getFeature("dependencies");
           if (dependencies == null )
               return null;  // no  dependency

           ArrayList <DependencyRelation> deps = new ArrayList();
          for (int i = 0; i < dependencies.size(); i++)
          {
              if (dependencies.get(i).getType().equalsIgnoreCase(relationship))
                  deps.add(dependencies.get(i));
          }
          if (deps.isEmpty())
              return null;       // no such dependency
          
          ArrayList <TextAnnotation1> relations = new ArrayList();   
          for (int i = 0; i < deps.size(); i++)
          {
              int relId = deps.get(i).getTargetId();
              relations.add( getAnnotationById(tokens, relId));
          }
          return relations;
       }     
        
       /******************************************************************************************************/
        // get the set of annotations with are conjunct to the given object,  along with the conjuct
        // Each element of the returned ArrayList s a two-annotation object:
        // First annotation is the conjunct word, next one is the conjunction (such as and, or, but)
        //
        // Note: for Stanford Depency Parser example: 
        // Bell, based in Los Angeles, makes and distributes electronic, computer and building products.
        // token "electronic" has dependencies=[conj(36), cc(38), conj(40)] and no relation between 38, 40
        /******************************************************************************************************/
        public ArrayList<TextAnnotation1[]> getConjuncts(ArrayList tokens)
        {            
            ArrayList<TextAnnotation1[]> conjArray = new ArrayList();
            TextAnnotation1[] conjunctAnnots = getNextConjuncts( tokens);
            if ( conjunctAnnots == null)
                return null;
            conjArray.add( conjunctAnnots);
          
              // Now check for additional  conjuncts
              TextAnnotation1 lconj = conjunctAnnots[0];
              while (lconj != null)
              {
                    TextAnnotation1[] lconjunctAnnots = lconj.getNextConjuncts( tokens);
                   if (lconjunctAnnots == null)
                       break;
                    conjArray.add( conjunctAnnots);
                    lconj = lconjunctAnnots[0];
              }
              return conjArray;
        }

    /******************************************************************************/
    public TextAnnotation1[]  getNextConjuncts(ArrayList tokens)
    {           
         TextAnnotation1 cc = getDependentAnnotation ("cc", tokens);
         if (cc == null)
             return null;
     
         // get the conjuct word
         ArrayList<TextAnnotation1> conjuncts  = cc.getDependentAnnotations("conj", tokens);
          if (conjuncts == null)
              return null;          // TBD: Error message
          
          int n = conjuncts.size() + 1;
          TextAnnotation1[] cta = new TextAnnotation1[n];
          cta[0] = cc;
          for (int i = 1; i < cta.length; i++)
              cta[i] = conjuncts.get(i-1);
          return  cta;
    }       
    
    /*****************************************************************************************************/
    // Get the Governor token for the specified relationship (dependency)  corresponding to self
    // If no such relation found return NULL
    /*****************************************************************************************************/
    public TextAnnotation1 getGovernorAnnotation(ArrayList <TextAnnotation1> tokens, String relation)
    {
        for (int i = 0; i < tokens.size(); i++)
        {
            TextAnnotation1 token =  tokens.get(i);
            int[] auxIds  = token.getDependentAnnotationIds(relation);     // relation's auxiliary for this token
            if  (auxIds  == null || auxIds.length == 0)           // no such auxilliary
                    continue;                   // token is the governer
            for (int j = 0; j < auxIds.length;  j++)
            {
                if (auxIds[j] == this.id)           // token has "this" as the dependant of specified relation
                    return  (token);
            }
        }
        return null;            // no such relation found
    }
    
    /*******************************************************************************/
    // Get the morphological root of a given word ( useful for plurals and verbs)
    public  String getRootWord()
    {
        return (String) getFeature(NLPRelations.MORPHO_ROOT);
    }

  //-----------------------------------------------------------------------------    
     // For accessing token attributes according to ordered sequene in a sentence  
     // for simplicity, we only compare the annotation's start offsets  here, although they are
     // not unique  within a sentence
     public int  compareTo ( Object obj)
     {
        TextAnnotation1 ta = (TextAnnotation1)obj;
         if (offsets[0] == ta.offsets[0])
             return 0;
         return (offsets[0] <  ta.offsets[0]) ? -1 : 1;
     }     
       //--------------------------------------------------------------------------------------------    
// get the full annotation from a list which completely contains the given annotation
    public static   TextAnnotation1 getContainingAnnot(ArrayList<TextAnnotation1> alist, 
        TextAnnotation1 annot)
    {
        for (int i = 0; i < alist.size(); i++)
        {
            if (alist.get(i).contains(annot))
                return alist.get(i);
        }
        return null;
    }
    
    //-------------------------------------------------------------------------------------------------/
// Return the Gate Annotation corresponding to this TextAnnotation
//-------------------------------------------------------------------------------------------------/
    public static Annotation getGateAnnotation(int id, Document gateDoc)
    {
        AnnotationSet annotations = gateDoc.getAnnotations();
        Annotation annot = annotations.get(new Integer(id));
        return annot;
    }
    //--------------------------------------------------------------------------------------------------------
    // Check if this token is related to another token by a "discourse" relation
    //--------------------------------------------------------------------------------------------------------
    public boolean isDiscourseToken(ArrayList<TextAnnotation1> tokenList)
    {
        TextAnnotation1 govAnnotation = getGovernorAnnotation(tokenList, "discourse");
        return (govAnnotation != null);             // some annotation referes to it as discourse
    }    
    
      /**************************************************************/
    // static functions 
    //--------------------------------------------------------------------------------/
    // Is a given token a noun? Note: also includes pronouns
    //
      public static  boolean isNoun(TextAnnotation1 token)
      {
          String cat = (String)token.getFeature("category");
           for (int i = 0; i < NOUN_CATEGORIES.length; i++)
          {
              if (NOUN_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
      
      public static  boolean isPersonNoun(TextAnnotation1 token)
      {
          String cat = (String)token.getFeature("category");
           for (int i = 0; i < PERSON_NOUN_CATEGORIES.length; i++)
          {
              if (PERSON_NOUN_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
      
     //--------------------------------------------------------------------------------/
     // Is a given token a pronoun? Specifically tests for pronouns
      public static  boolean isPronoun(TextAnnotation1 token)
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
      public static  boolean isNumber(TextAnnotation1 token)
      {
          String cat = (String)token.getFeature("category");
          for (int i = 0; i < CARDINAL_CATEGORIES.length; i++)
          {
              if (CARDINAL_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
    // Is a given token a verb?
      public static  boolean isVerb(TextAnnotation1 token)
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
      public static  boolean isAdjective(gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation token)
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
      public static  boolean isCardinal(gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation token)
      {
          String cat = (String)token.getFeature("category");
          for (int i = 0; i < CARDINAL_CATEGORIES.length; i++)
          {
              if (CARDINAL_CATEGORIES[i].equals(cat))
                  return true;
          }
          return false;
      }
} 

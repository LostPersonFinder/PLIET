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

import gov.nih.nlm.lpf.emails.nlpproc.structure.LinkedAnchorMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class Clause implements ClausalConstants
{
    private static Logger log = Logger.getLogger(Clause.class);
    
    static String[] ClausalDependency = {
            "xcomp", "ccomp",  "advcl",  "partmod", "rcmod", "purpcl", "pcomp","dep"};
    static int[] ClausalAction =   {
         COMPLEMENT_CLS, OBJECT_CLS, MODIFIER_CLS,  PARTMOD_CLS, 
         RCMOD_CLS, PURPOSE_CLS, PREPOSITION_CLS, UNKNOWN_DEP_CLS
     };

    
    // Find the intro word:  depending upon the link type
    //public static   String[] ClauseTypes = {"advcl", "dep", "ccomp",  "purpcl", "xcomp"};
   // public static   String[] introTypes = {"marker", "aux", "complm", "", "aux"};

    // Attributes of a clause
    
    public boolean adjectival;                      // if true, adjective is used as a verb of the clause
    public int clauseLevel;                            // 1 => primary clauses
    public String clauseType;                      // :relation to parent xcomp", "ccomp", etc, for subordinates
    public int clauseUsage;                        // from Clausal Action:  is used as  an object , adjective, ... etc
    public boolean isInterrogative;                             // is the sentence a it a question? 
    
    public TextAnchor clauseHead;           // Head word/verb within the clause 
    public TextAnchor clausalGovernor;   // Governor (A verb/adjective/noun) of this clause ( clauseHead of parent clause)
    //public TextAnchor   governorAnchor;   // which mentions the clauseHead as dependent  

    public String  introText;                           // link/marker such as to, for, etc:, which introduces  the clause 
    public TextAnchor subject;                    // Subject  (Anchor) for this clause
    
    // Relationship to other phrases  in the sentence
    public LinkedAnchorMap verbObjectMap;           // phrasal objects associated wit the verb
    public LinkedAnchorMap subjObjectMap;            // phrasal objects associated wit the subject
    
    public Clause parentClause;                               // parent clause of this clause, null if a main clause
    public ArrayList<Clause> xcompClauses;        // clauses that are connected to it with "xcomp" relations, or empty
    public ArrayList<Clause>  subordinates;          // lower level clauses : empty if not complex
    
    // housekeeping - must be set for primary clauses
    public TextAnnotation sentence;                         // sentence containing the clause, 

     // default constructor
     public Clause( TextAnchor head, String type, TextAnchor gov)
     {
          this( head, type,  "",   gov);
     }
     
     public Clause(TextAnchor head, String relation, String marker,  TextAnchor gov)
    {
        adjectival =  false;                        // default: main verb (clause head) is a verb, not adjective
        clauseType  = relation;
        clauseHead = head;
        clausalGovernor = gov;                  // The mentioning  anchor (verb or noun or Adj  - Clause type dependent
             
        // following will be filled in later through caller or lower level clauses
        subject= null;                  
        parentClause = null;         
        xcompClauses = new ArrayList();
        subordinates = new ArrayList();
        introText = "";
        clauseLevel = 1;       // default
    }
     
     public void setAdjectivalVerb(boolean isAdjective)
     {
         adjectival = isAdjective;
     }
     
      public boolean isAdjectivalVerb()
     {
         return adjectival;
     }
 
     /************************************************************************
      * Set the higher level clause to which it is a subordinate or complement etc.
      * @param parent
      * @param relation 
      */
     public void setParentClause(Clause parent, String marker)
     {
        parentClause = parent;
        if (parentClause == null)
        {
            clauseLevel = 1;
             setClauseUsage("");
        }
         else
         {
             setClauseUsage(clauseType);
             clauseLevel = parentClause.clauseLevel+1;
             parentClause.addSubordinate(this);
             sentence  = parentClause.sentence;
              isInterrogative = false;                              // not used
             
             setClauseIntroText(marker);         // text preceding the clause
         }
    } 
         /*---------------------------------------------------------------------------------------------------*/
     protected void setClauseUsage(String clauseType)
     {
       // check if it is used as an object of the parent clause, with its internal subject
         clauseUsage = -1;
         if (clauseLevel == 1)          // a primary clause
         {
            clauseUsage = STATEMENT_CLS;         // top level;
            return;
         }
        for (int i = 0; i < ClausalDependency.length; i++)
        {
            if (clauseType.equals(ClausalDependency[i]))
            {
                clauseUsage = ClausalAction[i];
                break;
            }
        }
        if (clauseUsage == -1)
            log.error("Could not find Clause action for relation " + clauseType);
     }
     
    /************************************************************************/  
     public int getClauseUsage ()
     {
         return clauseUsage;
     }
   
       /*----------------------------------------------------------------------------------------------------*/ 
    // The clause is used an equal (for xcomp relations) if there is no Intro text
    public void  setClauseIntroText(String intro)
    {
       introText = (intro == null) ? "" :  intro.toLowerCase();
    }    
        
    
   /*----------------------------------------------------------------------------------------------------*/ 
    // The clause is used an equal (for xcomp relations) if there is no intro  text
    public String getClauseIntroText()
    {
        return introText;
    } 
  
 
/*----------------------------------------------------------------------------------------------------*/    
   /* public static String getintro(String clauseType)
    {
        for (int i = 0; i < ClauseTypes.length; i++)
        {
            if (clauseType.equals(ClauseTypes[i]))
                return introTypes[i];
        }
        return null;
    }*/
 
    /***************************************************************************************/  
    public void setSentence(TextAnnotation sent, boolean interrogative)
    {
        sentence = sent;
       isInterrogative = interrogative;
    }
    
     /***************************************************************************************/    
    
  // invoked from lower level sub
     protected void addSubordinate(Clause sub)
    {
        subordinates.add(sub);  
        if (sub.clauseType.equals("xcomp"))
             addComplement(sub);            // also store in a  separate list
            
    } 
      /***************************************************************************************/        
     
     public boolean hasSubordinates()
     {
         return (subordinates.isEmpty());
     }
     
     public ArrayList<Clause>  getSubordinates()
     {
         return subordinates;
     }
    /*--------------------------------------------------------------------------------------------------*/ 
      protected void addComplement(Clause xcomp)
    {
        xcompClauses.add(xcomp);                // xcomp clauses
    }      
      /*--------------------------------------------------------------------------------------------------*/
      
     public boolean isLeafClause()
     {
         return subordinates.isEmpty();
     }
     
     /*--------------------------------------------------------------------------------------------------*/
     public boolean isPrimaryClause()
     {
         return (clauseLevel == 1);
     }
     /*--------------------------------------------------------------------------------------------------*/
     
     // does it have xcomp clauses which may share the same subject
     public boolean  hasComplementClause()
     {
         return (!xcompClauses.isEmpty());
     }
     /*--------------------------------------------------------------------------------------------------*/
     // Get the location of the clause (i.e. sentence Id and Verb Id of the clause
     public TextAnnotation  getSentence()
     {
         return sentence;
     }   
  
  /********************************************************************************/   
   //   Retrieve various  tyes of clauses attached to verb/subject/objects of this clause 
   /*--------------------------------------------------------------------------------------------------*/
     // Get the modifier clause, if any,  for the verb 
  /*--------------------------------------------------------------------------------------------------*/
     public ArrayList<Clause> getVerbModifierClauses()
     { 
         if (subordinates.isEmpty())
             return null;
         ArrayList adjCls = new ArrayList();
         for (int i = 0; i < subordinates.size(); i++)
         {
             Clause sub = subordinates.get(i);
             int usage = sub.getClauseUsage();
             if (usage == MODIFIER_CLS || usage == UNKNOWN_DEP_CLS)
                 adjCls.add(sub);
             // Partmod could be both for noun or verb
             else if (usage == PARTMOD_CLS && (sub.clausalGovernor.getType() == TextAnchor.VERB_ANCHOR))
                 adjCls.add(sub);
         }
         return adjCls.isEmpty() ? null : adjCls;
     }
     /*------------------------------------------------------------------------------------------------------*/
     public ArrayList<Clause> getSubjectModifierClauses()
     {
         if (subordinates.isEmpty())
             return null;
         ArrayList modCls = new ArrayList();
         for (int i = 0; i < subordinates.size(); i++)
         {
             Clause sub = subordinates.get(i);
             if (sub.clausalGovernor != this.subject)
                 continue;
             int usage = sub.getClauseUsage();
             if (usage == RCMOD_CLS || usage == PARTMOD_CLS)
                    modCls.add(sub);
         }
         return modCls.isEmpty() ? null : modCls;
     }
  
   /*------------------------------------------------------------------------------------------------------*/
       public Clause getObjectModifierClause(TextAnchor obj)
       {
         if (subordinates.isEmpty())
             return null;
         
         for (int i = 0; i < subordinates.size(); i++)
         {
             Clause sub = subordinates.get(i);
             if (sub.clausalGovernor != this.subject)
                 continue;
             int usage = sub.getClauseUsage();
             if (usage == RCMOD_CLS || usage == PARTMOD_CLS)
                    return sub;
             
             // recurse for lower level objects
             ArrayList <Clause> lowerSubs = sub.subordinates;
             if (lowerSubs == null || lowerSubs.isEmpty())
                 continue;
              
              for (int j = 0; j < lowerSubs.size(); j++)
              {
                  Clause  lcls = lowerSubs.get(j).getObjectModifierClause(obj);
                  if (lcls != null)
                     return lcls;
              }
         }
         return null;
     }
 /*--------------------------------------------------------------------------------------------------*/
     // Get the Object  (ccomp) clause, if any,  for the verb 
  /*--------------------------------------------------------------------------------------------------*/
     public Clause  getObjectClause()
     { 
         if (subordinates.isEmpty())
             return null;
         ArrayList ccompCls = new ArrayList();
         for (int i = 0; i < subordinates.size(); i++)
         {
             Clause sub = subordinates.get(i);
             int usage = sub.getClauseUsage();
             if (usage == OBJECT_CLS)
                 return sub;
         }
         return null;
     }
     /*--------------------------------------------------------------------------------------------------*/    
       // A preposition clause could be attached to a subject, verb or object
      /*--------------------------------------------------------------------------------------------------*/   
       public Clause getPrepositionClause(TextAnchor obj)
       {
         if (subordinates.isEmpty())
             return null;

         for (int i = 0; i < subordinates.size(); i++)
         {
             Clause sub = subordinates.get(i);
             int usage = sub.getClauseUsage();
             if (usage == PREPOSITION_CLS && sub.clausalGovernor == obj)
                return sub;

             // recurse for lower level objects
             ArrayList <Clause> lowerSubs = sub.subordinates;
             if (lowerSubs == null || lowerSubs.isEmpty())
                 return null;
                 
              for (int j = 0; j < lowerSubs.size(); j++)
              {
                  Clause   lcls = lowerSubs.get(j).getPrepositionClause(obj);
                  if (lcls != null)
                      return lcls;
              }
         }
         return null;
     }
       
    /*--------------------------------------------------------------------------------------------------*/
    // get the offsets in the sentence bounding ths clause
       public TextAnnotation[]  getClauseRange()
       {
           ArrayList <TextAnnotation>  annotations = clauseHead.getCoveringAnnotations();
           Collections.sort(annotations);
           TextAnnotation[] range = new TextAnnotation[2];
           range[0] = annotations.get(0);
           range[1] =  annotations.get(annotations.size()-1);
           return range;
       }
       
/*--------------------------------------------------------------------------------------------------*/
// get the text covered by this clause
   public  String getClauseText(String documentText)
   {
      TextAnnotation[] range = getClauseRange();
      Integer start = range[0].offsets[0];
      Integer end = range[1].offsets[1];
      String clauseText = documentText.substring(start, end);
       return clauseText;
   }
 /*--------------------------------------------------------------------------------------------------*/

     public String toPrint()
     {
         String str = "Clause Level: " + clauseLevel + ", Type: " + clauseType  +", Head: " + clauseHead.getText();
         str += ", Introductory word: " + introText;
          if (subject != null)
              str += ", Subject: " + subject.getCoveringText();
         if (parentClause != null) str += "\n Parent Clause: " + parentClause.clauseHead.getText() + ", Governor: " + clausalGovernor ;
         
         // Print phrasal objects of the Verb
         if (verbObjectMap != null && verbObjectMap.size() > 0)
         {
            String ostr = "\n  Verb  objects: " ;
            Iterator <String> it = verbObjectMap.keySet().iterator();
            while (it.hasNext())
            {
                String link = it.next();
                ArrayList <TextAnchor> objects = verbObjectMap.get(link);
                ostr += "\n    Link: " + link +" --" ;
                for (int i = 0; i < objects.size(); i++)
                {
                    TextAnchor obj = objects.get(i);
                    if (obj != null)
                    //ostr += obj.toFullString();
                    {
                        if (i > 0) ostr += "; ";
                        ostr += obj.getCoveringText();
                    }
                }  
             }
            str += ostr;
         }
         
         // check for objects of the Subject
         if (subjObjectMap != null && subjObjectMap.size() > 0)
         {
            String ostr = "\n  Subject  objects: " ;
            Iterator <String> it = subjObjectMap.keySet().iterator();
            while (it.hasNext())
            {
                String link = it.next();
                ArrayList <TextAnchor> objects = subjObjectMap.get(link);
                ostr += "\n    Link: " + link +" --" ;
                for (int i = 0; i < objects.size(); i++)
                {
                    TextAnchor obj = objects.get(i);               
                    //ostr += obj.toFullString();
                    if ( i > 0) ostr += ", ";
                    ostr += obj.getCoveringText();
                }  
             }
            str += ostr;
         }
         
         if (subordinates != null)
         {
             String subStr = "\n    Number of subordinate clauses: " + subordinates.size();
             for (int i = 0; i < subordinates.size(); i++)
                    subStr += "\n" + subordinates.get(i).toPrint();        
             str += subStr;
         }
         return str;
     }   

         
}

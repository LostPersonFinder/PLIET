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
package gov.nih.nlm.lpf.emails.nlpproc.extract;

import gate.Annotation;
import gate.AnnotationSet;

import gate.FeatureMap;
import  gate.stanford.DependencyRelation;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;

/**
 *
 * @author 
 */
public class FirstPersonAnphMatcher
{
    protected ArrayList<Annotation> pronounList;
    protected ArrayList<Annotation>   docAnnotationList;
    protected AnnotationSet   docAnnotationSet;
    public FirstPersonAnphMatcher( ArrayList<Annotation> prnList, AnnotationSet  docAnnots)
    {
       pronounList = prnList;
       docAnnotationSet = docAnnots;
    }

    TreeMap<String, Annotation> getMatches( )
    {
       docAnnotationList = new ArrayList(docAnnotationSet);  
        AnnotationSet persons = docAnnotationSet.get("Person");
         
       TreeMap<String, Annotation> anaphoraMap = new TreeMap();
       
       for (int i = 0; i < pronounList.size(); i++)
       {
           Annotation anaphor  = pronounList.get(i);
           Annotation  antcToken = getCopularAntecedent (anaphor);
           if (antcToken == null) 
               continue;
         
           // If there is a Person in the AnnotationSet corresponding to this antecedent Token, get it
           Iterator <Annotation> it = persons.iterator();
           while (it.hasNext())
           {
               Annotation person = it.next();
               if (person.overlaps(antcToken))
               {
                   antcToken = person;
                   break;
               }
           }
           String anphText = (String)anaphor.getFeatures().get("string");
           anaphoraMap.put(anphText.toLowerCase(), antcToken);
       }
       // setUnresolvedAntecedents( anaphoraMap);
       return anaphoraMap;
    }
  /*---------------------------------------------------------------------------------------------------------------------------*/
  // Check if the given first person  anaphor (I,  my, we, us, etc.) is the subject a term (noun)
  // which has a corresponding  copula verb "be"
  // then we don't want to resolve the anaphora
  // For example:  "I am John"   prnAnnot and ptoken => I,  annot => John
  // copularVerb = am  subject of am  =>I , which matches prnAnot
  // Second block checks for the case:  "my name is John", starting with the prnAnnot "my"
  //----------------------------------------------------------------------------------------------------------------------------*/
    protected  Annotation getCopularAntecedent (Annotation prnAnnot)
    {
        Long startOffset =prnAnnot.getStartNode().getOffset();
        Long endOffset = prnAnnot.getEndNode().getOffset();
        
        Annotation ptoken = null;           // same pronoun annotation in the Token set
        AnnotationSet tokens =  docAnnotationSet.get("Token", startOffset, endOffset);
        if (tokens == null)
            return null;
        Iterator<Annotation> it = tokens.iterator();
        while (it.hasNext() && ptoken == null)
        {
            Annotation next =it.next();
            if (next.coextensive(prnAnnot))         // same offsets
                ptoken = next;   
           }

        //  Find if this  annotation is the object of a copular verb. (I am John => [am, John])
        // If so, get its object
        //
        for (int i = 0; i <  docAnnotationList.size(); i++)
        {
            Annotation annot = docAnnotationList.get(i);      
            FeatureMap features = annot.getFeatures();
            if (features == null)       // for email "body",  "subject" etc.
                 continue;
            
            // is it a noun?
            String category = (String)annot.getFeatures().get("category");
            if (category == null || !category.matches("NN|NNP|NNPS"))       // include NN for relatives such as brother, friends etc.
                continue;
            
            // is it object of a Copular Verb?
            Annotation copularVerb= getDependentAnnotation(annot, "cop", docAnnotationSet);
            if (copularVerb == null)
                continue;
            else
            {
                // check the verb through its morph  (is/was/am/are => be)
                String morph = (String)copularVerb.getFeatures().get("morph");
                 if (!morph.equals("be"))           
                     continue;
            }
            // Check if the subject of this token matches the pronoun
            // if so, this is the antecedent
            Annotation subject = getDependentAnnotation(annot, "nsubj|nsubjpass", docAnnotationSet);
            if ( subject != null)
            {
                if (subject.coextensive(prnAnnot))             
                {
                     Annotation antecedent = annot;
                     return antecedent;
                }
                else            
                {
                     // check if it is the subject is "name", and its possessive is this token
                    // must match IDs in the Token set
                     String subjStr = (String)subject.getFeatures().get("string");
                     Annotation poss  = getDependentAnnotation(subject, "poss", docAnnotationSet);
                     if (subjStr.equalsIgnoreCase("name") && (poss != null) && poss.getId() == ptoken.getId())
                     {
                         Annotation antecedent = annot;
                         return antecedent;
                     }
                }
            }
        }       // end - for: docAnnotationList.
         return null;       
    }          
    /*----------------------------------------------------------------------------------------*/
   // Find a dependent relation for a given annotation  in the set      
    public Annotation  getDependentAnnotation(Annotation token, 
        String relationship, AnnotationSet docAnnotations)
    {
      if (token.getFeatures() == null)
          return null;
       ArrayList<DependencyRelation>  
           dependencies = (ArrayList) (token.getFeatures().get("dependencies"));
       if (dependencies == null )
           return null;  // no  dependency

      for (int i = 0; i < dependencies.size(); i++)
      {
          if (dependencies.get(i).getType().matches(relationship))
          {
              int targetId = dependencies.get(i).getTargetId();
              return docAnnotations.get(targetId);
          }
      }  
      return  null;       // no such dependency
    }         
    
    
    protected void setUnresolvedAntecedents(TreeMap<String, Annotation> anaphoraMap)
    {
        // perform second pass, setting unresolved anaphora
        Iterator <String> it = anaphoraMap.keySet().iterator();
        Annotation person = null;
        while (it.hasNext())
        {
            String prn = it.next();
            person = anaphoraMap.get(prn);
            if (prn.matches("i|me|my|mine") && person != null)
                break;
        }
         while (it.hasNext())
        {
            String prn = it.next();
            if (anaphoraMap.get(prn) == null)
            person = anaphoraMap.get(prn);
            anaphoraMap.put(prn, person);
        }
         return;
    }
}

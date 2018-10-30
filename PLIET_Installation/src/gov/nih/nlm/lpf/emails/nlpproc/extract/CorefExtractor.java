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
import gate.Document;
import gate.FeatureMap;

import  gate.stanford.DependencyRelation;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class CorefExtractor
{
    private  static final Logger log = Logger.getLogger(CorefExtractor.class);
    
    Document gateDoc;
   // HashMap <Integer, AnaphoraInfo> anaphoraList = new HashMap();
    AnnotationSet docAnnotationSet;
    String documentText;
    
    public CorefExtractor(gate.Document aGateDoc)
    {
        gateDoc = aGateDoc;
        docAnnotationSet = gateDoc.getAnnotations();
        documentText = gateDoc.getContent().toString();
    }
    
    /****************************************************************************/
     public   TreeMap<Annotation, Annotation>  extractCorefInfo()
     {
        AnnotationSet attrSet = docAnnotationSet.get("Attribute");
        if (attrSet == null || attrSet.size() == 0)
          return null;
        
         ArrayList<Annotation> attrList = new ArrayList();
         attrList.addAll(attrSet);
        Collections.sort(attrList,  new gate.util.OffsetComparator ());

         //ArrayList  <CoreferenceInfo> prnAnaphor = buildPronounCoreferences(pronounSet);
        TreeMap<Annotation, Annotation> corefAnnotMap = storePass1Info(attrList);
         return corefAnnotMap;
     }

             
    //----------------------------------------------------------------------------------------------------------------------
    // Add anaphoric information by direct "Name" matching rule.
    // For example:  I saw an injured  man.  His name was  David.
    // Here we save "David" as the Coref for "man", connected through the "name" attribute
    //----------------------------------------------------------------------------------------------------------------------
    protected TreeMap<Annotation, Annotation> storePass1Info(
                ArrayList<Annotation> attrList )
    {
        TreeMap<Annotation, Annotation> corefMap = new TreeMap();
        
       for (int i = 0; i < attrList.size(); i++)
        {
            Annotation attrAnnot = attrList.get(i);
            String annotText = documentText.substring(
                attrAnnot.getStartNode().getOffset().intValue(),
                attrAnnot.getEndNode().getOffset().intValue());

           FeatureMap features = attrAnnot.getFeatures();
           String type = (String)features.get("type");
           if ( type == null || !(type.equals("name") || type.equals("name_passive")) )
               continue;
                
           Annotation nameAnnot = attrAnnot;
            //CorefInfo corefInfo = resolveNameCoref(attrAnnot, docAnnotationSet);
            
            // retrieve the two annotations linked by the :"name" attribute
           Annotation[] corefAnnots = null;
           if (type.equals("name"))
                corefAnnots = getNamedCorefPair( nameAnnot, docAnnotationSet);
           else if (type.equals("name_passive"))
               corefAnnots = getPassiveNamedCorefPair( nameAnnot, docAnnotationSet);
            if (corefAnnots != null)
                corefMap.put(corefAnnots[0], corefAnnots[1]);
        }
        return corefMap;
    }
 
    /*----------------------------------------------------------------------------------------------*/
    // Get the coreferenced pair connected by "name" as the subject
    // such as My son's name is David. His name is John. (son-<David, His -> John)
   /*----------------------------------------------------------------------------------------------*/
  protected Annotation[] getNamedCorefPair(Annotation nameAnnot, AnnotationSet docAnnotationSet)
  {
      Annotation nameToken =  getMatchingAnnotation(nameAnnot,  docAnnotationSet, "Token", true);
      
      // find the possesive noun/pronoun token  for this name (His, My...)
     int possId = getDependentAnnotationId(nameToken, "poss");
     Annotation possToken = (possId  > 0) ? docAnnotationSet.get(possId) : null;    //(son, my)
     
     // find the object token for which this is the name 
     int personId = getGovernorAnnotationId(nameToken, "nsubj");
     Annotation personToken = (personId > 0) ?  docAnnotationSet.get(personId) : null;   
         
      if (possToken != null && personToken != null)
         return (new Annotation[]{possToken, personToken});
     else
         return null;
  }
  
  
    /*----------------------------------------------------------------------------------------------*/
    // Get the coreferenced pair connected by "named" modifier (as "partmod" dependency)
    // such as:  a boy named  David.
   /*----------------------------------------------------------------------------------------------*/
  protected Annotation[] getPassiveNamedCorefPair(Annotation nameAnnot, AnnotationSet docAnnotationSet)
  {
      // The connection is between the "partmod" governor (girl) and the "dobj" relation (David)
      Annotation nameToken =  getMatchingAnnotation(nameAnnot,  docAnnotationSet, "Token", true);
      
      //find the subjectToken of the "partmod" relationship
      int  subjectId = getGovernorAnnotationId(nameToken, "partmod");
     Annotation  subjectToken =  (subjectId  > 0) ? docAnnotationSet.get(subjectId) : null;    //(David)
            
     int dobjId = getDependentAnnotationId(nameToken, "dobj");
     Annotation personToken = (dobjId  > 0) ? docAnnotationSet.get(dobjId) : null;    //(David)
   
      if (subjectToken   != null && personToken != null)
         return (new Annotation[]{subjectToken, personToken});
     else
         return null;
  }
  
    /**************************************************************************************************
     * Get the annotation in the AnnotationSet of a given type which corresponds to the input
     * annotation (in a different set). Note the offsets should be the same of both annotations
     ***************************************************************************************************/
    public Annotation getMatchingAnnotation(Annotation annot, AnnotationSet annotSet, 
            String type, boolean exactMatch)
    {
          Long startOffset = annot.getStartNode().getOffset();
          Long endOffset = annot.getEndNode().getOffset();
           
           // find the matching annotation in the Location list
           AnnotationSet  setToMatch = annotSet.get(type, startOffset, endOffset);
           if (setToMatch== null ||setToMatch.isEmpty())
               return null;         // none matching the given annotations offsets
           else
           {
               AnnotationSet  matchingAnnots = annotSet.get(type, startOffset, endOffset);
               if (matchingAnnots == null)
                   return null;
    
               if (exactMatch) 
               { 
                   return (matchingAnnots.size() == 1) ? matchingAnnots.iterator().next() : null;     // get the exact matching one
               }
              else
                     return matchingAnnots.iterator().next();  // return the first one anyway
            }
    } 
    /*-----------------------------------------------------------------------------------------------------------*/
       public int  getDependentAnnotationId (Annotation annot, String relation)
       {
           if (annot.getFeatures() == null)
               return -1;
           ArrayList<DependencyRelation>  dependencies = 
                    (ArrayList<DependencyRelation>) annot.getFeatures().get("dependencies");
           if (dependencies == null )
               return -1;
          for (int i = 0; i < dependencies.size(); i++)
          {
              if (dependencies.get(i).getType().equalsIgnoreCase(relation))
                  return dependencies.get(i).getTargetId();
          }
          return -1;
       }
   
     /*-----------------------------------------------------------------------------------------------------------*/  
    // Get the Annotation  which is the governor of a relation for this Annotation
     public int  getGovernorAnnotationId ( Annotation annot, String relation)
     {
         Iterator<Annotation>  it = docAnnotationSet.iterator();
         while (it.hasNext())
         {
             Annotation govAnnot = it.next();
             int depId = getDependentAnnotationId(govAnnot, relation);
             if ( depId == annot.getId())      
                 return govAnnot.getId(); 
        }
         return -1;
     }
}
/*
 * *---------------------------------------------------------------------------------------------------------------------------*/   
//   // Add cataphoric information by direct "Name" matching rule.
// For example:  I saw an injured  man.  His name was  David.
// Here we save "David" as the Coref for "man"
 /*
 * protected CorefInfo  resolveNameCoref(Annotation nameAnnot, AnnotationSet docAnnotationSet)
  {
      Annotation nameToken =  getMatchingAnnotation(nameAnnot,  docAnnotationSet, "Token", true);
      
      // find the possesive noun/pronoun token  for this name (his, man's,...)
     int possId = getDependentAnnotationId(nameToken, "poss");
     Annotation possAnnot = (possId  > 0) ? docAnnotationSet.get(possId) : null;
     
     // find the object token for which this is the name 
     Annotation personToken = null;
     Iterator<Annotation>  it = docAnnotationSet.iterator();
     while (it.hasNext())
     {
         Annotation annot = it.next();
         int subjId = getDependentAnnotationId(annot, "nsubj");
         if (subjId == nameToken.getId())          // 
         {
             personToken =  annot;         // Actual value (governor token) for name
             break;
         }
     }
       // Fill in the info for the Coref 
       CorefInfo corefInfo = new CorefInfo();
       corefInfo.anaphor = possAnnot;
       corefInfo.anaphorText = (String)possAnnot.getFeatures().get("string");

        // save the person info as antecedent
       corefInfo.coref = personToken;
       corefInfo.corefTokenId = personToken.getId().intValue();
       corefInfo.corefText = (String)personToken.getFeatures().get("string");

       // sentence ID of the anaphor (i.e. hos or son etc.
       AnnotationSet sentences =  docAnnotationSet.getCovering("Sentence", 
               corefInfo.anaphor.getStartNode().getOffset(),corefInfo.anaphor.getEndNode().getOffset());
       corefInfo.anaphorSentenceId = sentences.iterator().next().getId().intValue();
       return corefInfo;
  }
/*-----------------
       // Fill in the info for the Coref 
       CorefInfo corefInfo = new CorefInfo();
       corefInfo.anaphor = possAnnot;
       corefInfo.anaphorText = (String)possAnnot.getFeatures().get("string");

        // save the person info as antecedent
       corefInfo.coref = personToken;
       corefInfo.corefTokenId = personToken.getId().intValue();
       corefInfo.corefText = (String)personToken.getFeatures().get("string");

       // sentence ID of the anaphor (i.e. hos or son etc.
       AnnotationSet sentences =  docAnnotationSet.getCovering("Sentence", 
               corefInfo.anaphor.getStartNode().getOffset(),corefInfo.anaphor.getEndNode().getOffset());
       corefInfo.anaphorSentenceId = sentences.iterator().next().getId().intValue();
       return corefInfo;
 * */
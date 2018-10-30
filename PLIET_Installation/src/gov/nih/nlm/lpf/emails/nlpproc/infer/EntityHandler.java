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
package gov.nih.nlm.lpf.emails.nlpproc.infer;


import gov.nih.nlm.lpf.emails.nlpproc.analysis.AttributeAnalyzer;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NounAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import  gov.nih.nlm.lpf.emails.nlpproc.nlp.Clause;

import gov.nih.nlm.lpf.emails.nlpproc.structure.LPFEntity;
import gov.nih.nlm.lpf.emails.nlpproc.structure.PersonAttributes;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.CorefInfo;
import  gov.nih.nlm.lpf.emails.nlpproc.structure.ClausalAssertion;
import  gov.nih.nlm.lpf.emails.nlpproc.analysis.resolve.NameResolver;

import gov.nih.nlm.lpf.emails.nlpproc.ner.NERConstants;

import gov.nih.nlm.lpf.emails.util.Utils;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class EntityHandler 
{
    private static Logger log = Logger.getLogger(EntityHandler.class);
    
  
    protected ArrayList<TextAnnotation>personList;
    protected ArrayList<TextAnnotation> attributeList;
    protected AttributeAnalyzer attrAnalyzer;
    
    public EntityHandler(ArrayList<TextAnnotation>personList,  ArrayList<TextAnnotation> attributeList)
    {
        this.personList = personList;
        this.attributeList = attributeList;
        
       attrAnalyzer = new AttributeAnalyzer(attributeList);
    }
    
    // classify a given LPFentity  according to its subject  and reurn the designated class 
     public int classifyEntity(LPFEntity entity)
     {
         AttributeAnalyzer  attrAnalyzer = new AttributeAnalyzer(attributeList);
         // Check if the subject is a person's attribute (name, location, etc.)
         if (isAttributeSubject(entity))
         {
             entity.entityClass = NERConstants.PERSONAL_ATTRIB_ENTITY;
         }
     /*    else if (isAttributeObject(entity))
         {
             entity.entityClass = NERConstants.OBJECT_ATTR_ENTITY;
         }*/
         // Check if the subject is a Person
          else if ( isPersonSubject(entity, personList))   
         {
                entity.entityClass = NERConstants.PERSON_ENTITY;
         } 
         // Check if the subject is a LPF noun related to enquiry about a person'
         else if ( isLPFNounSubject(entity))      
         {
             entity.entityClass = NERConstants.NONPERSON_ENTITY;
         }
          else 
              entity.entityClass = NERConstants.UNKNOWN_TYPE_ENTITY;
         
         return entity.entityClass;
     }

  /*-----------------------------------------------------------------------------------------------------------*/
  // Check if the given anchor (subject of a clause) contains keywords referring 
  // to person's attribute (such as name, age, etc.)  rather than the person itself.
  // For example: "My name"  is John vs. "I"  am John.
  //-------------------------------------------------------------------------------------------------------------//
    public boolean isAttributeSubject(LPFEntity entity)
    {
       
        TextAnchor  subject = entity.getSubject();
        if (subject.getClass() != NounAnchor.class)
            return false;
        return (attrAnalyzer.getAttributeType(subject) != null);
    } 

  /*-----------------------------------------------------------------------------------------------------------*/
  // Check if the given object of a clause of this  contains keywords referring 
  // to person's attribute (such as name, age, etc.)  rather than the person itself.
  // For example: "My name"  is John vs. "I"  am John.
  //----------------------------------------------------------------------------------------------------------    
    /*         else if (isAttributeObject(entity))
         {
             entity.entityClass = NERConstants.OBJECT_ATTR_ENTITY;
         }*/
 /*---------------------------------------------------------------------------------------------------------*/
 // Check if the object is in the list of Nouns related to a disaster that conveys
 //  an enquiry or statement about a person. (Contact  was made with John Smith.)
  /*---------------------------------------------------------------------------------------------------------*/   
    public  boolean isLPFNounSubject(LPFEntity entity)
    {
       TextAnchor subject = entity.subject;
       // Cheeck if it is a non-person
       String textStr = subject.getText();
       if (Utils.isInList(textStr, NERConstants.NONPERSON_NOUNS))
            return true;
       return false;
    }
     //-------------------------------------------------------------------------------------------------------------//
    // TBD: Should look at coreference
    public  boolean isPersonSubject(LPFEntity entity, ArrayList<TextAnnotation> personList)
    {
       TextAnchor  subject = entity.getSubject();
        String textStr = subject.getText();
        String ptextStr = subject.getPhraseText();
        String word = subject.getGovernorToken().text;
        for (int i = 0; i < personList.size(); i++)
        {
            String persontext = personList.get(i).text;
            if (textStr.equalsIgnoreCase(persontext) ||
                (ptextStr.equalsIgnoreCase(persontext)) ||
                (word.equalsIgnoreCase(persontext)))    
                return true;
            else if (NameResolver.isEquivalentName(ptextStr, persontext))
                return true;
        }
        return false;
    }
    
  
  
    /****************************************************************************************************/
    // Add the  Attribute information for a person from the LPFEntity object to the corresponding
    // PersonAtribute structure. If none exists, create a new one.
    // It is expected that each attribute starts with a possessive term such as my, John's etc.
    // Note: the key in attributeMap is the AntecedentAnchor such as My or John etc.
    //-----------------------------------------------------------------------------------------------------------------------------

    protected TreeMap<TextAnchor, PersonAttributes> setPersonalAttribute(String msgText, 
        ArrayList<LPFEntity> attribEntities, TreeMap<TextAnchor, CorefInfo> corefMap)
    {
        TreeMap<TextAnchor, PersonAttributes> attributeMap = new TreeMap();
        
        for (int i = 0; i < attribEntities.size(); i++)
        {
            LPFEntity attribEntity = attribEntities.get(i);
             NounAnchor subject = (NounAnchor) attribEntity.getSubject();
            if (!subject.hasPossessive)
            {
                log.error ("No persons found for entity " + subject.getText());
                continue;
            }
            
            // find the reference subject to which it corresponds
            TextAnnotation possessiveAnnot = subject.possessiveAnnot;
            TextAnchor refAnchor = getReferenceAnchor( possessiveAnnot, corefMap);
            PersonAttributes currentAttrib = null;
            if (refAnchor != null)
                currentAttrib = attributeMap.get(refAnchor);
          
            // create a new one if not already exist
            if ((refAnchor != null) && (currentAttrib == null))
            {
                currentAttrib = new PersonAttributes(refAnchor);
                attributeMap.put(refAnchor, currentAttrib);
            }  
             if (currentAttrib != null)
                addAttributes(currentAttrib, subject, attribEntity.getAssertions());
        }
        return attributeMap;
    }
   /*--------------------------------------------------------------------------------------------------------*/ 
    // Get the reference Anchor corresponding to the given text annotattion
    // 
    TextAnchor getReferenceAnchor(TextAnnotation corefAnnot,
        TreeMap<TextAnchor, CorefInfo> corefMap)
    {
        TextAnchor refAnchor = null;
        if (corefMap != null)
        {
            Iterator <TextAnchor> iter = corefMap.keySet().iterator();
            while(iter.hasNext())
            {
               TextAnchor  key = iter.next();
                CorefInfo corefInfo = corefMap.get(key);
                if (corefInfo.referringAnnot.id  == corefAnnot.id)
                {
                    refAnchor = corefInfo.corefAnchor;
                    break;
                }
            }
        }
        return refAnchor;
    }

   /*--------------------------------------------------------------------------------------------------------*/ 
    // add the objects of assertions as the attribute values
   /*--------------------------------------------------------------------------------------------------------*/     
     protected void addAttributes(PersonAttributes currentAttrib,  TextAnchor subject,
         ArrayList < ClausalAssertion>  assertions)
     {
        String attrType = attrAnalyzer.getAttributeType(subject);            
        for (int i = 0;  i < assertions.size(); i++)
        {
            ClausalAssertion  assertion = assertions.get(i);
            Clause clause = assertion.clause;
            
            // Copular object: 
            //    (a) His name is John: John is the  copula object here. 
            //    (b) His age is 10
            TextAnchor[]  objectAnchors = ((VerbAnchor)(clause.clauseHead)).getCopularObjects();
            if (objectAnchors == null || objectAnchors.length == 0)
            {
                log.error ("No associated data found for Personal Attribute " + subject.getPhraseText());
                continue;
            }
            TextAnchor object = objectAnchors[0];
            currentAttrib.addAttribute(attrType, object);
        }
    }   
}

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

import gov.nih.nlm.lpf.emails.nlpproc.nlp.AdjectiveAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.NounAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.structure.TypedAnnotationMap;
import gov.nih.nlm.lpf.emails.nlpproc.structure.AnchorSet;

import org.apache.log4j.Logger;

import java.util.ArrayList;

import java.util.TreeMap;           // retains insertion order
import java.util.Iterator;
/**
 *
 * @author 
 */
public class AnchorGenerator implements NLPRelations
{
    private static Logger log = Logger.getLogger(AnchorGenerator.class);
    
        public static boolean debugAnchor = true;

        TypedAnnotationMap annotMap; 

        ArrayList<TextAnnotation> dependencies;
        ArrayList<TextAnnotation> tokens;

        ArrayList<NounAnchor> nouns;
        ArrayList<VerbAnchor> verbs;
        ArrayList<AdjectiveAnchor> adjectives;
        ArrayList<TextAnnotation> pronouns;

        String annotField;         // text field to which the annotations belong, not used here
        String documentText;        // Text of the document from which all strictures are  derived

        protected TextAnnotation sentence;
        protected TreeMap<TextAnnotation, VerbAnchor> verbTable;           // stores all verb info
        protected TreeMap<TextAnnotation, NounAnchor> nounTable;         // stores all noun  info
        protected TreeMap<TextAnnotation, AdjectiveAnchor> adjectiveTable;      //stores all adjective info
    
        protected ArrayList <TextAnchor> allAnchors = new ArrayList();
        
     /*  Constructor:
      * Param: sentenceAnnotations - HashMap with annotation type and corresponding annotations
      */
    public AnchorGenerator(String docText,
            TypedAnnotationMap sentenceAnnotations, String field)
    {
        documentText = docText;
        sentence = sentenceAnnotations.get("Sentence").get(0);
        System.out.println("\n>>>> Sentence:  " + sentence.text);
        annotMap = sentenceAnnotations;     // all annotations within a sentence
        annotField = field;                                  // Subject or Body
    }
    
    /*****************************************************************************************
     * Generate different types of anchors by examining the POS tags and dependency 
     * features in the set of Tokens associated with this sentence
     * @return 
     ****************************************************************************************/
    public int generateAnchors()
    {
        tokens = annotMap.get("Token");       
        
        // build a table (Map) , one entry per  Anchor, to contain all information for the  Anchor
        buildVerbAnchorTable(tokens);
        buildNounAnchorTable(tokens);
        buildAdjectiveAnchorTable(tokens);
        
        // Fill all Links in each table
        allAnchors.addAll(nounTable.values());
        allAnchors.addAll(verbTable.values());
        allAnchors.addAll(adjectiveTable.values());
        
       // fill all Links with dependent's Anchor 
        for (int i = 0; i < allAnchors.size(); i++)
        {
            TextAnchor anchor = allAnchors.get(i);
            anchor.fillLinkTargetAnchors(allAnchors);
        }
      
         for (int i = 0; i < allAnchors.size(); i++)
         {
             allAnchors.get(i) .setOtherProperties(allAnchors);
            if (debugAnchor) System.out.println( allAnchors.get(i).toFullString());
         }


    /*    LPFRecordInfo lpfInfo = new LPFRecordInfo();
        applyStandardRule(lpfInfo);
        System.out.println(lpfInfo.toString());
         * 
         */
         return 1;
     }
 
     /****************************************************************************************************
    * Build tables capturing the information  for tracing relations of each verb group
     ****************************************************************************************************/
     protected void buildVerbAnchorTable(ArrayList<TextAnnotation> tokens)
     {
         verbTable = new TreeMap();
         ArrayList <TextAnnotation> verbs;
         
         for (int i = 0; i < tokens.size(); i++)
         {
             TextAnnotation token = tokens.get(i);
             // ignore discourse tokens such as "Please", "Oh", etc. -- note: Sometimes the parser misses "please"
             if (token.isDiscourseToken(tokens) || token.text.equalsIgnoreCase("please"))
                 continue;     
             // ignore a set of other tokens that are misclassified as verb
             if (token.text.equalsIgnoreCase("male") || token.text.equalsIgnoreCase("female"))
                 continue;
             if (TextAnchor.isVerb(token))
             {
                 if (!consumedToken(verbTable, token))           // not a part of previous anchor
                 {
                     VerbAnchor vanchor;
                     vanchor = new VerbAnchor(sentence, token, tokens);
                    vanchor.initLinks(tokens);
                    verbTable.put(token,  vanchor);
                 }
             }
         }
     }
      
    /****************************************************************************************************
    * Build tables capturing the information  for tracing relations of each Noun group
     ****************************************************************************************************/
     protected void buildNounAnchorTable(ArrayList<TextAnnotation> tokens)
     {
         nounTable = new TreeMap();        
         for (int i = 0; i < tokens.size(); i++)
         {
             TextAnnotation token = tokens.get(i);
              if (token.isDiscourseToken(tokens))
                 continue; 
             if (TextAnchor.isNoun(token))
             {
                 if (!consumedToken(nounTable, token))           // not a part of previous anchor
                 {
                       NounAnchor nanchor = new NounAnchor(sentence, token, tokens);
                       nanchor.initLinks(tokens);
                       TextAnnotation govToken = nanchor.getGovernorToken();
                       nounTable.put(govToken,  nanchor);
                 }
             }
         }
     }
     
    /****************************************************************************************************
    * Build tables capturing the information  for tracing relations of each Noun group
     ****************************************************************************************************/
     protected void buildAdjectiveAnchorTable(ArrayList<TextAnnotation> tokens)
     {
         adjectiveTable = new TreeMap();
         for (int i = 0; i < tokens.size(); i++)
         {
             TextAnnotation token = tokens.get(i);
             if (token.isDiscourseToken(tokens))
                 continue; 
             if (TextAnchor.isAdjective(token))
             {
                 if (!consumedToken(adjectiveTable, token))           // a part of previous anchor
                 {
                       AdjectiveAnchor aanchor = new AdjectiveAnchor(sentence, token, tokens);
                       aanchor.initLinks(tokens);
                       adjectiveTable.put(token,  aanchor);
                 }
             }
         }
     }
      /****************************************************************************************************
      * Check if a token is already consumed by an anchor. It is consumed if :
      *   is a part of the word list for an existing  anchor or
      *****************************************************************************************************/
         protected boolean consumedToken(TreeMap map, TextAnnotation token)
     {
         Iterator<TextAnnotation>  it = map.keySet().iterator();
         while (it.hasNext())
         {
             TextAnchor anchor = (TextAnchor) map.get(it.next());
             if (anchor.containsAnnot(token))
                 return true;
         }
         return false;
     }

  /*****************************************************************************************************/
       public  TypedAnnotationMap getAllAnnotations()
       {
           return annotMap;
       }
       
/******************************  Other getters *******************************************************/       
       public  TextAnnotation getSentence()
            {return sentence;}

        // returns  all verb info
        public TreeMap<TextAnnotation, VerbAnchor> getVerbAnchors()
                    {return verbTable;  }        
        
         // returns  all Noun info
        public TreeMap<TextAnnotation, NounAnchor> getNounAnchors()
                    {return nounTable;  }  
        
           // returns  all Noun info
        public TreeMap<TextAnnotation, AdjectiveAnchor> getAdjectiveAnchors()
                    {return adjectiveTable;  }  
        
    /*****************************************************************************************************/      
        // get all data related to anchors in this sentence
        public  AnchorSet  getAnchorData()
        {
            AnchorSet anset = new AnchorSet();
            anset.documentText = documentText;
            anset.sentence = sentence;
            anset.sentenceAnnotations = annotMap;
            anset.verbTable = verbTable;
            anset.nounTable = nounTable;
            anset.adjectiveTable = adjectiveTable;
            anset.allAchors = allAnchors;
             return anset;
        }
 /*****************************************************************************************************/       
        public ArrayList<TextAnchor> getAllAnchors()
        {
            return allAnchors;
        }
            

     /***********************************************************************************************/
     protected TextAnnotation getAnnotationWithId(int tokenId)
     {
           for (int i = 0; i < tokens.size(); i++)
           {
               if (tokenId == tokens.get(i).id)
                   return tokens.get(i);
           }
           return null;
     }
       
    /***********************************************************************************************/
     protected TextAnchor getAnchorWithToken(int tokenId)
     {
         ArrayList<TextAnchor>[] allAchorList = new ArrayList[] {nouns, verbs, adjectives};
         for (int i = 0; i < allAchorList.length; i++)
         {
             ArrayList <TextAnchor> anchors = allAchorList[i];      //allAnchorList[i];
             if (anchors == null || anchors.isEmpty())
                 continue;
             
             for (int j = 0; j < anchors.size(); j++)
             {
                 TextAnchor anchor = anchors.get(i);
                 TextAnnotation[] words = anchor.getPhrasewords();
                 for (int j1 = 0; j1 < words.length; j1++)
                 {
                     if (words[j1].id == tokenId)           // match found
                         return anchor;
                 }
             }
         }
         return null;               // not found; error
     }
      
 }
        
     
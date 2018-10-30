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

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;

import  gov.nih.nlm.lpf.emails.nlpproc.structure.AnchorSet;
import gov.nih.nlm.lpf.emails.nlpproc.structure.ClauseTree;
import gov.nih.nlm.lpf.emails.nlpproc.structure.AnaphoraInfo;
import gov.nih.nlm.lpf.emails.nlpproc.structure.CorefInfo;

import gov.nih.nlm.lpf.emails.nlpproc.structure.EmailHeaderInfo;
import gov.nih.nlm.lpf.emails.nlpproc.structure.TypedAnnotationMap;

import gate.Document;
import java.util.TreeMap;
import java.util.ArrayList;

/**
 *
 * @author 
 */
public class ClausalRelationExtractor
{
    
    // Input to the Extractor
    TypedAnnotationMap[]  annotatedSentenceMaps;
     ArrayList<AnchorSet> anchorSets;                                         // each element => all anchors for a sentence 
    TreeMap <TextAnchor, AnaphoraInfo> anaphoraMap;         // TextAnchor with all anaphoraInfo for  the document
    TreeMap<TextAnchor, CorefInfo> corefMap;                          // Coreferences
    Document gateDoc;

    // information returned at the end of processing
    protected ArrayList <TextAnnotation> locationList = new ArrayList();
    protected ArrayList <TextAnnotation> personList = new ArrayList();
    protected ArrayList <TextAnnotation> attributeList = new ArrayList();
    protected ArrayList <TextAnnotation> fragmentSentences = new ArrayList();       // sentences with no verbs: fragments
    
    //----------------------
  TypedAnnotationMap emailHeaderAnnotationMap = null;
   EmailHeaderInfo emailHeaderInfo = null;
    
    
    /*********************************************************************************************
     * Constructor:
     * @param gateDoc - The Gate document corresponding to an  email message (subject and body)
     * @param annotatedSentenceMaps - An Array of AnnotationMaps (one per sentence, holding all 
     *                  annotations (Sentence, Token, Person, Location etc.)  for that sentence
     * @param anchorSets - Each element is  a the set of  Text Anchors generated for he corresponding  sentence
     * @param anaphoraMap - Map of all Anaphora info; Key corresponds to the TextAnchor for that Annaphora
     */
    public ClausalRelationExtractor(Document gateDoc, 
        TypedAnnotationMap[]  annotatedSentenceMaps,
        ArrayList<AnchorSet> anchorSets)
    {
        this.gateDoc = gateDoc;
        this.annotatedSentenceMaps = annotatedSentenceMaps;
        this.anchorSets = anchorSets;
    }
    
    
   /********************************************************************************************/
    // get All clauses in a sentence as a Map, the TextAnnotation keys being the Sentences
    // This is the topmost  processing performed for the email message, based upon
     // Annotated datareceived from the ANNIE Pipeline
     /******************************************************************************************/
    protected  TreeMap<TextAnnotation, ClauseTree>  extractClausesInSentences()
    {
        TreeMap<TextAnnotation, ClauseTree> lpfClauseMap = new TreeMap();

        
     for (int i = 0; i < annotatedSentenceMaps.length; i++)
     {
         ArrayList<TextAnnotation> locs = annotatedSentenceMaps[i].get("Location");
         if (locs != null)
             addToUniqueList(locationList, locs);
        
         ArrayList<TextAnnotation> persons = getPersonTypeAnnotations(annotatedSentenceMaps[i]);
         if (persons != null)
             addToUniqueList(personList, persons);
         
         // Get annotations indicating the attributes of a person
         ArrayList<TextAnnotation> attributes = annotatedSentenceMaps[i].get("Attribute");
         if ( attributes != null)
             addToUniqueList(attributeList, attributes);
     }
        // Exract  the set of clauses in a sentence by pursuing the sentence anchors
       for (int i = 0; i < anchorSets.size(); i++)
        {
             AnchorSet anchorSet = anchorSets.get(i);
            TextAnnotation sentenceAnnot = anchorSet.sentence;
            String text = sentenceAnnot.text.replaceAll("^(\\s+)", "").replaceAll("(\\s+)$", "");
            if (text.length() == 0)
                continue;
            
            // (n Sentences such as "name John" or "Named John" name is  not really verbs."
            if (text.toLowerCase().matches("(?)^(name|named\\W+)") )
            {
                 fragmentSentences.add(sentenceAnnot);
                 continue;
            }
          ClauseTree clauseTree = extractClausesInSentence(anchorSet);
          if (clauseTree == null)
            {
                fragmentSentences.add(sentenceAnnot);
                System.out.println(" >> Fragment:" + sentenceAnnot.text + "<<");
            }
            else
                lpfClauseMap.put(sentenceAnnot, clauseTree);        // sentence vs. clauses in sentence
        }
        return lpfClauseMap;
    }

    /*********************************************************************************/
    // add all objects from one list to another with no duplicates
    //----------------------------------------------------------------------------------------------------*.
    protected void addToUniqueList( ArrayList <TextAnnotation>aList, ArrayList <TextAnnotation> objs)   
    {
        for (int i = 0; i < objs.size(); i++)
        {
             TextAnnotation obj = objs.get(i);
             if (obj != null && obj.text.length() > 0)
             {
                 if (!aList.contains(obj))
                     aList.add(obj);
             }
        }
    }
    /************************************************************************************
    /* Get annotations that specify a person. It includes the PRN list that is not
     * already included under Person annotations
     **********************************************************************************/

    /**
     * /* Get annotations that specify a person.It includes the PRN list that is not
 already included under Person annotations
     */
    public ArrayList <TextAnnotation>  getPersonTypeAnnotations(
        TypedAnnotationMap  annotatedSentenceMap)
    { 
        ArrayList <TextAnnotation>  persons = annotatedSentenceMap.get("Person");
       ArrayList <TextAnnotation>  prns = annotatedSentenceMap.get("PRN");
       ArrayList <TextAnnotation>  which = annotatedSentenceMap.get("WHICH");
       if (which != null)
           prns.addAll(which);          // treat as one set of pronouns
        
       for (int i = 0; i < prns.size(); i++)
       {
           TextAnnotation prn = prns.get(i);
           boolean matching = false;
           for (int j = 0; j < persons.size() && !matching; j++)
           {
               if (prn.compareTo(persons.get(j)) == 0)          // same offsets
                   matching = true;
           }  
           if (!matching)           // not in persons list
               persons.add(prn);
       }
       return persons;
    }
   /****************************************************************************
     * Extract the clauses in a sentence by checking the anchor dependencies
     ****************************************************************************/
   protected ClauseTree   extractClausesInSentence(AnchorSet anchorSet)          
    { 

        // Now find relations netween these anchors
       ClauseTreeBuilder  ctreeBuilder = new ClauseTreeBuilder(anchorSet);
       ClauseTree  clauseTree  = ctreeBuilder. buildClauseTree(); 
        return clauseTree;
    }

/****************************************************************************/
  protected ArrayList <TextAnnotation> getFragmentSentences()
  {
      return fragmentSentences;
  }

}

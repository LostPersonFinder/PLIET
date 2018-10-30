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
package gov.nih.nlm.lpf.emails.nlpproc.structure;

import gov.nih.nlm.lpf.emails.nlpproc.nlp.NLPRelations;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor;
import gov.nih.nlm.lpf.emails.nlpproc.nlp.VerbAnchor.DependentClause;

/**
 *
 * @author 
 */
public class ClausalAnchor implements NLPRelations
{
    // Find the connector word:  depending upon the link type
  /*   public static int[] ClauseClass = {
         PRIMARY_TYPE, CCOMP_TYPE, PCOMP_TYPE,
         XCOMP_TYPE, ADVCL_TYPE};
      public static   String[] ClauseTypes = {"advcl", "dep", "ccomp",  "purpcl", "xcomp"};
      public static   String[] ConnectorTypes = { "marker", "aux", "complm", "", "aux"};
    */
     public String clauseType;                     // :xcomp", "ccomp", etc
     public TextAnchor clsHead;                  // Head word/verb within the clause 
     public TextAnchor clauseGovernor;     // governor anchor of the clause
     public String  connectionText;              // link/marker such as to, for, etc:, which introduces  the clause 
     protected boolean isObject;                       // is it used as object to the governor
     public TextAnchor xsubj;                       // Subject  (Anchor) for this clause
     
     public static DependentClause[]  ClauseDefs = VerbAnchor.DependentClauseDefs;

     
     // default constructor
     public ClausalAnchor( TextAnchor gov, String type)
     {
         clauseGovernor = gov;
         clauseType = type;          // :xcomp", "ccomp", etc
         isObject = true;
         xsubj = null;    // TBD: To be implemented for csubj/csubjpass clauses later
     }
    
     public ClausalAnchor(String type, String relation, TextAnchor head, TextAnchor gov)
    {
        this( type,  relation,  head,  gov, true);
    }  
     
     public ClausalAnchor(String type, String marker, TextAnchor head, TextAnchor gov, boolean isObj)
    {
        clauseType = type;      //"xcomp", "pcomp" etc.
        clsHead = head;
        clauseGovernor = gov;
        setConnectionText(marker);         // may be "", as per xcomps
        xsubj = null;
    } 
/*----------------------------------------------------------------------------------------------------*/     
     public static String getClauseType(int ctype)
    {
        if (ctype == PRIMARY_TYPE)
            return "";
        for (int i = 1; i < ClauseDefs.length; i++)
        {
            if (ctype == ClauseDefs[i].type)
                return  ClauseDefs[i].clauseType;
        }
        return null;
    }
 

 /*----------------------------------------------------------------------------------------------------*/ 
    // The clause is used an equal (for xcomp relations) if there is no connection text
    public void  setConnectionText(String connector)
    {
       connectionText = connector;
        isObject = (connector  != null && connector.length() > 0);
    }

   /*----------------------------------------------------------------------------------------------------*/ 
    // The clause is used an equal (for xcomp relations) if there is no connection text
    public String getConnectionText()
    {
        return connectionText;
    } 
    
    public boolean isObjectClause()
    {
        return (xsubj == null && isObject);
    }
/*----------------------------------------------------------------------------------------------------*/    
    public static String getConnector(String clauseType)
    {
        for (int i = 0; i < ClauseDefs.length; i++)
        {
            if (clauseType.equals(ClauseDefs[i].clauseType))
                return (ClauseDefs[i].connectorRelation);
        }
        return null;
    }
  /*----------------------------------------------------------------------------------------------------*/      
}

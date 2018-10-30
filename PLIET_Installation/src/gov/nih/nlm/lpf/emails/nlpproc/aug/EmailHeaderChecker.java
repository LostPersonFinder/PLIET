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
package gov.nih.nlm.lpf.emails.nlpproc.aug;

/**
 *
 * @author 
 */

import gov.nih.nlm.lpf.emails.nlpproc.nlp.TextAnnotation;


import gate.Document;
import gate.Annotation;
import gate.AnnotationSet;

public class EmailHeaderChecker
{
     Annotation subjectHeader;                           // Email Subject Header text
     int headerStart;
     int headerEnd;

    public EmailHeaderChecker( Document messageDoc)
    {
        Annotation emailHeader = null;
        AnnotationSet emailHeaderSet  = messageDoc.getAnnotations("Subject");
        if (emailHeaderSet != null && !emailHeaderSet.isEmpty())
        {   
            emailHeader = emailHeaderSet.iterator().next();
            headerStart = emailHeader.getStartNode().getOffset().intValue();
            headerEnd = emailHeader.getEndNode().getOffset().intValue();
        }
        return;
    }   
    
    // determine if a given token is within the header of the email
    public boolean isInHeader(Annotation annot)
    {
        if (subjectHeader == null)
            return false;
        int start = annot.getStartNode().getOffset().intValue();
        int end = annot.getEndNode().getOffset().intValue();
        return (start >= headerStart && end <= headerEnd);
    }
       
    
    // determine if a given TestAnnotation is within the header of the email
    public boolean isInHeader(TextAnnotation textAnnot)
    {
        if (subjectHeader == null)
            return false;
        int start = textAnnot.offsets[0];
        int end = textAnnot.offsets[1];
        return (start >= headerStart && end <= headerEnd);
    }
    

}

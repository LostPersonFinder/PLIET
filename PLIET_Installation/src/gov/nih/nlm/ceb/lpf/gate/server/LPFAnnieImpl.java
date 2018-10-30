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
package gov.nih.nlm.ceb.lpf.gate.server;
/*
 * LPF implementation of invoking the ANNIE Pipeline and getting back the results
 * ay by invoked from an HTTP servlet or from a stand-alone application.
 */
import  gov.nih.nlm.ceb.lpf.gate.shared.LPFAnnotOutputType;

import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.DOMSerializer;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.SerializerFactory;

import gate.*;
import gate.util.Out;
import gate.creole.ANNIEConstants;

import gate.util.GateException;
import gate.util.persistence.PersistenceManager;

public class LPFAnnieImpl
{

    boolean gateInited = false;
    static CorpusController application = null;

    public LPFAnnieImpl()
    {
    }
   // initialize in a Server context (when invoked through a Web service) 
    synchronized public void init(ServletContext ctx) throws GateException
    {
        if (!gateInited)
        {
            // Load ANNIE plugin
            File gateHome = new File(ctx.getRealPath("/WEB-INF"));

            Gate.setGateHome(gateHome);
            Gate.setUserConfigFile(new File(gateHome, "user-gate.xml"));
            Gate.init();
            Out.prln("...GATE initialised");
            try
            {
                Gate.getCreoleRegister().registerDirectories(
                    ctx.getResource("/WEB-INF/plugins/ANNIE"));

                File gappFile = new File(ctx.getRealPath("/WEB-INF/plugins/ANNIE"),
                    ANNIEConstants.DEFAULT_FILE);
                application =
                    (CorpusController) PersistenceManager.loadObjectFromFile(gappFile);
                /*				  
                Collection prset = application.getPRs();
                
                String [] additionalPRs = {
                "gate.creole.coref.Coreferencer",
                "gate.creole.coref.NominalCoref" 
                };
                for (int i = 0; i <  additionalPRs.length; i++) {
                FeatureMap params = Factory.newFeatureMap(); 
                ProcessingResource pr = (ProcessingResource) 
                Factory.createResource(additionalPRs[i], 
                params); 
                prset.add(pr);
                }
                 */
                gateInited = true;
            } catch (MalformedURLException me)
            {
                throw new GateException(me);
            } catch (IOException ioe)
            {
                throw new GateException(ioe);
            }
        }
        Out.prln("...ANNIE loaded");
    }
    
   // initialize in a  local context (when parameters are provided locally by the caller) 
    // Parameter ctxProp = context relaetd properties
     synchronized public void init(Properties ctxProp) throws GateException
    {
        if (!gateInited)
        {
            // Load ANNIE plugin
            File gateHome = new File(System.getProperty("GateHome"));

            Gate.setGateHome(gateHome);
           // Gate.setUserConfigFile(new File(gateHome, "user-gate.xml"));
            Gate.setUserConfigFile(new File(ctxProp.getProperty("userConfig")));
            Gate.init();
            Out.prln("...GATE initialised");
            try
            {
                // Parent Directory where all ANNIE Processing r"esources" exist
                URL url = new File(System.getProperty("ANNIEHome")).toURI().toURL();
                Gate.getCreoleRegister().registerDirectories(url);   
                
                // get path to  file ANNIE_with_defaults.gapp
                File gappFile = new File(System.getProperty("ANNIEHome"),
                    ANNIEConstants.DEFAULT_FILE);
                application =  (CorpusController) PersistenceManager.loadObjectFromFile(gappFile);
                gateInited = true;
            } catch (MalformedURLException me)
            {
                throw new GateException(me);
            } catch (IOException ioe)
            {
                throw new GateException(ioe);
            }
        }
        Out.prln("...ANNIE loaded");
    }
    
    

    public String annotate(String text, String contentType) throws GateException
    {
        return annotate(text, contentType, LPFAnnotOutputType.XML);
    }

    public String annotate(String text, String type, String outType) throws GateException
    {
        String ret = null;
//		try {
        Document doc = executeGateApp(text, type);
        ret  = extractAnnotations(doc, outType);
        String debugStr = extractAnnotations(doc, LPFAnnotOutputType.TEXT);
        //System.out.println(debugStr);
        destroyGateDoc(doc);
//		} catch (GateException ge) {
//			ret = "Error in annotating the text. See server logs.\n"+
//			LPFNamedEntityUtil.getStackTrace(ge);
//		}
        return ret;
    }

    synchronized Document executeGateApp(String text, String type) throws GateException
    {
        Corpus corpus = Factory.newCorpus("LPFCorpus");
        FeatureMap params = Factory.newFeatureMap();
        params.put("stringContent", text);
        params.put("preserveOriginalContent", new Boolean(true));
        params.put("collectRepositioningInfo", new Boolean(true));
        params.put("mimeType", type);
        params.put("encoding", "UTF-8");
        Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
        corpus.add(doc);

        // tell the pipeline about the corpus and run it
        application.setCorpus(corpus);
        application.execute();
        corpus.clear();

        return doc;
    }

    void destroyGateDoc(Document doc) throws GateException
    {
        Factory.deleteResource(doc);
    }

    public org.w3c.dom.Document annotateDOM(String text, String type) throws GateException
    {
        org.w3c.dom.Document ret = null;
        Document doc = executeGateApp(text, type);
        ret = extractAnnotationsDOM(doc);
        destroyGateDoc(doc);
        return ret;
    }

    String extractAnnotations(Document doc, String outType)
    {
        String ret = null;

        if (LPFAnnotOutputType.XML.equalsIgnoreCase(outType))
        {
            ret = xmlOutput(doc);
        } else
        {
            ret = textOutput(doc);
        }
        return ret;
    }

    org.w3c.dom.Document extractAnnotationsDOM(Document doc)
    {
        return domOutput(doc);
    }

    AnnotationSet getHeaderAnnotations(Document gateDoc)
    {
        String[] requiredTypes =
        {
            "Date",
            "Name",
            "Email",
            "Subject"
        };

        // Select that match above list with case insensitive.
        AnnotationSet headers = gateDoc.getAnnotations("Headers");
        Iterator<Annotation> iter = headers.iterator();

        Map<String, Annotation> requiredASMap = new HashMap<String, Annotation>();
        while (iter.hasNext())
        {
            Annotation a = iter.next();
            for (int i = 0; i < requiredTypes.length; i++)
            {
                if (requiredTypes[i].equalsIgnoreCase(a.getType()))
                {
                    requiredASMap.put(requiredTypes[i], a);
                }
            }
        }

        Iterator<Map.Entry<String, Annotation>> mapIter = requiredASMap.entrySet().iterator();
        while (mapIter.hasNext())
        {
            Map.Entry<String, Annotation> me = mapIter.next();
            headers.add(me.getValue().getStartNode(), me.getValue().getEndNode(), me.getKey(), me.getValue().getFeatures());
        }

        return getSubsetAnnotations(gateDoc.getAnnotations("Headers"), requiredASMap.keySet());
    }

    AnnotationSet getBodyAnnotations(Document gateDoc)
    {
        Set<String> requiredAnnots = new HashSet<String>();
        requiredAnnots.add("Person");
        requiredAnnots.add("Condition");
        requiredAnnots.add("Location");
        //TBD:
        //requiredAnnots.add("ReportedPerson");


        // Remove pronouns
        AnnotationSet as = getSubsetAnnotations(gateDoc.getAnnotations("Body"), requiredAnnots);
        FeatureMap fm = Factory.newFeatureMap();
        fm.put("ENTITY_MENTION_TYPE", "PRONOUN");
        AnnotationSet personAS = as.get("Person", fm);
        if (personAS != null)
        {
            Iterator<Annotation> iter = personAS.iterator();

            while (iter.hasNext())
            {
                as.remove(iter.next());
            }
        }
        return as;
    }

    Map<Annotation, List<Annotation>> getSubjectAnnotations(AnnotationSet bodyAS)
    {
        Map<Annotation, List<Annotation>> ret =
            new HashMap<Annotation, List<Annotation>>();

        Set<String> requiredAnnots = new HashSet<String>();
        requiredAnnots.add("ReportedPerson");
        //requiredAnnots.add("Sentence");
        
        AnnotationSet as = getSubsetAnnotations(bodyAS, requiredAnnots);
        if (as != null)
        {
            Iterator<Annotation> iter = as.iterator();
            while (iter.hasNext())
            {
                Annotation subject = iter.next();
                List<Annotation> related = new ArrayList<Annotation>();
                FeatureMap fm = subject.getFeatures();
                Integer intObj = (Integer) fm.get("Person");
                if (intObj != null)
                {
                    related.add(bodyAS.get(intObj));
                }
                intObj = (Integer) fm.get("Location");
                if (intObj != null)
                {
                    related.add(bodyAS.get(intObj));
                }
                intObj = (Integer) fm.get("Condition");               
                if (intObj != null)
                {
                    related.add(bodyAS.get(intObj));
                }
                ret.put(subject, related);
            }
        }
        return ret;
    }

    AnnotationSet getSubsetAnnotations(AnnotationSet as, Set<String> subsetNames)
    {
        AnnotationSet ret = null;
        if (as != null)
        {
            ret = as.get(subsetNames);
        }
        return ret;
    }

    String textOutput(Document gateDoc)
    {
        StringBuffer editableContent = new StringBuffer();
        //String ret = null;
        Map<Annotation, List<Annotation>> subjects = getSubjectAnnotations(gateDoc.getAnnotations("Body"));
        if (subjects != null)
        {
            Iterator<Annotation> subjectIter = subjects.keySet().iterator();
            while (subjectIter.hasNext())
            {
                Annotation subject = subjectIter.next();
                List<Annotation> subjectAnntList = subjects.get(subject);
                for (Annotation ann : subjectAnntList)
                {
                    editableContent.append(ann.getType()).append(": ");
                    editableContent.append(getText(gateDoc, ann)).append("\n");
                    if (ann.getType().equals("Person"))
                    {
                        FeatureMap features = ann.getFeatures();
                        Object gender = features.get("gender");
                        if (gender != null && gender instanceof String)
                        {
                            editableContent.append("Gender: ");
                            editableContent.append(features.get("gender")).append("\n");
                        }
                    }
                }
                editableContent.append("-------------------\n");
            }

        }
        return editableContent.toString();
    }

    /*	
    String textOutput (Document gateDoc) {
    StringBuffer editableContent = new StringBuffer();
    
    Set<Annotation> as = new HashSet<Annotation>();
    as.addAll(getBodyAnnotations(gateDoc));
    as.addAll(getHeaderAnnotations(gateDoc));
    FeatureMap features = gateDoc.getFeatures();
    String originalContent = (String)
    features.get(GateConstants.ORIGINAL_DOCUMENT_CONTENT_FEATURE_NAME);
    if (originalContent != null) {
    Iterator<Annotation> it = as.iterator();
    Annotation currAnnot;
    SortedAnnotationList sortedAnnotations = new SortedAnnotationList();
    
    while(it.hasNext()) {
    currAnnot = it.next();
    sortedAnnotations.addSortedExclusive(currAnnot);
    } 
    
    for (Annotation ann : sortedAnnotations) {
    editableContent.append(ann.getType()).append(": ");
    editableContent.append(originalContent.substring(
    ann.getStartNode().getOffset().intValue(), ann.getEndNode().getOffset().intValue())).append("\n");
    if(ann.getType().equals("Person")) {
    features = ann.getFeatures();
    Object gender = features.get("gender");
    if(gender != null && gender instanceof String) {
    editableContent.append("Gender: ");
    editableContent.append(features.get("gender")).append("\n");
    }
    }
    }
    }
    return editableContent.toString();
    }
     */
    String xmlOutput(Document gateDoc)
    {
        return getXMLString(domOutput(gateDoc));
    }

    org.w3c.dom.Document domOutput(Document gateDoc)
    {
        //System.out.println(gateDoc.toXml());
        //String ret = "";
        org.w3c.dom.Document ret = null;


        DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document domDoc = builder.newDocument();

            org.w3c.dom.Element root = domDoc.createElement("ReportSet");
            domDoc.appendChild(root);

            org.w3c.dom.Element reportEl = domDoc.createElement("Report");
            root.appendChild(reportEl);

            org.w3c.dom.Element reportedPersonsEl = domDoc.createElement("ReportedPersonList");
            reportEl.appendChild(reportedPersonsEl);

            Set<Annotation> headerSet = getHeaderAnnotations(gateDoc);
            if (headerSet != null && !headerSet.isEmpty())
            {
                addAnnotations(domDoc, reportEl, "Reporter", gateDoc, headerSet);
            }

            Map<Annotation, List<Annotation>> subjects = getSubjectAnnotations(gateDoc.getAnnotations("Body"));
            if (subjects != null && !subjects.isEmpty())
            {
                Iterator<Annotation> iter = subjects.keySet().iterator();
                while (iter.hasNext())
                {
                    List<Annotation> annList = subjects.get(iter.next());
                    addAnnotations(domDoc, reportedPersonsEl, "ReportedPerson", gateDoc, annList);
                }
            }
            ret = domDoc;
        } catch (ParserConfigurationException pce)
        {
            // Parser with specified options can't be built
            pce.printStackTrace();
        }

        return ret;
    }

    public static String getXMLString(org.w3c.dom.Node node)
    {
        String ret = null;
        if (node != null)
        {
            Properties props = OutputPropertiesFactory.getDefaultMethodProperties("xml");
            props.setProperty("indent", "yes");
            props.setProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "2");

            Serializer ser = SerializerFactory.getSerializer(props);

            java.io.StringWriter sw = new java.io.StringWriter();
            ser.setWriter(sw);
            try
            {
                DOMSerializer dser = ser.asDOMSerializer();
                dser.serialize(node);
                ret = sw.toString();
            } catch (IOException ioe)
            {
            }
        }
        return ret;
    }

    void addAnnotations(org.w3c.dom.Document domDoc,
        org.w3c.dom.Element parentEl,
        String tagName,
        Document gateDoc,
        Collection<Annotation> annotationSet)
    {

        if (annotationSet != null && !annotationSet.isEmpty())
        {
            org.w3c.dom.Element newEl = domDoc.createElement(tagName);
            parentEl.appendChild(newEl);
            Iterator<Annotation> anIter = annotationSet.iterator();
            Annotation currAnnot;
            SortedAnnotationList sortedAnnotations = new SortedAnnotationList();

            while (anIter.hasNext())
            {
                currAnnot = anIter.next();
                sortedAnnotations.addSortedExclusive(currAnnot);
            }

            Iterator<Annotation> sortedSetIter = sortedAnnotations.iterator();

            while (sortedSetIter.hasNext())
            {
                Annotation annot = sortedSetIter.next();
                org.w3c.dom.Element annotEl = domDoc.createElement(annot.getType());
                newEl.appendChild(annotEl);
                annotEl.appendChild(domDoc.createTextNode(getText(gateDoc, annot)));
                Iterator<Entry<Object, Object>> featureIter = annot.getFeatures().entrySet().iterator();
                while (featureIter.hasNext())
                {
                    Entry<Object, Object> feature = featureIter.next();
                    Object value = feature.getValue();
                    String valueStr = "";
                    if (value != null && value instanceof Collection)
                    {
                        Iterator<Object> iter = ((Collection) value).iterator();
                        StringBuffer buf = new StringBuffer();
                        buf.append("[");
                        while (iter.hasNext())
                        {
                            if (buf.length() > 1)
                            {
                                buf.append(";");
                            }
                            buf.append(iter.next().toString());
                        }
                        buf.append("]");
                        valueStr = buf.toString();
                    } else if (value != null)
                    {
                        valueStr = value.toString();
                    }
                    annotEl.setAttribute((String) feature.getKey(), valueStr);
                }
            }
        }
    }

    String getText(Document gateDoc, Annotation ann)
    {
        FeatureMap features = gateDoc.getFeatures();
        String originalContent = (String) features.get(GateConstants.ORIGINAL_DOCUMENT_CONTENT_FEATURE_NAME);

        return originalContent.substring(
            ann.getStartNode().getOffset().intValue(), ann.getEndNode().getOffset().intValue());
    }

    /**
     *
     */
    @SuppressWarnings("serial")
    public static class SortedAnnotationList extends Vector<Annotation>
    {

        public SortedAnnotationList()
        {
            super();
        }

        public boolean addSortedExclusive(Annotation annot)
        {
            Annotation currAnot = null;

            // overlapping check
            for (int i = 0; i < size(); ++i)
            {
                currAnot = (Annotation) get(i);
                if (annot.overlaps(currAnot))
                {
                    return false;
                }
            }

            long annotStart = annot.getStartNode().getOffset().longValue();
            long currStart;
            // insert
            for (int i = 0; i < size(); ++i)
            {
                currAnot = (Annotation) get(i);
                currStart = currAnot.getStartNode().getOffset().longValue();
                if (annotStart < currStart)
                {
                    insertElementAt(annot, i);
                    return true;
                }
            }

            int size = size();
            insertElementAt(annot, size);
            return true;
        }
    }
}

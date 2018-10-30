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
package gov.nih.nlm.ceb.lpf.gate.shared;

import gate.util.Out;
import gate.AnnotationSet;
import gate.Annotation;
import gate.DocumentContent;
import gate.FeatureMap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class LPFNamedEntityUtil {

  private static final String GEOCODER_REQUEST_PREFIX_FOR_XML = 
  	  "http://maps.google.com/maps/api/geocode/xml";

  private static final String GEOCODER_RESULT_XPATH = 
      "/GeocodeResponse[status/text()='OK']/result" +
      "[type/text()='locality'" +
      " or type/text()='sublocality'" +
      " or type/text()='country'" +
      " or type/text()='administrative_area_level_1'" +
      " or type/text()='administrative_area_level_2'" +
      " or type/text()='administrative_area_level_3'" +
      "]";

  String personName = null;
  String location = null;
  String lat = null;
  String lng = null;
  //String lat = "0";
  //String lng = "0";
  
	public LPFNamedEntityUtil() {
		
	}
	
	public boolean validateLocation(String address) 	{

		boolean ret = false;
		
		//if(lat.equals("0") && lng.equals("0")) return ret;
	    // query address
		try {

	    // prepare a URL to the geocoder
	    URL url = new URL(GEOCODER_REQUEST_PREFIX_FOR_XML + "?address=" + URLEncoder.encode(address, "UTF-8") + "&sensor=false");

	    // prepare an HTTP connection to the geocoder
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

	    Document geocoderResultDocument = null;
	    try {
	      // open the connection and get results as InputSource.
	      conn.connect();
	      InputSource geocoderResultInputSource = new InputSource(conn.getInputStream());

	      // read result and parse into XML Document
	      geocoderResultDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(geocoderResultInputSource);
	    } finally {
	      conn.disconnect();
	    }

	    // prepare XPath
	    XPath xpath = XPathFactory.newInstance().newXPath();

	    // extract the result
	    NodeList resultNodeList = null;
	    resultNodeList = (NodeList) xpath.evaluate(GEOCODER_RESULT_XPATH, geocoderResultDocument, XPathConstants.NODESET);
	    if(resultNodeList != null && resultNodeList.getLength() > 0) {
	    	NodeList nl = (NodeList)xpath.evaluate("formatted_address", resultNodeList.item(0), XPathConstants.NODESET);
        if(nl != null && nl.getLength() > 0) {
	    	  location = nl.item(0).getTextContent();
	    	  ret = true;
		      lat = (String) xpath.evaluate("geometry/location/lat/text()", resultNodeList.item(0), XPathConstants.STRING);
		      lng = (String) xpath.evaluate("geometry/location/lng/text()", resultNodeList.item(0), XPathConstants.STRING);
        }
	    }
		}
		catch (Exception e) {
			e.printStackTrace(Out.getPrintWriter());
		}
		return ret;
	}

	public String getValidLocation() {
		return location;
	}
	
	public String getLatLng() {
		if(lat == null && lng == null) 
			return null;
		
		StringBuffer buf = new StringBuffer();
		if(lat != null) {
			buf.append(lat);
		}
		buf.append(",");
		if(lng != null) {
			buf.append(lng);
		}
		return buf.toString();
	}
	
	public String getLat() {
		return lat;
	}
	
	public String getLng() {
		return lng;
	}
	
	public boolean validatePerson(String name) {
		return false;
	}
	
	public static String getStackTrace(Throwable aThrowable) {
    final Writer result = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(result);
    aThrowable.printStackTrace(printWriter);
    return result.toString();
  }
	
	public static String getAntecedentString(gate.Document gateDoc, AnnotationSet as, int annId) {
		String ret = null;
		Annotation ann = getAntecedent(as, annId);
		DocumentContent content = gateDoc.getContent();
		try {
		  ret = content.getContent(
				     ann.getStartNode().getOffset(), 
				     ann.getStartNode().getOffset()).toString();
		}
		catch (Exception e) {
		}
		return ret;
	}

	public static Annotation getAntecedent(AnnotationSet as, int annId) {
		Annotation ann = as.get(annId);
		if(ann != null) {
			FeatureMap fm = ann.getFeatures();
			if(fm != null && fm.get("ENTITY_MENTION_TYPE") != null) {
				  // This is a reference annotation. Look for antecedent.
			    List<Integer> refs = (List<Integer>)fm.get("matches");
			    for(int i = 0; refs != null && i < refs.size(); i++) {
			    	Integer annid = refs.get(i);
			      Annotation a = as.get(annid.intValue());
			      if(a.getFeatures().get("ENTITY_MENTION_TYPE") == null) {
			      	ann = a;
			      	break;
			      }
			    }
			}
		}
		return ann;
	}



}

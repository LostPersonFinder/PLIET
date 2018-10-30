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
package gov.nih.nlm.lpf.emails.gate.pipeline;

/**
 * This is an implementation of the ANNIE processing pipeline for annotating Email documents.
 * The annotations are retrieved and processed through a separate module to determine their
 * relationships.
 * 
 * Author: 
 * Date: August 29, 2012
 * 
 */

import java.net.URL;
import java.net.MalformedURLException;

import gate.Gate;
import gate.util.GateException;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import gov.nih.nlm.lpf.emails.util.Utils;
import org.apache.log4j.Logger;


public class PLAnnieAppWrapper  
{
    private Logger log = Logger.getLogger(PLAnnieAppWrapper.class);
    
    boolean gateInited = false;
    
    PLAnnieApp annie;

    public PLAnnieAppWrapper(Properties ctxProperties) throws GateException
    {
        if (!gateInited)
        {
            try
            {
                // Load ANNIE plugin
                File gateHome = new File(System.getProperty("GateHome"));

                Gate.setGateHome(gateHome);
               // Gate.setUserConfigFile(new File(gateHome, "user-gate.xml"));
                String userConfig = Utils.getDereferencedProperty(ctxProperties, "UserConfig");
                File file = new File(userConfig);
                if (!file.exists())
                {
                    log.error ("File " + file.getAbsolutePath() + " does not exist");
                    return;
                }
                Gate.setUserConfigFile(new File(userConfig));
                Gate.init();


                // Parent Directory where all ANNIE Processing r"esources" exist
                URL annieURL = new File(System.getProperty("ANNIEHome")).toURI().toURL();
                URL pluginHome = new File( System.getProperty("gate.plugins.home")).toURI().toURL();
                URL user_pluginHome = new File( System.getProperty("gate.user_plugins.home")).toURI().toURL();
                
                // Register the Anaphora resolver
                 File pronounResource = new File(user_pluginHome+"/pronoun_annotator");
                //Gate.getCreoleRegister().registerDirectories( pronounResource.toURI().toURL());
               
                // get path to  file ANNIE_with_defaults.gapp 
                // NOTE: the application pipeline is defined in the .gapp file below
               // Note: It must match the name used in GATEDeveloper for debugging the application
                String  gappStr = ctxProperties.getProperty("default-LPF-app");
                File gappFile = new File(gappStr);
                annie = new PLAnnieApp(annieURL, pluginHome, user_pluginHome, gappFile);
                gateInited = true;
                System.out.println("...GATE initialized");
            } 
            catch (MalformedURLException me)
            {
                throw new GateException(me);
            } 
/*           catch (IOException ioe)
            {
                throw new GateException(ioe);
            }
*/            
        }
        System.out.println("...Parser  loaded");
    }
                
  /**************************************************************************/  
    // Return the Annie instance as created using the gapp file
    public PLAnnieApp getAnnieInstance()
    {
        return annie;
    }
  /**************************************************************************/  

}

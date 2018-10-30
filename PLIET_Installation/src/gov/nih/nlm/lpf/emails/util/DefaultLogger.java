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
package gov.nih.nlm.lpf.emails.util;

import org.apache.log4j.PropertyConfigurator;
import java.util.Properties;

/**
 *
 * @author 
 */
public class DefaultLogger 
{
    private static Properties debugProperties = null;
    private static Properties infoProperties = null;
    
    public static void init()
    {
        if(debugProperties == null)
        {
            // create property for logging DEBUG and higher level info
            Properties dp = new Properties();
            dp.setProperty("log4j.rootCategory", "DEBUG, A1");
            dp.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
            dp.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
            dp.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d %-5p %c @ %m%n");
            debugProperties = dp;
        }
        
         if(infoProperties == null)
         {
            // create property for logging INFO and higher level info
            Properties ip = new Properties();
            ip.setProperty("log4j.rootCategory", "INFO, A1");
            ip.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
            ip.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
            ip.setProperty("log4j.appender.A1.layout.ConversionPattern", "%-5p @ %m%n");
            infoProperties = ip;  
         }
    }
            
    
    /**
     * Enable logging to the dafault (Console) appender
     **/
    public static void enableDebugLogging()
    {
        init();
        PropertyConfigurator.configure(debugProperties);
    }
    
    public static void enableInfoLogging()
    {
        init();
        PropertyConfigurator.configure(infoProperties);
    }
    
    public static void enableLogging()
    {
        enableInfoLogging();
    }
    
    public static void enableLogging(String configFilename)
    {
        PropertyConfigurator.configure(configFilename);    // as set in the file
    }
}

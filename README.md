ABOUT
-----
PLIET (People Locator Email Processing Task), as the name indicates, is an email processing task which was designed to receive and process emails from disaster-affected persons, and to send apprpopriate data from each email to NLM's People Locator (PL) Services over SOAP interface. NLP processing techniques and problem-specific vocabulary are used to determine each email's context, interpret the free text, and extract the required fields such as a person's name, location, and health status. For each message sent to PL, PLIET would receive an apprropriate response from it, and then format and send a corresponding reply to the original email sender. 

Although PLIET operates in conjunction with PL, various components of PLIET may be customized/used in stand-alone manner or integrated with other systems - to process "domain-specific" English text for such systems and/or to interface with an email server.

PLIET, written in Java, can be run both on Linux and Windows machines with appropriate configuration.

LICENCE
-------
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, an agency of the Department of Health and Human Services, United States Government.

The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

The license does not supersede any applicable United States law.

The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation (FAR) 48 C.F.R. Part52.227-14, Rights in Data?General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership, as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

-	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

-	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

-	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes of Health, and the names of any of the software developers shall not be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.



Documents
-----------
All documents related to PLIET (Released version V1) are placed in the "Documents" directory, and are as follows:
  PLIET-USAGE.docx - High level architecture and intended usage pd PLIET software
  PLIET_V1_Design.docx - Detailed design and implementation document of PLIET
  PLIET_V1_DesignNOperations.pptx - A PowerPont presentation discussing PLIET architecture, implementation, deficiencies, inherent problems in interpreting "informmal text" and its operational viability.

The "Related" subdirectory contains certain documents providing examples and/or discussions on NLP algorithms used by PLIET. 
  
Build and Install
------------------ 
All software and libraries to build PLIET are located in the subdirectory "PLIET_Installation." 
The procedure to build and install the module are described in the file InstallationNotes.txt in that directory. 
Note that deployment of operational PLIET cannot be provided here as it requires dedicated emailbox and interfaces to the People Locator Service, which is no longer available. 
However, the use of various modules PLIET, especially its NLP processing component, are described in PLIET-USAGE.docx

Prerequisites
-------------
Java compiler: Version V7 or higher
NetBeans 8.0 or higher from netbeans.org
Windows or Linux-RedHat operating system


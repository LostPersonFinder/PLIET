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

import com.sun.mail.imap.IMAPSSLStore;

import gate.util.Err;
import gate.util.GateException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class TestAnnotateServiceImpl extends HttpServlet {
	/** The Corpus Pipeline application to contain ANNIE */
	//private static boolean gateInited = false;
	final static String TEST_PREFIX = "TESTANNIE";
	final static String mailhost = "mail.nih.gov";
	final static String smtphost = "smtp.nih.gov";
	
	//String host = "NIHMLBXBB01.nih.gov";
	final static String username = "NIH\\disasterNN";
	final static String password = "xxxxyyyyzzzz";
  final static String sendHost = "SMTP.nih.gov";
  final static String fromAddr = "disasterNN@mail.nih.gov";
	final static String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
	
	
	private LPFAnnieImpl annie;
	
	final static String [] includeHeaders = {
		"To",
		"From", 
		"Subject", 
		"Date", 
		"Content-Type",
		"MIME-Version",
		"Content-Language",
		"Message-ID"
	};
	
	public static Properties props = new Properties();

	MonitorMessages monitor = new MonitorMessages();

	class MonitorMessages extends Thread {
    public void run() {
			Err.prln(this.getName()+": New monitoring thread started.");
    	while(true) {
    		try {
    		  processMails();
    		  Thread.sleep(10000);
    		}
    		catch(GateException e) {
    			Err.prln(this.getName()+": ANNIE error");
    			e.printStackTrace(Err.getPrintWriter());
    			Err.prln(this.getName()+": Stopping monitoring serice.");
    			break;
    		}
    		catch (InterruptedException e) {
    			Err.prln(this.getName()+": Email monitoring is interrupted! Stop monitoring emails.");
    			break;
    		}
    		catch(Exception e) {
    			Err.prln(this.getName()+e.getMessage());
    			//e.printStackTrace(Err.getPrintWriter());
    		}
    	}
    }
		
	}

	@Override
	public void init() throws ServletException {
		super.init();
		props.setProperty("mail.imap.host", mailhost);
		props.setProperty("mail.imap.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.imap.socketFactory.fallback", "false");
		props.setProperty("mail.imap.port", "993");
		props.setProperty("mail.imap.socketFactory.port", "993");

		props.setProperty("mail.smtp.host", smtphost);
		props.setProperty("mail.smtp.socketFactory.port", "465");
		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.smtp.auth", "true");
		props.setProperty("mail.smtp.port", "465");
		try {
			annie = AnnieFactory.annieInstance(getServletContext());
			initMonitor(false);
		} catch (GateException gex) {
			Err.prln("cannot initialise GATE...");
			throw new ServletException(gex);
		}
  }

	void initMonitor(boolean reinit) {
		synchronized (monitor) {
			if (monitor.getState() != Thread.State.NEW && reinit == true) {
				monitor.interrupt();
				try {
					monitor.join(5000);
				} catch (InterruptedException e) {
				}
				monitor = new MonitorMessages();
			}
			else if(monitor.getState() == Thread.State.TERMINATED) {
				monitor = new MonitorMessages();
			}

			if(monitor.getState() == Thread.State.NEW) {
			  monitor.start();
			}
		}
	}
	
	public Store getIMAPStore() throws MessagingException {


		
		URLName url = new URLName("imap", mailhost, 993, "", username, password);

		Session session = Session.getInstance(props, null);
		Store store = new IMAPSSLStore(session, url);
		store.connect();
		
		return store;

	}
	
	Message[] getMessages() throws MessagingException {
		Message[] ret = null;
		Folder folder = null;
		;
		Store store = null;

		try {
			store = getIMAPStore();
			// Get folder
			folder = store.getFolder("INBOX");
			if (!folder.exists()) {
				folder = store.getDefaultFolder();
			}
			folder.open(Folder.READ_ONLY);

			// Get directory
			ret = folder.getMessages();

		} finally {
			if (folder != null) {
				folder.close(false);
			}
			if (store != null) {
				store.close();
			}
		}

		return ret;
	}
	
	void processMails() throws AddressException, MessagingException, IOException, GateException {

		Folder folder = null;;
		Store store = null;
 		
		try {
 		store = getIMAPStore();
    // Get folder
    folder = store.getFolder("INBOX");
    if(!folder.exists()) {
    	folder = store.getDefaultFolder();
    }
    folder.open(Folder.READ_WRITE);

    // Get directory
    //Message message[] = folder.getMessages();
    FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
    Message messages[] = folder.search(ft);

    for (int i=0, n=messages.length; i<n; i++) {
    	//String sub = messages[i].getSubject();
    	//if( sub != null && sub.startsWith(TEST_PREFIX)) {
        sendReply(messages[i]);
        messages[i].setFlag(Flags.Flag.SEEN, true);
    	//}
    }
		//folder.expunge();
		}
		finally {
			if(folder != null) {
				folder.close(false);
			}
			if(store != null) {
				store.close();
			}
		}
  }
	
	void sendReply(Message userMessage) throws AddressException, MessagingException, IOException, GateException {
  	//Properties props = System.getProperties();
		Session session = Session.getDefaultInstance(props,
			new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username,password);
				}
			});
 

    // Define message
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(userMessage.getRecipients(Message.RecipientType.TO)[0].toString()));
    message.addRecipient(Message.RecipientType.TO, 
      new InternetAddress(userMessage.getFrom()[0].toString()));
    String subject = userMessage.getSubject();
    if(subject != null && !subject.startsWith("Re: ")) {
    	subject = "Re: "+subject;
    }
    message.setSubject(subject);
    message.setHeader("X-Mailer", TEST_PREFIX);
    message.setSentDate(new Date());
    StringBuffer buf = new StringBuffer();
    try {
      buf = new StringBuffer(getAnnotations(userMessage));
    }
    catch(IOException io) {
    	buf.append(getStackTrace(io));
    	throw io;
    }
    catch (MessagingException me) {
    	buf.append(getStackTrace(me));
    	throw me;
    }
    catch (GateException me) {
    	buf.append(getStackTrace(me));
    	throw me;
    }
    finally {
      buf.append("\n\n");
      buf.append(">>>>>>>>> Your message >>>>>>>>>>\n");
      buf.append(formatMessage(userMessage));
      message.setText(buf.toString());
      // Send message
      Transport.send(message);
    }

		
	}


	String getStackTrace(Throwable t) {
		String ret = "";
		if(t != null) {
  	  StringWriter sw = new StringWriter();
  	  t.printStackTrace(new PrintWriter(sw));
  	  ret = sw.toString();
		}
		
		return ret;
	}
	
	
	String getAnnotations(Message msg) throws IOException, MessagingException, GateException {
		String ret = null;
	  String input = formatMessage(msg);
	  ret = annie.annotate(input, "text/email");
		return ret;
	}
	
	org.w3c.dom.Document getAnnotationsDOM(Message msg) throws IOException, MessagingException, GateException {
		org.w3c.dom.Document ret = null;
	  String input = formatMessage(msg);
	  ret = annie.annotateDOM(input, "text/email");
		return ret;
	}
	
	void annotateAll(HttpServletResponse response) throws IOException,
			MessagingException, GateException, ParserConfigurationException {
		Message[] messages = null;
		Folder folder = null;
		;
		Store store = null;

		try {
			store = getIMAPStore();
			// Get folder
			folder = store.getFolder("INBOX");
			if (!folder.exists()) {
				folder = store.getDefaultFolder();
			}
			folder.open(Folder.READ_ONLY);

			// Get directory
			messages = folder.getMessages();

			org.w3c.dom.Document ret = null;

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			ret = builder.newDocument();

			org.w3c.dom.Element root = ret.createElement("ReportSet");
			ret.appendChild(root);

			for (int i = 0; i < messages.length; i++) {
				org.w3c.dom.Document doc = getAnnotationsDOM(messages[i]);
				if (doc != null) {
					org.w3c.dom.Element aDocRoot = doc.getDocumentElement();
					org.w3c.dom.NodeList reportList = aDocRoot
							.getElementsByTagName("Report");
					for (int n = 0, l = reportList.getLength(); n < l; n++) {
						org.w3c.dom.Node aNode = ret.adoptNode(reportList.item(n));
						org.w3c.dom.Element origMsg = ret.createElement("OriginalMessage");
						origMsg.appendChild(ret
								.createCDATASection(formatMessage(messages[i])));
						aNode.appendChild(origMsg);
						root.appendChild(aNode);
					}
				}
			}

			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/xml");
			response.getWriter().write(LPFAnnieImpl.getXMLString(ret));
		} finally {
			if (folder != null) {
				folder.close(false);
			}
			if (store != null) {
				store.close();
			}
		}

	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
	
	}

@Override
public void doGet(HttpServletRequest request, HttpServletResponse response)
throws ServletException, IOException {
  invokeServlet(request, response);
}


	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		invokeServlet(request, response);
	}
	
	public void invokeServlet (HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		try {
			boolean reinit = new Boolean(request.getParameter("reinit")).booleanValue();
			boolean annotateAll = new Boolean(request.getParameter("annotateAll")).booleanValue();
  	  //processMails();
		  initMonitor(reinit);
		  if(annotateAll) {
		    annotateAll(response);
		  }
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
	}

	String getMessageCharset(Message m ) throws MessagingException, MimeTypeParseException{
		MimeType mt = new MimeType(m.getContentType());
		String charset = mt.getParameter("charset");
		if(charset == null) {
			return "UTF-8";
		}
		
		return charset;
	}
	
	public String formatMessage(Message m) throws MessagingException {
		String ret = "";
		try {
			Session session = Session.getInstance(props);
			Message newMsg = new MimeMessage(session);
			for(Enumeration<Header> e = m.getMatchingHeaders(includeHeaders); e.hasMoreElements(); ) {
				Header h = e.nextElement();
				newMsg.addHeader(h.getName(), h.getValue());
			}
			Object content = m.getContent();
			if(content instanceof Multipart) {
				Part bodyPart = ((Multipart)content).getBodyPart(0);
				newMsg.setContent(bodyPart.getContent(), bodyPart.getContentType());
			}
			else if(content instanceof Part) {
				newMsg.setContent(content, ((Part)content).getContentType());
			}
			else {
				newMsg.setText(content.toString());
			}

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			StringBuffer firstLine = new StringBuffer();
			firstLine.append("From ").
			          append(m.getFrom()[0].toString()).
			          append(" ").
			          append(m.getReceivedDate().toString()).
			          append("\r\n");
			os.write(firstLine.toString().getBytes());
			
			newMsg.writeTo(os);
			
			String encoding = "UTF-8";
			try {
				encoding = getMessageCharset(newMsg);
			} 
			catch (Exception e) {
			}

			ret = os.toString(encoding);
			
			/*
			if (m instanceof IMAPMessage) {
				ArrayList<String> ignoreHeaders = new ArrayList<String>();
				for (Enumeration<Header> e = m.getNonMatchingHeaders(includeHeaders); 
				     e.hasMoreElements();) {
					ignoreHeaders.add(e.nextElement().getName());
				}
				// Add first line
				StringBuffer firstLine = new StringBuffer();
				firstLine.append("From ").
				          append(m.getFrom()[0].toString()).
				          append(" ").
				          append(m.getReceivedDate().toString()).
				          append("\r\n");
				os.write(firstLine.toString().getBytes());
				((IMAPMessage) m).writeTo(os, (String[]) ignoreHeaders
						.toArray(new String[0]));
			} else {
				m.writeTo(os);
			}
			ret = os.toString(encoding);
			*/
		} 
		catch (IOException ioe) {
			ret = ioe.getMessage();
		}
		return ret;
	}
}

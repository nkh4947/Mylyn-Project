/*******************************************************************************
 * Copyright (c) 2004, 2008 Brock Janiczak and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Brock Janiczak - initial API and implementation
 *     Tasktop Technologies - improvements
 *     Pawel Niewiadomski - fix for bug 288441
 *******************************************************************************/

/*
 * JiraSoapServiceServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.2RC3 Feb 28, 2005 (10:15:14 EST) WSDL2Java emitter.
 */
package com.atlassian.connector.eclipse.internal.jira.core.service.soap;

import java.util.Hashtable;

import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;

import org.apache.axis.transport.http.HTTPConstants;
import org.eclipse.mylyn.commons.net.WebUtil;
import org.eclipse.mylyn.internal.provisional.commons.soap.SoapHttpSender;

import com.atlassian.connector.eclipse.internal.jira.core.service.JiraClient;
import com.atlassian.connector.eclipse.internal.jira.core.wsdl.soap.JiraSoapServiceServiceLocator;

/**
 * @author Brock Janiczak
 * @author Steffen Pingel
 */
@SuppressWarnings("serial")
public class JiraSoapServiceLocator extends JiraSoapServiceServiceLocator {

	private final JiraClient client;

	public JiraSoapServiceLocator(org.apache.axis.EngineConfiguration config, JiraClient client) {
		super(config);
		this.client = client;
	}

	@Override
	public Call createCall() throws ServiceException {
		Call call = super.createCall();
		// JIRA does not accept compressed SOAP messages
		//call.setProperty(HTTPConstants.MC_GZIP_REQUEST, Boolean.TRUE);
		if (client.isCompressionEnabled()) {
			call.setProperty(HTTPConstants.MC_ACCEPT_GZIP, Boolean.TRUE);
			// work around a bug in JIRA 3.11 to 3.13.1 that causes server to return malformed HTTP header
			call.setProperty(SoapHttpSender.ALLOW_EMPTY_CONTENT_ENCODING, Boolean.TRUE);
		}

		call.setProperty(SoapHttpSender.LOCATION, client.getLocation());

		Hashtable<String, String> headers = new Hashtable<String, String>();
		headers.put(HTTPConstants.HEADER_USER_AGENT, WebUtil.getUserAgent("JiraConnector Axis/1.4")); //$NON-NLS-1$
		// some servers break with a 411 Length Required when chunked encoding is used
		headers.put(HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED, Boolean.FALSE.toString());
		call.setProperty(HTTPConstants.REQUEST_HEADERS, headers);
		return call;
	}

}

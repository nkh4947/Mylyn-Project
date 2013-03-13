/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Tasktop Technologies - initial API and implementation
 *******************************************************************************/
/**
 * JiraSoapServiceService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package com.atlassian.connector.eclipse.internal.jira.core.wsdl.soap;

@SuppressWarnings("all")
public interface JiraSoapServiceService extends javax.xml.rpc.Service {
	public java.lang.String getJirasoapserviceV2Address();

	public com.atlassian.connector.eclipse.internal.jira.core.wsdl.soap.JiraSoapService getJirasoapserviceV2()
			throws javax.xml.rpc.ServiceException;

	public com.atlassian.connector.eclipse.internal.jira.core.wsdl.soap.JiraSoapService getJirasoapserviceV2(java.net.URL portAddress)
			throws javax.xml.rpc.ServiceException;
}

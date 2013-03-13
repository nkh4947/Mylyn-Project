/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package com.atlassian.connector.eclipse.jira.tests.core;

import junit.framework.TestCase;

import org.eclipse.mylyn.commons.net.WebLocation;
import org.eclipse.mylyn.tests.util.TestFixture;

import com.atlassian.connector.eclipse.internal.jira.core.JiraClientFactory;
import com.atlassian.connector.eclipse.internal.jira.core.service.JiraServiceUnavailableException;

public class JiraClientFactoryServerUnrelatedTest extends TestCase {

	private JiraClientFactory clientFactory;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		clientFactory = JiraClientFactory.getDefault();
		TestFixture.resetTaskListAndRepositories();
	}

	@Override
	protected void tearDown() throws Exception {
		clientFactory.logOutFromAll();
	}

	public void testValidate() throws Exception {
		// invalid URL		
		try {
			clientFactory.validateConnection(new WebLocation("http://non.existant/repository", "user", "password"),
					null);
			fail("Expected exception");
		} catch (JiraServiceUnavailableException e) {
		}

		// not found		
		try {
			clientFactory.validateConnection(new WebLocation("http://www.atlassian.com/not-found", "user", "password"),
					null);
			fail("Expected exception");
		} catch (JiraServiceUnavailableException e) {
			assertEquals("No JIRA repository found at location. Invalid URL or proxy problem.", e.getMessage());
		}

		// RPC not enabled
		try {
			clientFactory.validateConnection(new WebLocation("http://mylyn.eclipse.org/jira-invalid", "user",
					"password"), null);
			fail("Expected exception");
		} catch (JiraServiceUnavailableException e) {
			assertEquals("JIRA RPC services are not enabled. Please contact your JIRA administrator.", e.getMessage());
		}

		// HTTP error
		try {
			clientFactory.validateConnection(new WebLocation("http://mylyn.eclipse.org/jira-proxy-error", "user",
					"password"), null);
			fail("Expected exception");
		} catch (JiraServiceUnavailableException e) {
			assertEquals("JIRA RPC services are not enabled. Please contact your JIRA administrator.", e.getMessage());
		}
	}

}

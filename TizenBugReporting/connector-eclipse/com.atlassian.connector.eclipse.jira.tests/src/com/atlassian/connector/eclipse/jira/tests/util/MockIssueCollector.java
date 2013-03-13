/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package com.atlassian.connector.eclipse.jira.tests.util;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.connector.eclipse.internal.jira.core.model.JiraIssue;
import com.atlassian.connector.eclipse.internal.jira.core.model.filter.IssueCollector;

/**
 * @author Steffen Pingel
 */
public class MockIssueCollector implements IssueCollector {

	public List<JiraIssue> issues = new ArrayList<JiraIssue>();

	public boolean done;

	public boolean started;

	public void collectIssue(JiraIssue issue) {
		issues.add(issue);
	}

	public void done() {
		done = true;
	}

	public int getMaxHits() {
		return 1000;
	}

	public boolean isCancelled() {
		return false;
	}

	public void start() {
		this.started = true;
	}

}
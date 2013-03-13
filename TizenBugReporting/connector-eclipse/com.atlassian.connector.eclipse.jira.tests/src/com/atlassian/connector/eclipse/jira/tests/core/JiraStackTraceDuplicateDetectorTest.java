/*******************************************************************************
 * Copyright (c) 2004, 2008 Eugene Kuleshov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eugene Kuleshov - initial API and implementation
 *     Tasktop Technologies - improvements
 *******************************************************************************/

package com.atlassian.connector.eclipse.jira.tests.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.tasks.ui.search.RepositorySearchResult;
import org.eclipse.mylyn.internal.tasks.ui.search.SearchHitCollector;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;

import com.atlassian.connector.eclipse.internal.jira.core.JiraClientFactory;
import com.atlassian.connector.eclipse.internal.jira.core.model.JiraIssue;
import com.atlassian.connector.eclipse.internal.jira.core.service.JiraClient;
import com.atlassian.connector.eclipse.internal.jira.ui.JiraSearchHandler;
import com.atlassian.connector.eclipse.jira.tests.util.JiraFixture;
import com.atlassian.connector.eclipse.jira.tests.util.JiraTestUtil;

/**
 * @author Eugene Kuleshov
 * @author Steffen Pingel
 */
public class JiraStackTraceDuplicateDetectorTest extends TestCase {

	private TaskRepository repository;

	private JiraClient client;

	@Override
	protected void setUp() throws Exception {
		JiraTestUtil.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		JiraTestUtil.tearDown();
	}

	protected void init(String url) throws Exception {
		repository = JiraTestUtil.init(url);
		client = JiraClientFactory.getDefault().getJiraClient(repository);
	}

	public void testStackTraceInDescription() throws Exception {
		init(jiraUrl());

		StringWriter sw = new StringWriter();
		new Exception().printStackTrace(new PrintWriter(sw));
		String stackTrace = sw.toString();
		JiraIssue issue1 = JiraTestUtil.newIssue(client, "testStackTraceDetector1");
		issue1.setDescription(stackTrace);
		issue1 = JiraTestUtil.createIssue(client, issue1);

		verifyDuplicate(stackTrace, issue1);
	}

	public void testStackTraceInComment() throws Exception {
		init(jiraUrl());

		StringWriter sw = new StringWriter();
		new Exception().printStackTrace(new PrintWriter(sw));
		String stackTrace = sw.toString();
		JiraIssue issue1 = JiraTestUtil.createIssue(client, "testStackTraceDetector2");
		client.updateIssue(issue1, stackTrace, null);

		verifyDuplicate(stackTrace, issue1);
	}

	private void verifyDuplicate(String stackTrace, final JiraIssue issue) throws Exception {
		ITask task1 = JiraTestUtil.createTask(repository, issue.getKey());
		assertEquals(issue.getSummary(), task1.getSummary());
		assertEquals(false, task1.isCompleted());
		assertNull(task1.getDueDate());

		JiraIssue issue2 = JiraTestUtil.newIssue(client, "testStackTraceDetector1");
		issue2.setDescription(stackTrace);

//		TaskData data = new TaskData(dataHandler.getAttributeMapper(repository), repository.getConnectorKind(),
//				repository.getRepositoryUrl(), "");
//		dataHandler.initializeTaskData(repository, data, new TaskMapping() {
//			@Override
//			public String getProduct() {
//				return issue.getProject().getId();
//			}
//		}, null);
//		data.getRoot().getAttribute(JiraAttribute.DESCRIPTION.id()).setValue(stackTrace);

		JiraSearchHandler detector = new JiraSearchHandler();
		IRepositoryQuery duplicatesQuery = TasksUi.getRepositoryModel().createRepositoryQuery(repository);
		assertTrue(detector.queryForText(repository, duplicatesQuery, null, stackTrace));
		SearchHitCollector collector = new SearchHitCollector(TasksUiInternal.getTaskList(), repository,
				duplicatesQuery);
		collector.run(new NullProgressMonitor());
		RepositorySearchResult searchResult = (RepositorySearchResult) collector.getSearchResult();
		assertTrue("Expected duplicated task " + issue.getId() + " : " + issue.getKey(),
				searchResult.getMatchCount() > 0);
		for (Object element : searchResult.getElements()) {
			ITask task = (ITask) element;
			if (task.getTaskId().equals(issue.getId())) {
				return;
			}
		}
		fail("Duplicated task not found " + issue.getId() + " : " + issue.getKey());
	}

	private String jiraUrl() {
		return JiraFixture.current().getRepositoryUrl();
	}
}

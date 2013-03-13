/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Eugene Kuleshov - improvements
 *     Pawel Niewiadomski - fixes for bug 288347, 
 *******************************************************************************/

package com.atlassian.connector.eclipse.internal.jira.core;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;
import org.eclipse.osgi.util.NLS;

import com.atlassian.connector.eclipse.internal.jira.core.html.HTML2TextReader;
import com.atlassian.connector.eclipse.internal.jira.core.model.Attachment;
import com.atlassian.connector.eclipse.internal.jira.core.model.Comment;
import com.atlassian.connector.eclipse.internal.jira.core.model.Component;
import com.atlassian.connector.eclipse.internal.jira.core.model.CustomField;
import com.atlassian.connector.eclipse.internal.jira.core.model.IssueField;
import com.atlassian.connector.eclipse.internal.jira.core.model.IssueLink;
import com.atlassian.connector.eclipse.internal.jira.core.model.IssueType;
import com.atlassian.connector.eclipse.internal.jira.core.model.JiraAction;
import com.atlassian.connector.eclipse.internal.jira.core.model.JiraIssue;
import com.atlassian.connector.eclipse.internal.jira.core.model.JiraStatus;
import com.atlassian.connector.eclipse.internal.jira.core.model.JiraVersion;
import com.atlassian.connector.eclipse.internal.jira.core.model.JiraWorkLog;
import com.atlassian.connector.eclipse.internal.jira.core.model.Priority;
import com.atlassian.connector.eclipse.internal.jira.core.model.Project;
import com.atlassian.connector.eclipse.internal.jira.core.model.ProjectRole;
import com.atlassian.connector.eclipse.internal.jira.core.model.Resolution;
import com.atlassian.connector.eclipse.internal.jira.core.model.SecurityLevel;
import com.atlassian.connector.eclipse.internal.jira.core.model.Subtask;
import com.atlassian.connector.eclipse.internal.jira.core.model.User;
import com.atlassian.connector.eclipse.internal.jira.core.model.Version;
import com.atlassian.connector.eclipse.internal.jira.core.service.JiraClient;
import com.atlassian.connector.eclipse.internal.jira.core.service.JiraException;
import com.atlassian.connector.eclipse.internal.jira.core.service.JiraInsufficientPermissionException;
import com.atlassian.connector.eclipse.internal.jira.core.service.JiraServiceUnavailableException;
import com.atlassian.connector.eclipse.internal.jira.core.service.JiraTimeFormat;
import com.atlassian.connector.eclipse.internal.jira.core.util.JiraUtil;
import com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteCustomFieldValue;
import com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteIssue;

/**
 * @author Mik Kersten
 * @author Rob Elves
 * @author Eugene Kuleshov
 * @author Steffen Pingel
 * @author Thomas Ehrnhoefer
 * @since 3.0
 */
public class JiraTaskDataHandler extends AbstractTaskDataHandler {

	/**
	 * Public for testing
	 */
	public static final class CommentDateComparator implements Comparator<Comment> {
		public int compare(Comment o1, Comment o2) {
			if (o1 != null && o2 != null) {
				if (o1.getCreated() != null && o2.getCreated() != null) {
					return o1.getCreated().compareTo(o2.getCreated());
				}
			}
			return 0;
		}
	}

	private static final String CONTEXT_ATTACHEMENT_FILENAME = "mylyn-context.zip"; //$NON-NLS-1$

	private static final String CONTEXT_ATTACHEMENT_FILENAME_LEGACY = "mylar-context.zip"; //$NON-NLS-1$

	private static final String CONTEXT_ATTACHMENT_DESCRIPTION = "mylyn/context/zip"; //$NON-NLS-1$

	private static final String CONTEXT_ATTACHMENT_DESCRIPTION_LEGACY = "mylar/context/zip"; //$NON-NLS-1$

	private static final boolean TRACE_ENABLED = Boolean.valueOf(Platform.getDebugOption("com.atlassian.connector.eclipse.jira.core/debug/dataHandler")); //$NON-NLS-1$

	private static final String REASSIGN_OPERATION = "reassign"; //$NON-NLS-1$

	public static final String STOP_PROGRESS_OPERATION = "301"; //$NON-NLS-1$

	public static final String START_PROGRESS_OPERATION = "4"; //$NON-NLS-1$	

	public static final Object IN_PROGRESS_STATUS = "3"; //$NON-NLS-1$

	public static final Object OPEN_STATUS = "1"; //$NON-NLS-1$

	public static final Object REOPEN_STATUS = "4"; //$NON-NLS-1$

	private static final String LEAVE_OPERATION = "leave"; //$NON-NLS-1$

	private static final JiraVersion TASK_DATA_VERSION_1_0 = new JiraVersion("1.0"); //$NON-NLS-1$

	private static final JiraVersion TASK_DATA_VERSION_2_0 = new JiraVersion("2.0"); //$NON-NLS-1$

	private static final JiraVersion TASK_DATA_VERSION_2_2 = new JiraVersion("2.2"); //$NON-NLS-1$

	private static final JiraVersion TASK_DATA_VERSION_CURRENT = new JiraVersion("3.0"); //$NON-NLS-1$

	private final IJiraClientFactory clientFactory;

	public JiraTaskDataHandler(IJiraClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor)
			throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(Messages.JiraTaskDataHandler_Getting_task, IProgressMonitor.UNKNOWN);

			JiraClient client = clientFactory.getJiraClient(repository);
			if (!client.getCache().hasDetails()) {
				client.getCache().refreshDetails(monitor);
			}
			JiraIssue jiraIssue = getJiraIssue(client, taskId, repository.getRepositoryUrl(), monitor);
			if (jiraIssue != null) {
				return createTaskData(repository, client, jiraIssue, null, monitor);
			}
			throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR, JiraCorePlugin.ID_PLUGIN,
					IStatus.OK, "JIRA ticket not found: " + taskId, null)); //$NON-NLS-1$

		} catch (JiraException e) {
			IStatus status = JiraCorePlugin.toStatus(repository, e);
			trace(status);
			throw new CoreException(status);
		} finally {
			monitor.done();
		}
	}

	private JiraIssue getJiraIssue(JiraClient client, String taskId, String repositoryUrl, IProgressMonitor monitor) //
			throws CoreException, JiraException {
		try {
			int id = Integer.parseInt(taskId);
			// TODO consider keeping a cache of id -> key in the JIRA core plug-in
//			AbstractTask task = TasksUiPlugin.getTaskList().getTask(repositoryUrl, "" + id);
//			if (task != null) {
//				return client.getIssueByKey(task.getTaskKey(), monitor);
//			} else {
			String issueKey = client.getKeyFromId(id + "", monitor); //$NON-NLS-1$
			return client.getIssueByKey(issueKey, monitor);
//			}
		} catch (NumberFormatException e) {
			return client.getIssueByKey(taskId, monitor);
		}
	}

	public TaskData createTaskData(TaskRepository repository, JiraClient client, JiraIssue jiraIssue,
			TaskData oldTaskData, IProgressMonitor monitor) throws JiraException {
		return createTaskData(repository, client, jiraIssue, oldTaskData, false, monitor);
	}

	public TaskData createTaskData(TaskRepository repository, JiraClient client, JiraIssue jiraIssue,
			TaskData oldTaskData, boolean forceCache, IProgressMonitor monitor) throws JiraException {
		TaskData data = new TaskData(getAttributeMapper(repository), JiraCorePlugin.CONNECTOR_KIND,
				repository.getRepositoryUrl(), jiraIssue.getId());
		initializeTaskData(repository, data, client, jiraIssue.getProject(), monitor);
		updateTaskData(data, jiraIssue, client, oldTaskData, forceCache, monitor);
		addOperations(data, jiraIssue, client, oldTaskData, forceCache, monitor);
		return data;
	}

	private Project ensureProjectHasDetails(JiraClient client, TaskRepository repository, Project project,
			IProgressMonitor monitor) throws JiraException {
		if (!project.hasDetails()) {
			client.getCache().refreshProjectDetails(project.getId(), monitor);
			return client.getCache().getProjectById(project.getId());
		}
		return project;
	}

	private final String[] OPTION_KEY_SEVERITY = { "10033", "10034", "10035", "10036", "10037" };

	private final String[] OPTION_VALUE_SEVERITY = { "1-Critical", "2-Major", "3-Average", "4-Minor", "5-Enhancement" };

	private final String[] OPTION_KEY_FREQUENCY = { "10050", "10051", "10052" };

	private final String[] OPTION_VALUE_FREQUENCY = { "Once", "Always", "Frequent" };

	private TaskAttribute createTizenAttribute(TaskData data, TizenAttribute key) {
		TaskAttribute attribute = data.getRoot().createAttribute(key.id());
		TaskAttributeMetaData metaData = attribute.getMetaData().defaults();
		metaData.setReadOnly(key.isReadOnly());
		metaData.setKind(key.getKind());
		metaData.setLabel(key.getName());
		metaData.setType(key.getType().getTaskType());
		metaData.putValue("type", key.getType().getKey());
		return attribute;
	}

	public void initializeTaskData(TaskRepository repository, TaskData data, JiraClient client, Project project,
			IProgressMonitor monitor) throws JiraException {
		project = ensureProjectHasDetails(client, repository, project, monitor);

//		TaskAttribute severity = createTizenAttribute(data, TizenAttribute.ATTRIBUTE_SEVERITY);
//		for (int i = 0; i < OPTION_KEY_SEVERITY.length; i++) {
//			severity.putOption(OPTION_KEY_SEVERITY[i], OPTION_VALUE_SEVERITY[i]);
//		}
//		severity.setValue(OPTION_KEY_SEVERITY[3]);
//
//		TaskAttribute frequency = createTizenAttribute(data, TizenAttribute.ATTRIBUTE_FREQUENCY);
//		for (int i = 0; i < OPTION_KEY_FREQUENCY.length; i++) {
//			frequency.putOption(OPTION_KEY_FREQUENCY[i], OPTION_VALUE_FREQUENCY[i]);
//		}

		data.setVersion(TASK_DATA_VERSION_CURRENT.toString());

		createAttribute(data, JiraAttribute.CREATION_DATE);
		TaskAttribute summaryAttribute = createAttribute(data, JiraAttribute.SUMMARY);
		summaryAttribute.getMetaData().setType(TaskAttribute.TYPE_SHORT_RICH_TEXT);
		TaskAttribute descriptionAttribute = createAttribute(data, JiraAttribute.DESCRIPTION);
		descriptionAttribute.getMetaData().setType(TaskAttribute.TYPE_LONG_RICH_TEXT);
		createAttribute(data, JiraAttribute.STATUS);
		createAttribute(data, JiraAttribute.ISSUE_KEY);
		createAttribute(data, JiraAttribute.TASK_URL);
		createAttribute(data, JiraAttribute.USER_ASSIGNED);
		createAttribute(data, JiraAttribute.USER_REPORTER);
		createAttribute(data, JiraAttribute.MODIFICATION_DATE);

		TaskAttribute projectAttribute = createAttribute(data, JiraAttribute.PROJECT);
		Project[] jiraProjects = client.getCache().getProjects();
		for (Project jiraProject : jiraProjects) {
			projectAttribute.putOption(jiraProject.getId(), jiraProject.getName());
		}
		projectAttribute.setValue(project.getId());

		TaskAttribute resolutions = createAttribute(data, JiraAttribute.RESOLUTION);
		Resolution[] jiraResolutions = client.getCache().getResolutions();
		if (jiraResolutions.length > 0) {
			for (Resolution resolution : jiraResolutions) {
				resolutions.putOption(resolution.getId(), resolution.getName());
			}
		} else {
			resolutions.putOption(Resolution.FIXED_ID, "Fixed"); //$NON-NLS-1$
			resolutions.putOption(Resolution.WONT_FIX_ID, "Won't Fix"); //$NON-NLS-1$
			resolutions.putOption(Resolution.DUPLICATE_ID, "Duplicate"); //$NON-NLS-1$
			resolutions.putOption(Resolution.INCOMPLETE_ID, "Incomplete"); //$NON-NLS-1$
			resolutions.putOption(Resolution.CANNOT_REPRODUCE_ID, "Cannot Reproduce"); //$NON-NLS-1$
		}

		TaskAttribute priorities = createAttribute(data, JiraAttribute.PRIORITY);
		Priority[] jiraPriorities = client.getCache().getPriorities();
		for (int i = 0; i < jiraPriorities.length; i++) {
			Priority priority = jiraPriorities[i];
			priorities.putOption(priority.getId(), priority.getName());
			if (i == (jiraPriorities.length / 2)) {
				priorities.setValue(priority.getId());
			}
		}

		TaskAttribute types = createAttribute(data, JiraAttribute.TYPE);
		IssueType[] jiraIssueTypes = project.getIssueTypes();
		if (jiraIssueTypes == null) {
			jiraIssueTypes = client.getCache().getIssueTypes();
		}
		for (int i = 0; i < jiraIssueTypes.length; i++) {
			IssueType type = jiraIssueTypes[i];
			if (!type.isSubTaskType()) {
				types.putOption(type.getId(), type.getName());
				// set first as default
				if (i == 0) {
					types.setValue(type.getId());
				}
			}
		}

		createAttribute(data, JiraAttribute.PARENT_KEY);
		createAttribute(data, JiraAttribute.PARENT_ID);

		createAttribute(data, JiraAttribute.DUE_DATE);
		createAttribute(data, JiraAttribute.ESTIMATE);
		if (!data.isNew()) {
			createAttribute(data, JiraAttribute.ACTUAL);
			createAttribute(data, JiraAttribute.INITIAL_ESTIMATE);
		}

		TaskAttribute affectsVersions = createAttribute(data, JiraAttribute.AFFECTSVERSIONS);
		for (Version version : project.getVersions()) {
			affectsVersions.putOption(version.getId(), version.getName());
		}

		TaskAttribute components = createAttribute(data, JiraAttribute.COMPONENTS);
		for (Component component : project.getComponents()) {
			components.putOption(component.getId(), component.getName());
		}

		TaskAttribute fixVersions = createAttribute(data, JiraAttribute.FIXVERSIONS);
		for (Version version : project.getVersions()) {
			fixVersions.putOption(version.getId(), version.getName());
		}

		TaskAttribute env = createAttribute(data, JiraAttribute.ENVIRONMENT);
		env.getMetaData().setType(TaskAttribute.TYPE_LONG_RICH_TEXT);

		if (!data.isNew()) {
			TaskAttribute commentAttribute = createAttribute(data, JiraAttribute.COMMENT_NEW);
			commentAttribute.getMetaData().setType(TaskAttribute.TYPE_LONG_RICH_TEXT);
		}

		SecurityLevel[] securityLevels = project.getSecurityLevels();
		if (securityLevels != null) {
			TaskAttribute securityLevelAttribute = createAttribute(data, JiraAttribute.SECURITY_LEVEL);
			for (SecurityLevel securityLevel : securityLevels) {
				securityLevelAttribute.putOption(securityLevel.getId(), securityLevel.getName());
			}
			securityLevelAttribute.setValue(SecurityLevel.NONE.getId());
		}

		data.getRoot().createAttribute(WorkLogConverter.ATTRIBUTE_WORKLOG_NEW);

//		data.getRoot().createAttribute(WorkLogConverter.ATTRIBUTE_WORKLOG_MYLYN_ACTIVITY_DELTA);

		TaskAttribute projectRoles = createAttribute(data, JiraAttribute.PROJECT_ROLES);
		projectRoles.putOption(IJiraConstants.NEW_COMMENT_VIEWABLE_BY_ALL, IJiraConstants.NEW_COMMENT_VIEWABLE_BY_ALL);
		ProjectRole[] roles = client.getCache().getProjectRoles();
		if (roles != null) {
			for (ProjectRole projectRole : roles) {
				projectRoles.putOption(projectRole.getName(), projectRole.getName());
			}
		}

		createAttribute(data, JiraAttribute.VOTES);
	}

	public TaskAttribute createAttribute(TaskData data, JiraAttribute key) {
		TaskAttribute attribute = data.getRoot().createAttribute(key.id());
		attribute.getMetaData().defaults() //
				.setReadOnly(key.isReadOnly())
				.setKind(key.getKind())
				.setLabel(key.getName())
				.setType(key.getType().getTaskType())
				.putValue(IJiraConstants.META_TYPE, key.getType().getKey());
		return attribute;
	}

	private void updateTaskData(TaskData data, JiraIssue jiraIssue, JiraClient client, TaskData oldTaskData,
			boolean forceCache, IProgressMonitor monitor) throws JiraException {
		String parentKey = jiraIssue.getParentKey();
		if (parentKey != null) {
			setAttributeValue(data, JiraAttribute.PARENT_KEY, parentKey);
		} else {
			removeAttribute(data, JiraAttribute.PARENT_KEY);
		}

		String parentId = jiraIssue.getParentId();
		if (parentId != null) {
			setAttributeValue(data, JiraAttribute.PARENT_ID, parentId);
		} else {
			removeAttribute(data, JiraAttribute.PARENT_ID);
		}

		Subtask[] subtasks = jiraIssue.getSubtasks();
		if (subtasks != null && subtasks.length > 0) {
			createAttribute(data, JiraAttribute.SUBTASK_IDS);
			createAttribute(data, JiraAttribute.SUBTASK_KEYS);
			for (Subtask subtask : subtasks) {
				addAttributeValue(data, JiraAttribute.SUBTASK_IDS, subtask.getIssueId());
				addAttributeValue(data, JiraAttribute.SUBTASK_KEYS, subtask.getIssueKey());
			}
		}

		IssueLink[] issueLinks = jiraIssue.getIssueLinks();
		if (issueLinks != null && issueLinks.length > 0) {
			HashMap<String, TaskAttribute> links = new HashMap<String, TaskAttribute>();
			for (IssueLink link : issueLinks) {
				String key;
				String desc;
				if (link.getInwardDescription() == null) {
					key = link.getLinkTypeId() + "outward"; //$NON-NLS-1$
					desc = link.getOutwardDescription();
				} else {
					key = link.getLinkTypeId() + "inward"; //$NON-NLS-1$
					desc = link.getInwardDescription();
				}
				String label = capitalize(desc) + ":"; //$NON-NLS-1$
				TaskAttribute attribute = links.get(key);
				if (attribute == null) {
					attribute = data.getRoot().createAttribute(IJiraConstants.ATTRIBUTE_LINK_PREFIX + key);
					attribute.getMetaData() //
							.setKind(TaskAttribute.KIND_DEFAULT)
							.setLabel(label)
							.setType(JiraFieldType.ISSUELINKS.getTaskType())
							.putValue(IJiraConstants.META_TYPE, JiraFieldType.ISSUELINKS.getKey());
					links.put(key, attribute);
				}
				attribute.addValue(link.getIssueKey());

				if (link.getInwardDescription() != null) {
					attribute = data.getRoot().getMappedAttribute(JiraAttribute.LINKED_IDS.id());
					if (attribute == null) {
						attribute = createAttribute(data, JiraAttribute.LINKED_IDS);
					}
					addAttributeValue(data, JiraAttribute.LINKED_IDS, link.getIssueId());
				}
			}
		}

		setAttributeValue(data, JiraAttribute.CREATION_DATE, JiraUtil.dateToString(jiraIssue.getCreated()));
		setAttributeValue(data, JiraAttribute.SUMMARY, jiraIssue.getSummary());
		setAttributeValue(data, JiraAttribute.DESCRIPTION, jiraIssue.getDescription());
		setAttributeValue(data, JiraAttribute.ISSUE_KEY, jiraIssue.getKey());
		setAttributeValue(data, JiraAttribute.TASK_URL, jiraIssue.getUrl());
		setAttributeValue(data, JiraAttribute.RESOLUTION, //
				jiraIssue.getResolution() == null ? "" : jiraIssue.getResolution().getId()); //$NON-NLS-1$
		setAttributeValue(data, JiraAttribute.MODIFICATION_DATE, JiraUtil.dateToString(jiraIssue.getUpdated()));
		setAttributeValue(data, JiraAttribute.USER_ASSIGNED,
				getPerson(data, client, jiraIssue.getAssignee(), jiraIssue.getAssigneeName()));
		setAttributeValue(data, JiraAttribute.USER_REPORTER,
				getPerson(data, client, jiraIssue.getReporter(), jiraIssue.getReporterName()));
		setAttributeValue(data, JiraAttribute.PROJECT, jiraIssue.getProject().getId());

		if (jiraIssue.getStatus() != null) {
			TaskAttribute attribute = data.getRoot().getAttribute(JiraAttribute.STATUS.id());
			attribute.putOption(jiraIssue.getStatus().getId(), jiraIssue.getStatus().getName());
			attribute.setValue(jiraIssue.getStatus().getId());
		}

		if (jiraIssue.getPriority() != null) {
			setAttributeValue(data, JiraAttribute.PRIORITY, jiraIssue.getPriority().getId());
		} else {
			removeAttribute(data, JiraAttribute.PRIORITY);
		}

		SecurityLevel securityLevel = jiraIssue.getSecurityLevel();
		if (securityLevel != null) {
			TaskAttribute attribute = data.getRoot().getAttribute(JiraAttribute.SECURITY_LEVEL.id());
			if (attribute == null) {
				// the repository configuration may not have information about available security 
				// information for older JIRA versions
				attribute = createAttribute(data, JiraAttribute.SECURITY_LEVEL);
				attribute.getMetaData().setReadOnly(true);
			}
			if (!attribute.getOptions().containsKey(securityLevel.getId())) {
				attribute.putOption(securityLevel.getId(), securityLevel.getName());
			}
			attribute.setValue(securityLevel.getId());
		}

		IssueType issueType = jiraIssue.getType();
		if (issueType != null) {
			TaskAttribute attribute = setAttributeValue(data, JiraAttribute.TYPE, issueType.getId());
			if (issueType.isSubTaskType()) {
				attribute.getMetaData() //
						.setReadOnly(true)
						.putValue(IJiraConstants.META_SUB_TASK_TYPE, Boolean.toString(true));
				attribute.clearOptions();
				attribute.putOption(issueType.getId(), issueType.getName());
			}
		} else {
			removeAttribute(data, JiraAttribute.TYPE);
		}

		// if no time was logged initial estimate and estimate are the same value, only include estimate in this case
		if (jiraIssue.getActual() > 0) {
			setAttributeValue(data, JiraAttribute.INITIAL_ESTIMATE, jiraIssue.getInitialEstimate() + ""); //$NON-NLS-1$
		} else {
			removeAttribute(data, JiraAttribute.INITIAL_ESTIMATE);
		}
		setAttributeValue(data, JiraAttribute.ESTIMATE, jiraIssue.getEstimate() + ""); //$NON-NLS-1$
		setAttributeValue(data, JiraAttribute.ACTUAL, jiraIssue.getActual() + ""); //$NON-NLS-1$

		if (jiraIssue.getDue() != null) {
			setAttributeValue(data, JiraAttribute.DUE_DATE, JiraUtil.dateToString(jiraIssue.getDue()));
		} else if (!jiraIssue.hasDueDate()) {
			removeAttribute(data, JiraAttribute.DUE_DATE);
		}

		if (jiraIssue.getComponents() != null) {
			for (Component component : jiraIssue.getComponents()) {
				addAttributeValue(data, JiraAttribute.COMPONENTS, component.getId());
			}
		}

		if (jiraIssue.getReportedVersions() != null) {
			for (Version version : jiraIssue.getReportedVersions()) {
				addAttributeValue(data, JiraAttribute.AFFECTSVERSIONS, version.getId());
			}
		}

		if (jiraIssue.getFixVersions() != null) {
			for (Version version : jiraIssue.getFixVersions()) {
				addAttributeValue(data, JiraAttribute.FIXVERSIONS, version.getId());
			}
		}

		if (jiraIssue.getEnvironment() != null) {
			setAttributeValue(data, JiraAttribute.ENVIRONMENT, jiraIssue.getEnvironment());
		} else {
			removeAttribute(data, JiraAttribute.ENVIRONMENT);
		}

		if (jiraIssue.getVotes() > 0) {
			setAttributeValue(data, JiraAttribute.VOTES, Integer.toString(jiraIssue.getVotes()));
		} else {
			removeAttribute(data, JiraAttribute.VOTES);
		}

		addAttributeValue(data, JiraAttribute.PROJECT_ROLES, IJiraConstants.NEW_COMMENT_VIEWABLE_BY_ALL);

		addComments(data, jiraIssue, client);
		addAttachments(data, jiraIssue, client);
		addCustomFields(data, jiraIssue);

		addWorklog(data, jiraIssue, client, oldTaskData, forceCache, monitor);

		updateMarkup(data, jiraIssue, client, oldTaskData, forceCache, monitor);

		HashSet<String> editableKeys = getEditableKeys(data, jiraIssue, client, oldTaskData, forceCache, monitor);
		updateProperties(data, editableKeys);
	}

	/**
	 * Stores user in cache if <code>fullName</code> is provided. Otherwise user is retrieved from cache.
	 */
	public static IRepositoryPerson getPerson(TaskData data, JiraClient client, String userId, String fullName) {
		if (userId == null || JiraRepositoryConnector.UNASSIGNED_USER.equals(userId)) {
			userId = ""; //$NON-NLS-1$
		}
		User user;
		if (fullName != null) {
			user = client.getCache().putUser(userId, fullName);
		} else {
			user = client.getCache().getUser(userId);
		}
		IRepositoryPerson person = data.getAttributeMapper().getTaskRepository().createPerson(userId);
		if (user != null) {
			person.setName(user.getFullName());
		}
		return person;
	}

	private void addComments(TaskData data, JiraIssue jiraIssue, JiraClient client) {
		int i = 1;

		// ensure that the comments are in the correct order for display in the task editor
		// fix fpr PLE-953
		List<Comment> comments = new ArrayList<Comment>(Arrays.asList(jiraIssue.getComments()));
		Collections.sort(comments, new CommentDateComparator());

		for (Comment comment : comments) {
			TaskAttribute attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_COMMENT + i);
			TaskCommentMapper taskComment = TaskCommentMapper.createFrom(attribute);
			taskComment.setAuthor(getPerson(data, client, comment.getAuthor(), null));
			taskComment.setNumber(i);
			String commentText = comment.getComment();
			if (comment.isMarkupDetected()) {
				commentText = stripTags(commentText);
			}
			taskComment.setText(commentText);
			taskComment.setCreationDate(comment.getCreated());
			// TODO taskComment.setUrl()
			taskComment.applyTo(attribute);

			// add level attribute
			if (comment.getRoleLevel() != null) {
				TaskAttribute level = attribute.createAttribute(IJiraConstants.COMMENT_SECURITY_LEVEL);
				level.setValue(comment.getRoleLevel());
			}
			i++;
		}
	}

	private void addAttachments(TaskData data, JiraIssue jiraIssue, JiraClient client) {
		int i = 1;
		for (Attachment attachment : jiraIssue.getAttachments()) {
			TaskAttribute attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_ATTACHMENT + i);
			TaskAttachmentMapper taskAttachment = TaskAttachmentMapper.createFrom(attribute);
			taskAttachment.setAttachmentId(attachment.getId());
			taskAttachment.setAuthor(getPerson(data, client, attachment.getAuthor(), null));
			taskAttachment.setFileName(attachment.getName());
			if (CONTEXT_ATTACHEMENT_FILENAME.equals(attachment.getName())) {
				taskAttachment.setDescription(CONTEXT_ATTACHMENT_DESCRIPTION);
			} else if (CONTEXT_ATTACHEMENT_FILENAME_LEGACY.equals(attachment.getName())) {
				taskAttachment.setDescription(CONTEXT_ATTACHMENT_DESCRIPTION_LEGACY);
			} else {
				taskAttachment.setDescription(attachment.getName());
			}
			taskAttachment.setLength(attachment.getSize());
			taskAttachment.setCreationDate(attachment.getCreated());
			taskAttachment.setUrl(client.getBaseUrl() + "/secure/attachment/" + attachment.getId() + "/" //$NON-NLS-1$ //$NON-NLS-2$
					+ attachment.getName());
			taskAttachment.applyTo(attribute);
			i++;
		}
	}

	private void addCustomFields(TaskData data, JiraIssue jiraIssue) {
		for (CustomField field : jiraIssue.getCustomFields()) {
			String mappedKey = mapCommonAttributeKey(field.getId());
			String name = field.getName() + ":"; //$NON-NLS-1$
			String kind = JiraAttribute.valueById(mappedKey).getKind();
			String type = field.getKey();
			String taskType = JiraFieldType.fromKey(type).getTaskType();
			if (taskType == null && type != null && type.startsWith(IJiraConstants.JIRA_TOOLKIT_PREFIX)) {
				taskType = TaskAttribute.TYPE_SHORT_TEXT;

			}

			TaskAttribute attribute = data.getRoot().createAttribute(mappedKey);
			attribute.getMetaData().defaults() //
					.setKind(kind)
					.setLabel(name)
					.setReadOnly(field.isReadOnly())
					.setType(taskType)
					.putValue(IJiraConstants.META_TYPE, type);
			for (String value : field.getValues()) {
				attribute.addValue(value);
			}
		}
	}

	private HashSet<String> getEditableKeys(TaskData data, JiraIssue jiraIssue, JiraClient client,
			TaskData oldTaskData, boolean forceCache, IProgressMonitor monitor) throws JiraException {
		HashSet<String> editableKeys = new HashSet<String>();
		if (!JiraRepositoryConnector.isClosed(jiraIssue)) {
			if (useCachedInformation(jiraIssue, oldTaskData, forceCache)) {
				if (oldTaskData == null) {
					// caching forced but no information available
					data.setPartial(true);
					return editableKeys;
				}

				// avoid server round-trips
				for (TaskAttribute attribute : oldTaskData.getRoot().getAttributes().values()) {
					if (!attribute.getMetaData().isReadOnly()) {
						editableKeys.add(attribute.getId());
					}
				}

				TaskAttribute attribute = oldTaskData.getRoot().getAttribute(IJiraConstants.ATTRIBUTE_READ_ONLY);
				if (attribute != null) {
					data.getRoot().deepAddCopy(attribute);
				}
			} else {
				try {
					IssueField[] editableAttributes = client.getEditableAttributes(jiraIssue.getKey(), monitor);
					if (editableAttributes != null) {
						for (IssueField field : editableAttributes) {
							editableKeys.add(mapCommonAttributeKey(field.getId()));
						}
					}
				} catch (JiraInsufficientPermissionException e) {
					trace(e);
					// flag as read-only to avoid calling getEditableAttributes() on each sync
					data.getRoot().createAttribute(IJiraConstants.ATTRIBUTE_READ_ONLY);
				}
			}
		}
		return editableKeys;
	}

	private void updateProperties(TaskData data, HashSet<String> editableKeys) {
		for (TaskAttribute attribute : data.getRoot().getAttributes().values()) {
			TaskAttributeMetaData properties = attribute.getMetaData();
			boolean editable = editableKeys.contains(attribute.getId().toLowerCase());
			if (editable && (attribute.getId().startsWith(IJiraConstants.ATTRIBUTE_CUSTOM_PREFIX) //
					|| !JiraAttribute.valueById(attribute.getId()).isHidden())) {
				properties.setKind(TaskAttribute.KIND_DEFAULT);
			}

			if (TaskAttribute.COMMENT_NEW.equals(attribute.getId())
					|| TaskAttribute.RESOLUTION.equals(attribute.getId())
					|| TaskAttribute.USER_ASSIGNED.equals(attribute.getId())
					|| JiraAttribute.PROJECT_ROLES.id().equals(attribute.getId())) {
				properties.setReadOnly(false);
			} else {
				// make attributes read-only if can't find editing options
				String key = properties.getValue(IJiraConstants.META_TYPE);
				Map<String, String> options = attribute.getOptions();
				if (JiraFieldType.SELECT.getKey().equals(key) && (options.isEmpty() || properties.isReadOnly())) {
					properties.setReadOnly(true);
				} else if (JiraFieldType.MULTISELECT.getKey().equals(key) && options.isEmpty()) {
					properties.setReadOnly(true);
				} else {
					properties.setReadOnly(!editable);
				}
			}
		}
	}

	private void addAttributeValue(TaskData data, JiraAttribute key, String value) {
		data.getRoot().getAttribute(key.id()).addValue(value);
	}

	private TaskAttribute setAttributeValue(TaskData data, JiraAttribute key, String value) {
		TaskAttribute attribute = data.getRoot().getAttribute(key.id());
		// XXX a null value might indicate an invalid issue
		if (value != null) {
			attribute.setValue(value);
		}
		return attribute;
	}

	private TaskAttribute setAttributeValue(TaskData data, JiraAttribute key, IRepositoryPerson person) {
		TaskAttribute attribute = data.getRoot().getAttribute(key.id());
		data.getAttributeMapper().setRepositoryPerson(attribute, person);
		return attribute;
	}

	private boolean useCachedInformation(JiraIssue issue, TaskData oldTaskData, boolean forceCache) {
		if (forceCache) {
			return true;
		}
		if (oldTaskData != null && issue.getStatus() != null) {
			TaskAttribute attribute = oldTaskData.getRoot().getMappedAttribute(TaskAttribute.STATUS);
			if (attribute != null) {
				return attribute.getValue().equals(issue.getStatus().getId());
			}
		}
		return false;
	}

//	private void removeAttributes(TaskData data, String keyPrefix) {
//		List<TaskAttribute> attributes = new ArrayList<TaskAttribute>(data.getRoot().getAttributes().values());
//		for (TaskAttribute attribute : attributes) {
//			if (attribute.getId().startsWith(keyPrefix)) {
//				removeAttribute(data, attribute.getId());
//			}
//		}
//	}

	private void removeAttribute(TaskData data, JiraAttribute key) {
		data.getRoot().removeAttribute(key.id());
	}

	/**
	 * Removes attribute values without removing attribute to preserve order of attributes
	 */
//	private void removeAttributeValues(TaskData data, String attributeId) {
//		data.getRoot().getAttribute(attributeId).clearValues();
//	}
	private String capitalize(String s) {
		if (s.length() > 1) {
			char c = s.charAt(0);
			char uc = Character.toUpperCase(c);
			if (uc != c) {
				return uc + s.substring(1);
			}
		}
		return s;
	}

	public static String stripTags(String text) {
		if (text == null || text.length() == 0) {
			return ""; //$NON-NLS-1$
		}
		StringReader stringReader = new StringReader(text);
		HTML2TextReader html2TextReader = new HTML2TextReader(stringReader);
		try {
			char[] chars = new char[text.length()];
			int len = html2TextReader.read(chars, 0, text.length());
			if (len == -1) {
				return ""; //$NON-NLS-1$
			}
			return new String(chars, 0, len);
		} catch (IOException e) {
			return text;
		}
	}

	/**
	 * Replaces the values in fields that are suspected to contain rendered markup with the source values retrieved
	 * through SOAP.
	 * 
	 * @param forceCache
	 */
	private void updateMarkup(TaskData data, JiraIssue jiraIssue, JiraClient client, TaskData oldTaskData,
			boolean forceCache, IProgressMonitor monitor) throws JiraException {
		if (!jiraIssue.isMarkupDetected()) {
			return;
		}
		if (useCachedData(jiraIssue, oldTaskData, forceCache)) {
			if (oldTaskData == null) {
				// caching forced but no information available
				data.setPartial(true);
				return;
			}

			// use cached information
			if (data.getRoot().getAttribute(TaskAttribute.DESCRIPTION) != null) {
				setAttributeValue(data, JiraAttribute.DESCRIPTION,
						getAttributeValue(oldTaskData, JiraAttribute.DESCRIPTION));
			}
			if (data.getRoot().getAttribute(IJiraConstants.ATTRIBUTE_ENVIRONMENT) != null) {
				setAttributeValue(data, JiraAttribute.ENVIRONMENT,
						getAttributeValue(oldTaskData, JiraAttribute.ENVIRONMENT));
			}
			for (CustomField field : jiraIssue.getCustomFields()) {
				if (field.isMarkupDetected()) {
					String attributeId = mapCommonAttributeKey(field.getId());
					TaskAttribute oldAttribute = oldTaskData.getRoot().getAttribute(attributeId);
					if (oldAttribute != null) {
						TaskAttribute attribute = data.getRoot().getAttribute(attributeId);
						attribute.setValues(oldAttribute.getValues());
					}
				}
			}
			int i = 1;
			for (Comment comment : jiraIssue.getComments()) {
				if (comment.isMarkupDetected()) {
					String attributeId = TaskAttribute.PREFIX_COMMENT + i;
					TaskAttribute oldAttribute = oldTaskData.getRoot().getAttribute(attributeId);
					if (oldAttribute != null) {
						TaskCommentMapper oldComment = TaskCommentMapper.createFrom(oldAttribute);
						TaskAttribute attribute = data.getRoot().getAttribute(attributeId);
						TaskCommentMapper newComment = TaskCommentMapper.createFrom(attribute);
						newComment.setText(oldComment.getText());
						newComment.applyTo(attribute);
					}
				}
				i++;
			}
			return;
		}

		// consider preserving HTML 
		try {
			RemoteIssue remoteIssue = client.getSoapClient().getIssueByKey(jiraIssue.getKey(), monitor);
			if (data.getRoot().getAttribute(TaskAttribute.DESCRIPTION) != null) {
				if (remoteIssue.getDescription() == null) {
					setAttributeValue(data, JiraAttribute.DESCRIPTION, ""); //$NON-NLS-1$
				} else {
					setAttributeValue(data, JiraAttribute.DESCRIPTION, remoteIssue.getDescription());
				}
			}
			if (data.getRoot().getAttribute(IJiraConstants.ATTRIBUTE_ENVIRONMENT) != null) {
				if (remoteIssue.getEnvironment() == null) {
					setAttributeValue(data, JiraAttribute.ENVIRONMENT, ""); //$NON-NLS-1$
				} else {
					setAttributeValue(data, JiraAttribute.ENVIRONMENT, remoteIssue.getEnvironment());
				}
			}
			RemoteCustomFieldValue[] fields = remoteIssue.getCustomFieldValues();
			for (CustomField field : jiraIssue.getCustomFields()) {
				if (field.isMarkupDetected()) {
					innerLoop: for (RemoteCustomFieldValue remoteField : fields) {
						if (field.getId().equals(remoteField.getCustomfieldId())) {
							String attributeId = mapCommonAttributeKey(field.getId());
							TaskAttribute attribute = data.getRoot().getAttribute(attributeId);
							if (attribute != null) {
								attribute.setValues(Arrays.asList(remoteField.getValues()));
							}
							break innerLoop;
						}
					}
				}
			}
		} catch (JiraInsufficientPermissionException e) {
			// ignore
			trace(e);
		}
		boolean retrieveComments = false;
		for (Comment comment : jiraIssue.getComments()) {
			if (comment.isMarkupDetected()) {
				retrieveComments = true;
			}
		}
		if (retrieveComments) {
			try {
				Comment[] remoteComments = client.getSoapClient().getComments(jiraIssue.getKey(), monitor);
				int i = 1;
				for (Comment remoteComment : remoteComments) {
					String attributeId = TaskAttribute.PREFIX_COMMENT + i;
					TaskAttribute attribute = data.getRoot().getAttribute(attributeId);
					if (attribute != null) {
						TaskCommentMapper comment = TaskCommentMapper.createFrom(attribute);
						comment.setText(remoteComment.getComment());
						comment.applyTo(attribute);
					}
					i++;
				}
			} catch (JiraInsufficientPermissionException e) {
				// ignore
				trace(e);
			} catch (JiraServiceUnavailableException e) {
				if ("Invalid element in com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteComment - level".equals(e.getMessage())) { //$NON-NLS-1$
					// XXX ignore, see bug 260614
					trace(e);
				} else {
					throw e;
				}
			}
		}
	}

	private boolean useCachedData(JiraIssue jiraIssue, TaskData oldTaskData, boolean forceCache) {
		if (forceCache) {
			return true;
		}
		if (jiraIssue.getUpdated() != null && oldTaskData != null) {
			if (jiraIssue.getUpdated().equals(getDateValue(oldTaskData, JiraAttribute.MODIFICATION_DATE))) {
				return true;
			}
		}
		return false;
	}

	private void addWorklog(TaskData data, JiraIssue jiraIssue, JiraClient client, TaskData oldTaskData,
			boolean forceCache, IProgressMonitor monitor) throws JiraException {
		if (useCachedData(jiraIssue, oldTaskData, forceCache)) {
			if (useCachedInformation(jiraIssue, oldTaskData, forceCache)) {
				if (oldTaskData == null) {
					// caching forced but no information available
					data.setPartial(true);
					return;
				}
				List<TaskAttribute> attributes = oldTaskData.getAttributeMapper().getAttributesByType(oldTaskData,
						WorkLogConverter.TYPE_WORKLOG);
				for (TaskAttribute taskAttribute : attributes) {
					data.getRoot().deepAddCopy(taskAttribute);
				}
				TaskAttribute attribute = oldTaskData.getRoot().getAttribute(
						IJiraConstants.ATTRIBUTE_WORKLOG_NOT_SUPPORTED);
				if (attribute != null) {
					data.getRoot().deepAddCopy(attribute);
				}
				return;
			}
		}
		try {
			JiraWorkLog[] remoteWorklogs = client.getWorklogs(jiraIssue.getKey(), monitor);
			if (remoteWorklogs != null) {
				int i = 1;
				for (JiraWorkLog remoteWorklog : remoteWorklogs) {
					String attributeId = WorkLogConverter.PREFIX_WORKLOG + "-" + i; //$NON-NLS-1$
					TaskAttribute attribute = data.getRoot().createAttribute(attributeId);
					attribute.getMetaData().setType(WorkLogConverter.TYPE_WORKLOG);
					new WorkLogConverter().applyTo(remoteWorklog, attribute);
					i++;
				}
			} else {
				data.getRoot().createAttribute(IJiraConstants.ATTRIBUTE_WORKLOG_NOT_SUPPORTED);
			}
		} catch (JiraInsufficientPermissionException e) {
			// ignore
			trace(e);
			data.getRoot().createAttribute(IJiraConstants.ATTRIBUTE_WORKLOG_NOT_SUPPORTED);
		}
	}

	public void addOperations(TaskData data, JiraIssue issue, JiraClient client, TaskData oldTaskData,
			boolean forceCache, IProgressMonitor monitor) throws JiraException {
		// avoid server round-trips
		if (useCachedInformation(issue, oldTaskData, forceCache)) {
			if (oldTaskData == null) {
				// caching forced but no information available
				data.setPartial(true);
				return;
			}

			List<TaskAttribute> attributes = oldTaskData.getAttributeMapper().getAttributesByType(oldTaskData,
					TaskAttribute.TYPE_OPERATION);
			for (TaskAttribute taskAttribute : attributes) {
				data.getRoot().deepAddCopy(taskAttribute);
			}
			return;
		}

		JiraStatus status = issue.getStatus();
		String label;
		if (status != null) {
			label = NLS.bind(Messages.JiraTaskDataHandler_Leave_as_X, status.getName());
		} else {
			label = Messages.JiraTaskDataHandler_Leave;
		}
		TaskAttribute attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_OPERATION + LEAVE_OPERATION);
		TaskOperation.applyTo(attribute, LEAVE_OPERATION, label);
		// set as default
		attribute = data.getRoot().createAttribute(TaskAttribute.OPERATION);
		TaskOperation.applyTo(attribute, LEAVE_OPERATION, label);

		// TODO need more accurate status matching
//		if (!JiraRepositoryConnector.isCompleted(data)) {
//			attribute = operationContainer.createAttribute(REASSIGN_OPERATION);
//			operation = TaskOperation.createFrom(attribute);
//			operation.setLabel("Reassign to");
//			operation.applyTo(attribute);
//
//			String attributeId = REASSIGN_OPERATION + "::" + JiraAttribute.USER_ASSIGNED.getParamName();
//			TaskAttribute associatedAttribute = createAttribute(data, attributeId);
//			associatedAttribute.setValue(client.getUserName());
//			attribute.putMetaDataValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID, attributeId);
//		}

		JiraAction[] availableActions = client.getAvailableActions(issue.getKey(), monitor);
		if (availableActions != null) {
			for (JiraAction action : availableActions) {
				attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_OPERATION + action.getId());
				TaskOperation.applyTo(attribute, action.getId(), action.getName());

				String[] fields = client.getActionFields(issue.getKey(), action.getId(), monitor);
				for (String field : fields) {
					if (TaskAttribute.RESOLUTION.equals(mapCommonAttributeKey(field))) {
						attribute.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID,
								TaskAttribute.RESOLUTION);
					}
					// TODO handle other action fields
				}
			}
		}
	}

	@Override
	public RepositoryResponse postTaskData(TaskRepository repository, TaskData taskData,
			Set<TaskAttribute> changedAttributes, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(Messages.JiraTaskDataHandler_Sending_task, IProgressMonitor.UNKNOWN);
			JiraClient client = clientFactory.getJiraClient(repository);
			if (client == null) {
				throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR, JiraCorePlugin.ID_PLUGIN,
						IStatus.ERROR, "Unable to create Jira client", null)); //$NON-NLS-1$
			}
			try {
				if (!client.getCache().hasDetails()) {
					client.getCache().refreshDetails(monitor);
				}

				JiraIssue issue = buildJiraIssue(taskData);
				if (taskData.isNew()) {
					if (issue.getType().isSubTaskType() && issue.getParentId() != null) {
						issue = client.createSubTask(issue, monitor);
					} else {
						issue = client.createIssue(issue, monitor);
					}
					if (issue == null) {
						throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
								JiraCorePlugin.ID_PLUGIN, IStatus.OK, "Could not create issue.", null)); //$NON-NLS-1$
					}

					if (taskData.getRoot().getAttribute(IJiraConstants.ATTRIBUTE_WORKLOG_NOT_SUPPORTED) == null) {
						postWorkLog(repository, client, taskData, issue, monitor);
					}

					// this is severely broken: should return id instead
					//return issue.getKey();
					return new RepositoryResponse(ResponseKind.TASK_CREATED, issue.getId());
				} else {
					String operationId = getOperationId(taskData);
					String newComment = getNewComment(taskData);

					Set<String> changeIds = new HashSet<String>();
					if (changedAttributes != null) {
						for (TaskAttribute ta : changedAttributes) {
							changeIds.add(ta.getId());
						}
					}

					// check if the visibility of the comment needs to be set 
					Comment soapComment = null;
					TaskAttribute commentVisibilityAttribute = taskData.getRoot().getMappedAttribute(
							JiraAttribute.PROJECT_ROLES.id());
					if (commentVisibilityAttribute != null) {
						String commentVisibility = commentVisibilityAttribute.getValue();
						if (!IJiraConstants.NEW_COMMENT_VIEWABLE_BY_ALL.equals(commentVisibility)) {
							// not relevant for later processing
							changeIds.remove(JiraAttribute.PROJECT_ROLES.id());

							if (newComment != null && newComment.length() > 0) {
								soapComment = new Comment();
								soapComment.setComment(newComment);
								soapComment.setRoleLevel(commentVisibility);

								newComment = null;
							}
						}
					}

					boolean handled = false;
					boolean advWorkflowHandled = false;

					// if only reassigning do not do the workflow
					if (!handled && changeIds.contains(TaskAttribute.USER_ASSIGNED)) {
						Set<String> anythingElse = new HashSet<String>(changeIds);
						anythingElse.removeAll(Arrays.asList(TaskAttribute.USER_ASSIGNED, TaskAttribute.COMMENT_NEW));
						if (anythingElse.size() == 0) {
							// no more changes, so that's a re-assign operation (we can't count on operationId == REASSIGN_OPERATION)
							client.assignIssueTo(issue, JiraClient.ASSIGNEE_USER, getAssignee(taskData), newComment,
									monitor);
							handled = true;
						}
					}

					// if only adv workflow do not do the standard workflow
					if (!handled && changeIds.contains(TaskAttribute.OPERATION)) {
						Set<String> anythingElse = new HashSet<String>(changeIds);
						anythingElse.removeAll(Arrays.asList(TaskAttribute.OPERATION, TaskAttribute.COMMENT_NEW));
						if (anythingElse.size() == 0) {
							// no more changes, so that's a adv workflow operation
							client.advanceIssueWorkflow(issue, operationId, newComment, monitor);
							handled = true;
							advWorkflowHandled = true;
						}
					}

					// stop progress must be run before issue is updated because assignee can be changed on update and this will cause stop progress to fail
					if (!handled && STOP_PROGRESS_OPERATION.equals(operationId)) {
						client.advanceIssueWorkflow(issue, operationId, null, monitor); //comment will be updated in the normal workflow, so don't post it here
						advWorkflowHandled = true;
					}

					// if only comment was modified do not do the workflow
					if (!handled //
							&& !JiraRepositoryConnector.isClosed(issue)
							&& taskData.getRoot().getMappedAttribute(IJiraConstants.ATTRIBUTE_READ_ONLY) == null
							&& !changeIds.equals(Collections.singleton(TaskAttribute.COMMENT_NEW))
							&& !(STOP_PROGRESS_OPERATION.equals(operationId) && changeIds.equals(Collections.singleton(TaskAttribute.OPERATION)))) {
						client.updateIssue(issue, newComment, monitor);
						handled = true;
					}

					// at last try to at least post the comment (if everything else failed)
					if (!handled && newComment != null && newComment.length() > 0) {
						client.addCommentToIssue(issue.getKey(), newComment, monitor);
						handled = true;
					} else if (soapComment != null) {
						client.addCommentToIssue(issue.getKey(), soapComment, monitor);
						handled = true;
					}

					postWorkLog(repository, client, taskData, issue, monitor);

					// and do advanced workflow if necessary
					if (!advWorkflowHandled && !LEAVE_OPERATION.equals(operationId)
							&& !REASSIGN_OPERATION.equals(operationId) && !STOP_PROGRESS_OPERATION.equals(operationId)) {
						client.advanceIssueWorkflow(issue, operationId, null, monitor); //comment gets updated in the normal workflow already, so don"t post it a second time
					}
					return new RepositoryResponse(ResponseKind.TASK_UPDATED, issue.getId());
				}
			} catch (JiraException e) {
				IStatus status = JiraCorePlugin.toStatus(repository, e);
				trace(status);
				throw new CoreException(status);
			}
		} finally {
			monitor.done();
		}
	}

	private void postWorkLog(TaskRepository repository, JiraClient client, TaskData taskData, JiraIssue issue,
			IProgressMonitor monitor) throws JiraException {
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(WorkLogConverter.ATTRIBUTE_WORKLOG_NEW);
		if (attribute != null) {
			TaskAttribute submitFlagAttribute = attribute.getAttribute(WorkLogConverter.ATTRIBUTE_WORKLOG_NEW_SUBMIT_FLAG);
			//if flag is set and true, submit
			if (submitFlagAttribute != null && submitFlagAttribute.getValue().equals(String.valueOf(true))) {
				JiraWorkLog log = new WorkLogConverter().createFrom(attribute);
				client.addWorkLog(issue.getKey(), log, monitor);
			}
		}
	}

	private String getNewComment(TaskData taskData) {
		String newComment = ""; //$NON-NLS-1$
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
		if (attribute != null) {
			newComment = taskData.getAttributeMapper().getValue(attribute);
		}
		return newComment;
	}

	private String getAssignee(TaskData taskData) {
		String asignee = ""; //$NON-NLS-1$
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(TaskAttribute.USER_ASSIGNED);
		if (attribute != null) {
			asignee = taskData.getAttributeMapper().getValue(attribute);
		}
		return asignee;
	}

	private String getOperationId(TaskData taskData) {
		String operationId = ""; //$NON-NLS-1$
		TaskAttribute attribute = taskData.getRoot().getMappedAttribute(TaskAttribute.OPERATION);
		if (attribute != null) {
			operationId = taskData.getAttributeMapper().getValue(attribute);
		}
		if (operationId.length() == 0) {
			operationId = LEAVE_OPERATION;
		}
		return operationId;
	}

	@Override
	public boolean initializeTaskData(TaskRepository repository, TaskData data, ITaskMapping initializationData,
			IProgressMonitor monitor) throws CoreException {
		if (initializationData == null) {
			return false;
		}
		String product = initializationData.getProduct();
		if (product == null) {
			return false;
		}
		JiraClient client = clientFactory.getJiraClient(repository);
		if (!client.getCache().hasDetails()) {
			try {
				client.getCache().refreshDetails(monitor);
			} catch (JiraException ex) {
				IStatus status = JiraCorePlugin.toStatus(repository, ex);
				trace(status);
				throw new CoreException(status);
			}
		}
		Project project = getProject(client, product);
		if (project == null) {
			return false;
		}
		if (!project.hasDetails()) {
			try {
				client.getCache().refreshProjectDetails(project.getId(), monitor);
			} catch (JiraException e) {
				final IStatus status = JiraCorePlugin.toStatus(repository, e);
				trace(status);
				throw new CoreException(status);
			}
		}

		try {
			initializeTaskData(repository, data, client, project, monitor);
		} catch (JiraException e) {
			final IStatus status = JiraCorePlugin.toStatus(repository, e);
			trace(status);
			throw new CoreException(status);
		}
		return true;
	}

	private Project getProject(JiraClient client, String product) {
		Project[] projects = client.getCache().getProjects();
		for (Project project : projects) {
			if (product.equals(project.getName()) || product.equals(project.getKey())) {
				return project;
			}
		}
		return null;
	}

	@Override
	public boolean initializeSubTaskData(TaskRepository repository, TaskData taskData, TaskData parentTaskData,
			IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(Messages.JiraTaskDataHandler_Creating_subtask, IProgressMonitor.UNKNOWN);

			JiraClient client = JiraClientFactory.getDefault().getJiraClient(repository);
			if (!client.getCache().hasDetails()) {
				client.getCache().refreshDetails(new SubProgressMonitor(monitor, 1));
			}

			TaskAttribute projectAttribute = parentTaskData.getRoot().getAttribute(TaskAttribute.PRODUCT);
			if (projectAttribute == null) {
				throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR, JiraCorePlugin.ID_PLUGIN,
						IStatus.OK, "The parent task does not have a valid project.", null)); //$NON-NLS-1$
			}

			Project project = client.getCache().getProjectById(projectAttribute.getValue());
			initializeTaskData(repository, taskData, client, project, monitor);

			new JiraTaskMapper(taskData).merge(new JiraTaskMapper(parentTaskData));
			taskData.getRoot().getAttribute(JiraAttribute.PROJECT.id()).setValue(project.getId());
			taskData.getRoot().getAttribute(JiraAttribute.DESCRIPTION.id()).setValue(""); //$NON-NLS-1$
			taskData.getRoot().getAttribute(JiraAttribute.SUMMARY.id()).setValue(""); //$NON-NLS-1$

			// set subtask type
			TaskAttribute typeAttribute = taskData.getRoot().getAttribute(JiraAttribute.TYPE.id());
			typeAttribute.clearOptions();
			IssueType[] jiraIssueTypes = client.getCache().getIssueTypes();
			for (IssueType type : jiraIssueTypes) {
				if (type.isSubTaskType()) {
					typeAttribute.putOption(type.getId(), type.getName());
				}
			}
			Map<String, String> options = typeAttribute.getOptions();
			if (options.size() == 0) {
				throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR, JiraCorePlugin.ID_PLUGIN,
						IStatus.OK, "The repository does not support subtasks.", null)); //$NON-NLS-1$
			} else if (options.size() == 1) {
				typeAttribute.getMetaData().setReadOnly(true);
			}
			typeAttribute.setValue(options.keySet().iterator().next());
			typeAttribute.getMetaData().putValue(IJiraConstants.META_SUB_TASK_TYPE, Boolean.TRUE.toString());

			// set parent id
			TaskAttribute attribute = taskData.getRoot().getAttribute(JiraAttribute.PARENT_ID.id());
			attribute.setValue(parentTaskData.getTaskId());
			attribute = taskData.getRoot().getAttribute(JiraAttribute.PARENT_KEY.id());
			attribute.setValue(parentTaskData.getRoot().getAttribute(JiraAttribute.ISSUE_KEY.id()).getValue());

			return true;
		} catch (JiraException e) {
			IStatus status = JiraCorePlugin.toStatus(repository, e);
			trace(status);
			throw new CoreException(status);
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean canInitializeSubTaskData(TaskRepository taskRepository, ITask task) {
		// for backwards compatibility with earlier versions that did not set the subtask flag in 
		// JiraRepositoryConnector.updateTaskFromTaskData() return true as a fall-back
		String value = task.getAttribute(IJiraConstants.META_SUB_TASK_TYPE);
		return (value == null) ? true : !Boolean.parseBoolean(value);
	}

	public static JiraIssue buildJiraIssue(TaskData taskData) {
		JiraIssue issue = new JiraIssue();
		issue.setId(taskData.getTaskId());
		issue.setKey(getAttributeValue(taskData, JiraAttribute.ISSUE_KEY));
		issue.setSummary(getAttributeValue(taskData, JiraAttribute.SUMMARY));
		issue.setDescription(getAttributeValue(taskData, JiraAttribute.DESCRIPTION));

		// TODO sync due date between jira and local planning
		issue.setDue(getDateValue(taskData, JiraAttribute.DUE_DATE));

		String parentId = getAttributeValue(taskData, JiraAttribute.PARENT_ID);
		if (parentId != null) {
			issue.setParentId(parentId);
		}

		String securityLevelId = getAttributeValue(taskData, JiraAttribute.SECURITY_LEVEL);
		if (securityLevelId != null) {
			issue.setSecurityLevel(new SecurityLevel(securityLevelId));
		}

		String estimate = getAttributeValue(taskData, JiraAttribute.ESTIMATE);
		if (estimate != null) {
			try {
				issue.setEstimate(Long.parseLong(estimate));
			} catch (NumberFormatException e) {
			}
		}

		estimate = getAttributeValue(taskData, JiraAttribute.INITIAL_ESTIMATE);
		if (estimate != null) {
			try {
				issue.setInitialEstimate(Long.parseLong(estimate));
			} catch (NumberFormatException e) {
			}
		}

		issue.setProject(new Project(getAttributeValue(taskData, JiraAttribute.PROJECT)));

		TaskAttribute typeAttribute = getAttribute(taskData, JiraAttribute.TYPE);
		boolean subTaskType = (typeAttribute != null) ? hasSubTaskType(typeAttribute) : false;
		IssueType issueType = new IssueType(getAttributeValue(taskData, JiraAttribute.TYPE), subTaskType);
		issue.setType(issueType);

		issue.setStatus(new JiraStatus(getAttributeValue(taskData, JiraAttribute.STATUS)));

		TaskAttribute componentsAttribute = taskData.getRoot().getMappedAttribute(IJiraConstants.ATTRIBUTE_COMPONENTS);
		if (componentsAttribute != null) {
			ArrayList<Component> components = new ArrayList<Component>();
			for (String value : componentsAttribute.getValues()) {
				components.add(new Component(value));
			}
			issue.setComponents(components.toArray(new Component[components.size()]));
		}

		TaskAttribute fixVersionAttr = taskData.getRoot().getMappedAttribute(IJiraConstants.ATTRIBUTE_FIXVERSIONS);
		if (fixVersionAttr != null) {
			ArrayList<Version> fixVersions = new ArrayList<Version>();
			for (String value : fixVersionAttr.getValues()) {
				fixVersions.add(new Version(value));
			}
			issue.setFixVersions(fixVersions.toArray(new Version[fixVersions.size()]));
		}

		TaskAttribute affectsVersionAttr = taskData.getRoot().getMappedAttribute(
				IJiraConstants.ATTRIBUTE_AFFECTSVERSIONS);
		if (affectsVersionAttr != null) {
			ArrayList<Version> affectsVersions = new ArrayList<Version>();
			for (String value : affectsVersionAttr.getValues()) {
				affectsVersions.add(new Version(value));
			}
			issue.setReportedVersions(affectsVersions.toArray(new Version[affectsVersions.size()]));
		}

		issue.setReporter(getAttributeValue(taskData, JiraAttribute.USER_REPORTER));

		String assignee = getAttributeValue(taskData, JiraAttribute.USER_ASSIGNED);
		issue.setAssignee(JiraRepositoryConnector.getAssigneeFromAttribute(assignee));

		issue.setEnvironment(getAttributeValue(taskData, JiraAttribute.ENVIRONMENT));
		String priorityId = getAttributeValue(taskData, JiraAttribute.PRIORITY);
		if (priorityId != null) {
			issue.setPriority(new Priority(priorityId));
		}

		ArrayList<CustomField> customFields = new ArrayList<CustomField>();
		for (TaskAttribute attribute : taskData.getRoot().getAttributes().values()) {
			if (attribute.getId().startsWith(IJiraConstants.ATTRIBUTE_CUSTOM_PREFIX)) {
				String id = attribute.getId().substring(IJiraConstants.ATTRIBUTE_CUSTOM_PREFIX.length());
				String type = attribute.getMetaData().getValue(IJiraConstants.META_TYPE);
				CustomField field = new CustomField(id, type, "", attribute.getValues()); //$NON-NLS-1$
				customFields.add(field);
			}
		}
		issue.setCustomFields(customFields.toArray(new CustomField[customFields.size()]));

		String resolutionId = getAttributeValue(taskData, JiraAttribute.RESOLUTION);
		if (resolutionId != null) {
			issue.setResolution(new Resolution(resolutionId));
		}

		return issue;
	}

	public static boolean hasSubTaskType(TaskAttribute typeAttribute) {
		return Boolean.parseBoolean(typeAttribute.getMetaData().getValue(IJiraConstants.META_SUB_TASK_TYPE));
	}

	private static TaskAttribute getAttribute(TaskData taskData, JiraAttribute key) {
		return taskData.getRoot().getAttribute(key.id());
	}

	private static String getAttributeValue(TaskData taskData, JiraAttribute key) {
		TaskAttribute attribute = taskData.getRoot().getAttribute(key.id());
		return (attribute != null) ? attribute.getValue() : null;
	}

	private static Date getDateValue(TaskData data, JiraAttribute key) {
		TaskAttribute attribute = data.getRoot().getAttribute(key.id());
		return (attribute != null) ? data.getAttributeMapper().getDateValue(attribute) : null;
	}

	private static void trace(IStatus status) {
		if (TRACE_ENABLED) {
			StatusHandler.log(status);
		}
	}

	private static void trace(Exception e) {
		if (TRACE_ENABLED) {
			StatusHandler.log(new Status(IStatus.ERROR, JiraCorePlugin.ID_PLUGIN,
					"Error receiving infromation from JIRA", e)); //$NON-NLS-1$
		}
	}

	@Override
	public TaskAttributeMapper getAttributeMapper(TaskRepository taskRepository) {
		JiraClient client = clientFactory.getJiraClient(taskRepository);
		return new JiraAttributeMapper(taskRepository, client);
	}

	@Override
	public void migrateTaskData(TaskRepository taskRepository, TaskData taskData) {
		String taskDataVersion = taskData.getVersion();
		JiraVersion version = new JiraVersion(taskDataVersion != null ? taskDataVersion : "0.0"); //$NON-NLS-1$
		// 1.0: the value was stored in the attribute rather than the key
		if (version.isSmallerOrEquals(TASK_DATA_VERSION_1_0)) {
			for (TaskAttribute attribute : taskData.getRoot().getAttributes().values()) {
				if (TaskAttribute.PRODUCT.equals(attribute.getId())) {
					String projectName = attribute.getValue();
					Map<String, String> options = taskData.getAttributeMapper().getOptions(attribute);
					for (String key : options.keySet()) {
						String value = options.get(key);
						if (projectName.equals(value)) {
							attribute.setValue(key);
						}
						attribute.putOption(key, value);
					}
				} else if (TaskAttribute.USER_ASSIGNED.equals(attribute.getId())) {
					attribute.getMetaData().setReadOnly(false);
				} else {
					JiraFieldType type = JiraFieldType.fromKey(attribute.getMetaData().getValue(
							IJiraConstants.META_TYPE));
					if ((JiraFieldType.SELECT == type || JiraFieldType.MULTISELECT == type)
							&& !attribute.getOptions().isEmpty()) {
						// convert option values to keys: version 1.0 stored value whereas 2.0 stores keys 
						Set<String> values = new HashSet<String>(attribute.getValues());
						attribute.clearValues();
						Map<String, String> options = attribute.getOptions();
						for (String key : options.keySet()) {
							if (values.contains(options.get(key))) {
								attribute.addValue(key);
							}
						}
					}
				}
			}
		}
		// 2.0: the type was not always set
		if (version.isSmallerOrEquals(TASK_DATA_VERSION_2_0)) {
			Collection<TaskAttribute> attributes = new ArrayList<TaskAttribute>(taskData.getRoot()
					.getAttributes()
					.values());
			for (TaskAttribute attribute : attributes) {
				if (attribute.getId().startsWith(TaskAttribute.PREFIX_OPERATION)) {
					if (attribute.getValue().equals(REASSIGN_OPERATION)) {
						taskData.getRoot().removeAttribute(attribute.getId());
						continue;
					} else {
						TaskAttribute associatedAttribute = taskData.getAttributeMapper().getAssoctiatedAttribute(
								attribute);
						if (associatedAttribute != null && associatedAttribute.getId().equals("resolution")) { //$NON-NLS-1$
							TaskAttribute resolutionAttribute = taskData.getRoot().getAttribute(
									JiraAttribute.RESOLUTION.id());
							if (resolutionAttribute != null) {
								Map<String, String> options = associatedAttribute.getOptions();
								for (String key : options.keySet()) {
									resolutionAttribute.putOption(key, options.get(key));
								}
								resolutionAttribute.getMetaData().setType(TaskAttribute.TYPE_SINGLE_SELECT);
								resolutionAttribute.getMetaData().setReadOnly(false);
							}
							attribute.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID,
									JiraAttribute.RESOLUTION.id());
							attribute.removeAttribute(associatedAttribute.getId());
						}
					}
				} else if (attribute.getId().equals(JiraAttribute.TYPE.id())) {
					if (attribute.getOptions().isEmpty()) {
						// sub task
						JiraClient client = clientFactory.getJiraClient(taskRepository);
						IssueType[] jiraIssueTypes = client.getCache().getIssueTypes();
						for (IssueType type : jiraIssueTypes) {
							if (attribute.getValue().equals(type.getName())) {
								attribute.putOption(type.getId(), type.getName());
								attribute.setValue(type.getId());
								break;
							}
						}
					}
				}
				attribute.getMetaData().setType(getType(attribute));
			}
		}
		// migration for v2.1 is now handled in the framework 
		// store long values instead of formatted time spans
		if (version.isSmallerOrEquals(TASK_DATA_VERSION_2_2)) {
			for (TaskAttribute attribute : taskData.getRoot().getAttributes().values()) {
				JiraTimeFormat format = new JiraTimeFormat();
				if (isTimeSpanAttribute(attribute)) {
					String value = attribute.getValue();
					if (value.length() > 0) {
						try {
							Long.parseLong(value);
						} catch (NumberFormatException e) {
							try {
								attribute.setValue(String.valueOf(format.parse(value)));
							} catch (ParseException e1) {
								attribute.setValue(""); //$NON-NLS-1$
							}
						}
					}
				}
			}
		}
		if (version.isSmallerOrEquals(TASK_DATA_VERSION_CURRENT)) {
			taskData.setVersion(TASK_DATA_VERSION_CURRENT.toString());
		}
	}

	public static boolean isTimeSpanAttribute(TaskAttribute attribute) {
		return JiraAttribute.INITIAL_ESTIMATE.id().equals(attribute.getId())
				|| JiraAttribute.ESTIMATE.id().equals(attribute.getId())
				|| JiraAttribute.ACTUAL.id().equals(attribute.getId());
	}

	private String getType(TaskAttribute taskAttribute) {
		if (JiraAttribute.DESCRIPTION.id().equals(taskAttribute.getId())) {
			return TaskAttribute.TYPE_LONG_RICH_TEXT;
		}
		if (JiraAttribute.COMMENT_NEW.id().equals(taskAttribute.getId())) {
			return TaskAttribute.TYPE_LONG_RICH_TEXT;
		}
		if (JiraAttribute.SUMMARY.id().equals(taskAttribute.getId())) {
			return TaskAttribute.TYPE_SHORT_RICH_TEXT;
		}
		if (TaskAttribute.OPERATION.equals(taskAttribute.getId())
				|| taskAttribute.getId().startsWith(TaskAttribute.PREFIX_OPERATION)) {
			return TaskAttribute.TYPE_OPERATION;
		}
		if (taskAttribute.getId().startsWith(TaskAttribute.PREFIX_COMMENT)) {
			return TaskAttribute.TYPE_COMMENT;
		}
		if (taskAttribute.getId().startsWith(TaskAttribute.PREFIX_ATTACHMENT)) {
			return TaskAttribute.TYPE_ATTACHMENT;
		}
		JiraFieldType fieldType = null;
		if (JiraAttribute.CREATION_DATE.id().equals(taskAttribute.getId())
				|| JiraAttribute.DUE_DATE.id().equals(taskAttribute.getId())
				|| JiraAttribute.MODIFICATION_DATE.id().equals(taskAttribute.getId())) {
			fieldType = JiraFieldType.DATE;
			taskAttribute.getMetaData().putValue(IJiraConstants.META_TYPE, fieldType.getKey());
		}
		if (fieldType == null) {
			fieldType = JiraFieldType.fromKey(taskAttribute.getMetaData().getValue(IJiraConstants.META_TYPE));
		}
		if (fieldType.getTaskType() != null) {
			return fieldType.getTaskType();
		}
		fieldType = JiraAttribute.valueById(taskAttribute.getId()).getType();
		if (fieldType.getTaskType() != null) {
			return fieldType.getTaskType();
		}
		String existingType = taskAttribute.getMetaData().getType();
		if (existingType != null) {
			return existingType;
		}
		return TaskAttribute.TYPE_SHORT_TEXT;
	}

	public String mapCommonAttributeKey(String key) {
		if ("summary".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.SUMMARY.id();
		} else if ("description".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.DESCRIPTION.id();
		} else if ("priority".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.PRIORITY.id();
		} else if ("resolution".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.RESOLUTION.id();
		} else if ("assignee".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.USER_ASSIGNED.id();
		} else if ("environment".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.ENVIRONMENT.id();
		} else if ("issuetype".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.TYPE.id();
		} else if ("components".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.COMPONENTS.id();
		} else if ("versions".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.AFFECTSVERSIONS.id();
		} else if ("fixVersions".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.FIXVERSIONS.id();
		} else if ("timetracking".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.ESTIMATE.id();
		} else if ("duedate".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.DUE_DATE.id();
		}
		if (key.startsWith("issueLink")) { //$NON-NLS-1$
			return IJiraConstants.ATTRIBUTE_LINK_PREFIX + key;
		}
		if (key.startsWith("customfield")) { //$NON-NLS-1$
			return IJiraConstants.ATTRIBUTE_CUSTOM_PREFIX + key;
		}
		if ("security".equals(key)) { //$NON-NLS-1$
			return JiraAttribute.SECURITY_LEVEL.id();
		}
		return key;
	}
}
/*******************************************************************************
 * Copyright (c) 2009 Atlassian and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlassian - initial API and implementation
 ******************************************************************************/

package com.atlassian.connector.eclipse.internal.jira.core;

public final class TizenAttribute {

	private final String id;

	private final boolean isHidden;

	private final boolean isReadOnly;

	private final String name;

	private final JiraFieldType type;

	private static final String CUSTOM_FILED_PREFIX_ID = "attribute.jira.custom::";

	private static final String CUSTOM_FIELD_SEVERITY_POSTFIX_ID = "customfield_10010";

	private static final String CUSTOM_FIELD_FREQUENCY_POSTFIX_ID = "customfield_10011";

	public static final TizenAttribute ATTRIBUTE_SEVERITY = new TizenAttribute(CUSTOM_FILED_PREFIX_ID
			+ CUSTOM_FIELD_SEVERITY_POSTFIX_ID, JiraFieldType.SELECT, "Severity: ", false, false);

	public static final TizenAttribute ATTRIBUTE_FREQUENCY = new TizenAttribute(CUSTOM_FILED_PREFIX_ID
			+ CUSTOM_FIELD_FREQUENCY_POSTFIX_ID, JiraFieldType.SELECT, "Frequency: ", false, false);

	public static final TizenAttribute ATTRIBUTE_USER_ASSIGNED = new TizenAttribute(JiraAttribute.USER_ASSIGNED.id(),
			JiraFieldType.SELECT, Messages.JiraAttribute_Assigned_to, false, false);

	private TizenAttribute(String id, JiraFieldType type, String name) {
		this.id = id;
		this.type = type;
		this.name = name;
		isHidden = true;
		isReadOnly = true;
	}

	private TizenAttribute(String id, JiraFieldType type, String name, boolean isHidden, boolean isReadOnly) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.isHidden = isHidden;
		this.isReadOnly = isReadOnly;
	}

	public String id() {
		return id;
	}

	public String getName() {
		return name;
	}

	public JiraFieldType getType() {
		return type;
	}

	public boolean isHidden() {
		return isHidden;
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	public String getKind() {
		return isHidden ? null : "task.common.kind.default";
	}
}
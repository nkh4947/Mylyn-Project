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
 *******************************************************************************/

package com.atlassian.connector.eclipse.internal.jira.core.model.filter;

import java.io.Serializable;

import com.atlassian.connector.eclipse.internal.jira.core.model.Priority;

/**
 * @author Brock Janiczak
 */
public class PriorityFilter implements Filter, Serializable {
	private static final long serialVersionUID = 1L;

	private final Priority[] priorities;

	public PriorityFilter(Priority[] priorities) {
		assert (priorities != null);
		assert (priorities.length > 0);

		this.priorities = priorities;
	}

	public Priority[] getPriorities() {
		return this.priorities;
	}

	PriorityFilter copy() {
		return new PriorityFilter(this.priorities);
	}
}

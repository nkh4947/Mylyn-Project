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

import com.atlassian.connector.eclipse.internal.jira.core.model.Component;

/**
 * Restricts to issues that have one of the specified components. This filter can only be used in conjunction with a
 * {@link ProjectFilter}. If no components are specified it is assumed the user is looking for issues wih no assigned
 * components. If you are looking for issues with any component, don't add a component filter.
 * 
 * @see com.gbst.jira.core.model.filter.ProjectFilter
 * @author Brock Janiczak
 */
public class ComponentFilter implements Filter, Serializable {
	private static final long serialVersionUID = 1L;

	private final Component[] components;

	private final boolean containsNone;

	public ComponentFilter(Component[] components, boolean containsNone) {
		assert (components != null);
		this.containsNone = containsNone;
		this.components = components;
	}

	public Component[] getComponents() {
		return this.components;
	}

	public boolean hasNoComponent() {
		return containsNone;
	}

	public ComponentFilter copy() {
		Component[] copy = new Component[components.length];
		System.arraycopy(components, 0, copy, 0, components.length);
		return new ComponentFilter(copy, containsNone);
	}
}

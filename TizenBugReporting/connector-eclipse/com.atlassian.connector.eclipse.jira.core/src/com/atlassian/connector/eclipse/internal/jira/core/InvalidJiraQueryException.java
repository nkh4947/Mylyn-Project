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

package com.atlassian.connector.eclipse.internal.jira.core;

/**
 * @author Steffen Pingel
 */
public class InvalidJiraQueryException extends RuntimeException {

	private static final long serialVersionUID = -9151805689314153246L;

	public InvalidJiraQueryException(String string) {
		super(string);
	}

}

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

// Only one is required
/**
 * @author Brock Janiczak
 */
public class EstimateVsActualFilter implements Filter, Serializable {
	private static final long serialVersionUID = 1L;

	private final long minVariation;

	private final long maxVariation;

	public EstimateVsActualFilter(long minVariation, long maxVariation) {
		this.minVariation = minVariation;
		this.maxVariation = maxVariation;
	}

	public long getMaxVariation() {
		return this.maxVariation;
	}

	public long getMinVariation() {
		return this.minVariation;
	}

	EstimateVsActualFilter copy() {
		return new EstimateVsActualFilter(this.minVariation, this.maxVariation);
	}
}

/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.atlassian.connector.eclipse.internal.jira.core.html;

import java.io.IOException;
import java.io.Reader;

/**
 * <p>
 * Moved into this package from <code>org.eclipse.jface.internal.text.revisions</code>.
 * </p>
 */
/**
 * COPY OF: org.eclipse.jface.internal.text.html.SingleCharReader
 */
public abstract class SingleCharReader extends Reader {

	/**
	 * @see Reader#read()
	 */
	@Override
	public abstract int read() throws IOException;

	/**
	 * @see Reader#read(char[],int,int)
	 */
	@Override
	public int read(char cbuf[], int off, int len) throws IOException {
		int end = off + len;
		for (int i = off; i < end; i++) {
			int ch = read();
			if (ch == -1) {
				if (i == off) {
					return -1;
				}
				return i - off;
			}
			cbuf[i] = (char) ch;
		}
		return len;
	}

	/**
	 * @see Reader#ready()
	 */
	@Override
	public boolean ready() throws IOException {
		return true;
	}

	/**
	 * Returns the readable content as string.
	 * 
	 * @return the readable content as string
	 * @exception IOException
	 *                in case reading fails
	 */
	public String getString() throws IOException {
		StringBuilder buf = new StringBuilder();
		int ch;
		while ((ch = read()) != -1) {
			buf.append((char) ch);
		}
		return buf.toString();
	}
}

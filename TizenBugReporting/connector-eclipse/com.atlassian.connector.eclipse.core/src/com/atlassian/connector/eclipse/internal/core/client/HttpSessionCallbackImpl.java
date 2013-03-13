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

package com.atlassian.connector.eclipse.internal.core.client;

import com.atlassian.connector.commons.api.ConnectionCfg;
import com.atlassian.connector.eclipse.internal.core.AtlassianCorePlugin;
import com.atlassian.connector.eclipse.internal.ui.IBrandingConstants;
import com.atlassian.theplugin.commons.exception.HttpProxySettingsException;
import com.atlassian.theplugin.commons.remoteapi.rest.AbstractHttpSession;
import com.atlassian.theplugin.commons.remoteapi.rest.HttpSessionCallback;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.WebUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of HttpSessionCallback that can handle setting the HttpClient information on a per-server basis
 * 
 * @author Shawn Minto
 * @author Wojciech Seliga
 */
public class HttpSessionCallbackImpl implements HttpSessionCallback {

	private final String userAgent;

	/** synchronized on this HttpSessionCallbackImpl */
	private final Map<ConnectionCfg, HttpClient> httpClients = new HashMap<ConnectionCfg, HttpClient>();

	private final Map<String, ConnectionCfg> locations = new HashMap<String, ConnectionCfg>();

	public HttpSessionCallbackImpl() {
		userAgent = IBrandingConstants.PRODUCT_NAME + "/" + AtlassianCorePlugin.getDefault().getVersion();
	}

	public synchronized HttpClient getHttpClient(ConnectionCfg server) throws HttpProxySettingsException {
		HttpClient httpClient = httpClients.get(server);

		// TODO handle the case where we dont have a client initialized
		assert (httpClient != null);

		httpClient.getParams().setParameter(HttpMethodParams.USER_AGENT, userAgent);
		return httpClient;
	}

	public void configureHttpMethod(AbstractHttpSession session, HttpMethod method) {
		// nothing to do here
	}

	public synchronized void removeClient(ConnectionCfg server) {
		HttpClient client = httpClients.remove(server);
		if (client != null) {
			shutdown(client);
		}
	}

	public synchronized void removeClient(AbstractWebLocation location) {
		ConnectionCfg server = locations.remove(location.getUrl());
		if (server != null) {
			removeClient(server);
		}
	}

	public synchronized void updateHostConfiguration(AbstractWebLocation location, ConnectionCfg serverCfg) {
		HttpClient httpClient = httpClients.get(serverCfg);
		if (httpClient == null) {
			httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
			httpClients.put(serverCfg, httpClient);
			locations.put(location.getUrl(), serverCfg);
			WebUtil.addConnectionManager(httpClient.getHttpConnectionManager());
		}
		setupHttpClient(location, httpClient);
	}

	private void setupHttpClient(AbstractWebLocation location, HttpClient httpClient) {
		HostConfiguration hostConfiguration = WebUtil.createHostConfiguration(httpClient, location,
				new NullProgressMonitor());
		httpClient.setHostConfiguration(hostConfiguration);
		httpClient.getParams().setAuthenticationPreemptive(true);
	}

	@Override
	protected void finalize() throws Throwable {
		for (HttpClient httpClient : httpClients.values()) {
			shutdown(httpClient);
		}
		httpClients.clear();
	}

	public void shutdown(HttpClient httpClient) {
		HttpConnectionManager mgr = httpClient.getHttpConnectionManager();
		WebUtil.removeConnectionManager(mgr);
		((MultiThreadedHttpConnectionManager) mgr).shutdown();
	}

	public void clear() {
		locations.clear();
		httpClients.clear();
	}

	public void disposeClient(ConnectionCfg server) {
		removeClient(server);
	}

	public Cookie[] getCookiesHeaders(ConnectionCfg server) {
		try {
			return getHttpClient(server).getState().getCookies();
		} catch (HttpProxySettingsException e) {
			return new Cookie[0];
		}
	}

}

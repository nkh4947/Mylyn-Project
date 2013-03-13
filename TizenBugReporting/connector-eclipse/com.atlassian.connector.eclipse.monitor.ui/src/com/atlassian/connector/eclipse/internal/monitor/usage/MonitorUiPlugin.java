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

package com.atlassian.connector.eclipse.internal.monitor.usage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.workbench.WorkbenchUtil;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;

import com.atlassian.connector.eclipse.internal.branding.ui.RuntimeUtil;
import com.atlassian.connector.eclipse.internal.monitor.usage.dialogs.PermissionToMonitorDialog;
import com.atlassian.connector.eclipse.monitor.core.MonitorCorePlugin;

/**
 * @author Mik Kersten
 * @author Shawn Minto
 */
public class MonitorUiPlugin extends AbstractUIPlugin {

	private static final long HOUR = 3600 * 1000;

	public static final long DEFAULT_DELAY_BETWEEN_TRANSMITS = 7 * 24 * HOUR;

	public static final String MONITOR_LOG_NAME_OLD = "monitor-log.xml";

	public static final String MONITOR_LOG_NAME = "usage-data.xml";

	public static final String ID_PLUGIN = "com.atlassian.connector.eclipse.monitor.ui"; //$NON-NLS-1$

	private static final long FIVE_MINUTES_IN_MS = 5 * 60 * 1000;

	private static MonitorUiPlugin plugin;

	public static class UiUsageMonitorStartup implements IStartup {
		public void earlyStartup() {
			// everything happens on normal start
		}
	}

	public MonitorUiPlugin() {
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(MonitorUiPreferenceConstants.PREF_MONITORING_ENABLED)) {
					if (isMonitoringEnabled()) {
						MonitorCorePlugin.getDefault().startMonitoring();
					} else {
						MonitorCorePlugin.getDefault().stopMonitoring();
					}
				}
			}
		});

		if (isMonitoringEnabled()) {
			MonitorCorePlugin.getDefault().startMonitoring();
		}

		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					if (isFirstTime() && !RuntimeUtil.suppressConfigurationWizards()) {
						askUserToEnableMonitoring();
					}
				} catch (Throwable t) {
					StatusHandler.log(new Status(IStatus.ERROR, MonitorUiPlugin.ID_PLUGIN,
							Messages.MonitorUiPlugin_failed_to_start, t));
				}
			}
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (isMonitoringEnabled()) {
			MonitorCorePlugin.getDefault().stopMonitoring();
		}

		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static MonitorUiPlugin getDefault() {
		return plugin;
	}

	/**
	 * One time action (after this plugin was installed) - after the plugin was installed inform user that monitoring is
	 * enabled.
	 */
	private void askUserToEnableMonitoring() {
		final IPreferenceStore store = getPreferenceStore();

		UIJob informUserJob = new UIJob("Ask User about Usage Data Monitoring") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				// must not use boolean here, it will not be stored
				store.setValue(MonitorUiPreferenceConstants.PREF_MONITORING_FIRST_TIME, "false");

				if (!isMonitoringEnabled()) {
					if (new PermissionToMonitorDialog(WorkbenchUtil.getShell()).open() == IDialogConstants.YES_ID) {
						setMonitoringEnabled(true);
					}
				}
				return Status.OK_STATUS;
			}
		};

		informUserJob.setPriority(Job.INTERACTIVE);
		informUserJob.schedule(FIVE_MINUTES_IN_MS);
	}

	public static IPreferenceStore getPrefs() {
		return getDefault().getPreferenceStore();
	}

	public boolean isFirstTime() {
		return !getPreferenceStore().contains(MonitorUiPreferenceConstants.PREF_MONITORING_FIRST_TIME)
				|| getPreferenceStore().getBoolean(MonitorUiPreferenceConstants.PREF_MONITORING_FIRST_TIME);
	}

	public boolean isMonitoringEnabled() {
		return getPreferenceStore().getBoolean(MonitorUiPreferenceConstants.PREF_MONITORING_ENABLED);
	}

	public void setMonitoringEnabled(boolean b) {
		getPreferenceStore().setValue(MonitorUiPreferenceConstants.PREF_MONITORING_ENABLED, b);
	}

}

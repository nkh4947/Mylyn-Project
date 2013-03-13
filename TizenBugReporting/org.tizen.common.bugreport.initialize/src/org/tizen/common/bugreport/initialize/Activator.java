package org.tizen.common.bugreport.initialize;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryDelta;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryDelta.Type;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryManager;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import com.atlassian.connector.eclipse.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tizen.common.bugreport.initialize"; //$NON-NLS-1$
	public static final String TIZEN_BUGREPORT_URL = "http://slp-info.sec.samsung.net/bugs";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		
		TaskRepositoryManager repositoryManager = TasksUiPlugin.getRepositoryManager();
		
		
      TaskRepository tizenRepository = repositoryManager.getRepository(JiraCorePlugin.CONNECTOR_KIND, TIZEN_BUGREPORT_URL);
      
//      repositoryManager.addRepository(tizenRepository);
//      repositoryManager.applyMigrators(tizenRepository);
      
      AuthenticationCredentials credentials = new AuthenticationCredentials("ho.namkoong", "skarndgh1");
      tizenRepository.setCredentials(AuthenticationType.REPOSITORY, credentials, true);
      repositoryManager.notifyRepositorySettingsChanged(tizenRepository,
              new TaskRepositoryDelta(Type.CREDENTIALS, AuthenticationType.REPOSITORY));
		
		
		System.out.println("ASDF");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}

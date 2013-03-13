package org.tizen.common.bugreport.editor;

import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.ui.forms.editor.IFormPage;
import org.tizen.common.bugreport.Activator;
import org.tizen.common.bugreport.util.TizenTasksUiUtil;

import com.atlassian.connector.eclipse.internal.jira.core.JiraCorePlugin;
import com.atlassian.connector.eclipse.internal.jira.ui.editor.JiraTaskEditorPageFactory;

@SuppressWarnings("restriction")
public class TizenTaskEditorPageFactory extends JiraTaskEditorPageFactory {

    private static String PLANNING_PAGE_FACTORY_ID = "org.eclipse.mylyn.tasks.ui.pageFactory.Planning";
    private static String CONTEXT_PAGE_FACTORY_ID = "org.eclipse.mylyn.context.ui.pageFactory.Context";
    private static String JIRA_PAGE_FACTORY_ID = "org.eclipse.mylyn.jira.ui.pageFactory";
    
    private static String[] CONFLICT_PAGE_IDS = {PLANNING_PAGE_FACTORY_ID, CONTEXT_PAGE_FACTORY_ID, JIRA_PAGE_FACTORY_ID};
    
    public TizenTaskEditorPageFactory()
    {
        this.setId("TizenTaskEditorPageFactory");
    }

    public IFormPage createPage(TaskEditor parentEditor)
    {
        return new TizenTaskEditorPage(parentEditor);
    }

    @Override
    public boolean canCreatePageFor(TaskEditorInput input) {
        ITask task = input.getTask();
        if (task.getConnectorKind().equals(JiraCorePlugin.CONNECTOR_KIND) && task.getUrl().equals(Activator.TIZEN_BUGREPORT_URL)) {
            return true;
        } else if (TizenTasksUiUtil.isOutgoingNewTask(input.getTask(), JiraCorePlugin.CONNECTOR_KIND, Activator.TIZEN_BUGREPORT_URL)) {
            return true;
        }
        return false;
    }
    
    @Override
    public String[] getConflictingIds(TaskEditorInput input) {
        return CONFLICT_PAGE_IDS;
    }
}

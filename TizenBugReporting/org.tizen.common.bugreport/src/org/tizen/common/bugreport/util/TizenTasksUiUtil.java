package org.tizen.common.bugreport.util;

import java.io.ObjectInputStream.GetField;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.tizen.common.bugreport.model.TizenAttribute;

public class TizenTasksUiUtil {
    
    public static boolean isOutgoingNewTask(ITask task, String connectorKind) {
        return isOutgoingNewTask(task, connectorKind, null);
    }
    
    public static boolean isOutgoingNewTask(ITask task, String connectorKind, String url) {
        Assert.isNotNull(task);
        Assert.isNotNull(connectorKind);
        if(!connectorKind.equals(task.getAttribute(ITasksCoreConstants.ATTRIBUTE_OUTGOING_NEW_CONNECTOR_KIND))) {
            return false;
        }
        
        if(url != null) {
            if(!url.equals(task.getAttribute(ITasksCoreConstants.ATTRIBUTE_OUTGOING_NEW_REPOSITORY_URL))) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isTizenAttribute(TaskAttribute attribute) {
        if(TizenAttribute.TIZEN_ATTRIBUTE_ID_MAP.contains(attribute.getId())) {
            return true;
        }
        return false;
    }
    
}

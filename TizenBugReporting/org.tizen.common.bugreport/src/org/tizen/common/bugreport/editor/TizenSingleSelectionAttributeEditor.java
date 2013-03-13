package org.tizen.common.bugreport.editor;

import org.eclipse.mylyn.internal.tasks.ui.editors.SingleSelectionAttributeEditor;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.swt.graphics.Color;
import org.tizen.common.bugreport.util.TizenTasksUiUtil;

public class TizenSingleSelectionAttributeEditor extends SingleSelectionAttributeEditor {

    public TizenSingleSelectionAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
        super(manager, taskAttribute);
    }

    
    @Override
    protected void decorateIncoming(Color color) {
        if(!TizenTasksUiUtil.isTizenAttribute(this.getTaskAttribute())) {
            super.decorateIncoming(color);
        }
    }
}

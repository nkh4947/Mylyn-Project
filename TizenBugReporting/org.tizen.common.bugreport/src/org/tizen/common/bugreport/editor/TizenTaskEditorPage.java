package org.tizen.common.bugreport.editor;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;
import org.tizen.common.bugreport.model.TizenAttribute;

import com.atlassian.connector.eclipse.internal.jira.core.JiraAttribute;
import com.atlassian.connector.eclipse.internal.jira.ui.editor.JiraTaskEditorPage;

@SuppressWarnings({ "restriction" })
public class TizenTaskEditorPage extends JiraTaskEditorPage {

    private String[] OPTION_KEY_SEVERITY = {"10033", "10034", "10035", "10036", "10037"};
    private String[] OPTION_VALUE_SEVERITY = {"1-Critical", "2-Major", "3-Average", "4-Minor", "5-Enhancement"};
    
    private String[] OPTION_KEY_FREQUENCY = {"10050", "10051", "10052"};
    private String[] OPTION_VALUE_FREQUENCY = {"Once", "Always", "Frequent"};
    
    private String[] OPTION_KEY_REPORT = {"hyungoo1.kang", "choiey", "yeongkyoon.lee", "chywoo.park", "kh5325.kim"};
    private String[] OPTION_VALUE_REPORT = {"SDK Project lead: Hyun-Goo Kang", "Documentation: Erin Choi", "Emulator: YeongKyoon Lee", "Installer: Sungho Park", "IDE: KangHo Kim"};
    
    public TizenTaskEditorPage(TaskEditor editor) {
        super(editor);
    }

    @Override
    protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
        TaskData data = this.getModel().getTaskData();
        
        createTizenAttribute(data, TizenAttribute.ATTRIBUTE_SEVERITY, OPTION_KEY_SEVERITY, OPTION_VALUE_SEVERITY, 3);
        createTizenAttribute(data, TizenAttribute.ATTRIBUTE_FREQUENCY, OPTION_KEY_FREQUENCY, OPTION_VALUE_FREQUENCY, 2);
        
        
        removeAttribute(data, TizenAttribute.USER_ASSIGNED_KEY);
        createTizenAttribute(data, TizenAttribute.ATTRIBUTE_USER_ASSIGNED, OPTION_KEY_REPORT, OPTION_VALUE_REPORT, 4);
        
        Set<TaskEditorPartDescriptor> parts = super.createPartDescriptors();
        removePart(parts, ID_PART_PEOPLE);
        removePart(parts, ID_PART_ACTIONS);
        return parts;
    }
    
    private boolean removePart(Set<TaskEditorPartDescriptor> parts, String partId) {
        Iterator<TaskEditorPartDescriptor> iter;
        iter = parts.iterator();
        while (iter.hasNext()) {
            TaskEditorPartDescriptor part = iter.next();
            if (part.getId().equals(partId)) {
                parts.remove(part);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String getId() {
        return "TizenTaskEditorPage";
    }
    
    @Override
    public void doSubmit() {
        this.getEditor().setMessage(null, IMessageProvider.NONE);
        super.doSubmit();
    }
    
    private void createTizenAttribute(TaskData data, TizenAttribute key, String[] optionKeys, String[] optionValues, int defaultValue) {
        TaskAttribute attribute = data.getRoot().createAttribute(key.id());
        
        TaskAttributeMetaData metaData = attribute.getMetaData().defaults();
        metaData.setReadOnly(key.isReadOnly());
        metaData.setKind(key.getKind());
        metaData.setLabel(key.getName());
        metaData.setType(key.getType().getTaskType());
        metaData.putValue("type", key.getType().getKey());
        
        for(int i=0; i<optionKeys.length; i++) {
            attribute.putOption(optionKeys[i], optionValues[i]);
        }
        
        if(defaultValue > 0 && defaultValue < optionKeys.length) {
            attribute.setValue(optionKeys[defaultValue]);
        }
    }
    
    private void removeAttribute(TaskData data, String key) {
        data.getRoot().removeAttribute(key);
    }
    
    @Override
    protected AttributeEditorFactory createAttributeEditorFactory() {
        return new TizenAttributeEditorFactory(getModel(), getTaskRepository(), getEditorSite());
    }
}
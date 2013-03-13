package org.tizen.common.bugreport.model;

import java.util.HashSet;

import com.atlassian.connector.eclipse.internal.jira.core.JiraAttribute;
import com.atlassian.connector.eclipse.internal.jira.core.JiraFieldType;
import com.atlassian.connector.eclipse.internal.jira.core.Messages;

@SuppressWarnings("restriction")
public final class TizenAttribute {

    private final String id;
    private final boolean isHidden;
    private final boolean isReadOnly;
    private final String name;
    private final JiraFieldType type;
    
    private static final String CUSTOM_FIELD_SEVERITY_KEY = "attribute.jira.custom::customfield_10010";
    private static final String CUSTOM_FIELD_FREQUENCY_KEY = "attribute.jira.custom::customfield_10011";
    public static final String USER_ASSIGNED_KEY = JiraAttribute.USER_ASSIGNED.id();
    
    public static final TizenAttribute ATTRIBUTE_SEVERITY = new TizenAttribute(CUSTOM_FIELD_SEVERITY_KEY, JiraFieldType.SELECT , "Severity: ", false, false);
    public static final TizenAttribute ATTRIBUTE_FREQUENCY = new TizenAttribute(CUSTOM_FIELD_FREQUENCY_KEY, JiraFieldType.SELECT, "Frequency: ", false, false);
    public static final TizenAttribute ATTRIBUTE_USER_ASSIGNED = new TizenAttribute(USER_ASSIGNED_KEY, JiraFieldType.SELECT, "Report to: ", false, false);
    public static final HashSet<String> TIZEN_ATTRIBUTE_ID_MAP = new HashSet<String>();
    
    static {
        TIZEN_ATTRIBUTE_ID_MAP.add(CUSTOM_FIELD_SEVERITY_KEY);
        TIZEN_ATTRIBUTE_ID_MAP.add(CUSTOM_FIELD_FREQUENCY_KEY);
        TIZEN_ATTRIBUTE_ID_MAP.add(USER_ASSIGNED_KEY);
    }
    
    private TizenAttribute(String id, JiraFieldType type, String name)
    {
        this.id = id;
        this.type = type;
        this.name = name;
        isHidden = true;
        isReadOnly = true;
    }

    private TizenAttribute(String id, JiraFieldType type, String name, boolean isHidden, boolean isReadOnly)
    {
        this.id = id;
        this.type = type;
        this.name = name;
        this.isHidden = isHidden;
        this.isReadOnly = isReadOnly;
    }

    public String id()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public JiraFieldType getType()
    {
        return type;
    }

    public boolean isHidden()
    {
        return isHidden;
    }

    public boolean isReadOnly()
    {
        return isReadOnly;
    }

    public String getKind()
    {
        return isHidden ? null : "task.common.kind.default";
    }
}
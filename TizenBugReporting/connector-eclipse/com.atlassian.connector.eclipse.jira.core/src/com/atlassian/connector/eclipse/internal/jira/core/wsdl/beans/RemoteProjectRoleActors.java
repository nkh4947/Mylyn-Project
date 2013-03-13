/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Tasktop Technologies - initial API and implementation
 *******************************************************************************/
/**
 * RemoteProjectRoleActors.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans;

@SuppressWarnings("all")
public class RemoteProjectRoleActors extends com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteRoleActors implements
		java.io.Serializable {
	private com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteProject project;

	public RemoteProjectRoleActors() {
	}

	public RemoteProjectRoleActors(com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteProjectRole projectRole,
			com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteRoleActor[] roleActors,
			com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteUser[] users,
			com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteProject project) {
		super(projectRole, roleActors, users);
		this.project = project;
	}

	/**
	 * Gets the project value for this RemoteProjectRoleActors.
	 * 
	 * @return project
	 */
	public com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteProject getProject() {
		return project;
	}

	/**
	 * Sets the project value for this RemoteProjectRoleActors.
	 * 
	 * @param project
	 */
	public void setProject(com.atlassian.connector.eclipse.internal.jira.core.wsdl.beans.RemoteProject project) {
		this.project = project;
	}

	private java.lang.Object __equalsCalc = null;

	public synchronized boolean equals(java.lang.Object obj) {
		if (!(obj instanceof RemoteProjectRoleActors))
			return false;
		RemoteProjectRoleActors other = (RemoteProjectRoleActors) obj;
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (__equalsCalc != null) {
			return (__equalsCalc == obj);
		}
		__equalsCalc = obj;
		boolean _equals;
		_equals = super.equals(obj)
				&& ((this.project == null && other.getProject() == null) || (this.project != null && this.project.equals(other.getProject())));
		__equalsCalc = null;
		return _equals;
	}

	private boolean __hashCodeCalc = false;

	public synchronized int hashCode() {
		if (__hashCodeCalc) {
			return 0;
		}
		__hashCodeCalc = true;
		int _hashCode = super.hashCode();
		if (getProject() != null) {
			_hashCode += getProject().hashCode();
		}
		__hashCodeCalc = false;
		return _hashCode;
	}

	// Type metadata
	private static org.apache.axis.description.TypeDesc typeDesc = new org.apache.axis.description.TypeDesc(
			RemoteProjectRoleActors.class, true);

	static {
		typeDesc.setXmlType(new javax.xml.namespace.QName("http://beans.soap.rpc.jira.atlassian.com",
				"RemoteProjectRoleActors"));
		org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("project");
		elemField.setXmlName(new javax.xml.namespace.QName("", "project"));
		elemField.setXmlType(new javax.xml.namespace.QName("http://beans.soap.rpc.jira.atlassian.com", "RemoteProject"));
		elemField.setNillable(true);
		typeDesc.addFieldDesc(elemField);
	}

	/**
	 * Return type metadata object
	 */
	public static org.apache.axis.description.TypeDesc getTypeDesc() {
		return typeDesc;
	}

	/**
	 * Get Custom Serializer
	 */
	public static org.apache.axis.encoding.Serializer getSerializer(java.lang.String mechType,
			java.lang.Class _javaType, javax.xml.namespace.QName _xmlType) {
		return new org.apache.axis.encoding.ser.BeanSerializer(_javaType, _xmlType, typeDesc);
	}

	/**
	 * Get Custom Deserializer
	 */
	public static org.apache.axis.encoding.Deserializer getDeserializer(java.lang.String mechType,
			java.lang.Class _javaType, javax.xml.namespace.QName _xmlType) {
		return new org.apache.axis.encoding.ser.BeanDeserializer(_javaType, _xmlType, typeDesc);
	}

}

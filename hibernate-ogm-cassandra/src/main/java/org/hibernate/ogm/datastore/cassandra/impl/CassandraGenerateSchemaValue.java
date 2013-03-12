/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.cassandra.impl;

/**
 * @author Khanh Tuong Maudoux
 */
public enum CassandraGenerateSchemaValue {
	DEFAULT("default"),
	CREATE_DROP("create-drop"),
	CREATE("create");
	
	private final String value;


	private CassandraGenerateSchemaValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
	
	public static boolean isValid(String value) {
		return DEFAULT.getValue().equals( value ) || CREATE.getValue().equals( value ) || CREATE_DROP.getValue().equals( value );
	}
}

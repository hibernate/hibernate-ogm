/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.grid;

/**
 * The key of an entity, association or row.
 * <p>
 * Note that exposing the state of instances through arrays is a deliberate decision for the sake of performance. The
 * contents of these arrays must not be modified by the caller.
 *
 * @author Gunnar Morling
 */
public interface Key {

	/**
	 * Returns the table name of this key.
	 */
	String getTable();

	/**
	 * Returns the column names of this key.
	 */
	String[] getColumnNames();

	/**
	 * Returns the column values of this key.
	 */
	Object[] getColumnValues();
}

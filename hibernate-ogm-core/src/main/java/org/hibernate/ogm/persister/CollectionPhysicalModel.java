/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.persister;

import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Emmanuel Bernard
 */
public interface CollectionPhysicalModel extends CollectionPersister {
	/**
	 * The table to join to.
	 */
	public String getTableName();

	/**
	 * The columns to join on
	 */
	public String[] getKeyColumnNames();

	/**
	 * Get the names of the collection index columns if
	 * this is an indexed collection (optional operation)
	 */
	public String[] getIndexColumnNames();

	/**
	 * Get the names of the collection element columns (or the primary
	 * key columns in the case of a one-to-many association)
	 */
	public String[] getElementColumnNames();

	public String getIdentifierColumnName();

	//The following should really be moved somewhere else or the interface renamed
	public GridType getKeyGridType();

	public GridType getElementGridType();
}

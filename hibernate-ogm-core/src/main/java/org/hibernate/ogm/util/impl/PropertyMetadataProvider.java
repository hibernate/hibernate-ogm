/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.util.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.type.GridType;

/**
 * @author Emmanuel Bernard
 */
public class PropertyMetadataProvider {
	private String tableName;
	private String[] keyColumnNames;
	private GridType keyGridType;
	private Object key;
	private SessionImplementor session;
	private AssociationKey collectionMetadataKey;
	private Association collectionMetadata;
	private Object[] columnValues;
	private GridDialect gridDialect;

	//fluent methods for populating data

	public PropertyMetadataProvider gridDialect(GridDialect gridDialect) {
		this.gridDialect = gridDialect;
		return this;
	}

	//optional: data retrieved from gridManager if not set up
	public PropertyMetadataProvider tableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public PropertyMetadataProvider keyColumnNames(String[] keyColumnNames) {
		this.keyColumnNames = keyColumnNames;
		return this;
	}

	public PropertyMetadataProvider keyGridType(GridType keyGridType) {
		this.keyGridType = keyGridType;
		return this;
	}

	public PropertyMetadataProvider session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	public PropertyMetadataProvider key(Object key) {
		this.key = key;
		return this;
	}

	public PropertyMetadataProvider keyColumnValues(Object[] columnValues) {
		this.columnValues = columnValues;
		return this;
	}


	//action methods

	private AssociationKey getCollectionMetadataKey() {
		if ( collectionMetadataKey == null ) {
			final Object[] columnValues = getKeyColumnValues();
			collectionMetadataKey = new AssociationKey( tableName, keyColumnNames, columnValues );
		}
		return collectionMetadataKey;
	}

	private Object[] getKeyColumnValues() {
		if ( columnValues == null ) {
			columnValues = LogicalPhysicalConverterHelper.getColumnsValuesFromObjectValue(
					key, keyGridType, keyColumnNames, session
			);
		}
		return columnValues;
	}

	public Tuple createAndPutAssociationTuple(RowKey rowKey) {
		Tuple associationTuple = gridDialect.createTupleAssociation( getCollectionMetadataKey(), rowKey);
		getCollectionMetadata().put( rowKey, associationTuple);
		return associationTuple;
	}

	public Association getCollectionMetadata() {
		if ( collectionMetadata == null ) {
			collectionMetadata = gridDialect.getAssociation( getCollectionMetadataKey() );
			if (collectionMetadata == null) {
				collectionMetadata = gridDialect.createAssociation( getCollectionMetadataKey() );
			}
		}
		return collectionMetadata;
	}

	public Association getCollectionMetadataOrNull() {
		if ( collectionMetadata == null ) {
			collectionMetadata = gridDialect.getAssociation( getCollectionMetadataKey() );
		}
		return collectionMetadata;
	}

	public void flushToCache() {
		if ( getCollectionMetadata().isEmpty() ) {
			gridDialect.removeAssociation( getCollectionMetadataKey() );
			collectionMetadata = null;
		}
		else {
			gridDialect.updateAssociation( getCollectionMetadata(), getCollectionMetadataKey() );
		}
	}
}

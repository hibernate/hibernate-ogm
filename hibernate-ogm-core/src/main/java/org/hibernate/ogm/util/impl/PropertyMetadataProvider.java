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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.type.GridType;

/**
 * @author Emmanuel Bernard
 */
public class PropertyMetadataProvider {
	private GridMetadataManager gridManager;
	private Cache<AssociationKey, Map<RowKey,Map<String, Object>>> associationCache;
	private String tableName;
	private String[] keyColumnNames;
	private GridType keyGridType;
	private Object key;
	private SessionImplementor session;
	private AssociationKey collectionMetadataKey;
	private Map<RowKey,Map<String,Object>> collectionMetadata;
	private Object[] columnValues;

	//fluent methods for populating data

	public PropertyMetadataProvider gridManager(GridMetadataManager gridManager) {
		this.gridManager = gridManager;
		return this;
	}

	//optional: data retrieved from gridManager if not set up
	public PropertyMetadataProvider associationCache(Cache<AssociationKey, Map<RowKey,Map<String, Object>>> associationCache) {
		this.associationCache = associationCache;
		return this;
	}

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

	private Cache<AssociationKey, Map<RowKey,Map<String, Object>>> getAssociationCache() {
		if ( associationCache == null) {
			associationCache = GridMetadataManagerHelper.getAssociationCache( gridManager );
		}
		return associationCache;
	}

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

	public Map<RowKey,Map<String,Object>> getCollectionMetadata() {
		if ( collectionMetadata == null ) {
			collectionMetadata = gridManager.getGridDialect().getAssociation( getCollectionMetadataKey(), getAssociationCache() );
			if (collectionMetadata == null) {
				collectionMetadata = gridManager.getGridDialect().createAssociation( getCollectionMetadataKey(), getAssociationCache() );
			}
		}
		return collectionMetadata;
	}

	public Map<RowKey,Map<String,Object>> getCollectionMetadataOrNull() {
		if ( collectionMetadata == null ) {
			collectionMetadata = gridManager.getGridDialect().getAssociation( getCollectionMetadataKey(), getAssociationCache() );
		}
		return collectionMetadata;
	}

	public void flushToCache() {
		if ( getCollectionMetadata().size() == 0 ) {
			gridManager.getGridDialect().removeAssociation( getCollectionMetadataKey(), getAssociationCache() );
			collectionMetadata = null;
		}
		else {
			gridManager.getGridDialect().updateAssociation( getCollectionMetadata(), getCollectionMetadataKey(), getAssociationCache() );
		}
	}
}

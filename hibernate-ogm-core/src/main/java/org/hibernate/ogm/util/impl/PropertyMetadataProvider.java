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
package org.hibernate.ogm.util.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.ogm.grid.PropertyKey;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.type.GridType;

/**
 * @author Emmanuel Bernard
 */
public class PropertyMetadataProvider {
	private GridMetadataManager gridManager;
	private Cache<PropertyKey, List<Map<String, Object>>> propertyCache;
	private String tableName;
	private String[] keyColumnNames;
	private GridType keyGridType;
	private Object key;
	private SessionImplementor session;
	private PropertyKey collectionMetadataKey;
	private List<Map<String,Object>> collectionMetadata;
	private Object[] columnValues;

	// alternative gridManager or propertyCache
	public PropertyMetadataProvider gridManager(GridMetadataManager gridManager) {
		this.gridManager = gridManager;
		return this;
	}

	// alternative gridManager or propertyCache
	public PropertyMetadataProvider propertyCache(Cache<PropertyKey, List<Map<String, Object>>> propertyCache) {
		this.propertyCache = propertyCache;
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

	public PropertyMetadataProvider columnValues(Object[] columnValues) {
		this.columnValues = columnValues;
		return this;
	}

	private Cache<PropertyKey, List<Map<String, Object>>> getPropertyCache() {
		if (propertyCache == null) {
			propertyCache = GridMetadataManagerHelper.getPropertyCache( gridManager );
		}
		return propertyCache;
	}

	private PropertyKey getCollectionMetadataKey() {
		if ( collectionMetadataKey == null ) {
			final Object[] columnValues = getColumnValues();
			collectionMetadataKey = new PropertyKey( tableName, keyColumnNames, columnValues );
		}
		return collectionMetadataKey;
	}

	private Object[] getColumnValues() {
		if ( columnValues == null ) {
			columnValues = LogicalPhysicalConverterHelper.getColumnsValuesFromObjectValue(
					key, keyGridType, keyColumnNames, session
			);
		}
		return columnValues;
	}

	public List<Map<String,Object>> getCollectionMetadata() {
		if ( collectionMetadata == null ) {
			collectionMetadata = getPropertyCache().get( getCollectionMetadataKey() );
			if (collectionMetadata == null) {
				collectionMetadata = new ArrayList<Map<String,Object>>();
			}
		}
		return collectionMetadata;
	}

	public void flushToCache() {
		if ( getCollectionMetadata().size() == 0 ) {
			getPropertyCache().remove( getCollectionMetadataKey() );
		}
		else {
			getPropertyCache().put( getCollectionMetadataKey(), getCollectionMetadata() );
		}
	}

	public Map<String, Object> findMatchingTuple(Map<String, Object> tupleKey) {
		Map<String,Object> matchingTuple = null;
		for ( Map<String,Object> collTuple : getCollectionMetadata() ) {
			boolean notFound = false;
			for ( String columnName : tupleKey.keySet() ) {
				final Object value = collTuple.get( columnName );
				//values should not be null
				if ( ! tupleKey.get(columnName).equals( value ) ) {
					notFound = true;
					break;
				}
			}
			if ( ! notFound ) {
				matchingTuple = collTuple;
			}
		}
		return matchingTuple;
	}
}

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

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.persister.CollectionPhysicalModel;
import org.hibernate.ogm.persister.EntityKeyBuilder;
import org.hibernate.ogm.persister.OgmCollectionPersister;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Arrays;

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
	private OgmCollectionPersister collectionPersister;
	private boolean inverse;
	private Type propertyType;
	private String[] rowKeyColumnNames;

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

	public PropertyMetadataProvider inverse() {
		this.inverse = true;
		return this;
	}


	//action methods

	private AssociationKey getCollectionMetadataKey() {
		if ( collectionMetadataKey == null ) {
			final Object[] columnValues = getKeyColumnValues();
			collectionMetadataKey = new AssociationKey( tableName, keyColumnNames, columnValues );
			if (collectionPersister != null) {
				EntityKey entityKey;
				if ( inverse ) {
					//look for the other side of the collection, build the key of the other side's entity
					OgmEntityPersister elementPersister = (OgmEntityPersister) collectionPersister.getElementPersister();
					entityKey = EntityKeyBuilder.fromPersister(
							elementPersister,
							(Serializable) key,
							session
					);
					collectionMetadataKey.setCollectionRole( buildCollectionRole(collectionPersister) );
				}
				else {
					//we are on the right side, use the association property
					collectionMetadataKey.setCollectionRole( getUnqualifiedRole( collectionPersister ) );
					entityKey = EntityKeyBuilder.fromPersister(
							(OgmEntityPersister) collectionPersister.getOwnerEntityPersister(),
							(Serializable) key,
							session
					);
				}
				collectionMetadataKey.setOwnerEntityKey( entityKey );
				//TODO add information on the collection type, set, map, bag, list etc

				AssociationKind type = collectionPersister.getElementType().isEntityType() ? AssociationKind.ASSOCIATION : AssociationKind.EMBEDDED;
				collectionMetadataKey.setAssociationKind( type );
				collectionMetadataKey.setRowKeyColumnNames( collectionPersister.getRowKeyColumnNames() );
			}
			else if ( propertyType != null ) {
				collectionMetadataKey.setAssociationKind( propertyType.isEntityType() ? AssociationKind.ASSOCIATION : AssociationKind.EMBEDDED );
				if ( propertyType instanceof EntityType ) {
					EntityType entityType = (EntityType) propertyType;
					OgmEntityPersister associatedPersister = (OgmEntityPersister) entityType.getAssociatedJoinable( session.getFactory() );
					EntityKey entityKey = new EntityKey(
							associatedPersister.getTableName(),
							associatedPersister.getIdentifierColumnNames(),
							columnValues
					);
					collectionMetadataKey.setOwnerEntityKey( entityKey );
					collectionMetadataKey.setRowKeyColumnNames( rowKeyColumnNames );
					collectionMetadataKey.setCollectionRole( getCollectionRoleFromToOne( associatedPersister ) );
				}
				else {
					throw new AssertionFailure( "Cannot detect associated entity metadata. propertyType is of unexpected type: " + propertyType.getClass() );
				}
			}
			else {
				throw new AssertionFailure( "Cannot detect associated entity metadata: collectionPersister and propertyType are both null" );
			}
		}
		return collectionMetadataKey;
	}

	/*
	 * Try and find the inverse association matching from the associated entity
	 * If a match is found, use the other side's association name as role
	 * Otherwise use the table name
	 */
	private String getCollectionRoleFromToOne(OgmEntityPersister associatedPersister) {
		//code logic is slightly duplicated but the input and context is different, hence this choice
		Type[] propertyTypes = associatedPersister.getPropertyTypes();
		String otherSidePropertyName = null;
		for ( int index = 0 ; index <  propertyTypes.length ; index++ ) {
			Type type = propertyTypes[index];
			if ( type.isAssociationType() && type.isCollectionType() ) {
				boolean matching = isCollectionMatching( (CollectionType) type, tableName );
				if ( matching ) {
					otherSidePropertyName = associatedPersister.getPropertyNames()[index];
					break;
				}
			}
		}
		return otherSidePropertyName != null ? otherSidePropertyName : tableName;
	}

	private boolean isCollectionMatching(CollectionType type, String primarySideTableName) {
		String collectionRole = type.getRole();
		CollectionPhysicalModel reverseCollectionPersister = (CollectionPhysicalModel) session.getFactory().getCollectionPersister( collectionRole );
		boolean isSameTable = primarySideTableName.equals( reverseCollectionPersister.getTableName() );
		return isSameTable && Arrays.equals( keyColumnNames, reverseCollectionPersister.getKeyColumnNames() );
	}

	/*
	 * Try and find the inverse association matching from the associated entity
	 * If a match is found, use the other side's association name as role
	 * Otherwise use the table name
	 */
	private String buildCollectionRole(OgmCollectionPersister collectionPersister) {
		String otherSidePropertyName = null;
		Loadable elementPersister = (Loadable) collectionPersister.getElementPersister();
		Type[] propertyTypes = elementPersister.getPropertyTypes();

		for ( int index = 0 ; index <  propertyTypes.length ; index++ ) {
			Type type = propertyTypes[index];
			if ( type.isAssociationType() ) {
				if ( collectionPersister.isOneToMany() && ! type.isCollectionType() ) {
					if ( Arrays.equals( keyColumnNames, elementPersister.getPropertyColumnNames( index ) ) ) {
						//the property match
						otherSidePropertyName = elementPersister.getPropertyNames()[index];
						break;
					}
				}
				else if ( ! collectionPersister.isOneToMany() && type.isCollectionType() ) {
					boolean matching = isCollectionMatching( (CollectionType) type, collectionPersister.getTableName() );
					if ( matching ) {
						otherSidePropertyName = elementPersister.getPropertyNames()[index];
						break;
					}
				}
			}
		}
		return otherSidePropertyName != null ? otherSidePropertyName : tableName;
	}

	private String getUnqualifiedRole(CollectionPersister persister) {
		String entity = persister.getOwnerEntityPersister().getEntityName();
		String role = persister.getRole();
		return role.substring( entity.length() + 1 );
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
		Tuple associationTuple = gridDialect.createTupleAssociation( getCollectionMetadataKey(), rowKey );
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

	public PropertyMetadataProvider collectionPersister(OgmCollectionPersister collectionPersister) {
		this.collectionPersister = collectionPersister;
		return this;
	}

	public PropertyMetadataProvider propertyType(Type type) {
		this.propertyType = type;
		return this;
	}

	public PropertyMetadataProvider rowKeyColumnNames(String[] rowKeyColumnNames) {
		this.rowKeyColumnNames = rowKeyColumnNames;
		return this;
	}
}

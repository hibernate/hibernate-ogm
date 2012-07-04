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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.persister.EntityKeyBuilder;
import org.hibernate.ogm.persister.OgmCollectionPersister;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.ogm.type.ManyToOneType;

import java.io.Serializable;
import java.util.Set;

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
	private GridType gridPropertyType;
	private OgmEntityPersister persister;
	private boolean inverse;

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
					//inverse side of a collection, build the key of the other side's entity
					//FIXME: inverse: update collection role to add assoc table + collection role??
					collectionMetadataKey.setCollectionRole( tableName );
					entityKey = EntityKeyBuilder.fromPersister(
							(OgmEntityPersister) collectionPersister.getElementPersister(),
							(Serializable) key,
							session
					);
				}
				else {
					if ( ! collectionPersister.isInverse() ) {
						//owner side of the collection
						collectionMetadataKey.setCollectionRole( getUnqualifiedRole( collectionPersister ) );
					}
					else {
						// aligned with the logic updating the inverse side
						collectionMetadataKey.setCollectionRole( tableName );
					}
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
			else if ( gridPropertyType != null ) {
				if ( gridPropertyType instanceof ManyToOneType ) {
					String entityName = ( ( ManyToOneType ) gridPropertyType ).getReturnedClass().getName();
					String proeprtyName = "foo" ; //( ( ManyToOneType ) gridPropertyType ).getReferencedPropertyName();
					SessionFactoryImplementor factory = session.getFactory();
//					EntityPersister entityPersister = factory.getEntityPersister( entityName );
//					entityPersister.getEntityMetamodel()
					Set<String> collectionRolesByEntityParticipant = factory.getCollectionRolesByEntityParticipant( persister.getEntityName() );
					if ( collectionRolesByEntityParticipant == null || collectionRolesByEntityParticipant.size() == 0 ) {
						System.out.println( "******* Not participating in  collection " + persister.getEntityName() );
					}
					else {
						System.out.println( "******* Participating in  collection " + persister.getEntityName() );
					}

				}
				else {
					System.out.println( "*********** On est pas dans la merde " + tableName + ":" + keyColumnNames[0] + ":" + gridPropertyType.getClass() );
				}
			}
			else {
				System.out.println("*********** On est pas dans la merde " + tableName + ":" + keyColumnNames[0]);
			}
		}
		return collectionMetadataKey;
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

	public PropertyMetadataProvider gridPropertyType(GridType gridPropertyType) {
		this.gridPropertyType = gridPropertyType;
		return this;
	}

	public PropertyMetadataProvider persister(OgmEntityPersister persister) {
		this.persister = persister;
		return this;
	}
}

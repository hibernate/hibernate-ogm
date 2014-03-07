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
package org.hibernate.ogm.grid.impl;

import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.persister.CollectionPhysicalModel;
import org.hibernate.ogm.persister.OgmCollectionPersister;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;


/**
 * @author Gunnar Morling
 *
 */
public class AssociationKeyMetadataBuilder {

	private String table;
	private String[] columnNames;
	private String[] rowKeyColumnNames;

	private OgmCollectionPersister collectionPersister;
	private boolean inverse;
	private Type propertyType;
	private SessionFactoryImplementor sessionFactory;
	/*
	 * Return true if the other side association has been searched and not been found
	 * The other side association is searched when we are looking forward to update it
	 * and need to build the corresponding association key.
	 *
	 * It uses Boolean instead of boolean to make sure it's used only after being calculated
	 */
	private Boolean isBidirectional;

	public AssociationKeyMetadataBuilder() {
	}

	public AssociationKeyMetadataBuilder setTable(String table) {
		this.table = table;
		return this;
	}

	public AssociationKeyMetadataBuilder setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
		return this;
	}

	public AssociationKeyMetadataBuilder setRowKeyColumnNames(String[] rowKeyColumnNames) {
		this.rowKeyColumnNames = rowKeyColumnNames;
		return this;
	}

	public AssociationKeyMetadataBuilder setCollectionPersister(OgmCollectionPersister collectionPersister) {
		this.collectionPersister = collectionPersister;
		return this;
	}

	public AssociationKeyMetadataBuilder setPropertyType(Type propertyType) {
		this.propertyType = propertyType;
		return this;
	}

	public AssociationKeyMetadataBuilder inverse() {
		this.inverse = true;
		return this;
	}

	public AssociationKeyMetadataBuilder setSessionFactory(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		return this;
	}

	public AssociationKeyMetadata build() {
		Contracts.assertNotNull( table, "table" );
		Contracts.assertNotNull( columnNames, "columnNames" );
		Contracts.assertNotNull( rowKeyColumnNames, "rowKeyColumnNames" );

		AssociationKind associationKind = null;
		String collectionRole = null;

		// We have a collection on the main side
		if (collectionPersister != null) {
			// we are explicitly looking to update the non owning side
			if ( inverse ) {
				collectionRole = buildCollectionRole( collectionPersister );
			}
			else {
				//we are on the right side, use the association property
				collectionRole = getUnqualifiedRole( collectionPersister );
			}

			associationKind = collectionPersister.getElementType().isEntityType() ? AssociationKind.ASSOCIATION : AssociationKind.EMBEDDED_COLLECTION;
		}
		// We have a to-one on the main side
		else if ( propertyType != null ) {
			associationKind = propertyType.isEntityType() ? AssociationKind.ASSOCIATION : AssociationKind.EMBEDDED_COLLECTION;

			if ( propertyType instanceof EntityType ) {
				EntityType entityType = (EntityType) propertyType;
				OgmEntityPersister associatedPersister = (OgmEntityPersister) entityType.getAssociatedJoinable( sessionFactory );
				collectionRole = getCollectionRoleFromToOne( associatedPersister );
			}
			else {
				throw new AssertionFailure( "Cannot detect associated entity metadata. propertyType is of unexpected type: " + propertyType.getClass() );
			}
		}
		else {
			throw new AssertionFailure( "Cannot detect associated entity metadata: collectionPersister and propertyType are both null" );
		}

		return new AssociationKeyMetadata( table, columnNames, rowKeyColumnNames, associationKind, collectionRole, isBidirectional );
	}

	/*
	 * Try and find the inverse association matching from the associated entity
	 * If a match is found, use the other side's association name as role
	 * Otherwise use the table name
	 */
	//TODO we could cache such knowledge in a service if that turns out to be costly
	private String buildCollectionRole(OgmCollectionPersister collectionPersister) {
		String otherSidePropertyName = null;
		Loadable elementPersister = (Loadable) collectionPersister.getElementPersister();
		Type[] propertyTypes = elementPersister.getPropertyTypes();

		for ( int index = 0 ; index <  propertyTypes.length ; index++ ) {
			Type type = propertyTypes[index];
			//we try and restrict type search as much as possible
			if ( type.isAssociationType() ) {
				boolean matching = false;
				//if the main side collection is a one-to-many, the reverse side should be a to-one is not a collection
				if ( collectionPersister.isOneToMany() && ! type.isCollectionType() ) {
					matching = isToOneMatching( elementPersister, index, type );
				}
				//if the main side collection is not a one-to-many, the reverse side should be a collection
				else if ( ! collectionPersister.isOneToMany() && type.isCollectionType() ) {
					matching = isCollectionMatching( (CollectionType) type, collectionPersister.getTableName() );
				}
				if ( matching ) {
					otherSidePropertyName = elementPersister.getPropertyNames()[index];
					break;
				}
			}
		}
		return processOtherSidePropertyName( otherSidePropertyName );
	}

	private String getUnqualifiedRole(CollectionPersister persister) {
		String entity = persister.getOwnerEntityPersister().getEntityName();
		String role = persister.getRole();
		return role.substring( entity.length() + 1 );
	}

	/*
	 * Try and find the inverse association matching from the associated entity
	 * If a match is found, use the other side's association name as role
	 * Otherwise use the table name
	 */
	//TODO we could cache such knowledge in a service if that turns out to be costly
	private String getCollectionRoleFromToOne(OgmEntityPersister associatedPersister) {
		//code logic is slightly duplicated but the input and context is different, hence this choice
		Type[] propertyTypes = associatedPersister.getPropertyTypes();
		String otherSidePropertyName = null;
		for ( int index = 0 ; index <  propertyTypes.length ; index++ ) {
			Type type = propertyTypes[index];
			boolean matching = false;
			//we try and restrict type search as much as possible
			//we look for associations that also are collections
			if ( type.isAssociationType() && type.isCollectionType() ) {
				matching = isCollectionMatching( (CollectionType) type, table );
			}
			//we look for associations that are to-one
			else if ( type.isAssociationType() && ! type.isCollectionType() ) { //isCollectionType redundant but kept for readability
				matching = isToOneMatching( associatedPersister, index, type );
			}
			if ( matching ) {
				otherSidePropertyName = associatedPersister.getPropertyNames()[index];
				break;
			}
		}
		return processOtherSidePropertyName( otherSidePropertyName );
	}

	private boolean isToOneMatching(Loadable elementPersister, int index, Type type) {
		if ( ( (EntityType) type ).isOneToOne() ) {
			// If that's a OneToOne check the associated property name and see if it matches where we come from
			// we need to do that as OneToOne don't define columns
			OneToOneType oneToOneType = (OneToOneType) type;
			String associatedProperty = oneToOneType.getRHSUniqueKeyPropertyName();
			if ( associatedProperty != null ) {
				OgmEntityPersister mainSidePersister = (OgmEntityPersister) oneToOneType.getAssociatedJoinable( sessionFactory );
				try {
					int propertyIndex = mainSidePersister.getPropertyIndex( associatedProperty );
					return mainSidePersister.getPropertyTypes()[propertyIndex] == propertyType;
				}
				catch ( HibernateException e ) {
					//not the right property
					//probably should not happen
				}
			}
		}
		//otherwise we do a key column comparison to see if it matches
		return Arrays.equals( columnNames, elementPersister.getPropertyColumnNames( index ) );
	}

	private boolean isCollectionMatching(CollectionType type, String primarySideTableName) {
		// Find the reverse side collection and check if the table name and key columns are matching
		// what we have on the main side
		String collectionRole = type.getRole();
		CollectionPhysicalModel reverseCollectionPersister = (CollectionPhysicalModel) sessionFactory.getCollectionPersister( collectionRole );
		boolean isSameTable = primarySideTableName.equals( reverseCollectionPersister.getTableName() );
		return isSameTable && Arrays.equals( columnNames, reverseCollectionPersister.getKeyColumnNames() );
	}

	private String processOtherSidePropertyName(String otherSidePropertyName) {
		//if we found the matching property on the reverse side, we are
		//bidirectional, otherwise we are not
		if ( otherSidePropertyName != null ) {
			isBidirectional = Boolean.TRUE;
		}
		else {
			isBidirectional = Boolean.FALSE;
			otherSidePropertyName = table;
		}
		return otherSidePropertyName;
	}
}

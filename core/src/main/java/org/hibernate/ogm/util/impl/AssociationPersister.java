/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2014 Red Hat Inc. and/or its affiliates and other contributors
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

import java.io.Serializable;
import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.impl.PropertyOptionsContext;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.persister.CollectionPhysicalModel;
import org.hibernate.ogm.persister.EntityKeyBuilder;
import org.hibernate.ogm.persister.OgmCollectionPersister;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;

/**
 * Implements the logic for updating associations. Configured in a fluent manner, followed by a call to
 * {@link #flushToCache()} which invokes the given {@link GridDialect} to apply the changes to the datastore.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class AssociationPersister {
	private GridType keyGridType;
	private Object key;
	private SessionImplementor session;
	private AssociationKey associationKey;
	private Association association;
	private Object[] columnValues;
	private GridDialect gridDialect;
	private OgmCollectionPersister collectionPersister;
	private boolean inverse;
	private Type propertyType;
	private AssociationContext associationContext;

	/*
	 * Return true if the other side association has been searched and not been found
	 * The other side association is searched when we are looking forward to update it
	 * and need to build the corresponding association key.
	 *
	 * It uses Boolean instead of boolean to make sure it's used only after being calculated
	 */
	private Boolean isBidirectional;
	private AssociationKeyMetadata associationKeyMetadata;

	/**
	 * The entity type hosting the association, i.e. the entity on this side of the association (not necessarily the
	 * association owner).
	 */
	private final Class<?> hostingEntityType;
	private Object hostingEntity;
	private Boolean hostingEntityRequiresReadAfterUpdate;
	private EntityPersister hostingEntityPersister;

	public AssociationPersister(Class<?> entityType) {
		this.hostingEntityType = entityType;
	}

	//fluent methods for populating data

	public AssociationPersister gridDialect(GridDialect gridDialect) {
		this.gridDialect = gridDialect;
		return this;
	}

	public AssociationPersister keyGridType(GridType keyGridType) {
		this.keyGridType = keyGridType;
		return this;
	}

	public AssociationPersister session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	public AssociationPersister key(Object key) {
		this.key = key;
		return this;
	}

	public AssociationPersister keyColumnValues(Object[] columnValues) {
		this.columnValues = columnValues;
		return this;
	}

	public AssociationPersister inverse() {
		this.inverse = true;
		return this;
	}

	public AssociationPersister hostingEntity(Object entity) {
		this.hostingEntity = entity;
		return this;
	}

	public AssociationPersister collectionPersister(OgmCollectionPersister collectionPersister) {
		this.collectionPersister = collectionPersister;
		return this;
	}

	public AssociationPersister propertyType(Type type) {
		this.propertyType = type;
		return this;
	}

	public AssociationPersister associationKeyMetadata(AssociationKeyMetadata associationKeyMetadata) {
		this.associationKeyMetadata = associationKeyMetadata;
		return this;
	}

	//action methods

	private AssociationKey getAssociationKey() {
		if ( associationKey == null ) {
			final Object[] columnValues = getKeyColumnValues();
			String collectionRole = null;
			EntityKey ownerEntityKey = null;
			AssociationKind associationKind = null;

			// We have a collection on the main side
			if (collectionPersister != null) {
				EntityKey entityKey;
				// we are explicitly looking to update the non owning side
				if ( inverse ) {
					//look for the other side of the collection, build the key of the other side's entity
					OgmEntityPersister elementPersister = (OgmEntityPersister) collectionPersister.getElementPersister();
					entityKey = EntityKeyBuilder.fromPersister(
							elementPersister,
							(Serializable) key,
							session
					);
					collectionRole = buildCollectionRole( collectionPersister );
				}
				else {
					//we are on the right side, use the association property
					collectionRole = getUnqualifiedRole( collectionPersister );
					entityKey = EntityKeyBuilder.fromPersister(
							(OgmEntityPersister) collectionPersister.getOwnerEntityPersister(),
							(Serializable) key,
							session
					);
				}
				ownerEntityKey = entityKey;
				//TODO add information on the collection type, set, map, bag, list etc

				AssociationKind type = collectionPersister.getElementType().isEntityType() ? AssociationKind.ASSOCIATION : AssociationKind.EMBEDDED_COLLECTION;
				associationKind = type;
			}
			// We have a to-one on the main side
			else if ( propertyType != null ) {
				associationKind = propertyType.isEntityType() ? AssociationKind.ASSOCIATION : AssociationKind.EMBEDDED_COLLECTION;
				if ( propertyType instanceof EntityType ) {
					EntityType entityType = (EntityType) propertyType;
					OgmEntityPersister associatedPersister = (OgmEntityPersister) entityType.getAssociatedJoinable( session.getFactory() );
					EntityKey entityKey = new EntityKey(
							associatedPersister.getEntityKeyMetadata(),
							columnValues
					);
					ownerEntityKey = entityKey;
					collectionRole = getCollectionRoleFromToOne( associatedPersister );
				}
				else {
					throw new AssertionFailure( "Cannot detect associated entity metadata. propertyType is of unexpected type: " + propertyType.getClass() );
				}
			}
			else {
				throw new AssertionFailure( "Cannot detect associated entity metadata: collectionPersister and propertyType are both null" );
			}

			associationKey = new AssociationKey( associationKeyMetadata, columnValues, collectionRole, ownerEntityKey, associationKind );
		}

		return associationKey;
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
				matching = isCollectionMatching( (CollectionType) type, associationKeyMetadata.getTable() );
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

	private boolean isCollectionMatching(CollectionType type, String primarySideTableName) {
		// Find the reverse side collection and check if the table name and key columns are matching
		// what we have on the main side
		String collectionRole = type.getRole();
		CollectionPhysicalModel reverseCollectionPersister = (CollectionPhysicalModel) session.getFactory().getCollectionPersister( collectionRole );
		boolean isSameTable = primarySideTableName.equals( reverseCollectionPersister.getTableName() );
		return isSameTable && Arrays.equals( associationKeyMetadata.getColumnNames(), reverseCollectionPersister.getKeyColumnNames() );
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

	private boolean isToOneMatching(Loadable elementPersister, int index, Type type) {
		if ( ( (EntityType) type ).isOneToOne() ) {
			// If that's a OneToOne check the associated property name and see if it matches where we come from
			// we need to do that as OneToOne don't define columns
			OneToOneType oneToOneType = (OneToOneType) type;
			String associatedProperty = oneToOneType.getRHSUniqueKeyPropertyName();
			if ( associatedProperty != null ) {
				OgmEntityPersister mainSidePersister = (OgmEntityPersister) oneToOneType.getAssociatedJoinable( session.getFactory() );
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
		return Arrays.equals( associationKeyMetadata.getColumnNames(), elementPersister.getPropertyColumnNames( index ) );
	}

	private String processOtherSidePropertyName(String otherSidePropertyName) {
		//if we found the matching property on the reverse side, we are
		//bidirectional, otherwise we are not
		if ( otherSidePropertyName != null ) {
			isBidirectional = Boolean.TRUE;
		}
		else {
			isBidirectional = Boolean.FALSE;
			otherSidePropertyName = associationKeyMetadata.getTable();
		}
		return otherSidePropertyName;
	}

	private String getUnqualifiedRole(CollectionPersister persister) {
		String entity = persister.getOwnerEntityPersister().getEntityName();
		String role = persister.getRole();
		return role.substring( entity.length() + 1 );
	}

	private Object[] getKeyColumnValues() {
		if ( columnValues == null ) {
			columnValues = LogicalPhysicalConverterHelper.getColumnsValuesFromObjectValue(
					key, keyGridType, associationKeyMetadata.getColumnNames(), session
			);
		}
		return columnValues;
	}

	public Tuple createAndPutAssociationTuple(RowKey rowKey) {
		Tuple associationTuple = gridDialect.createTupleAssociation( getAssociationKey(), rowKey );
		getAssociation().put( rowKey, associationTuple);
		return associationTuple;
	}

	/*
	 * Load a collection and create it if it is not found
	 */
	public Association getAssociation() {
		if ( association == null ) {
			// Compute bi-directionality first
			AssociationKey key = getAssociationKey();
			if ( isBidirectional == Boolean.FALSE ) {
				//fake association to prevent unidirectional associations to keep record of the inverse side
				association = new Association();
			}
			else {
				association = gridDialect.getAssociation( key, getAssociationContext() );
				if (association == null) {
					association = gridDialect.createAssociation( key, getAssociationContext() );
				}
			}
		}
		return association;
	}

	/*
	 * Does not create a collection if it is not found
	 */
	public Association getAssociationOrNull() {
		if ( association == null ) {
			association = gridDialect.getAssociation( getAssociationKey(), getAssociationContext() );
		}
		return association;
	}

	public void flushToCache() {
		//If we don't have a bidirectional association, do not update the info
		//to prevent unidirectional associations to keep record of the inverse side
		if ( isBidirectional != Boolean.FALSE ) {
			if ( getAssociation().isEmpty() ) {
				gridDialect.removeAssociation( getAssociationKey(), getAssociationContext() );
				association = null;
			}
			else {
				gridDialect.updateAssociation( getAssociation(), getAssociationKey(), getAssociationContext() );
			}


			updateHostingEntityIfRequired();
		}
	}

	/**
	 * Reads the entity hosting the association from the datastore and applies any property changes from the server
	 * side.
	 */
	private void updateHostingEntityIfRequired() {
		if ( hostingEntity != null && hostingEntityRequiresReadAfterUpdate() ) {
			EntityPersister entityPersister = getHostingEntityPersister();

			entityPersister.processUpdateGeneratedProperties(
					entityPersister.getIdentifier( hostingEntity, session ),
					hostingEntity,
					new Object[entityPersister.getPropertyNames().length],
					session
			);
		}
	}

	/**
	 * Whether the association in question is stored within an entity structure ("embedded") and this entity has
	 * properties whose value is generated in the datastore (such as a version attribute) or not.
	 *
	 * @param metadataProvider persister of the association
	 * @param ownerPersister persister of the owning entity
	 * @return {@code true} in case the represented association is stored within an entity which has server-generated
	 * properties, and thus must be re-read after an update to the association, {@code false} otherwise.
	 */
	public boolean hostingEntityRequiresReadAfterUpdate() {
		if ( hostingEntityRequiresReadAfterUpdate == null ) {
			boolean storedInEntityStructure = gridDialect.isStoredInEntityStructure( getAssociationKey(), getAssociationContext() );
			boolean hasUpdateGeneratedProperties = getHostingEntityPersister().hasUpdateGeneratedProperties();

			hostingEntityRequiresReadAfterUpdate = storedInEntityStructure && hasUpdateGeneratedProperties;
		}

		return hostingEntityRequiresReadAfterUpdate;
	}

	private EntityPersister getHostingEntityPersister() {
		if ( hostingEntityPersister == null ) {
			hostingEntityPersister = session.getFactory().getEntityPersister( hostingEntityType.getName() );
		}

		return hostingEntityPersister;
	}

	public AssociationContext getAssociationContext() {
		if ( associationContext == null ) {
			OptionsServiceContext serviceContext = session.getFactory()
					.getServiceRegistry()
					.getService( OptionsService.class )
					.context();

			associationContext = new AssociationContext(
					new PropertyOptionsContext( serviceContext, hostingEntityType, getAssociationKey().getCollectionRole() )
			);
		}

		return associationContext;
	}
}

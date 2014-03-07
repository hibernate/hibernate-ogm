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

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationTypeContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.persister.EntityKeyBuilder;
import org.hibernate.ogm.persister.OgmCollectionPersister;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.EntityType;
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
			EntityKey ownerEntityKey = null;

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
				}
				else {
					//we are on the right side, use the association property
					entityKey = EntityKeyBuilder.fromPersister(
							(OgmEntityPersister) collectionPersister.getOwnerEntityPersister(),
							(Serializable) key,
							session
					);
				}
				ownerEntityKey = entityKey;
				//TODO add information on the collection type, set, map, bag, list etc
			}
			// We have a to-one on the main side
			else if ( propertyType != null ) {
				if ( propertyType instanceof EntityType ) {
					EntityType entityType = (EntityType) propertyType;
					OgmEntityPersister associatedPersister = (OgmEntityPersister) entityType.getAssociatedJoinable( session.getFactory() );
					EntityKey entityKey = new EntityKey(
							associatedPersister.getEntityKeyMetadata(),
							columnValues
					);
					ownerEntityKey = entityKey;
				}
				else {
					throw new AssertionFailure( "Cannot detect associated entity metadata. propertyType is of unexpected type: " + propertyType.getClass() );
				}
			}
			else {
				throw new AssertionFailure( "Cannot detect associated entity metadata: collectionPersister and propertyType are both null" );
			}

			associationKey = new AssociationKey( associationKeyMetadata, columnValues, ownerEntityKey );
		}

		return associationKey;
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
			if ( associationKeyMetadata.getIsBidirectional() == Boolean.FALSE ) {
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
		if ( associationKeyMetadata.getIsBidirectional() != Boolean.FALSE ) {
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
	 * @return {@code true} in case the represented association is stored within an entity which has server-generated
	 * properties, and thus must be re-read after an update to the association, {@code false} otherwise.
	 */
	public boolean hostingEntityRequiresReadAfterUpdate() {
		if ( hostingEntityRequiresReadAfterUpdate == null ) {
			boolean storedInEntityStructure = gridDialect.isStoredInEntityStructure( associationKeyMetadata, getAssociationContext() );
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
					new AssociationTypeContext( serviceContext.getPropertyOptions( hostingEntityType, getAssociationKey().getCollectionRole() ) ),
					OgmSession.getSessionStore().getSessionContext()
			);
		}

		return associationContext;
	}
}

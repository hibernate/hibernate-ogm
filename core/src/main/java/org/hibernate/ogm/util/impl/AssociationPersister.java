/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.io.Serializable;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.impl.AssociationContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Implements the logic for updating associations. Configured in a fluent manner, followed by a call to
 * {@link #flushToDatastore()} which invokes the given {@link GridDialect} to apply the changes to the datastore.
 * <p>
 * Unlike ORM style persisters, this class is tied to a specific association instance with specific ids. In other words,
 * instances cannot be shared by {@link SessionFactory} level objects like {@link EntityPersister}.
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
	private AssociationContext associationContext;
	private AssociationTypeContext associationTypeContext;
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

	public AssociationPersister session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	// one of the following two methods is to be invoked, not both

	public AssociationPersister key(Object key, GridType keyGridType) {
		this.key = key;
		this.keyGridType = keyGridType;
		return this;
	}

	public AssociationPersister keyColumnValues(Object[] columnValues) {
		this.columnValues = columnValues;
		return this;
	}

	public AssociationPersister hostingEntity(Object entity) {
		this.hostingEntity = entity;
		return this;
	}

	public AssociationPersister associationTypeContext(AssociationTypeContext associationTypeContext) {
		this.associationTypeContext = associationTypeContext;
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

			EntityKey ownerEntityKey;
			if ( key != null ) {
				ownerEntityKey = EntityKeyBuilder.fromPersister(
						(OgmEntityPersister) getHostingEntityPersister(),
						(Serializable) key,
						session );
			}
			else {
				ownerEntityKey  = new EntityKey(
						((OgmEntityPersister) getHostingEntityPersister()).getEntityKeyMetadata(),
						columnValues
				);
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

	/*
	 * Load a collection and create it if it is not found
	 */
	public Association getAssociation() {
		if ( association == null ) {
			AssociationKey key = getAssociationKey();
			association = gridDialect.getAssociation( key, getAssociationContext() );
			if (association == null) {
				association = gridDialect.createAssociation( key, getAssociationContext() );
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

	/**
	 * Writes out the changes gathered in the {@link Association} managed by this persister to the datastore.
	 */
	public void flushToDatastore() {
		if ( getAssociation().isEmpty() ) {
			gridDialect.removeAssociation( getAssociationKey(), getAssociationContext() );
			association = null;
		}
		else {
			gridDialect.insertOrUpdateAssociation( getAssociationKey(), getAssociation(), getAssociationContext() );
		}

		updateHostingEntityIfRequired();
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
			boolean storedInEntityStructure = gridDialect.isStoredInEntityStructure( associationKeyMetadata, associationTypeContext );
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

	/**
	 * Returns an {@link AssociationContext} to be passed to {@link GridDialect} operations targeting the association
	 * managed by this persister.
	 */
	private AssociationContext getAssociationContext() {
		if ( associationContext == null ) {
			associationContext = new AssociationContextImpl(
					associationTypeContext,
					hostingEntity != null ? OgmEntityEntryState.getStateFor( session, hostingEntity ).getTuple() : null
			);
		}

		return associationContext;
	}
}

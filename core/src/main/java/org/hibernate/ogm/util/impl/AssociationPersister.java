/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import static org.hibernate.ogm.util.impl.TransactionContextHelper.transactionContext;

import java.io.Serializable;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.impl.AssociationContextImpl;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
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
	private SharedSessionContractImplementor session;
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

	private AssociationPersister(Builder builder) {
		this.hostingEntityType = builder.targetEntityType;
		this.hostingEntity = builder.hostingEntity;
		this.gridDialect = builder.gridDialect;
		this.session = builder.session;
		this.key = builder.key;
		this.keyGridType = builder.keyGridType;
		this.associationTypeContext = builder.associationTypeContext;
		this.associationKeyMetadata = builder.associationKeyMetadata;
		this.columnValues = builder.keyColumnValues;
	}

	public static class Builder {

		public Class<?> targetEntityType;
		//fluent methods for populating data

		public AssociationKeyMetadata associationKeyMetadata;
		public AssociationTypeContext associationTypeContext;
		public GridType keyGridType;
		public Object key;
		public SharedSessionContractImplementor session;
		public GridDialect gridDialect;
		public Object hostingEntity;

		private Object[] keyColumnValues;

		public Builder(Class<?> targetEntityType) {
			this.targetEntityType = targetEntityType;
		}

		public Builder gridDialect(GridDialect gridDialect) {
			this.gridDialect = gridDialect;
			return this;
		}

		public Builder session(SharedSessionContractImplementor session) {
			this.session = session;
			return this;
		}

		// one of the following two methods is to be invoked, not both

		public Builder key(Object key, GridType keyGridType) {
			this.key = key;
			this.keyGridType = keyGridType;
			return this;
		}

		public Builder keyColumnValues(Object[] columnValues) {
			this.keyColumnValues = columnValues;
			return this;
		}

		public Builder hostingEntity(Object hostingEntity) {
			this.hostingEntity = hostingEntity;
			return this;
		}

		public Builder associationTypeContext(AssociationTypeContext associationTypeContext) {
			this.associationTypeContext = associationTypeContext;
			return this;
		}

		public Builder associationKeyMetadata(AssociationKeyMetadata associationKeyMetadata) {
			this.associationKeyMetadata = associationKeyMetadata;
			return this;
		}

		public AssociationPersister build() {
			return new AssociationPersister( this );
		}
	}

	//action methods

	public AssociationKey getAssociationKey() {
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
						( (OgmEntityPersister) getHostingEntityPersister() ).getEntityKeyMetadata(),
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
	 * Load an association and create it if it is not found
	 */
	public Association getAssociation() {
		if ( association == null ) {
			AssociationKey key = getAssociationKey();

			if ( hostingEntity != null ) {
				OgmEntityEntryState entryState = OgmEntityEntryState.getStateFor( session, hostingEntity );
				if ( entryState.hasAssociation( associationKeyMetadata.getCollectionRole() ) ) {
					association = entryState.getAssociation( associationKeyMetadata.getCollectionRole() );
				}
				else {
					association = gridDialect.getAssociation( key, getAssociationContext() );
					OgmEntityEntryState.getStateFor( session, hostingEntity )
							.setAssociation( associationKeyMetadata.getCollectionRole(), association );
				}
			}
			else {
				association = gridDialect.getAssociation( key, getAssociationContext() );
			}

			if ( association == null ) {
				association = gridDialect.createAssociation( key, getAssociationContext() );
				if ( hostingEntity != null ) {
					OgmEntityEntryState.getStateFor( session, hostingEntity )
							.setAssociation( associationKeyMetadata.getCollectionRole(), association );
				}
			}
		}
		return association;
	}

	/*
	 * Does not create an association if it is not found
	 */
	public Association getAssociationOrNull() {
		if ( association == null ) {
			if ( hostingEntity != null ) {
				OgmEntityEntryState entryState = OgmEntityEntryState.getStateFor( session, hostingEntity );
				if ( entryState.hasAssociation( associationKeyMetadata.getCollectionRole() ) ) {
					association = entryState.getAssociation( associationKeyMetadata.getCollectionRole() );
					return association;
				}
			}

			if ( association == null ) {
				association = gridDialect.getAssociation( getAssociationKey(), getAssociationContext() );
				if ( hostingEntity != null ) {
					OgmEntityEntryState.getStateFor( session, hostingEntity )
							.setAssociation( associationKeyMetadata.getCollectionRole(), association );
				}
			}
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
			OgmEntityEntryState.getStateFor( session, hostingEntity ).setAssociation( associationKeyMetadata.getCollectionRole(), null );
		}
		else if ( !getAssociation().getOperations().isEmpty() ) {
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
			OgmEntityPersister entityPersister = getHostingEntityPersister();

			if ( GridDialects.hasFacet( gridDialect, GroupingByEntityDialect.class ) ) {
				( (GroupingByEntityDialect) gridDialect ).flushPendingOperations( getAssociationKey().getEntityKey(),
						entityPersister.getTupleContext( session ) );
			}

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

	private OgmEntityPersister getHostingEntityPersister() {
		if ( hostingEntityPersister == null ) {
			hostingEntityPersister = session.getFactory().getMetamodel().entityPersister( hostingEntityType );
		}

		return (OgmEntityPersister) hostingEntityPersister;
	}

	/**
	 * Returns an {@link AssociationContext} to be passed to {@link GridDialect} operations targeting the association
	 * managed by this persister.
	 */
	public AssociationContext getAssociationContext() {
		if ( associationContext == null ) {
			associationContext = new AssociationContextImpl(
					associationTypeContext,
					hostingEntity != null ? OgmEntityEntryState.getStateFor( session, hostingEntity ).getTuplePointer() : new TuplePointer(),
					transactionContext( session )
			);
		}

		return associationContext;
	}
}

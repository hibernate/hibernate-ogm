/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.dialect.impl.AssociationTypeContextImpl;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.RowKeyBuilder;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.util.impl.LogicalPhysicalConverterHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Updates the inverse side of bi-directional many-to-one/one-to-one associations, managed by the entity on the main
 * side.
 * <p>
 * Tied to one specific entity on the main side, so instances of this class must not be cached or re-used.
 *
 * @author Gunnar Morling
 * @author Emmanuel Bernard
 */
class EntityAssociationUpdater {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final OgmEntityPersister persister;

	private final GridDialect gridDialect;
	private Tuple resultset;
	private int tableIndex;
	private Serializable id;
	private SharedSessionContractImplementor session;
	private boolean[] propertyMightRequireInverseAssociationManagement;

	EntityAssociationUpdater(OgmEntityPersister persister) {
		this.persister = persister;
		this.gridDialect = persister.getFactory().getServiceRegistry().getService( GridDialect.class );
	}

	// fluent methods populating data

	/**
	 * Sets the tuple representing the entity whose inverse associations should be updated.
	 */
	public EntityAssociationUpdater resultset(Tuple resultset) {
		this.resultset = resultset;
		return this;
	}

	public EntityAssociationUpdater tableIndex(int tableIndex) {
		this.tableIndex = tableIndex;
		return this;
	}

	public EntityAssociationUpdater id(Serializable id) {
		this.id = id;
		return this;
	}

	public EntityAssociationUpdater session(SharedSessionContractImplementor session) {
		this.session = session;
		return this;
	}

	public EntityAssociationUpdater propertyMightRequireInverseAssociationManagement(boolean[] propertyInverseAssociationManagementMayBeRequired) {
		this.propertyMightRequireInverseAssociationManagement = propertyInverseAssociationManagementMayBeRequired;
		return this;
	}

	//action methods

	/**
	 * Updates all inverse associations managed by a given entity.
	 */
	public void addNavigationalInformationForInverseSide() {
		if ( log.isTraceEnabled() ) {
			log.trace( "Adding inverse navigational information for entity: " + MessageHelper.infoString( persister, id, persister.getFactory() ) );
		}

		for ( int propertyIndex = 0; propertyIndex < persister.getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			if ( persister.isPropertyOfTable( propertyIndex, tableIndex ) ) {
				AssociationKeyMetadata associationKeyMetadata = getInverseAssociationKeyMetadata( propertyIndex );

				// there is no inverse association for the given property
				if ( associationKeyMetadata == null ) {
					continue;
				}

				Object[] newColumnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset(
						resultset,
						persister.getPropertyColumnNames( propertyIndex )
				);

				//don't index null columns, this means no association
				if ( ! CollectionHelper.isEmptyOrContainsOnlyNull( ( newColumnValues ) ) ) {
					addNavigationalInformationForInverseSide( propertyIndex, associationKeyMetadata, newColumnValues );
				}
			}
		}
	}

	/**
	 * Removes all inverse associations managed by a given entity.
	 */
	public void removeNavigationalInformationFromInverseSide() {
		if ( log.isTraceEnabled() ) {
			log.trace( "Removing inverse navigational information for entity: " + MessageHelper.infoString( persister, id, persister.getFactory() ) );
		}

		for ( int propertyIndex = 0; propertyIndex < persister.getEntityMetamodel().getPropertySpan(); propertyIndex++ ) {
			if ( persister.isPropertyOfTable( propertyIndex, tableIndex ) ) {


				AssociationKeyMetadata associationKeyMetadata = getInverseAssociationKeyMetadata( propertyIndex );

				// there is no inverse association for the given property
				if ( associationKeyMetadata == null ) {
					continue;
				}

				Object[] oldColumnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset(
						resultset,
						persister.getPropertyColumnNames( propertyIndex )
				);

				//don't index null columns, this means no association
				if ( ! CollectionHelper.isEmptyOrContainsOnlyNull( oldColumnValues ) ) {
					removeNavigationalInformationFromInverseSide( propertyIndex, associationKeyMetadata, oldColumnValues );
				}
			}
		}
	}

	private void addNavigationalInformationForInverseSide(int propertyIndex, AssociationKeyMetadata associationKeyMetadata, Object[] newColumnValue) {

		RowKey rowKey = getInverseRowKey( associationKeyMetadata, newColumnValue );

		Tuple associationRow = new Tuple();
		for ( String column : rowKey.getColumnNames() ) {
			associationRow.put( column, rowKey.getColumnValue( column ) );
		}

		AssociationPersister associationPersister = createInverseAssociationPersister( propertyIndex, associationKeyMetadata, newColumnValue );
		associationPersister.getAssociation().put( rowKey, associationRow );

		if ( associationPersister.getAssociationContext().getEntityTuplePointer().getTuple() == null ) {
			throw log.entityTupleNotFound( associationKeyMetadata.getCollectionRole(), associationPersister.getAssociationKey().getEntityKey() );
		}

		associationPersister.flushToDatastore();
	}

	private void removeNavigationalInformationFromInverseSide(int propertyIndex, AssociationKeyMetadata associationKeyMetadata, Object[] oldColumnValue) {
		AssociationPersister associationPersister = createInverseAssociationPersister( propertyIndex, associationKeyMetadata, oldColumnValue );

		Association association = associationPersister.getAssociationOrNull();

		// The association might be empty if the navigation information have already been removed.
		// This typically happens when the entity owning the inverse association has already been deleted prior to
		// deleting the entity owning the association and a {@code @NotFound(action = NotFoundAction.IGNORE)} is
		// involved.
		if ( association != null && !association.isEmpty() ) {
			RowKey rowKey = getInverseRowKey( associationKeyMetadata, oldColumnValue );
			association.remove( rowKey );
			associationPersister.flushToDatastore();
		}
	}

	private AssociationPersister createInverseAssociationPersister(int propertyIndex, AssociationKeyMetadata associationKeyMetadata, Object[] keyColumnValues) {
		OptionsServiceContext serviceContext = session.getFactory()
				.getServiceRegistry()
				.getService( OptionsService.class )
				.context();

		Class<?> entityType = persister.getPropertyTypes()[propertyIndex].getReturnedClass();
		OgmEntityPersister inverseEntityPersister = (OgmEntityPersister) persister.getFactory().getMetamodel().entityPersister( entityType );

		String mainSidePropertyName = persister.getPropertyNames()[propertyIndex];

		AssociationTypeContext associationTypeContext = new AssociationTypeContextImpl.Builder( serviceContext )
				.associationKeyMetadata( associationKeyMetadata )
				.hostingEntityPersister( inverseEntityPersister )
				.mainSidePropertyName( mainSidePropertyName )
				.build();

		return new AssociationPersister.Builder(
					persister.getPropertyTypes()[propertyIndex].getReturnedClass()
				)
				.hostingEntity( getReferencedEntity( propertyIndex ) )
				.gridDialect( gridDialect )
				.associationKeyMetadata( associationKeyMetadata )
				.keyColumnValues( keyColumnValues )
				.session( session )
				.associationTypeContext( associationTypeContext )
				.build();
	}

	/**
	 * Returns the object referenced by the specified property (which represents an association).
	 */
	private Object getReferencedEntity(int propertyIndex) {
		Object referencedEntity = null;

		GridType propertyType = persister.getGridPropertyTypes()[propertyIndex];
		Serializable id = (Serializable) propertyType.hydrate(
				resultset, persister.getPropertyColumnNames( propertyIndex ), session, null
		);

		if ( id != null ) {
			EntityPersister hostingEntityPersister = session.getFactory().getMetamodel().entityPersister(
					propertyType.getReturnedClass()
			);

			// OGM-931 Loading the entity through Session#get() to make sure it is fetched from the store if it is
			// detached atm. but also to benefit from 2L caching etc.
			Class<?> entityType = hostingEntityPersister.getMappedClass();
			referencedEntity = ( (Session) session ).get( entityType, id );

			// In case the entity is scheduled for deletion, obtain it from the PC
			if ( referencedEntity == null ) {
				referencedEntity = session.getPersistenceContext().getEntity(
					session.generateEntityKey( id, hostingEntityPersister )
				);
			}
		}

		return referencedEntity;
	}

	/**
	 * Gets the row key of the inverse association represented by the given meta-data, pointing the entity with current
	 * {@link EntityAssociationUpdater#id}
	 *
	 * @param associationKeyMetadata meta-data for the inverse association of interest
	 * @param associationColumnValues the column values identifying the entity on the inverse side of the association
	 * @return the row key of the inverse association
	 */
	private RowKey getInverseRowKey(AssociationKeyMetadata associationKeyMetadata, Object[] associationColumnValues) {
		Tuple rowKeyValues = new Tuple();

		// add the fk column
		for ( int index = 0; index < associationKeyMetadata.getColumnNames().length; index++ ) {
			rowKeyValues.put( associationKeyMetadata.getColumnNames()[index], associationColumnValues[index] );
		}

		// add the id column
		persister.getGridIdentifierType().nullSafeSet( rowKeyValues, id, persister.getIdentifierColumnNames(), session );

		return new RowKeyBuilder()
			.addColumns( associationKeyMetadata.getRowKeyColumnNames() )
			.values( rowKeyValues )
			.build();
	}

	private AssociationKeyMetadata getInverseAssociationKeyMetadata(int propertyIndex) {
		// a quick test for excluding properties which for sure don't manage an inverse association
		if ( !propertyMightRequireInverseAssociationManagement[propertyIndex] ) {
			return null;
		}

		return BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( persister, propertyIndex );
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.RowKeyBuilder;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.LogicalPhysicalConverterHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.tuple.entity.EntityMetamodel;

/**
 * Updates the inverse side of bi-directional many-to-one/one-to-one associations, managed by the entity on the main
 * side.
 *
 * @author Gunnar Morling
 * @author Emmanuel Bernard
 */
class EntityAssociationUpdater {

	private static final Log log = LoggerFactory.make();

	private final OgmEntityPersister persister;
	private final GridDialect gridDialect;
	private Tuple resultset;
	private Object[] fields;
	private int tableIndex;
	private Serializable id;
	private SessionImplementor session;

	private boolean[] propertyInverseAssociationManagementMayBeRequired;

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

	public EntityAssociationUpdater fields(Object[] fields) {
		this.fields = fields;
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

	public EntityAssociationUpdater session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	public EntityAssociationUpdater propertyInverseAssociationManagementMayBeRequired(boolean[] propertyInverseAssociationManagementMayBeRequired) {
		this.propertyInverseAssociationManagementMayBeRequired = propertyInverseAssociationManagementMayBeRequired;
		return this;
	}

	//action methods

	/**
	 * Updates all inverse associations managed by a given entity.
	 */
	public void addNavigationalInformationForReverseSide() {
		if ( log.isTraceEnabled() ) {
			log.trace( "Adding inverse navigational information for entity: " + MessageHelper.infoString( persister, id, persister.getFactory() ) );
		}
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		for ( int propertyIndex = 0; propertyIndex < entityMetamodel.getPropertySpan(); propertyIndex++ ) {
			if ( persister.isPropertyOfTable( propertyIndex, tableIndex ) ) {
				if ( propertyInverseAssociationManagementMayBeRequired[propertyIndex] ) {
					//add to property cache
					Object[] newColumnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset(
							resultset,
							persister.getPropertyColumnNames( propertyIndex )
					);
					//don't index null columns, this means no association
					if ( ! CollectionHelper.isEmptyOrContainsOnlyNull( ( newColumnValues  ) ) ) {
						addNavigationalInformationForReverseSide(
								propertyIndex,
								newColumnValues);
					}
				}
			}
		}
	}

	/**
	 * Removes all inverse associations managed by a given entity.
	 */
	public void removeNavigationalInformationFromReverseSide() {
		if ( log.isTraceEnabled() ) {
			log.trace( "Removing inverse navigational information for entity: " + MessageHelper.infoString( persister, id, persister.getFactory() ) );
		}
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		for ( int propertyIndex = 0; propertyIndex < entityMetamodel.getPropertySpan(); propertyIndex++ ) {
			if ( persister.isPropertyOfTable( propertyIndex, tableIndex ) ) {
				if ( propertyInverseAssociationManagementMayBeRequired[propertyIndex] ) {
					//remove from property cache
					Object[] oldColumnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset(
							resultset,
							persister.getPropertyColumnNames( propertyIndex )
					);

					//don't index null columns, this means no association
					if ( ! CollectionHelper.isEmptyOrContainsOnlyNull( oldColumnValues ) ) {
						removeNavigationalInformationFromReverseSide(
								propertyIndex,
								oldColumnValues);
					}
				}
			}
		}
	}

	private void addNavigationalInformationForReverseSide(int propertyIndex, Object[] newColumnValue) {
		AssociationKeyMetadata associationKeyMetadata = BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( persister, propertyIndex );

		// there is no inverse association for the given property
		if ( associationKeyMetadata == null ) {
			return;
		}

		AssociationPersister associationPersister = new AssociationPersister(
					persister.getPropertyTypes()[propertyIndex].getReturnedClass()
				)
				.hostingEntity( getReferencedEntity( propertyIndex ) )
				.gridDialect( gridDialect )
				.associationKeyMetadata(  associationKeyMetadata )
				.keyColumnValues( newColumnValue )
				.session( session )
				.roleOnMainSide( persister.getPropertyNames()[propertyIndex] );

		Tuple tuple = new Tuple();
		//add the id column
		final String[] identifierColumnNames = persister.getIdentifierColumnNames();
		persister.getGridIdentifierType().nullSafeSet( tuple, id, identifierColumnNames, session );
		//add the fk column
		persister.getGridPropertyTypes()[propertyIndex].nullSafeSet(
							tuple,
							fields[propertyIndex],
							associationKeyMetadata.getColumnNames(),
							persister.getPropertyColumnInsertable()[propertyIndex],
							session
					);

		Object[] columnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset( tuple, associationKeyMetadata.getRowKeyColumnNames() );
		final RowKey rowKey = new RowKey( associationKeyMetadata.getRowKeyColumnNames(), columnValues );

		Tuple assocEntryTuple = associationPersister.createAndPutAssociationTuple( rowKey );
		for ( String column : tuple.getColumnNames() ) {
			assocEntryTuple.put( column, tuple.get( column ) );
		}

		associationPersister.flushToDatastore();
	}

	private void removeNavigationalInformationFromReverseSide(int propertyIndex, Object[] oldColumnValue) {
		AssociationKeyMetadata associationKeyMetadata = BiDirectionalAssociationHelper.getInverseAssociationKeyMetadata( persister, propertyIndex );

		// there is no inverse association for the given property
		if ( associationKeyMetadata == null ) {
			return;
		}

		AssociationPersister associationPersister = new AssociationPersister(
					persister.getPropertyTypes()[propertyIndex].getReturnedClass()
				)
				.hostingEntity( getReferencedEntity( propertyIndex ) )
				.gridDialect( gridDialect )
				.associationKeyMetadata( associationKeyMetadata )
				.keyColumnValues( oldColumnValue )
				.session( session )
				.roleOnMainSide( persister.getPropertyNames()[propertyIndex] );

		//add fk column value in TupleKey
		Tuple tupleKey = new Tuple();
		for (int index = 0 ; index < associationKeyMetadata.getColumnNames().length ; index++) {
			tupleKey.put( associationKeyMetadata.getColumnNames()[index], oldColumnValue[index] );
		}
		//add id value in TupleKey
		persister.getGridIdentifierType().nullSafeSet( tupleKey, id, persister.getIdentifierColumnNames(), session );

		Association propertyValues = associationPersister.getAssociation();
		if ( propertyValues != null ) {
			//Map's equals operation delegates to all it's key and value, should be fine for now
			//this is a StarToOne case ie the FK is on the owning entity
			final RowKey matchingTuple = new RowKeyBuilder()
					.addColumns( associationKeyMetadata.getRowKeyColumnNames() )
					.values( tupleKey )
					.build();
			//TODO what should we do if that's null?
			associationPersister.getAssociation().remove( matchingTuple );

			associationPersister.flushToDatastore();
		}
	}

	/**
	 * Returns the object referenced by the specified property (which represents an association).
	 */
	private Object getReferencedEntity(int propertyIndex) {
		GridType propertyType = persister.getGridPropertyTypes()[propertyIndex];

		Serializable id = (Serializable) propertyType.hydrate(
				resultset, persister.getPropertyColumnNames( propertyIndex ), session, null
		);

		if ( id != null ) {
			EntityPersister hostingEntityPersister = session.getFactory().getEntityPersister(
					propertyType.getReturnedClass().getName()
			);

			return session.getPersistenceContext().getEntity(
					session.generateEntityKey( id, hostingEntityPersister )
			);
		}

		return null;
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister;

import static org.hibernate.ogm.util.impl.ArrayHelper.EMPTY_STRING_ARRAY;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.grid.impl.RowKeyBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.impl.AssociationPersister;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.LogicalPhysicalConverterHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.Type;

class EntityDehydrator {

	private static final Log log = LoggerFactory.make();

	private Tuple resultset;
	private Object[] fields;
	private boolean[] includeProperties;
	private boolean[][] includeColumns;
	private int tableIndex;
	private Serializable id;
	private SessionImplementor session;
	private GridType[] gridPropertyTypes;
	private OgmEntityPersister persister;
	private boolean addPropertyMetadata = true;
	private boolean dehydrate = true;
	private boolean removePropertyMetadata = true;
	private GridType gridIdentifierType;
	private GridDialect gridDialect;

	// fluent methods populating data

	public EntityDehydrator persister(OgmEntityPersister persister) {
		this.persister = persister;
		return this;
	}

	public EntityDehydrator gridPropertyTypes(GridType[] gridPropertyTypes) {
		this.gridPropertyTypes = gridPropertyTypes;
		return this;
	}

	public EntityDehydrator gridIdentifierType(GridType gridIdentifierType) {
		this.gridIdentifierType = gridIdentifierType;
		return this;
	}

	public EntityDehydrator resultset(Tuple resultset) {
		this.resultset = resultset;
		return this;
	}

	public EntityDehydrator fields(Object[] fields) {
		this.fields = fields;
		return this;
	}

	public EntityDehydrator includeProperties(boolean[] includeProperties) {
		this.includeProperties = includeProperties;
		return this;
	}

	public EntityDehydrator includeColumns(boolean[][] includeColumns) {
		this.includeColumns = includeColumns;
		return this;
	}

	public EntityDehydrator tableIndex(int tableIndex) {
		this.tableIndex = tableIndex;
		return this;
	}

	public EntityDehydrator id(Serializable id) {
		this.id = id;
		return this;
	}

	public EntityDehydrator session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	public EntityDehydrator gridDialect(GridDialect gridDialect) {
		this.gridDialect = gridDialect;
		return this;
	}

	//action methods

	public EntityDehydrator onlyRemovePropertyMetadata() {
		this.addPropertyMetadata = false;
		this.dehydrate = false;
		this.removePropertyMetadata = true;
		return this;
	}

	public void dehydrate() {
		if ( log.isTraceEnabled() ) {
			log.trace( "Dehydrating entity: " + MessageHelper.infoString( persister, id, persister.getFactory() ) );
		}
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		final boolean[] uniqueness = persister.getPropertyUniqueness();
		final Type[] propertyTypes = persister.getPropertyTypes();
		for ( int propertyIndex = 0; propertyIndex < entityMetamodel.getPropertySpan(); propertyIndex++ ) {
			if ( persister.isPropertyOfTable( propertyIndex, tableIndex ) ) {
				final Type propertyType = propertyTypes[propertyIndex];
				boolean isStarToOne = propertyType.isAssociationType() && ! propertyType.isCollectionType();
				final boolean createMetadata = isStarToOne || uniqueness[propertyIndex];
				if ( removePropertyMetadata && createMetadata ) {
					//remove from property cache
					Object[] oldColumnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset(
							resultset,
							persister.getPropertyColumnNames( propertyIndex )
					);

					//don't index null columns, this means no association
					if ( ! isEmptyOrAllColumnsNull( oldColumnValues ) ) {
						doRemovePropertyMetadata(
								tableIndex,
								propertyIndex,
								oldColumnValues);
					}
				}

				if ( dehydrate && includeProperties[propertyIndex] ) {
					//dehydrate
					gridPropertyTypes[propertyIndex].nullSafeSet(
							resultset,
							fields[propertyIndex],
							persister.getPropertyColumnNames( propertyIndex ),
							includeColumns[propertyIndex],
							session
					);
				}

				if ( addPropertyMetadata && createMetadata ) {
					//add to property cache
					Object[] newColumnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset(
							resultset,
							persister.getPropertyColumnNames( propertyIndex )
					);
					//don't index null columns, this means no association
					if ( ! isEmptyOrAllColumnsNull( newColumnValues ) ) {
						doAddPropertyMetadata(
								tableIndex,
								propertyIndex,
								newColumnValues);
					}
				}
			}
		}
	}

	private void doAddPropertyMetadata(int tableIndex, int propertyIndex, Object[] newColumnValue) {

		String[] propertyColumnNames = persister.getPropertyColumnNames( propertyIndex );
		String[] rowKeyColumnNames = buildRowKeyColumnNamesForStarToOne( persister, propertyColumnNames );
		String tableName = persister.getTableName( tableIndex );
		EntityKeyMetadata targetEntityKeyMetadata = persister.getEntityKeyMetadata();

		AssociationKeyMetadata associationKeyMetadata = new AssociationKeyMetadata(
				tableName,
				propertyColumnNames,
				rowKeyColumnNames,
				// Because it is an association to one entity it should not need an index column
				EMPTY_STRING_ARRAY,
				new AssociatedEntityKeyMetadata(
					targetEntityKeyMetadata.getColumnNames(),
					targetEntityKeyMetadata
				),
				true
		);

		AssociationPersister associationPersister = new AssociationPersister(
					persister.getPropertyTypes()[propertyIndex].getReturnedClass()
				)
				.hostingEntity( getReferencedEntity( propertyIndex ) )
				.gridDialect( gridDialect )
				.associationKeyMetadata(  associationKeyMetadata )
				.keyColumnValues( newColumnValue )
				.session( session )
				//does not set .collectionPersister as it does not make sense here for a ToOne or a unique key
				.propertyType( persister.getPropertyTypes()[propertyIndex] )
				.roleOnMainSide( persister.getPropertyNames()[propertyIndex] )
				.inverse();

		Tuple tuple = new Tuple();
		//add the id column
		final String[] identifierColumnNames = persister.getIdentifierColumnNames();
		gridIdentifierType.nullSafeSet( tuple, id, identifierColumnNames, session );
		//add the fk column
		gridPropertyTypes[propertyIndex].nullSafeSet(
							tuple,
							fields[propertyIndex],
							propertyColumnNames,
							includeColumns[propertyIndex],
							session
					);

		Object[] columnValues = LogicalPhysicalConverterHelper.getColumnValuesFromResultset( tuple, rowKeyColumnNames );
		final RowKey rowKey = new RowKey( rowKeyColumnNames, columnValues );

		Tuple assocEntryTuple = associationPersister.createAndPutAssociationTuple( rowKey );
		for ( String column : tuple.getColumnNames() ) {
			assocEntryTuple.put( column, tuple.get( column ) );
		}

		associationPersister.flushToCache();
	}

	// Here the RowKey is made of the foreign key columns pointing to the associated entity
	// and the identifier columns of the owner's entity
	// We use the same order as the collection: id column names, foreign key column names
	public static String[] buildRowKeyColumnNamesForStarToOne(OgmEntityPersister persister, String[] keyColumnNames) {
		String[] identifierColumnNames = persister.getIdentifierColumnNames();
		int length = identifierColumnNames.length + keyColumnNames.length;
		String[] rowKeyColumnNames = new String[length];
		System.arraycopy( identifierColumnNames, 0, rowKeyColumnNames, 0, identifierColumnNames.length );
		System.arraycopy( keyColumnNames, 0, rowKeyColumnNames, identifierColumnNames.length, keyColumnNames.length );
		return rowKeyColumnNames;
	}

	private void doRemovePropertyMetadata(int tableIndex,
										int propertyIndex,
										Object[] oldColumnValue) {
		String[] propertyColumnNames = persister.getPropertyColumnNames( propertyIndex );
		String[] rowKeyColumnNames = buildRowKeyColumnNamesForStarToOne( persister, propertyColumnNames );
		String tableName = persister.getTableName( tableIndex );
		EntityKeyMetadata targetEntityKeyMetadata = persister.getEntityKeyMetadata();

		AssociationKeyMetadata associationKeyMetadata = new AssociationKeyMetadata(
				tableName,
				propertyColumnNames,
				rowKeyColumnNames,
				// Because it is an association to one entity it should not need an index column
				EMPTY_STRING_ARRAY,
				new AssociatedEntityKeyMetadata(
					targetEntityKeyMetadata.getColumnNames(),
					targetEntityKeyMetadata
				),
				true
		);

		AssociationPersister associationPersister = new AssociationPersister(
					persister.getPropertyTypes()[propertyIndex].getReturnedClass()
				)
				.hostingEntity( getReferencedEntity( propertyIndex ) )
				.gridDialect( gridDialect )
				.associationKeyMetadata( associationKeyMetadata )
				.keyColumnValues( oldColumnValue )
				.session( session )
				//does not set .collectionPersister as it does not make sense here for a ToOne or a unique key
				.propertyType( persister.getPropertyTypes()[propertyIndex] )
				.roleOnMainSide( persister.getPropertyNames()[propertyIndex] )
				.inverse();
		//add fk column value in TupleKey
		Tuple tupleKey = new Tuple();
		for (int index = 0 ; index < propertyColumnNames.length ; index++) {
			tupleKey.put( propertyColumnNames[index], oldColumnValue[index] );
		}
		//add id value in TupleKey
		gridIdentifierType.nullSafeSet( tupleKey, id, persister.getIdentifierColumnNames(), session );

		Association propertyValues = associationPersister.getAssociation();
		if ( propertyValues != null ) {
			//Map's equals operation delegates to all it's key and value, should be fine for now
			//this is a StarToOne case ie the FK is on the owning entity
			final RowKey matchingTuple = new RowKeyBuilder()
					.addColumns( buildRowKeyColumnNamesForStarToOne( persister, propertyColumnNames ) )
					.values( tupleKey )
					.build();
			//TODO what should we do if that's null?
			associationPersister.getAssociation().remove( matchingTuple );

			associationPersister.flushToCache();
		}
	}

	private Object getReferencedEntity(int propertyIndex) {
		GridType propertyType = gridPropertyTypes[propertyIndex];
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

	private boolean isEmptyOrAllColumnsNull(Object[] objects) {
		for ( Object object : objects ) {
			if ( object != null ) {
				return false;
			}
		}
		return true;
	}
}

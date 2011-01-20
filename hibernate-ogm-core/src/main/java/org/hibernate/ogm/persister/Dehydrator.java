package org.hibernate.ogm.persister;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.impl.LogicalPhysicalConverterHelper;
import org.hibernate.ogm.util.impl.PropertyMetadataProvider;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.Type;

class Dehydrator {
	private static final Logger log = LoggerFactory.getLogger( Dehydrator.class );

	private Map<String, Object> resultset;
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


	public Dehydrator persister(OgmEntityPersister persister) {
		this.persister = persister;
		return this;
	}

	public Dehydrator gridPropertyTypes(GridType[] gridPropertyTypes) {
		this.gridPropertyTypes = gridPropertyTypes;
		return this;
	}

	public Dehydrator gridIdentifierType(GridType gridIdentifierType) {
		this.gridIdentifierType = gridIdentifierType;
		return this;
	}

	public Dehydrator resultset(Map<String, Object> resultset) {
		this.resultset = resultset;
		return this;
	}

	public Dehydrator fields(Object[] fields) {
		this.fields = fields;
		return this;
	}

	public Dehydrator includeProperties(boolean[] includeProperties) {
		this.includeProperties = includeProperties;
		return this;
	}

	public Dehydrator includeColumns(boolean[][] includeColumns) {
		this.includeColumns = includeColumns;
		return this;
	}

	public Dehydrator tableIndex(int tableIndex) {
		this.tableIndex = tableIndex;
		return this;
	}

	public Dehydrator id(Serializable id) {
		this.id = id;
		return this;
	}

	public Dehydrator session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	public Dehydrator onlyRemovePropertyMetadata() {
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
		final Cache<AssociationKey, List<Map<String,Object>>> associationCache = GridMetadataManagerHelper.getAssociationCache(
				session.getFactory()
		);
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
								associationCache,
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
								associationCache,
								tableIndex,
								propertyIndex,
								newColumnValues);
					}
				}
			}
		}
	}

	private void doAddPropertyMetadata(Cache<AssociationKey, List<Map<String,Object>>> associationCache,
										  int tableIndex,
										  int propertyIndex,
										  Object[] newColumnValue) {

		PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
		        .associationCache( associationCache )
				.keyColumnNames( persister.getPropertyColumnNames( propertyIndex ) )
				.columnValues( newColumnValue )
				.session( session )
				.tableName( persister.getTableName( tableIndex ) );
		List<Map<String,Object>> propertyValues = metadataProvider.getCollectionMetadata();
		final Map<String, Object> tuple = new HashMap<String, Object>( 4 );
		//add the id column
		gridIdentifierType.nullSafeSet( tuple, id, persister.getIdentifierColumnNames(), session );
		//add the fk column
		gridPropertyTypes[propertyIndex].nullSafeSet(
							tuple,
							fields[propertyIndex],
							persister.getPropertyColumnNames( propertyIndex ),
							includeColumns[propertyIndex],
							session
					);
		propertyValues.add( tuple );
		metadataProvider.flushToCache();
	}

	private void doRemovePropertyMetadata(Cache<AssociationKey, List<Map<String,Object>>> associationCache,
										  int tableIndex,
										  int propertyIndex,
										  Object[] oldColumnValue) {
		PropertyMetadataProvider metadataProvider = new PropertyMetadataProvider()
		        .associationCache( associationCache )
				.keyColumnNames( persister.getPropertyColumnNames( propertyIndex ) )
				.columnValues( oldColumnValue )
				.session( session )
				.tableName( persister.getTableName( tableIndex ) );
		Map<String,Object> idTuple = getTupleKey();
		List<Map<String,Object>> propertyValues = metadataProvider.getCollectionMetadata();
		if ( propertyValues != null ) {
			//Map's equals operation delegates to all it's key and value, should be fine for now
			final Map<String, Object> matchingTuple = metadataProvider.findMatchingTuple( idTuple );
			//TODO what should we do if that's null?
			if (matchingTuple != null) {
				metadataProvider.getCollectionMetadata().remove( matchingTuple );
			}
			metadataProvider.flushToCache();
		}
	}

	private boolean isEmptyOrAllColumnsNull(Object[] objects) {
		for (Object object : objects) {
			if ( object != null ) return false;
		}
		return true;
	}

	private Map<String,Object> getTupleKey() {
		Map<String,Object> tupleKey = new HashMap<String,Object>(4);
		gridIdentifierType.nullSafeSet( tupleKey, id, persister.getIdentifierColumnNames(), session );
		return tupleKey;
	}
}
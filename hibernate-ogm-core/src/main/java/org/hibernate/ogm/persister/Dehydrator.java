package org.hibernate.ogm.persister;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.ogm.grid.PropertyKey;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.ogm.type.GridType;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.tuple.entity.EntityMetamodel;

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


	public Dehydrator persister(OgmEntityPersister persister) {
		this.persister = persister;
		return this;
	}

	public Dehydrator gridPropertyTypes(GridType[] gridPropertyTypes) {
		this.gridPropertyTypes = gridPropertyTypes;
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
		final Cache<PropertyKey, List<Serializable>> propertyCache = GridMetadataManagerHelper.getPropertyCache( session.getFactory() );
		for ( int propertyIndex = 0; propertyIndex < entityMetamodel.getPropertySpan(); propertyIndex++ ) {
			if ( persister.isPropertyOfTable( propertyIndex, tableIndex ) ) {
				if ( removePropertyMetadata ) {
					//remove from property cache
					if ( uniqueness[propertyIndex] ) {
						Object[] oldColumnValues = Helper.getColumnValuesFromResultset( resultset,
								propertyIndex, persister );
						//don't index null columns, this means not association
						if ( ! isEmptyOrAllColumnsNull( oldColumnValues ) ) {
							doRemovePropertyMetadata(
									propertyCache,
									tableIndex,
									propertyIndex,
									oldColumnValues);
						}
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

				if ( addPropertyMetadata ) {
					//add to property cache
					if ( uniqueness[propertyIndex] ) {
						Object[] newColumnValues = Helper.getColumnValuesFromResultset( resultset,
								propertyIndex, persister );
						//don't index null columns, this means not association
						if ( ! isEmptyOrAllColumnsNull( newColumnValues ) ) {
							doAddPropertyMetadata(
									propertyCache,
									tableIndex,
									propertyIndex,
									newColumnValues);
						}
					}
				}
			}
		}
	}

	private void doAddPropertyMetadata(Cache<PropertyKey, List<Serializable>> propertyCache,
										  int tableIndex,
										  int propertyIndex,
										  Object[] newColumnValue) {
		PropertyKey propertyKey = new PropertyKey(
				persister.getTableName( tableIndex ),
				persister.getPropertyColumnNames(propertyIndex),
				newColumnValue);
		List<Serializable> propertyValues = propertyCache.get( propertyKey );
		if ( propertyValues == null ) {
			propertyValues = new ArrayList<Serializable>();
		}
		propertyValues.add( id );
		propertyCache.put( propertyKey, propertyValues );
	}

	private void doRemovePropertyMetadata(Cache<PropertyKey, List<Serializable>> propertyCache,
										  int tableIndex,
										  int propertyIndex,
										  Object[] oldColumnValue) {

		PropertyKey propertyKey = new PropertyKey(
				persister.getTableName( tableIndex ),
				persister.getPropertyColumnNames(propertyIndex),
				oldColumnValue);
		List<Serializable> propertyValues = propertyCache.get( propertyKey );
		if ( propertyValues != null ) {
			final boolean lastId = propertyValues.size() == 1 && id.equals( propertyValues.get( 0 ) );
			if ( lastId ) {
				propertyCache.remove( propertyKey );
			}
			else {
				//TODO should we remove all (ie remove till it returns false?
				propertyValues.remove( id );
				propertyCache.put( propertyKey, propertyValues );
			}
		}
	}

	private boolean isEmptyOrAllColumnsNull(Object[] objects) {
		for (Object object : objects) {
			if ( object != null ) return false;
		}
		return true;
	}
}
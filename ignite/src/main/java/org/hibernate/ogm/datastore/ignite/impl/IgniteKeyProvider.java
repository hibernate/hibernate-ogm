/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.persistencestrategy.IgniteSerializableEntityKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

public class IgniteKeyProvider {

	public static IgniteKeyProvider INSTANCE = new IgniteKeyProvider();

	private static final Log LOG = LoggerFactory.getLogger();

	public IgniteSerializableEntityKey getEntityCacheKey(EntityKey key) {
		return new IgniteSerializableEntityKey( key );
	}

	/**
	 * Converting entity key to string key
	 *
	 * @param key entity key
	 * @return string key
	 */
	public String getEntityKeyString(EntityKey key) {
		return getKeyStringByColumnValues( key.getColumnValues() );
	}

	/**
	 * Converting id source key to string key
	 *
	 * @param key id source key
	 * @return string key
	 */
	public String getIdSourceKeyString(IdSourceKey key) {
		return key.getColumnValue();
	}

	private String getKeyStringByColumnValues(Object[] columnValues) {
		return StringUtils.join( columnValues, "-" );
	}

	/**
	 * Get the entity type from the metadata
	 *
	 * @param keyMetadata metadata
	 * @return type
	 */
	public String getEntityType(String entity) {
		if ( entity.indexOf( "." ) >= 0 ) {
			String[] arr = entity.split( "\\." );
			if ( arr.length != 2 ) {
				throw LOG.invalidEntityName( entity );
			}
			return arr[1];
		}
		return entity;
	}

	/**
	 * Get the entity cache name from the metadata
	 *
	 * @param keyMetadata metadata
	 * @return
	 */
	public String getEntityCache(EntityKeyMetadata keyMetadata) {
		if ( keyMetadata == null ) {
			throw LOG.mustNotBeNull( "EntityKeyMetadata" );
		}
		return getEntityCache( keyMetadata.getTable() );
	}

	public String getEntityCache(String entity) {
		if ( entity.indexOf( "." ) >= 0 ) {
			String[] arr = entity.split( "\\." );
			if ( arr.length != 2 ) {
				throw LOG.invalidEntityName( entity );
			}
			return arr[0];
		}
		return entity;
	}

	/**
	 * Get the cache name from the metadata
	 *
	 * @param keyMetadata metadata
	 * @return
	 */
	public String getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		if ( keyMetadata == null ) {
			throw LOG.mustNotBeNull( "AssociationKeyMetadata" );
		}
		return getEntityCache( keyMetadata.getName() );
	}
}

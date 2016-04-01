/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.hibernate.ogm.datastore.ignite.persistencestrategy.IgniteSerializableEntityKey;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;

public class IgniteKeyProvider {

	public static IgniteKeyProvider INSTANCE = new IgniteKeyProvider();

	public IgniteSerializableEntityKey getEntityCacheKey(EntityKey key) {
		return new IgniteSerializableEntityKey( key );
	}

	/**
	 * Converting entity key to string key
	 * @param key entity key
	 * @return string key
	 */
	public String getEntityKeyString(EntityKey key) {
		return getKeyStringByColumnValues( key.getColumnValues() );
	}

	/**
	 * Converting association key to string key
	 * @param key association key
	 * @return string key
	 */
	public String getAssociationKeyString(AssociationKey key) {
		return getKeyStringByColumnValues( key.getColumnValues() );
	}

	/**
	 * Converting id source key to string key
	 * @param key id source key
	 * @return string key
	 */
	public String getIdSourceKeyString(IdSourceKey key) {
		return getKeyStringByColumnValues( key.getColumnValues() );
	}

	/**
	 * Converting row key to string key
	 * @param key row key
	 * @return string key
	 */
	public String getRowKeyString(RowKey rowKey) {
		return getKeyStringByColumnValues( rowKey.getColumnValues() );
	}

	private String getKeyStringByColumnValues(Object[] columnValues) {
		return StringUtils.join( columnValues, "-" );
	}

	/**
	 * Get the entity type from the metadata
	 * @param keyMetadata metadata
	 * @return type
	 */
	public String getEntityType(EntityKeyMetadata keyMetadata) {
		if (keyMetadata == null) {
			throw new IgniteHibernateException("EntityKeyMetadata is null");
		}
		String entity = keyMetadata.getTable();
		if (entity.indexOf( "." ) >= 0) {
			String[] arr = entity.split( "\\." );
			if (arr.length != 2) {
				throw new IgniteHibernateException("Invalid entity name " + entity);
			}
			return arr[1];
		}
		else {
			return keyMetadata.getTable();
		}
	}

	/**
	 * Get the entity cache name from the metadata
	 * @param keyMetadata metadata
	 * @return
	 */
	public String getEntityCache(EntityKeyMetadata keyMetadata) {
		if (keyMetadata == null) {
			throw new IgniteHibernateException("EntityKeyMetadata is null");
		}
		return getEntityCache(keyMetadata.getTable());
	}

	public String getEntityCache(String entity) {
		if (entity.indexOf( "." ) >= 0) {
			String[] arr = entity.split( "\\." );
			if (arr.length != 2) {
				throw new IgniteHibernateException("Invalid entity name " + entity);
			}
			return arr[0];
		}
		return entity;
	}

	/**
	 * Get the cache name from the metadata
	 * @param keyMetadata metadata
	 * @return
	 */
	public String getAssociationCache(AssociationKeyMetadata keyMetadata) {
		if (keyMetadata == null) {
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
		}
		return getEntityCache( keyMetadata.getTable() );
	}

	/**
	 * Get the type of object from the metadata
	 * @param keyMetadata metadata
	 * @return
	 */
	public String getAssociationType(AssociationKeyMetadata keyMetadata) {
		if (keyMetadata == null) {
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
		}
		return keyMetadata.getTable();
	}

	/**
	 * 
	 * Get the cache name from the metadata
	 * @param keyMetadata metadata
	 * @return
	 */
	public String getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		if (keyMetadata == null) {
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
		}
		return getEntityCache( keyMetadata.getName() );
	}
}

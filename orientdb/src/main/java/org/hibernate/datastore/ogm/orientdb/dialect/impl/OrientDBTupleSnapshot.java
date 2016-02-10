/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.dialect.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hibernate.datastore.ogm.orientdb.constant.OrientDBConstant;

import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.datastore.ogm.orientdb.utils.AssociationUtil;
import org.hibernate.datastore.ogm.orientdb.utils.ORidBagUtil;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class OrientDBTupleSnapshot implements TupleSnapshot {

	private static Log LOG = LoggerFactory.getLogger();
	private final Map<String, Object> dbNameValueMap;

	private Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata;
	private Map<String, String> rolesByColumn;
	private EntityKeyMetadata entityKeyMetadata;

	public OrientDBTupleSnapshot(Map<String, Object> dbNameValueMap,
			Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata,
			Map<String, String> rolesByColumn,
			EntityKeyMetadata entityKeyMetadata) {
		this.dbNameValueMap = dbNameValueMap;
		this.associatedEntityKeyMetadata = associatedEntityKeyMetadata;
		this.rolesByColumn = rolesByColumn;
		this.entityKeyMetadata = entityKeyMetadata;
		LOG.info( "1.dbNameValueMap:" + dbNameValueMap );
		LOG.info( "1.associatedEntityKeyMetadata:" + associatedEntityKeyMetadata );
	}

	public OrientDBTupleSnapshot(Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata,
			Map<String, String> rolesByColumn,
			EntityKeyMetadata entityKeyMetadata) {
		this( new HashMap<String, Object>(), associatedEntityKeyMetadata, rolesByColumn, entityKeyMetadata );
		LOG.info( "2.dbNameValueMap:" + dbNameValueMap );
		LOG.info( "2.associatedEntityKeyMetadata:" + associatedEntityKeyMetadata );
	}

	@Override
	public Object get(String targetColumnName) {
		LOG.info( "targetColumnName: " + targetColumnName );
		Object value = null;
		if ( targetColumnName.equals( OrientDBConstant.SYSTEM_VERSION ) && value == null ) {
			value = Integer.valueOf( 0 );
		}
		else if ( associatedEntityKeyMetadata.containsKey( targetColumnName ) ) {
			LOG.info( "associated targetColumnName: " + targetColumnName );
			String mappedByName = AssociationUtil.getMappedByFieldName( associatedEntityKeyMetadata.get( targetColumnName ) );
			String inOrientDbField = "in_".concat( mappedByName );
			Map<String, Object> associatedEntity = loadAssociatedEntity( associatedEntityKeyMetadata.get( targetColumnName ), targetColumnName );
			if ( associatedEntity != null ) {
				for ( Map.Entry<String, Object> entry : associatedEntity.entrySet() ) {
					LOG.info( "name: " + entry.getKey() + "; value:" + entry.getValue() );
				}
				String string = (String) associatedEntity.get( inOrientDbField );
				value = ORidBagUtil.convertStringToORidBag( string );
				dbNameValueMap.remove( targetColumnName );
			}
		}
		else {
			value = dbNameValueMap.get( targetColumnName );
			LOG.info( "targetColumnName: " + targetColumnName + "; value: " + value );
		}
		return value;
	}

	private Map<String, Object> loadAssociatedEntity(AssociatedEntityKeyMetadata associatedEntityKeyMetadata, String targetColumnName) {
		String mappedByName = AssociationUtil.getMappedByFieldName( associatedEntityKeyMetadata );
		String inOrientDbField = "in_".concat( mappedByName );
		LOG.info( "mappedByName: " + mappedByName + "; inOrientDbField:" + inOrientDbField );
		LOG.info( "inOrientDbField: " + inOrientDbField + ".loaded? :" + dbNameValueMap.containsKey( inOrientDbField ) );
		Map<String, Object> value = null;
		if ( dbNameValueMap.containsKey( inOrientDbField ) ) {
			value = (Map<String, Object>) dbNameValueMap.get( inOrientDbField );
			LOG.info( "value: " + value.getClass() );
		}
		return value;
	}

	@Override
	public boolean isEmpty() {
		LOG.info( "isEmpty" );
		return dbNameValueMap.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		LOG.info( "getColumnNames" );
		return dbNameValueMap.keySet();
	}

	/**
	 * Whether this snapshot has been newly created (meaning it doesn't have an actual {@link Node} yet) or not. A node
	 * will be in the "new" state between the {@code createTuple()} call and the next {@code insertOrUpdateTuple()}
	 * call.
	 */
	public boolean isNew() {
		return dbNameValueMap == null || ( dbNameValueMap != null && dbNameValueMap.isEmpty() );
	}

}

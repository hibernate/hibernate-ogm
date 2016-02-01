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

import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * @author chernolyassv
 */
public class OrientDBTupleSnapshot implements TupleSnapshot {

	private static Log LOG = LoggerFactory.getLogger();
	private final Map<String, Object> dbNameValueMap;

	private Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata;
	private Map<String, String> rolesByColumn;
	private EntityKeyMetadata entityKeyMetadata;

	public OrientDBTupleSnapshot(Map<String, Object> dbNameValueMap, Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata,
			Map<String, String> rolesByColumn, EntityKeyMetadata entityKeyMetadata) {
		this.dbNameValueMap = dbNameValueMap;
		this.associatedEntityKeyMetadata = associatedEntityKeyMetadata;
		this.rolesByColumn = rolesByColumn;
		this.entityKeyMetadata = entityKeyMetadata;
		LOG.info( "dbNameValueMap:" + dbNameValueMap );
	}

	public OrientDBTupleSnapshot(EntityKeyMetadata entityKeyMetadata) {
		this( new HashMap<String, Object>(), null, null, entityKeyMetadata );
	}

	@Override
	public Object get(String targetColumnName) {
		LOG.info( "targetColumnName: " + targetColumnName );
		return dbNameValueMap.get( targetColumnName );
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

}

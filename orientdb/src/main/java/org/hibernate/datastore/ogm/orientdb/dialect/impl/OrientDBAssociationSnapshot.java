/*
* Hibernate OGM, Domain model persistence for NoSQL datastores
* 
* License: GNU Lesser General Public License (LGPL), version 2.1 or later
* See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
*/

package org.hibernate.datastore.ogm.orientdb.dialect.impl;

import java.util.Map;
import java.util.Set;
import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 * @see org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jAssociationSnapshot
 */

public class OrientDBAssociationSnapshot implements AssociationSnapshot {

	private static Log LOG = LoggerFactory.getLogger();
	private final Map<RowKey, Tuple> tuples;

	public OrientDBAssociationSnapshot(Map<RowKey, Tuple> tuples) {
		this.tuples = tuples;
	}

	@Override
	public Tuple get(RowKey rowKey) {
		LOG.info( "get: rowKey :" + rowKey );
		Tuple tuple = tuples.get( rowKey );
		return tuple;
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
		LOG.info( "containsKey: rowKey :" + rowKey );
		return tuples.containsKey( rowKey );
	}

	@Override
	public int size() {
		return tuples.size();
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return tuples.keySet();

	}
}

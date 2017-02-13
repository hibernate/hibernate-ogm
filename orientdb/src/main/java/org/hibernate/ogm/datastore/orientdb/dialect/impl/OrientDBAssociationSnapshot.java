/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.orientdb.dialect.impl;

import java.util.Map;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;

import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * * Represents the association snapshot as loaded by the datastore.
 * <p>
 * Interface implemented by the datastore dialect to avoid data duplication in memory (if possible). Note that this
 * snapshot will not be modified by the Hibernate OGM engine
 * </p>
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

public class OrientDBAssociationSnapshot implements AssociationSnapshot {

	private static Log log = LoggerFactory.getLogger();
	private final Map<RowKey, Tuple> tuples;

	public OrientDBAssociationSnapshot(Map<RowKey, Tuple> tuples) {
		this.tuples = tuples;
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
		return tuples.containsKey( rowKey );
	}

	@Override
	public Tuple get(RowKey rowKey) {
		log.debugf( "get: rowKey : %s", rowKey );
		return tuples.get( rowKey );
	}

	@Override
	public int size() {
		return tuples.size();
	}

	@Override
	public Iterable<RowKey> getRowKeys() {
		return tuples.keySet();
	}

}

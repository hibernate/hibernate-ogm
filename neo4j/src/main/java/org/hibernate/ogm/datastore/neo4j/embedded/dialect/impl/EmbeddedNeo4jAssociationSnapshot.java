/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Represents the association snapshot as loaded by Neo4j.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public final class EmbeddedNeo4jAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, Tuple> tuples;

	public EmbeddedNeo4jAssociationSnapshot(Map<RowKey, Tuple> tuples) {
		this.tuples = tuples;
	}

	@Override
	public Tuple get(RowKey rowKey) {
		Tuple tuple = tuples.get( rowKey );
		return tuple;
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
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

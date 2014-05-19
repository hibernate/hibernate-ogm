/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.neo4j.graphdb.PropertyContainer;

/**
 * Represents the Tuple snapshot as loaded by the Neo4j datastore.
 * <p>
 * A {@link org.neo4j.graphdb.Node} represents a {@link org.hibernate.ogm.datastore.spi.Tuple}. Columns are mapped as properties of a the Node.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public final class Neo4jTupleSnapshot implements TupleSnapshot {

	private final PropertyContainer node;

	public Neo4jTupleSnapshot(PropertyContainer node) {
		this.node = node;
	}

	@Override
	public Object get(String column) {
		if ( node.hasProperty( column ) ) {
			return node.getProperty( column );
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		return !node.getPropertyKeys().iterator().hasNext();
	}

	@Override
	public Set<String> getColumnNames() {
		Set<String> names = new HashSet<String>();
		for ( String string : node.getPropertyKeys() ) {
			names.add( string );
		}
		return names;
	}

}

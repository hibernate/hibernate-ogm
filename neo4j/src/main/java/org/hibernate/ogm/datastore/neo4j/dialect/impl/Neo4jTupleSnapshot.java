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
 * Any {@link PropertyContainer} (node or relationship) can represent a tuple. The columns of the tuple are mapped as
 * properties of the property container.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public final class Neo4jTupleSnapshot implements TupleSnapshot {

	private final PropertyContainer propertyContainer;

	public Neo4jTupleSnapshot(PropertyContainer propertyContainer) {
		this.propertyContainer = propertyContainer;
	}

	@Override
	public Object get(String column) {
		if ( propertyContainer.hasProperty( column ) ) {
			return propertyContainer.getProperty( column );
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		return !propertyContainer.getPropertyKeys().iterator().hasNext();
	}

	@Override
	public Set<String> getColumnNames() {
		Set<String> names = new HashSet<String>();
		for ( String string : propertyContainer.getPropertyKeys() ) {
			names.add( string );
		}
		return names;
	}

}

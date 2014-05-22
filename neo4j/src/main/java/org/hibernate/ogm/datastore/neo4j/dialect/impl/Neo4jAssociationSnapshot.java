/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

/**
 * Represents the association snapshot as loaded by Neo4j.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public final class Neo4jAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, Tuple> tuples = new HashMap<RowKey, Tuple>();

	public Neo4jAssociationSnapshot(Node ownerNode, AssociationKey associationKey) {
		for ( Relationship relationship : relationships( ownerNode, associationKey ) ) {
			RowKey rowKey = convert( associationKey, relationship );
			Tuple tuple = new Tuple( new Neo4jTupleSnapshot( relationship ) );
			tuples.put( rowKey, tuple );
		}
	}

	@Override
	public Tuple get(RowKey rowKey) {
		return tuples.get( rowKey );
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

	private Iterable<Relationship> relationships(Node ownerNode, AssociationKey associationKey) {
		return ownerNode.getRelationships( Direction.OUTGOING, CypherCRUD.relationshipType( associationKey ) );
	}

	private RowKey convert(AssociationKey associationKey, PropertyContainer container) {
		String[] columnNames = associationKey.getRowKeyColumnNames();
		Object[] values = new Object[columnNames.length];

		for ( int i = 0; i < columnNames.length; i++ ) {
			String columnName = columnNames[i];
			if ( container.hasProperty( columnName ) ) {
				values[i] = container.getProperty( columnName );
			}
		}

		return new RowKey( associationKey.getTable(), columnNames, values );
	}
}

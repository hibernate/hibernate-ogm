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

import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * Represents the association snapshot as loaded by Neo4j.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public final class Neo4jAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, Tuple> tuples = new HashMap<RowKey, Tuple>();

	public Neo4jAssociationSnapshot(Node ownerNode, AssociationKey associationKey, AssociationContext associationContext) {
		for ( Relationship relationship : relationships( ownerNode, associationKey ) ) {
			Neo4jTupleAssociationSnapshot snapshot = new Neo4jTupleAssociationSnapshot( relationship, associationKey, associationContext );
			RowKey rowKey = convert( associationKey, associationContext, snapshot );
			tuples.put( rowKey, new Tuple( snapshot ) );
		}
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

	private Iterable<Relationship> relationships(Node ownerNode, AssociationKey associationKey) {
		return ownerNode.getRelationships( Direction.BOTH, CypherCRUD.relationshipType( associationKey ) );
	}

	private RowKey convert(AssociationKey associationKey, AssociationContext associationContext, Neo4jTupleAssociationSnapshot snapshot) {
		String[] columnNames = associationKey.getMetadata().getRowKeyColumnNames();
		Object[] values = new Object[columnNames.length];

		for ( int i = 0; i < columnNames.length; i++ ) {
			values[i] = snapshot.get( columnNames[i] );
		}

		return new RowKey( associationKey.getTable(), columnNames, values );
	}
}

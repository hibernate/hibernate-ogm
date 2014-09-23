/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.neo4j.graphdb.DynamicRelationshipType.withName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Contracts;
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

	public Neo4jAssociationSnapshot(Node ownerNode, AssociationKey associationKey, AssociatedEntityKeyMetadata associatedEntityKeyMetadata, String relationshipType) {
		Contracts.assertParameterNotNull( relationshipType, "relationshipType" );

		for ( Relationship relationship : relationships( ownerNode, associationKey, relationshipType ) ) {
			Neo4jTupleAssociationSnapshot snapshot = new Neo4jTupleAssociationSnapshot( relationship, associationKey, associatedEntityKeyMetadata );
			RowKey rowKey = convert( associationKey, snapshot );
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

	private static Iterable<Relationship> relationships(Node ownerNode, AssociationKey associationKey, String relationshipType) {
		return ownerNode.getRelationships( Direction.BOTH, withName( relationshipType ) );
	}

	private RowKey convert(AssociationKey associationKey, Neo4jTupleAssociationSnapshot snapshot) {
		String[] columnNames = associationKey.getMetadata().getRowKeyColumnNames();
		Object[] values = new Object[columnNames.length];

		for ( int i = 0; i < columnNames.length; i++ ) {
			values[i] = snapshot.get( columnNames[i] );
		}

		return new RowKey( columnNames, values );
	}
}

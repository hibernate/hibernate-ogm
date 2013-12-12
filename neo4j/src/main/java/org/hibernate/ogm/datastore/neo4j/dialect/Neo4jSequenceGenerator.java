/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect;

import java.util.Map;

import org.hibernate.ogm.grid.RowKey;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.UniqueFactory;

/**
 * Generates the next value in a sequence for a {@link RowKey}.
 * <p>
 * The next value in the sequence is saved in a node identified by the RowKey identifier.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jSequenceGenerator {

	private static final String ID_SEQUENCE_PROPERTY = "id_sequence_ogm";

	private static final String VALUE_SEQUENCE_PROPERTY = "value_sequence_ogm";

	private final GraphDatabaseService neo4jDb;

	private final String sequenceIndexName;

	public Neo4jSequenceGenerator(GraphDatabaseService neo4jDb, String indexName) {
		this.neo4jDb = neo4jDb;
		this.sequenceIndexName = indexName;
	}

	/**
	 * Generate the next value in a sequence for a given {@link RowKey}.
	 *
	 * @param rowKey
	 *            identifies the sequence
	 * @param increment
	 *            the difference between to consecutive values in the sequence
	 * @param initialValue
	 *            the first value returned when a new sequence is created
	 * @return the next value in a sequence
	 */
	public int nextValue(RowKey rowKey, int increment, final int initialValue) {
		Node sequenceNode = getOrCreateSequence( rowKey, initialValue );
		return updateSequence( sequenceNode, increment );
	}

	private Node getOrCreateSequence(RowKey key, final int initialValue) {
		Transaction tx = neo4jDb.beginTx();
		try {
			UniqueFactory<Node> factory = nodeFactory( initialValue );
			Node sequenceNode = factory.getOrCreate( ID_SEQUENCE_PROPERTY, generateId( key ) );
			tx.success();
			return sequenceNode;
		}
		finally {
			tx.finish();
		}
	}

	private Object generateId(RowKey key) {
		StringBuilder builder = new StringBuilder( key.getTable() );
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			builder.append( key.getColumnNames()[i] );
			builder.append( key.getColumnValues()[i] );
		}
		return builder.toString();
	}

	private UniqueFactory<Node> nodeFactory(final int initialValue) {
		UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory( neo4jDb, sequenceIndexName ) {
			@Override
			protected void initialize(Node created, Map<String, Object> properties) {
				created.setProperty( ID_SEQUENCE_PROPERTY, properties.get( ID_SEQUENCE_PROPERTY ) );
				created.setProperty( VALUE_SEQUENCE_PROPERTY, initialValue );
			}
		};
		return factory;
	}

	private int updateSequence(Node sequence, int increment) {
		Transaction tx = neo4jDb.beginTx();
		Lock lock = null;
		try {
			lock = tx.acquireWriteLock( sequence );
			int nextValue = updateSequenceValue( sequence, increment );
			tx.success();
			lock.release();
			return nextValue;
		}
		finally {
			tx.finish();
		}
	}

	private int updateSequenceValue(Node sequence, int increment) {
		int currentValue = (Integer) sequence.getProperty( VALUE_SEQUENCE_PROPERTY );
		int updatedValue = currentValue + increment;
		sequence.setProperty( VALUE_SEQUENCE_PROPERTY, updatedValue );
		return currentValue;
	}

}

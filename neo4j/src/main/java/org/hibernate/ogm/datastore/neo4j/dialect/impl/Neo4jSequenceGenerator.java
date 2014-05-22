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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.grid.RowKey;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;

/**
 * Generates the next value in a sequence for a RowKey.
 * <p>
 * A {@link RowKey} for a sequence contains exactly the following information:
 * <ul>
 * <li>The table where the sequence names are stored
 * <li>One column which contains the name of the sequence as value
 * </ul>
 * <p>
 * A sequence is a node with the labels:
 * <ul>
 * <li>{@code RowKey#getTable()}
 * <li>The sequence name (contained in the column value of the {@code RowKey})
 * </ul>
 * <p>
 * The sequence name is also used has property name for the current value of the sequence. The reason to have the
 * sequence name as label is that currently Cypher does not support a query in the format MERGE ... WHERE ... ON
 * CREATE... Using the sequence name as label we can avoid the where clause and create a new sequence if it does not
 * exist.
 * <p>
 * Adding an attribute containing the name of the sequence we can avoid the creation of two nodes representing the same
 * sequence by adding a unique constraint on it.
 * <p>
 * Using cypher, an example of a sequence node looks like this:
 *
 * <pre>
 * (:hibernate_sequences {sequence_name: 'ExampleSequence', ExampleSequence: 3})
 * </pre>
 * <p>
 * A lock is acquired on the node every time the sequence needs to be updated.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jSequenceGenerator {

	public static final String SEQUENCE_NAME_PROPERTY = "sequence_name";

	private static final Log log = LoggerFactory.getLogger();

	/**
	 * Defines the amount of time the generation of the sequence should be attempted. A value of 1 should be enough
	 * because errors are expected only when two thread try to create the same node.
	 */
	private static final int MAX_GENERATION_ATTEMPT = 5;

	private final ConcurrentMap<RowKey, String> queryCache = new ConcurrentHashMap<RowKey, String>();

	private final GraphDatabaseService neo4jDb;

	private final ExecutionEngine engine;

	public Neo4jSequenceGenerator(GraphDatabaseService neo4jDb) {
		this.neo4jDb = neo4jDb;
		this.engine = new ExecutionEngine( neo4jDb );
	}

	/**
	 * The unique constraint is created on the property "sequence_name" only if it does not exists already. It is required
	 * to avoid the creation of multiple nodes representing the same sequence.
	 *
	 * @param neo4jDb the db where the schema will be updated
	 */
	public void createUniqueConstraint(Set<String> generatorsKey) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();
			for ( String generatorKey : generatorsKey ) {
				Label label = DynamicLabel.label( generatorKey );
				if ( isMissingUniqueConstraint( neo4jDb, label, SEQUENCE_NAME_PROPERTY ) ) {
					neo4jDb.schema().constraintFor( label ).assertPropertyIsUnique( SEQUENCE_NAME_PROPERTY ).create();
				}
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	private boolean isMissingUniqueConstraint(GraphDatabaseService neo4jDb, Label sequenceLabel, String segmentName) {
		Iterable<ConstraintDefinition> constraints = neo4jDb.schema().getConstraints( sequenceLabel );
		for ( ConstraintDefinition constraint : constraints ) {
			if ( constraint.isConstraintType( ConstraintType.UNIQUENESS ) ) {
				for ( String property : constraint.getPropertyKeys() ) {
					if ( segmentName.equals( property ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Generate the next value in a sequence for a given {@link RowKey}.
	 * <p>
	 * Unique constraints violation might occurs during the creation of the sequences.
	 * In this case the method will try again as many times as defined in the
	 * {@link #MAX_GENERATION_ATTEMPT} constant.
	 *
	 * @param rowKey identifies the sequence
	 * @param increment the difference between to consecutive values in the sequence
	 * @param initialValue the first value returned when a new sequence is created
	 * @return the next value in a sequence
	 */
	public int nextValue(RowKey rowKey, int increment, final int initialValue) {
		int number = 0;
		while ( number++ < MAX_GENERATION_ATTEMPT ) {
			try {
				return sequence( rowKey, increment, initialValue );
			}
			catch (Exception e) {
				// It might happen that two threads want to create a new sequence node for the first time.
				// In this case the two nodes will have the same sequence name, violating the unique constraint defined
				// at startup.
				log.errorGeneratingSequence( sequenceName( rowKey ), e );
			}
		}
		// This should never happen
		throw log.cannotGenerateSequence( sequenceName( rowKey ) );
	}

	private int sequence(RowKey rowKey, int increment, final int initialValue) {
		Transaction tx = neo4jDb.beginTx();
		Lock lock = null;
		try {
			Node sequence = getOrCreateSequence( rowKey, initialValue );
			lock = tx.acquireWriteLock( sequence );
			int nextValue = updateSequenceValue( sequenceName( rowKey ), sequence, increment );
			tx.success();
			lock.release();
			return nextValue;
		}
		finally {
			tx.close();
		}
	}

	/**
	 * Create a new node for the given {@link RowKey} or update the existing one.
	 *
	 * @param key the {@link RowKey} identifying the sequence
	 * @param initialValue the initial value of the sequence
	 * @return the node representing the sequence
	 */
	private Node getOrCreateSequence(RowKey rowKey, final int initialValue) {
		String updateSequenceQuery = getQuery( rowKey );
		Map<String, Object> parameters = new HashMap<String, Object>( 2 );
		parameters.put( "initialValue", initialValue );
		parameters.put( "sequenceName", sequenceName( rowKey ) );
		ExecutionResult result = engine.execute( updateSequenceQuery, parameters );
		ResourceIterator<Node> column = result.columnAs( "n" );
		Node node = null;
		if ( column.hasNext() ) {
			node = column.next();
		}
		column.close();
		return node;
	}

	/**
	 * Ex:
	 * <pre>
	 * MERGE (n:hibernate_sequences:DistributedRevisionControl) ON CREATE SET n.value = {initialValue} RETURN n
	 * </pre>
	 */
	private String getQuery(RowKey rowKey) {
		String query = queryCache.get( rowKey );
		if ( query == null ) {
			String sequenceName = sequenceName( rowKey );
			query = "MERGE (n:" + rowKey.getTable() + ":`" + sequenceName + "`) ON CREATE SET n.`" + sequenceName + "` = {initialValue}, n." + SEQUENCE_NAME_PROPERTY
					+ " = {sequenceName} RETURN n";

			String cached = queryCache.putIfAbsent( rowKey, query );
			if ( cached != null ) {
				query = cached;
			}
		}
		return query;
	}

	private String sequenceName(RowKey key) {
		return (String) key.getColumnValues()[0];
	}

	private int updateSequenceValue(String sequenceName, Node sequence, int increment) {
		int currentValue = (Integer) sequence.getProperty( sequenceName );
		int updatedValue = currentValue + increment;
		sequence.setProperty( sequenceName, updatedValue );
		return currentValue;
	}

}

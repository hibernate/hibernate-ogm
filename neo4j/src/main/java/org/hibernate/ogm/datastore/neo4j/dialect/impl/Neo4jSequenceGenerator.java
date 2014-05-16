/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 * CREATE... Using the sequence name as label we can avoid the where clause and create a new sequence if it doesn not
 * exist.
 * <p>
 * Using cypher, an example of a sequence node looks like this:
 * <pre>
 * (n:hibernate_sequences:ExampleSequence {ExampleSequence: 3})
 * </pre>
 * <p>
 * A lock is acquired on the node every time the sequence needs to be updated.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jSequenceGenerator {

	private static final Log log = LoggerFactory.getLogger();

	/**
	 * Defines the amount of time the generation of the sequence should be attempted. A value of 1 should be enough
	 * because errors are expected only when two thread try to create the same node.
	 */
	private static final int MAX_GENERATION_ATTEMPT = 5;

	private final Map<RowKey, String> queryCache = new HashMap<RowKey, String>();

	private final GraphDatabaseService neo4jDb;

	public Neo4jSequenceGenerator(GraphDatabaseService neo4jDb) {
		this.neo4jDb = neo4jDb;
	}

	/**
	 * Change the schema of the db creating a unique constraint on the nodes labeled as OGM_SEQUENCE.
	 * <p>
	 * The unique constraint is created on the property "sequenceId" only if it does not exists already. It is required
	 * to avoid the creation of multiple nodes with the same id.
	 *
	 * @param neo4jDb the db where the schema will be updated
	 */
	public void createUniqueConstraint(Map<String, Set<String>> sequences) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();
			for ( Entry<String, Set<String>> entry : sequences.entrySet() ) {
				String generatorKey = entry.getKey();
				for ( String segmentName : entry.getValue() ) {
					Label label = DynamicLabel.label( generatorKey );
					if ( isMissingUniqueConstraint( neo4jDb, label, segmentName ) ) {
						neo4jDb.schema().constraintFor( label ).assertPropertyIsUnique( segmentName ).create();
					}
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
	 * Unique constraints violation might occurs during the creation of the sequences because Neo4j cannot apply a lock
	 * to a node that does not exist yet. In this case the method will try again as many times as defined in the
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
				// It might happen that two threads want to create a new sequence node for the first time (Neo4j cannot
				// acquire a lock an a node that doesn't exist).
				// In this case the two node will have the same sequence name and the same initial value, viol;ating the
				// uniwue constraint defined at startup.
				log.errorGeneratingSequence( e );
			}
		}
		// This should never happen
		throw log.cannotGenerateSequence();
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
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put( "initialValue", initialValue );
		ExecutionEngine engine = new ExecutionEngine( neo4jDb );
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
	 * MERGE (n:hibernate_sequences:DistributedRevisionControl)  ON CREATE SET n.value = {initialValue} RETURN n
	 * </pre>
	 */
	private String getQuery(RowKey rowKey) {
		String query = queryCache.get( rowKey );
		if ( query == null ) {
			query = "MERGE (n:" + rowKey.getTable() + ":" + sequenceName( rowKey ) + ") ON CREATE SET n.`" + sequenceName( rowKey )
					+ "` = {initialValue} RETURN n";
			queryCache.put( rowKey, query );
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

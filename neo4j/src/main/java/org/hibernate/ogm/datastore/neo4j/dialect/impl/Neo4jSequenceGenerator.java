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
 * The value of a sequence is stored in a node identified by an id representing a {@link RowKey}. The node representing
 * a sequence is going to be labeled OGM_SEQUENCE. A unique constraint on the id of the nodes labeled as OGM_SEQUENCE is
 * created to avoid duplicated node with the same id. The next value of the sequence is saved in the "value" property.
 * <p>
 * Using cypher, an example of a sequence node looks like this:
 *
 * <pre>
 * (n:OGM_SEQUENCE {sequenceId:"tableColumnNameCOlumnValue", value: 3})
 * </pre>
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jSequenceGenerator {

	private static final String ID_SEQUENCE_PROPERTY = "sequenceId";
	private static final String VALUE_SEQUENCE_PROPERTY = "value";
	private static final Label SEQUENCE_LABEL = DynamicLabel.label( "OGM_SEQUENCE" );

	/**
	 * <pre>
	 * MERGE (n:OGM_SEQUENCE {sequenceId: {sequenceId}}) ON CREATE SET n.value = {initialValue} RETURN n</pre>
	 */
	private static final String UPDATE_NODE_QUERY = "MERGE (n:" + SEQUENCE_LABEL.name() + "{" + ID_SEQUENCE_PROPERTY + ": {sequenceId}}) ON CREATE SET n."
			+ VALUE_SEQUENCE_PROPERTY + " = {initialValue} RETURN n";

	private final GraphDatabaseService neo4jDb;

	public Neo4jSequenceGenerator(GraphDatabaseService neo4jDb) {
		this.neo4jDb = neo4jDb;
		createUniqueConstraints( neo4jDb );
	}

	/**
	 * Change the schema of the db creating a unique constraint on the nodes labeled as OGM_SEQUENCE.
	 * <p>
	 * The unique constraint is created on the property "sequenceId" only if it does not exists already.
	 * It is required to avoid the creation of multiple nodes with the same id.
	 *
	 * @param neo4jDb the db where the schema will be updated
	 */
	private void createUniqueConstraints(GraphDatabaseService neo4jDb) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();
			if ( isMissingUniqueConstraint( neo4jDb ) ) {
				neo4jDb.schema().constraintFor( SEQUENCE_LABEL ).assertPropertyIsUnique( ID_SEQUENCE_PROPERTY ).create();
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	private boolean isMissingUniqueConstraint(GraphDatabaseService neo4jDb) {
		Iterable<ConstraintDefinition> constraints = neo4jDb.schema().getConstraints( SEQUENCE_LABEL );
		for ( ConstraintDefinition constraint : constraints ) {
			if ( constraint.isConstraintType( ConstraintType.UNIQUENESS ) ) {
				for ( String property : constraint.getPropertyKeys() ) {
					if ( ID_SEQUENCE_PROPERTY.equals( property ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Generate the next value in a sequence for a given {@link RowKey}.
	 *
	 * @param rowKey identifies the sequence
	 * @param increment the difference between to consecutive values in the sequence
	 * @param initialValue the first value returned when a new sequence is created
	 * @return the next value in a sequence
	 */
	public int nextValue(RowKey rowKey, int increment, final int initialValue) {
		Transaction tx = neo4jDb.beginTx();
		Lock lock = null;
		try {
			Node sequence = getOrCreateSequence( rowKey, initialValue );
			lock = tx.acquireWriteLock( sequence );
			int nextValue = updateSequenceValue( sequence, increment );
			tx.success();
			lock.release();
			return nextValue;
		}
		finally {
			tx.close();
		}
	}

	/**
	 * Generate a unique id for a given {@link RowKey}.
	 *
	 * @param key the RowKey to identify
	 * @return the unique id
	 */
	private Object generateId(RowKey key) {
		StringBuilder builder = new StringBuilder( key.getTable() );
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			builder.append( key.getColumnNames()[i] );
			builder.append( key.getColumnValues()[i] );
		}
		return builder.toString();
	}

	/**
	 * Create a new node for the given {@link RowKey} or update the existing one.
	 *
	 * @param key the {@link RowKey} identifying the sequence
	 * @param initialValue the initial value of the sequence
	 * @return the node representing the sequence
	 */
	private Node getOrCreateSequence(RowKey key, final int initialValue) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put( "sequenceId", generateId( key ) );
		parameters.put( "initialValue", initialValue );
		ExecutionEngine engine = new ExecutionEngine( neo4jDb );
		ExecutionResult result = engine.execute( UPDATE_NODE_QUERY, parameters );
		ResourceIterator<Node> column = result.columnAs( "n" );
		Node node = null;
		if ( column.hasNext() ) {
			node = column.next();
		}
		column.close();
		return node;
	}

	private int updateSequenceValue(Node sequence, int increment) {
		int currentValue = (Integer) sequence.getProperty( VALUE_SEQUENCE_PROPERTY );
		int updatedValue = currentValue + increment;
		sequence.setProperty( VALUE_SEQUENCE_PROPERTY, updatedValue );
		return currentValue;
	}

}

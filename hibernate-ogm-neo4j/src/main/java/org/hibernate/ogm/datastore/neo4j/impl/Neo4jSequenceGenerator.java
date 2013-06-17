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
package org.hibernate.ogm.datastore.neo4j.impl;

import java.util.Arrays;
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
		UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory( neo4jDb, sequenceIndexName ) {
			@Override
			protected void initialize(Node created, Map<String, Object> properties) {
				created.setProperty( ID_SEQUENCE_PROPERTY, properties.get( ID_SEQUENCE_PROPERTY ) );
				created.setProperty( VALUE_SEQUENCE_PROPERTY, initialValue );
			}
		};
		Node sequenceNode = factory.getOrCreate( ID_SEQUENCE_PROPERTY, generateId( key ) );
		return sequenceNode;
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

	private static Object generateId(RowKey key) {
		return generateId( key.getTable(), key.getColumnNames(), key.getColumnValues() );
	}

	private static String generateId(String table, String[] columnNames, Object[] columnValues) {
		StringBuilder builder = new StringBuilder( table );
		for ( int i = 0; i < columnNames.length; i++ ) {
			builder.append( ":" );
			builder.append( columnValues[i] );
		}
		String[] split = builder.toString().split( ":" );
		Arrays.sort( split );
		return Arrays.toString( split );
	}

}

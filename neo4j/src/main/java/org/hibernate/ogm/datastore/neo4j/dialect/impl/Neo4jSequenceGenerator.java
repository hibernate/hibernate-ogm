/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.IdGeneratorKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKeyMetadata.IdGeneratorType;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
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
 * <li> the label {@link NodeLabel#SEQUENCE}
 * </ul>
 * <p>
 * Sequences are created at startup.
 * <p>
 * Using cypher, an example of a sequence node looks like this:
 *
 * <pre>
 * (:hibernate_sequences:SEQUENCE { sequence_name = 'ExampleSequence', current_value : 3 })
 * </pre>
 * <p>
 * A write lock is acquired on the node every time the sequence needs to be updated.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public class Neo4jSequenceGenerator {

	private static final String INITIAL_VALUE_QUERY_PARAM = "initialValue";
	private static final String SEQUENCE_NAME_QUERY_PARAM = "sequenceName";

	/**
	 * Name of the property of SEQUENCE nodes which holds the sequence name.
	 */
	private static final String SEQUENCE_NAME_PROPERTY = "sequence_name";

	/**
	 * Name of the property of SEQUENCE nodes which holds the next value.
	 */
	private static final String SEQUENCE_VALUE_PROPERTY = "next_val";

	/**
	 * Query for creating SEQUENCE nodes.
	 */
	private static final String SEQUENCE_CREATION_QUERY = "MERGE (n:" + NodeLabel.SEQUENCE.name() + " {" + SEQUENCE_NAME_PROPERTY + ": {sequenceName}} ) ON CREATE SET n." + SEQUENCE_VALUE_PROPERTY + " = {initialValue} RETURN n";

	/**
	 * Query for retrieving the next value from SEQUENCE nodes.
	 */
	private static final String SEQUENCE_VALUE_QUERY = "MATCH (n:" + NodeLabel.SEQUENCE.name() + ") WHERE n." + SEQUENCE_NAME_PROPERTY + " = {sequenceName} RETURN n";

	private final BoundedConcurrentHashMap<String, String> queryCache;

	private final GraphDatabaseService neo4jDb;

	private final ExecutionEngine engine;

	public Neo4jSequenceGenerator(GraphDatabaseService neo4jDb, int sequenceCacheMaxSize) {
		this.neo4jDb = neo4jDb;
		this.engine = new ExecutionEngine( neo4jDb );
		this.queryCache = new BoundedConcurrentHashMap<String, String>( sequenceCacheMaxSize, 20, BoundedConcurrentHashMap.Eviction.LIRS );
	}

	/**
	 * Create the sequence nodes setting the initial value if the node does not exists already.
	 * <p>
	 * All nodes are created inside the same transaction
	 *
	 * @param identifierGenerators the generators representing the sequences
	 */
	public void createSequences(Set<IdentifierGenerator> identifierGenerators) {
		addUniqueConstraints( identifierGenerators );
		addSequences( identifierGenerators );
	}

	private void addUniqueConstraints(Set<IdentifierGenerator> identifierGenerators) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();
			for ( IdentifierGenerator identifierGenerator : identifierGenerators ) {
				addUniqueConstraint( identifierGenerator );
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	private void addUniqueConstraint(IdentifierGenerator identifierGenerator) {
		if ( identifierGenerator instanceof OgmSequenceGenerator ) {
			addUniqueConstraintForSequence( ( (OgmSequenceGenerator) identifierGenerator ).generatorKey() );
		}
		else if ( identifierGenerator instanceof OgmTableGenerator ) {
			addUniqueConstraintForTableBasedSequence( ( (OgmTableGenerator) identifierGenerator ).generatorKey() );
		}
	}

	private void addUniqueConstraintForSequence(IdGeneratorKeyMetadata generatorKeyMetadata) {
		if ( isMissingUniqueConstraint( NodeLabel.SEQUENCE ) ) {
			neo4jDb.schema().constraintFor( NodeLabel.SEQUENCE ).assertPropertyIsUnique( SEQUENCE_NAME_PROPERTY ).create();
		}
	}

	private void addUniqueConstraintForTableBasedSequence(IdGeneratorKeyMetadata generatorKeyMetadata) {
		Label generatorKeyLabel = DynamicLabel.label( generatorKeyMetadata.getName() );
		if ( isMissingUniqueConstraint( generatorKeyLabel ) ) {
			neo4jDb.schema().constraintFor( generatorKeyLabel ).assertPropertyIsUnique( generatorKeyMetadata.getKeyColumnName() ).create();
		}
	}

	private boolean isMissingUniqueConstraint(Label generatorKeyLabel) {
		Iterable<ConstraintDefinition> constraints = neo4jDb.schema().getConstraints( generatorKeyLabel );
		for ( ConstraintDefinition constraint : constraints ) {
			if ( constraint.isConstraintType( ConstraintType.UNIQUENESS ) ) {
				return false;
			}
		}
		return true;
	}

	private void addSequences(Set<IdentifierGenerator> identifierGenerators) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();
			for ( IdentifierGenerator generator : identifierGenerators ) {
				addSequence( generator );
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	private void addSequence(IdentifierGenerator identifierGenerator) {
		if ( identifierGenerator instanceof OgmSequenceGenerator ) {
			OgmSequenceGenerator sequenceGenerator = (OgmSequenceGenerator) identifierGenerator;
			addSequence( sequenceGenerator.generatorKey(), sequenceGenerator.getInitialValue() );
		}
		else if ( identifierGenerator instanceof OgmTableGenerator ) {
			OgmTableGenerator sequenceGenerator = (OgmTableGenerator) identifierGenerator;
			addTableSequence( sequenceGenerator.generatorKey(), sequenceGenerator.getSegmentValue(), sequenceGenerator.getInitialValue() );
		}
	}

	/**
	 * Ex.:
	 * <pre>
	 * MERGE (n:hibernate_sequences:TABLE_BASED_SEQUENCE {sequence_name: {sequenceName}}) ON CREATE SET n.current_value = {initialValue} RETURN n
	 * </pre>
	 */
	private void addTableSequence(IdGeneratorKeyMetadata generatorKeyMetadata, String sequenceName, int initialValue) {
		Label generatorKeyLabel = DynamicLabel.label( generatorKeyMetadata.getName() );
		String query = "MERGE (n" + labels( generatorKeyLabel.name(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) + " { " + generatorKeyMetadata.getKeyColumnName() + ": {"
				+ SEQUENCE_NAME_QUERY_PARAM + "}} ) ON CREATE SET n." + generatorKeyMetadata.getValueColumnName() + " = {" + INITIAL_VALUE_QUERY_PARAM + "} RETURN n";
		engine.execute( query, params( sequenceName, initialValue ) );
	}

	/**
	 * Ex.:
	 * <pre>
	 * MERGE (n:SEQUENCE {sequence_name: {sequenceName}}) ON CREATE SET n.current_value = {initialValue} RETURN n
	 * </pre>
	 */
	private void addSequence(IdGeneratorKeyMetadata generatorKeyMetadata, int initialValue) {
		engine.execute( SEQUENCE_CREATION_QUERY, params( generatorKeyMetadata.getName(), initialValue ) );
	}

	private Map<String, Object> params(String sequenceName, int initialValue) {
		Map<String, Object> params = new HashMap<String, Object>( 2 );
		params.put( INITIAL_VALUE_QUERY_PARAM, initialValue );
		params.put( SEQUENCE_NAME_QUERY_PARAM, sequenceName );
		return params;
	}

	/**
	 * Generate the next value in a sequence for a given {@link RowKey}.
	 *
	 * @param generatorKey identifies the generator
	 * @param increment the difference between to consecutive values in the sequence
	 * @return the next value in a sequence
	 */
	public int nextValue(IdGeneratorKey generatorKey, int increment) {
		return sequence( generatorKey, increment );
	}

	private int sequence(IdGeneratorKey generatorKey, int increment) {
		Transaction tx = neo4jDb.beginTx();
		Lock lock = null;
		try {
			Node sequence = getSequence( generatorKey );
			lock = tx.acquireWriteLock( sequence );
			int nextValue = updateSequenceValue( generatorKey, sequence, increment );
			tx.success();
			lock.release();
			return nextValue;
		}
		finally {
			tx.close();
		}
	}

	/**
	 * Given a {@link RowKey}, get the corresponding sequence node.
	 *
	 * @param key the {@link RowKey} identifying the sequence
	 * @return the node representing the sequence
	 */
	private Node getSequence(IdGeneratorKey generatorKey) {
		String updateSequenceQuery = getQuery( generatorKey );
		ExecutionResult result = engine.execute( updateSequenceQuery, singletonMap( SEQUENCE_NAME_QUERY_PARAM, (Object) sequenceName( generatorKey ) ) );
		ResourceIterator<Node> column = result.columnAs( "n" );
		Node node = null;
		if ( column.hasNext() ) {
			node = column.next();
		}
		column.close();
		return node;
	}

	private String getQuery(IdGeneratorKey generatorKey) {
		return generatorKey.getMetadata().getType() == IdGeneratorType.TABLE ? getTableQuery( generatorKey ) : SEQUENCE_VALUE_QUERY;
	}

	/**
	 * Ex.:
	 * <pre>
	 * MATCH (n:hibernate_sequences:TABLE_BASED_SEQUENCE) WHERE n.sequence_name = {sequenceName} RETURN n
	 * </pre>
	 */
	private String getTableQuery(IdGeneratorKey generatorKey) {
		String query = queryCache.get( generatorKey.getTable() );
		if ( query == null ) {
			query = "MATCH (n" + labels( generatorKey.getTable(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) + ") WHERE n." + generatorKey.getMetadata().getKeyColumnName() + " = {"
						+ SEQUENCE_NAME_QUERY_PARAM + "} RETURN n";
			String cached = queryCache.putIfAbsent( generatorKey.getTable(), query );
			if ( cached != null ) {
				query = cached;
			}
		}
		return query;
	}

	private String labels(String... labels) {
		StringBuilder builder = new StringBuilder();
		for ( String label : labels ) {
			builder.append( ":`" );
			builder.append( label );
			builder.append( "`" );
		}
		return builder.toString();
	}

	private String sequenceName(IdGeneratorKey key) {
		return key.getMetadata().getType() == IdGeneratorType.SEQUENCE ? key.getMetadata().getName() : (String) key.getColumnValues()[0];
	}

	private int updateSequenceValue(IdGeneratorKey generatorKey, Node sequence, int increment) {
		String valueProperty = generatorKey.getMetadata().getType() == IdGeneratorType.TABLE ? generatorKey.getMetadata().getValueColumnName() : SEQUENCE_VALUE_PROPERTY;
		int currentValue = (Integer) sequence.getProperty( valueProperty );
		int updatedValue = currentValue + increment;
		sequence.setProperty( valueProperty, updatedValue );
		return currentValue;
	}
}

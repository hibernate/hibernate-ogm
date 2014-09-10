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

import org.hibernate.HibernateException;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;
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
 * Generates the next value of an id sequence as represented by {@link IdSourceKey}.
 * <p>
 * Both, {@link IdSourceType#TABLE} and {@link IdSourceType#SEQUENCE} are supported. For the table strategy, nodes in
 * the following form are used (the exact property names and the label value can be configured using the options exposed
 * by {@link OgmTableGenerator}):
 *
 * <pre>
 * (:hibernate_sequences:TABLE_BASED_SEQUENCE { sequence_name = 'ExampleSequence', current_value : 3 })
 * </pre>
 *
 * For the sequence strategy, nodes in the following form are used (the sequence name can be configured using the option
 * exposed by {@link OgmSequenceGenerator}):
 *
 * <pre>
 * (:SEQUENCE { sequence_name = 'ExampleSequence', next_val : 3 })
 * </pre>
 *
 * Sequences are created at startup.
 * <p>
 * A write lock is acquired on the node every time the sequence needs to be updated.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class Neo4jSequenceGenerator {

	private static final String INITIAL_VALUE_QUERY_PARAM = "initialValue";
	private static final String SEQUENCE_NAME_QUERY_PARAM = "sequenceName";

	/**
	 * Name of the property of SEQUENCE nodes which holds the sequence name. ORM's default for emulated sequences,
	 * "sequence_name", is used.
	 */
	private static final String SEQUENCE_NAME_PROPERTY = "sequence_name";

	/**
	 * Name of the property of SEQUENCE nodes which holds the next value. ORM's default for emulated sequences,
	 * "next_val", is used.
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
	public void createSequences(Set<PersistentNoSqlIdentifierGenerator> identifierGenerators) {
		addUniqueConstraints( identifierGenerators );
		addSequences( identifierGenerators );
	}

	private void addUniqueConstraints(Set<PersistentNoSqlIdentifierGenerator> identifierGenerators) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();
			for ( PersistentNoSqlIdentifierGenerator identifierGenerator : identifierGenerators ) {
				addUniqueConstraint( identifierGenerator );
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	private void addUniqueConstraint(PersistentNoSqlIdentifierGenerator identifierGenerator) {
		if ( identifierGenerator.getGeneratorKeyMetadata().getType() == IdSourceType.SEQUENCE ) {
			addUniqueConstraintForSequence( identifierGenerator.getGeneratorKeyMetadata() );
		}
		if ( identifierGenerator.getGeneratorKeyMetadata().getType() == IdSourceType.TABLE ) {
			addUniqueConstraintForTableBasedSequence( identifierGenerator.getGeneratorKeyMetadata() );
		}
	}

	private void addUniqueConstraintForSequence(IdSourceKeyMetadata idSourceKeyMetadata) {
		if ( isMissingUniqueConstraint( NodeLabel.SEQUENCE ) ) {
			neo4jDb.schema().constraintFor( NodeLabel.SEQUENCE ).assertPropertyIsUnique( SEQUENCE_NAME_PROPERTY ).create();
		}
	}

	/**
	 * Adds a unique constraint to make sure that each node of the same "sequence table" is unique.
	 */
	private void addUniqueConstraintForTableBasedSequence(IdSourceKeyMetadata generatorKeyMetadata) {
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

	/**
	 * Adds a node for each generator of type {@link IdSourceType#SEQUENCE}. Table-based generators are created lazily
	 * at runtime.
	 *
	 * @param identifierGenerators the generators to process
	 */
	private void addSequences(Set<PersistentNoSqlIdentifierGenerator> identifierGenerators) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();
			for ( PersistentNoSqlIdentifierGenerator generator : identifierGenerators ) {
				addSequence( generator );
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	private void addSequence(PersistentNoSqlIdentifierGenerator identifierGenerator) {
		if ( identifierGenerator.getGeneratorKeyMetadata().getType() == IdSourceType.SEQUENCE ) {
			addSequence( identifierGenerator.getGeneratorKeyMetadata(), identifierGenerator.getInitialValue() );
		}
	}

	/**
	 * Ex.:
	 * <pre>
	 * MERGE (n:hibernate_sequences:TABLE_BASED_SEQUENCE {sequence_name: {sequenceName}}) ON CREATE SET n.current_value = {initialValue} RETURN n
	 * </pre>
	 */
	private void addTableSequence(IdSourceKeyMetadata idSourceKeyMetadata, String sequenceName, int initialValue) {
		Label generatorKeyLabel = DynamicLabel.label( idSourceKeyMetadata.getName() );
		String query = "MERGE (n" + labels( generatorKeyLabel.name(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) + " { " + idSourceKeyMetadata.getKeyColumnName() + ": {"
				+ SEQUENCE_NAME_QUERY_PARAM + "}} ) ON CREATE SET n." + idSourceKeyMetadata.getValueColumnName() + " = {" + INITIAL_VALUE_QUERY_PARAM + "} RETURN n";
		engine.execute( query, params( sequenceName, initialValue ) );
	}

	/**
	 * Ex.:
	 * <pre>
	 * MERGE (n:SEQUENCE {sequence_name: {sequenceName}}) ON CREATE SET n.current_value = {initialValue} RETURN n
	 * </pre>
	 */
	private void addSequence(IdSourceKeyMetadata idSourceKeyMetadata, int initialValue) {
		engine.execute( SEQUENCE_CREATION_QUERY, params( idSourceKeyMetadata.getName(), initialValue ) );
	}

	private Map<String, Object> params(String sequenceName, int initialValue) {
		Map<String, Object> params = new HashMap<String, Object>( 2 );
		params.put( INITIAL_VALUE_QUERY_PARAM, initialValue );
		params.put( SEQUENCE_NAME_QUERY_PARAM, sequenceName );
		return params;
	}

	/**
	 * Generate the next value in a sequence for a given {@link IdSourceKey}.
	 *
	 * @param idSourceKey identifies the generator
	 * @param increment the difference between to consecutive values in the sequence
	 * @param initialValue the initial value of the given generator
	 * @return the next value in a sequence
	 */
	public int nextValue(IdSourceKey idSourceKey, int increment, int initialValue) {
		Transaction tx = neo4jDb.beginTx();
		Lock lock = null;
		try {
			Node sequence = getSequence( idSourceKey );

			if ( sequence == null ) {
				// sequence nodes are expected to have been created up-front
				if ( idSourceKey.getMetadata().getType() == IdSourceType.SEQUENCE ) {
					throw new HibernateException( "Sequence missing: " + idSourceKey.getMetadata().getName() );
				}
				// table sequence nodes (think of them as rows in a generator table) are created upon first usage
				else {
					addTableSequence( idSourceKey.getMetadata(), (String) idSourceKey.getColumnValues()[0], initialValue );
					sequence = getSequence( idSourceKey );
				}
			}

			lock = tx.acquireWriteLock( sequence );
			int nextValue = updateSequenceValue( idSourceKey, sequence, increment );
			tx.success();
			lock.release();
			return nextValue;
		}
		finally {
			tx.close();
		}
	}

	/**
	 * Given a {@link IdSourceKey}, get the corresponding sequence node.
	 *
	 * @param idSourceKey the {@link IdSourceKey} identifying the sequence
	 * @return the node representing the sequence
	 */
	private Node getSequence(IdSourceKey idSourceKey) {
		String updateSequenceQuery = getQuery( idSourceKey );
		ExecutionResult result = engine.execute( updateSequenceQuery, singletonMap( SEQUENCE_NAME_QUERY_PARAM, (Object) sequenceName( idSourceKey ) ) );
		ResourceIterator<Node> column = result.columnAs( "n" );
		Node node = null;
		if ( column.hasNext() ) {
			node = column.next();
		}
		column.close();
		return node;
	}

	private String getQuery(IdSourceKey idSourceKey) {
		return idSourceKey.getMetadata().getType() == IdSourceType.TABLE ? getTableQuery( idSourceKey ) : SEQUENCE_VALUE_QUERY;
	}

	/**
	 * Ex.:
	 * <pre>
	 * MATCH (n:hibernate_sequences:TABLE_BASED_SEQUENCE) WHERE n.sequence_name = {sequenceName} RETURN n
	 * </pre>
	 */
	private String getTableQuery(IdSourceKey idSourceKey) {
		String query = queryCache.get( idSourceKey.getTable() );
		if ( query == null ) {
			query = "MATCH (n" + labels( idSourceKey.getTable(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) + ") WHERE n." + idSourceKey.getMetadata().getKeyColumnName() + " = {"
						+ SEQUENCE_NAME_QUERY_PARAM + "} RETURN n";
			String cached = queryCache.putIfAbsent( idSourceKey.getTable(), query );
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

	private String sequenceName(IdSourceKey key) {
		return key.getMetadata().getType() == IdSourceType.SEQUENCE ? key.getMetadata().getName() : (String) key.getColumnValues()[0];
	}

	private int updateSequenceValue(IdSourceKey idSourceKey, Node sequence, int increment) {
		String valueProperty = idSourceKey.getMetadata().getType() == IdSourceType.TABLE ? idSourceKey.getMetadata().getValueColumnName() : SEQUENCE_VALUE_PROPERTY;
		int currentValue = (Integer) sequence.getProperty( valueProperty );
		int updatedValue = currentValue + increment;
		sequence.setProperty( valueProperty, updatedValue );
		return currentValue;
	}
}

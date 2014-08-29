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

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.grid.IdSourceKey;
import org.hibernate.ogm.grid.IdSourceKeyMetadata;
import org.hibernate.ogm.grid.IdSourceKeyMetadata.IdSourceType;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.kernel.GraphDatabaseAPI;

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

	private static final Log log = LoggerFactory.getLogger();

	private static final String INITIAL_VALUE_QUERY_PARAM = "initialValue";
	private static final String SEQUENCE_NAME_QUERY_PARAM = "sequenceName";

	/**
	 * Name of the property of SEQUENCE nodes which holds the sequence name. ORM's default for emulated sequences,
	 * "sequence_name", is used.
	 */
	private static final String SEQUENCE_NAME_PROPERTY = OgmTableGenerator.DEF_SEGMENT_COLUMN;

	/**
	 * Name of the property of SEQUENCE nodes which holds the next value. ORM's default for emulated sequences,
	 * "next_val", is used.
	 */
	private static final String SEQUENCE_VALUE_PROPERTY = OgmTableGenerator.DEF_VALUE_COLUMN;

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
	 * @param sequences the generators representing the sequences
	 * @param registry
	 */
	public void createSequences(Set<PersistentNoSqlIdentifierGenerator> sequences, TransactionManager txManager) {
		addUniqueConstraints( sequences, txManager );
		addSequences( sequences, txManager );
	}

	private void addUniqueConstraints(Set<PersistentNoSqlIdentifierGenerator> identifierGenerators, TransactionManager txManager) {
		try {
			Transaction suspend = txManager.suspend();
			txManager.begin();
			for ( IdentifierGenerator identifierGenerator : identifierGenerators ) {
				addUniqueConstraint( identifierGenerator );
			}
			txManager.commit();
			if (suspend != null) {
				txManager.resume( suspend );
			}
		}
		catch (Exception e) {
			throw log.errorAddingUniqueConstraint( e );
		}
	}

	private void addSequences(Set<PersistentNoSqlIdentifierGenerator> identifierGenerators, TransactionManager txManager) {
		try {
			Transaction suspend = txManager.suspend();
			txManager.begin();
			for ( IdentifierGenerator generator : identifierGenerators ) {
				addSequence( generator );
			}
			txManager.commit();
			if (suspend != null) {
				txManager.resume( suspend );
			}
		}
		catch (Exception e) {
			throw log.errorAddingSequence( e );
		}
	}

	private void addUniqueConstraint(IdentifierGenerator identifierGenerator) {
		if ( identifierGenerator instanceof OgmSequenceGenerator ) {
			addUniqueConstraintForSequence( ( (OgmSequenceGenerator) identifierGenerator ).getGeneratorKeyMetadata() );
		}
		else if ( identifierGenerator instanceof OgmTableGenerator ) {
			addUniqueConstraintForTableBasedSequence( ( (OgmTableGenerator) identifierGenerator ).getGeneratorKeyMetadata() );
		}
	}

	private void addUniqueConstraintForSequence(IdSourceKeyMetadata idSourceKeyMetadata) {
		if ( isMissingUniqueConstraint( NodeLabel.SEQUENCE ) ) {
			neo4jDb.schema().constraintFor( NodeLabel.SEQUENCE ).assertPropertyIsUnique( SEQUENCE_NAME_PROPERTY ).create();
		}
	}

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

	private void addSequence(IdentifierGenerator identifierGenerator) {
		if ( identifierGenerator instanceof OgmSequenceGenerator ) {
			OgmSequenceGenerator sequenceGenerator = (OgmSequenceGenerator) identifierGenerator;
			addSequence( sequenceGenerator.getGeneratorKeyMetadata(), sequenceGenerator.getInitialValue() );
		}
		else if ( identifierGenerator instanceof OgmTableGenerator ) {
			OgmTableGenerator sequenceGenerator = (OgmTableGenerator) identifierGenerator;
			addTableSequence( sequenceGenerator.getGeneratorKeyMetadata(), sequenceGenerator.getSegmentValue(), sequenceGenerator.getInitialValue() );
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
	 * Generate the next value in a sequence for a given {@link RowKey}.
	 *
	 * @param idSourceKey identifies the generator
	 * @param increment the difference between to consecutive values in the sequence
	 * @return the next value in a sequence
	 */
	public int nextValue(IdSourceKey idSourceKey, int increment) {
		TransactionManager txManager = transactionManager();
		Lock lock = null;
		org.neo4j.graphdb.Transaction tx = null;
		try {
			Transaction suspend = txManager.suspend();
			tx = neo4jDb.beginTx();
			Node sequence = getSequence( idSourceKey );
			lock = tx.acquireWriteLock( sequence );
			int nextValue = updateSequenceValue( idSourceKey, sequence, increment );
			lock.release();
			txManager.commit();
			if ( suspend != null ) {
				txManager.resume( suspend );
			}
			return nextValue;
		}
		catch (Exception e) {
			throw log.errorIncrementingSequenceValue( e );
		}
	}

	@SuppressWarnings("deprecation") // Only way to obtain the txManager form neo4j
	private TransactionManager transactionManager() {
		return ( (GraphDatabaseAPI) neo4jDb ).getDependencyResolver().resolveDependency( TransactionManager.class );
	}

	/**
	 * Given a {@link RowKey}, get the corresponding sequence node.
	 *
	 * @param idSourceKey the {@link RowKey} identifying the sequence
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

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.dialect.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;

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
public class RemoteNeo4jSequenceGenerator extends BaseNeo4jSequenceGenerator {

	/**
	 * Query for creating SEQUENCE nodes.
	 */
	protected static final String SEQUENCE_CREATION_QUERY =
			"MERGE (n:" + NodeLabel.SEQUENCE.name() + " {" + SEQUENCE_NAME_PROPERTY + ": {sequenceName}} )"
			+ " RETURN n";

	protected static final String SEQUENCE_LOCK_QUERY =
			"MATCH (n:" + NodeLabel.SEQUENCE.name() + ")"
			+ " WHERE n." + SEQUENCE_NAME_PROPERTY + " = {sequenceName} "
			+ " SET n.__locked = true "
			+ " RETURN n";

	/**
	 * Query for retrieving the next value from SEQUENCE nodes.
	 */
	private static final String SEQUENCE_VALUE_QUERY =
			"MATCH (n:" + NodeLabel.SEQUENCE.name() + ")"
			+ " WHERE n." + SEQUENCE_NAME_PROPERTY + " = {sequenceName} "
			+ " REMOVE n.__locked "
			+ " SET n." + SEQUENCE_VALUE_PROPERTY + " = coalesce(n." + SEQUENCE_VALUE_PROPERTY + ", {initialValue}) + {increment}"
			+ " RETURN n." + SEQUENCE_VALUE_PROPERTY;

	private static final Log logger = LoggerFactory.getLogger();

	private final BoundedConcurrentHashMap<String, Statements> queryCache;

	private final RemoteNeo4jClient neo4jDb;

	public RemoteNeo4jSequenceGenerator(RemoteNeo4jClient neo4jDb, int sequenceCacheMaxSize) {
		this.neo4jDb = neo4jDb;
		this.queryCache = new BoundedConcurrentHashMap<String, Statements>( sequenceCacheMaxSize, 20, BoundedConcurrentHashMap.Eviction.LIRS );
	}

	/**
	 * Create the sequence nodes setting the initial value if the node does not exist already.
	 * <p>
	 * All nodes are created inside the same transaction.
	 */
	public void createSequencesConstraints(Statements statements, Iterable<Sequence> sequences) {
		addUniqueConstraintForSequences( statements );
	}

	public void createSequences(Statements statements, Iterable<Sequence> sequences) {
		addSequences( statements, sequences );
	}

	private void addUniqueConstraintForSequences(Statements statements) {
		Statement statement = createUniqueConstraintStatement( SEQUENCE_NAME_PROPERTY, NodeLabel.SEQUENCE.name() );
		statements.addStatement( statement );
	}

	/**
	 * Adds a unique constraint to make sure that each node of the same "sequence table" is unique.
	 */
	private void addUniqueConstraintForTableBasedSequence(Statements statements, IdSourceKeyMetadata generatorKeyMetadata) {
		Statement statement = createUniqueConstraintStatement( generatorKeyMetadata.getKeyColumnName(), generatorKeyMetadata.getName() );
		statements.addStatement( statement );
	}

	private Statement createUniqueConstraintStatement(String propertyName, String label) {
		String queryString = createUniqueConstraintQuery( propertyName, label );
		Statement statement = new Statement( queryString );
		return statement;
	}

	/**
	 * Adds a node for each generator of type {@link IdSourceType#SEQUENCE}. Table-based generators are created lazily
	 * at runtime.
	 *
	 * @param sequences the generators to process
	 */
	private void addSequences(Statements statements, Iterable<Sequence> sequences) {
		for ( Sequence sequence : sequences ) {
			addSequence( statements, sequence );
		}
	}

	private void addSequence(Statements statements, Sequence sequence) {
		Statement statement = new Statement( SEQUENCE_CREATION_QUERY, Collections.<String, Object>singletonMap( SEQUENCE_NAME_QUERY_PARAM, sequence.getName().render() ) );
		statements.addStatement( statement );
	}

	protected String acquireLockQuery(NextValueRequest request) {
		StringBuilder queryBuilder = new StringBuilder();
		IdSourceKeyMetadata metadata = request.getKey().getMetadata();
		queryBuilder.append( "MERGE (n" );
		queryBuilder.append( labels( metadata.getName(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) );
		queryBuilder.append( " { " );
		queryBuilder.append( metadata.getKeyColumnName() );
		queryBuilder.append( ": {" );
		queryBuilder.append( SEQUENCE_NAME_QUERY_PARAM );
		queryBuilder.append( "}} ) " );
		queryBuilder.append( " ON MATCH SET n.__locked=true " );
		queryBuilder.append( " ON CREATE SET n.__locked=true, n." );
		queryBuilder.append( metadata.getValueColumnName() );
		queryBuilder.append( " = " );
		queryBuilder.append( request.getInitialValue() );
		queryBuilder.append( " RETURN n." );
		queryBuilder.append( metadata.getValueColumnName() );
		String query = queryBuilder.toString();
		return query;
	}

	private void getUpdateTableSequenceQuery(Statements statements, NextValueRequest request) {
		Map<String, Object> params = params( request );

		// Acquire lock
		String acquireLockQuery = acquireLockQuery( request );
		Statement acquireLockStatement = new Statement( acquireLockQuery, params );
		acquireLockStatement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		statements.addStatement( acquireLockStatement );

		// Update value
		String updateQuery = increaseQuery( request );
		Statement updateStatement = new Statement( updateQuery, params );
		updateStatement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		statements.addStatement( updateStatement );
	}

	protected String increaseQuery(NextValueRequest request) {
		StringBuilder queryBuilder = new StringBuilder();
		IdSourceKeyMetadata metadata = request.getKey().getMetadata();
		Label generatorKeyLabel = DynamicLabel.label( metadata.getName() );
		queryBuilder.append( " MATCH (n " );
		queryBuilder.append( labels( generatorKeyLabel.name(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) );
		queryBuilder.append( " { " );
		queryBuilder.append( metadata.getKeyColumnName() );
		queryBuilder.append( ": {" );
		queryBuilder.append( SEQUENCE_NAME_QUERY_PARAM );
		queryBuilder.append( "}} )" );
		queryBuilder.append( " SET n." );
		queryBuilder.append( metadata.getValueColumnName() );
		queryBuilder.append( " = n." );
		queryBuilder.append( metadata.getValueColumnName() );
		queryBuilder.append( " + " );
		queryBuilder.append( request.getIncrement() );
		queryBuilder.append( " REMOVE n.__locked RETURN n." );
		queryBuilder.append( metadata.getValueColumnName() );
		String query = queryBuilder.toString();
		return query;
	}

	/**
	 * Generate the next value in a sequence for a given {@link IdSourceKey}.
	 *
	 * @return the next value in a sequence
	 */
	@Override
	public Long nextValue(NextValueRequest request) {
		String sequenceName = sequenceName( request.getKey() );
		// This method return 2 statements: the first one to acquire a lock and the second one to update the sequence node
		Statements statements = updateNextValueQuery( request );
		StatementsResponse statementsResponse = neo4jDb.executeQueriesInNewTransaction( statements );
		List<StatementResult> results = statementsResponse.getResults();
		// We use 1, because we are interested to the result of the second statement (the one that updates the node and returns the value)
		Number nextValue = (Number) results.get( 1 ).getData().get( 0 ).getRow().get( 0 );
		// sequence nodes are expected to have been created up-front
		if ( request.getKey().getMetadata().getType() == IdSourceType.SEQUENCE ) {
			if ( nextValue == null ) {
				throw logger.sequenceNotFound( sequenceName );
			}
		}
		// The only way I found to make it work in a multi-threaded environment is to first increment the value and then read it.
		// Our API allows for an initial value and to make sure that I'm actually reading the correct one,
		// the first time I need to decrement the value I obtain from the db.
		return nextValue.longValue() - request.getIncrement();
	}

	/*
	 * This will always return 2 statements: the first one to acquire a lock and the second one to update the sequence value
	 */
	private Statements updateNextValueQuery(NextValueRequest request) {
		return request.getKey().getMetadata().getType() == IdSourceType.TABLE
				? getTableQuery( request )
				: getSequenceIncrementQuery( request );
	}

	private Statements getSequenceIncrementQuery(NextValueRequest request) {
		// Acquire a lock on the node
		String sequenceName = sequenceName( request.getKey() );
		Statement lockStatement = new Statement( SEQUENCE_LOCK_QUERY, Collections.<String, Object>singletonMap( SEQUENCE_NAME_QUERY_PARAM, sequenceName ) );
		Statements statements = new Statements();
		statements.addStatement( lockStatement );

		// Increment the value on the node
		String query = SEQUENCE_VALUE_QUERY.replace( "{increment}", String.valueOf( request.getIncrement() ) ).replace( "{initialValue}", String.valueOf( request.getInitialValue() ) );
		Statement statement = new Statement( query, params( request ) );
		statement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		statements.addStatement( statement );
		return statements;
	}

	private Statements getTableQuery(NextValueRequest request) {
		String key = key( request );
		Statements statements = queryCache.get( key );
		if ( statements == null ) {
			statements = new Statements();
			getUpdateTableSequenceQuery( statements, request );
			Statements cached = queryCache.putIfAbsent( key, statements );
			if ( cached != null ) {
				statements = cached;
			}
		}
		return statements;
	}

	public void createUniqueConstraintsForTableSequences(Statements statements, Iterable<IdSourceKeyMetadata> tableIdGenerators) {
		for ( IdSourceKeyMetadata idSourceKeyMetadata : tableIdGenerators ) {
			if ( idSourceKeyMetadata.getType() == IdSourceType.TABLE ) {
				addUniqueConstraintForTableBasedSequence( statements, idSourceKeyMetadata );
			}
		}
	}
}

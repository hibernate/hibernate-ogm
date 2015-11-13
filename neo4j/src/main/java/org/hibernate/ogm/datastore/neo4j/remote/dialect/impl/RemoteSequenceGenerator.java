/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.dialect.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.remote.impl.Neo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
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
public class RemoteSequenceGenerator {

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
	private static final String SEQUENCE_CREATION_QUERY = "MERGE (n:" + NodeLabel.SEQUENCE.name() + " {" + SEQUENCE_NAME_PROPERTY
			+ ": {sequenceName}} ) RETURN n";

	private static final String SEQUENCE_LOCK_QUERY = "MATCH (n:" + NodeLabel.SEQUENCE.name() + ") WHERE n." + SEQUENCE_NAME_PROPERTY + " = {sequenceName} "
			+ " SET n.__locked = true " + " RETURN n";

	/**
	 * Query for retrieving the next value from SEQUENCE nodes.
	 */
	private static final String SEQUENCE_VALUE_QUERY = "MATCH (n:" + NodeLabel.SEQUENCE.name() + ") WHERE n." + SEQUENCE_NAME_PROPERTY + " = {sequenceName} "
			+ " REMOVE n.__locked " + " SET n." + SEQUENCE_VALUE_PROPERTY + " = coalesce(n." + SEQUENCE_VALUE_PROPERTY + ", {initialValue}) + {increment}"
			+ " RETURN n." + SEQUENCE_VALUE_PROPERTY;

	private final BoundedConcurrentHashMap<String, Statements> queryCache;

	private final Neo4jClient neo4jDb;

	public RemoteSequenceGenerator(Neo4jClient neo4jDb, int sequenceCacheMaxSize) {
		this.neo4jDb = neo4jDb;
		this.queryCache = new BoundedConcurrentHashMap<String, Statements>( sequenceCacheMaxSize, 20, BoundedConcurrentHashMap.Eviction.LIRS );
	}

	/**
	 * Create the sequence nodes setting the initial value if the node does not exists already.
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
		StringBuilder query = new StringBuilder( "CREATE CONSTRAINT ON (n:" );
		query.append( label );
		query.append( ") ASSERT n." );
		escapeIdentifier( query, propertyName );
		query.append( " IS UNIQUE" );
		Statement statement = new Statement( query.toString() );
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

	private void getUpdateTableSequenceQuery(Statements statements, IdSourceKeyMetadata idSourceKeyMetadata, String sequenceName, int initialValue,
			int increment) {
		String acquireLockQuery = acquireLockQuery( idSourceKeyMetadata, initialValue, increment );
		Statement acquireLockStatement = new Statement( acquireLockQuery, params( sequenceName, initialValue, increment ) );
		acquireLockStatement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		statements.addStatement( acquireLockStatement );

		String updateQuery = increaseQuery( idSourceKeyMetadata, increment );
		Statement updateStatement = new Statement( updateQuery, params( sequenceName, initialValue, increment ) );
		updateStatement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		statements.addStatement( updateStatement );
	}

	private String acquireLockQuery(IdSourceKeyMetadata idSourceKeyMetadata, int initialValue, int increment) {
		StringBuilder queryBuilder = new StringBuilder();
		Label generatorKeyLabel = DynamicLabel.label( idSourceKeyMetadata.getName() );
		queryBuilder.append( "MERGE (n" );
		queryBuilder.append( labels( generatorKeyLabel.name(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) );
		queryBuilder.append( " { " );
		queryBuilder.append( idSourceKeyMetadata.getKeyColumnName() );
		queryBuilder.append( ": {" );
		queryBuilder.append( SEQUENCE_NAME_QUERY_PARAM );
		queryBuilder.append( "}} ) " );
		queryBuilder.append( " ON MATCH SET n.__locked=true " );
		queryBuilder.append( " ON CREATE SET n.__locked=true, n." );
		queryBuilder.append( idSourceKeyMetadata.getValueColumnName() );
		queryBuilder.append( " = " );
		queryBuilder.append( initialValue );
		queryBuilder.append( " RETURN n." );
		queryBuilder.append( idSourceKeyMetadata.getValueColumnName() );
		String query = queryBuilder.toString();
		return query;
	}

	private String increaseQuery(IdSourceKeyMetadata idSourceKeyMetadata, int increment) {
		StringBuilder queryBuilder = new StringBuilder();
		Label generatorKeyLabel = DynamicLabel.label( idSourceKeyMetadata.getName() );
		queryBuilder.append( " MATCH (n " );
		queryBuilder.append( labels( generatorKeyLabel.name(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) );
		queryBuilder.append( " { " );
		queryBuilder.append( idSourceKeyMetadata.getKeyColumnName() );
		queryBuilder.append( ": {" );
		queryBuilder.append( SEQUENCE_NAME_QUERY_PARAM );
		queryBuilder.append( "}} )" );
		queryBuilder.append( " SET n." );
		queryBuilder.append( idSourceKeyMetadata.getValueColumnName() );
		queryBuilder.append( " = n." );
		queryBuilder.append( idSourceKeyMetadata.getValueColumnName() );
		queryBuilder.append( " + " );
		queryBuilder.append( increment );
		queryBuilder.append( " REMOVE n.__locked RETURN n." );
		queryBuilder.append( idSourceKeyMetadata.getValueColumnName() );
		String query = queryBuilder.toString();
		return query;
	}

	private Map<String, Object> params(String sequenceName, int initialValue) {
		Map<String, Object> params = new HashMap<String, Object>( 3 );
		params.put( INITIAL_VALUE_QUERY_PARAM, initialValue );
		params.put( SEQUENCE_NAME_QUERY_PARAM, sequenceName );
		return params;
	}

	private Map<String, Object> params(String sequenceName, int initialValue, int increment) {
		Map<String, Object> params = params( sequenceName, initialValue );
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
	public Number nextValue(IdSourceKey idSourceKey, int increment, int initialValue) {
		String sequenceName = sequenceName( idSourceKey );
		Statements statements = updateNextValueQuery( idSourceKey, initialValue, increment );
		StatementsResponse statementsResponse = neo4jDb.executeQueriesInNewTransaction( statements );
		List<StatementResult> results = statementsResponse.getResults();
		Integer nextValue = (Integer) results.get( 1 ).getData().get( 0 ).getRow().get( 0 );
		// sequence nodes are expected to have been created up-front
		if ( idSourceKey.getMetadata().getType() == IdSourceType.SEQUENCE ) {
			if ( nextValue == null ) {
				throw new HibernateException( "Sequence missing: " + sequenceName );
			}
		}
		return nextValue.longValue() - increment;
	}

	private String sequenceName(IdSourceKey key) {
		return key.getMetadata().getType() == IdSourceType.SEQUENCE ? key.getMetadata().getName() : (String) key.getColumnValues()[0];
	}

	private Statements updateNextValueQuery(IdSourceKey idSourceKey, int initialValue, int increment) {
		return idSourceKey.getMetadata().getType() == IdSourceType.TABLE
				? getTableQuery( idSourceKey, initialValue, increment )
				: getSequenceIncrementQuery( idSourceKey, initialValue, increment );
	}

	private Statements getSequenceIncrementQuery(IdSourceKey idSourceKey, int initialValue, int increment) {
		String sequenceName = sequenceName( idSourceKey );
		Statement lockStatement = new Statement( SEQUENCE_LOCK_QUERY, Collections.<String, Object>singletonMap( SEQUENCE_NAME_QUERY_PARAM, sequenceName ) );
		Statements statements = new Statements();
		statements.addStatement( lockStatement );

		String query = SEQUENCE_VALUE_QUERY.replace( "{increment}", String.valueOf( increment ) ).replace( "{initialValue}", String.valueOf( initialValue ) );
		Statement statement = new Statement( query, params( sequenceName( idSourceKey ), initialValue, increment ) );
		statement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		statements.addStatement( statement );
		return statements;
	}

	private Statements getTableQuery(IdSourceKey idSourceKey, int initialValue, int increment) {
		String key = key( idSourceKey, initialValue, increment );
		Statements statements = queryCache.get( key );
		if ( statements == null ) {
			statements = new Statements();
			getUpdateTableSequenceQuery( statements, idSourceKey.getMetadata(), sequenceName( idSourceKey ), initialValue, increment );
			Statements cached = queryCache.putIfAbsent( key, statements );
			if ( cached != null ) {
				statements = cached;
			}
		}
		return statements;
	}

	private String key(IdSourceKey idSourceKey, int initialValue, int increment) {
		return idSourceKey.getTable() + ":" + initialValue + ":" + increment;
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

	public void createUniqueConstraintsForTableSequences(Statements statements, Iterable<IdSourceKeyMetadata> tableIdGenerators) {
		for ( IdSourceKeyMetadata idSourceKeyMetadata : tableIdGenerators ) {
			if ( idSourceKeyMetadata.getType() == IdSourceType.TABLE ) {
				addUniqueConstraintForTableBasedSequence( statements, idSourceKeyMetadata );
			}
		}
	}
}

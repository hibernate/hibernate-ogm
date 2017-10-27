/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.RemoteNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.common.request.impl.RemoteStatement;
import org.hibernate.ogm.datastore.neo4j.remote.common.request.impl.RemoteStatements;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;

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
public class HttpNeo4jSequenceGenerator extends RemoteNeo4jSequenceGenerator {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final HttpNeo4jClient client;

	public HttpNeo4jSequenceGenerator(HttpNeo4jClient neo4jDb, int sequenceCacheMaxSize) {
		super( sequenceCacheMaxSize );
		this.client = neo4jDb;
	}

	/**
	 * Create the sequence nodes setting the initial value if the node does not exist already.
	 * <p>
	 * All nodes are created inside the same transaction.
	 */
	private void createSequencesConstraintsStatements(Statements statements, Iterable<Sequence> sequences) {
		Statement statement = createUniqueConstraintStatement( SEQUENCE_NAME_PROPERTY, NodeLabel.SEQUENCE.name() );
		statements.addStatement( statement );
	}

	/**
	 * Adds a node for each generator of type {@link IdSourceType#SEQUENCE}. Table-based generators are created lazily
	 * at runtime.
	 *
	 * @param sequences the generators to process
	 */
	private Statements createSequencesStatements(Iterable<Sequence> sequences) {
		Statements statements = new Statements();
		for ( Sequence sequence : sequences ) {
			addSequence( statements, sequence );
		}
		return statements;
	}

	private void addSequence(Statements statements, Sequence sequence) {
		Statement statement = new Statement( SEQUENCE_CREATION_QUERY, Collections.<String, Object>singletonMap( SEQUENCE_NAME_QUERY_PARAM, sequence.getName().render() ) );
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

	public void createUniqueConstraintsForTableSequences(Statements statements, Iterable<IdSourceKeyMetadata> tableIdGenerators) {
		for ( IdSourceKeyMetadata idSourceKeyMetadata : tableIdGenerators ) {
			if ( idSourceKeyMetadata.getType() == IdSourceType.TABLE ) {
				addUniqueConstraintForTableBasedSequence( statements, idSourceKeyMetadata );
			}
		}
	}

	@Override
	protected Number nextValue(RemoteStatements remoteStatements) {
		Statements statements = convert( remoteStatements );
		List<StatementResult> results = client.executeQueriesInNewTransaction( statements ).getResults();
		// We use 1, because we are interested to the result of the second statement (the one that updates the node and returns the value)
		Number nextValue = (Number) results.get( 1 ).getData().get( 0 ).getRow().get( 0 );
		return nextValue;
	}

	private Statements convert(RemoteStatements remoteStatements) {
		Statements statements = new Statements();
		for ( RemoteStatement remoteStatement : remoteStatements ) {
			Statement statement = new Statement();
			statement.setStatement( remoteStatement.getQuery() );
			statement.setParameters( remoteStatement.getParams() );
			if ( remoteStatement.isAsRow() ) {
				statement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
			}
			statements.addStatement( statement );
		}
		return statements;
	}

	@Override
	public void createSequences(List<Sequence> sequences, Iterable<IdSourceKeyMetadata> idSourceKeyMetadata) {
		createUniqueConstraints( sequences, idSourceKeyMetadata );

		Statements statements = createSequencesStatements( sequences );
		StatementsResponse response = client.executeQueriesInNewTransaction( statements );
		validateSequencesCreation( response );
	}

	private void createUniqueConstraints(List<Sequence> sequences, Iterable<IdSourceKeyMetadata> idSourceKeyMetadata) {
		Statements statements = new Statements();
		createSequencesConstraintsStatements( statements, sequences );
		createUniqueConstraintsForTableSequences( statements, idSourceKeyMetadata );

		StatementsResponse response = client.executeQueriesInNewTransaction( statements );
		validateConstraintsCreation( response );
	}

	private void validateSequencesCreation(StatementsResponse response) {
		if ( !response.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = response.getErrors().get( 0 );
			throw log.sequencesCreationException( errorResponse.getCode(), errorResponse.getMessage() );
		}
	}

	private void validateConstraintsCreation(StatementsResponse response) {
		if ( !response.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = response.getErrors().get( 0 );
			throw log.constraintsCreationException( errorResponse.getCode(), errorResponse.getMessage() );
		}
	}
}

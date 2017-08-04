/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.RemoteNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.common.request.impl.RemoteStatement;
import org.hibernate.ogm.datastore.neo4j.remote.common.request.impl.RemoteStatements;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.util.Resource;

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
public class BoltNeo4jSequenceGenerator extends RemoteNeo4jSequenceGenerator {

	private final BoltNeo4jClient client;

	public BoltNeo4jSequenceGenerator(BoltNeo4jClient client, int sequenceCacheMaxSize) {
		super( sequenceCacheMaxSize );
		this.client = client;
	}

	/**
	 * Create the sequence nodes setting the initial value if the node does not exist already.
	 * <p>
	 * All nodes are created inside the same transaction.
	 */
	private void createSequencesConstraintsStatements(List<Statement> statements, Iterable<Sequence> sequences) {
		Statement statement = createUniqueConstraintStatement( SEQUENCE_NAME_PROPERTY, NodeLabel.SEQUENCE.name() );
		statements.add( statement );
	}

	/**
	 * Adds a node for each generator of type {@link IdSourceType#SEQUENCE}. Table-based generators are created lazily
	 * at runtime.
	 *
	 * @param sequences the generators to process
	 */
	public List<Statement> createSequencesStatements(Iterable<Sequence> sequences) {
		List<Statement> statements = new ArrayList<Statement>();
		for ( Sequence sequence : sequences ) {
			addSequence( statements, sequence );
		}
		return statements;
	}

	/**
	 * Adds a unique constraint to make sure that each node of the same "sequence table" is unique.
	 */
	private void addUniqueConstraintForTableBasedSequence(List<Statement> statements, IdSourceKeyMetadata generatorKeyMetadata) {
		Statement statement = createUniqueConstraintStatement( generatorKeyMetadata.getKeyColumnName(), generatorKeyMetadata.getName() );
		statements.add( statement );
	}

	private Statement createUniqueConstraintStatement(String propertyName, String label) {
		String queryString = createUniqueConstraintQuery( propertyName, label );
		Statement statement = new Statement( queryString );
		return statement;
	}

	private void addSequence(List<Statement> statements, Sequence sequence) {
		Statement statement = new Statement( SEQUENCE_CREATION_QUERY, Collections.<String, Object>singletonMap( SEQUENCE_NAME_QUERY_PARAM, sequence.getName().render() ) );
		statements.add( statement );
	}

	/**
	 * Generate the next value in a sequence for a given {@link IdSourceKey}.
	 *
	 * @return the next value in a sequence
	 */
	@Override
	protected Number nextValue(RemoteStatements remoteStatements) {
		List<Statement> statements = convert( remoteStatements );

		StatementResult result = execute( statements );
		Number nextValue = result.single().get( 0 ).asNumber();
		return nextValue;
	}

	public void createUniqueConstraintsForTableSequences(List<Statement> statements, Iterable<IdSourceKeyMetadata> tableIdGenerators) {
		for ( IdSourceKeyMetadata idSourceKeyMetadata : tableIdGenerators ) {
			if ( idSourceKeyMetadata.getType() == IdSourceType.TABLE ) {
				addUniqueConstraintForTableBasedSequence( statements, idSourceKeyMetadata );
			}
		}
	}

	private List<Statement> convert(RemoteStatements remoteStatements) {
		List<Statement> statements = new ArrayList<>();
		for ( RemoteStatement remoteStatement : remoteStatements ) {
			Statement statement = new Statement( remoteStatement.getQuery(), remoteStatement.getParams() );
			statements.add( statement );
		}
		return statements;
	}

	private void createUniqueConstraints(List<Sequence> sequences, Iterable<IdSourceKeyMetadata> idSourceKeyMetadata) {
		List<Statement> statements = new ArrayList<Statement>();
		createSequencesConstraintsStatements( statements, sequences );
		createUniqueConstraintsForTableSequences( statements, idSourceKeyMetadata );

		execute( statements );
	}

	@Override
	public void createSequences(List<Sequence> sequences, Iterable<IdSourceKeyMetadata> idSourceKeyMetadata) {
		createUniqueConstraints( sequences, idSourceKeyMetadata );

		List<Statement> statements = createSequencesStatements( sequences );
		execute( statements );
	}

	private StatementResult execute( List<Statement> statements ) {
		Driver driver = client.getDriver();
		Session session = null;
		try {
			session = driver.session();
			Transaction tx = null;

			try {
				tx = session.beginTransaction();
				StatementResult result = runAll( tx, statements );
				tx.success();
				return result;
			}
			finally {
				close( tx );
			}
		}
		finally {
			close( session );
		}
	}

	private StatementResult runAll(Transaction tx, List<Statement> statements) {
		StatementResult result = null;
		for ( Statement statement : statements ) {
			result = tx.run( statement );
			validate( result );
		}
		return result;
	}

	private void validate(StatementResult result) {
		result.hasNext();
	}

	private void close(Resource closable) {
		if ( closable != null ) {
			closable.close();
		}
	}
}

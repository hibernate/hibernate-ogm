/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;

import java.util.List;
import java.util.Properties;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jSchemaDefiner;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.neo4j.graphdb.Label;

/**
 * Initialize the schema for the Neo4j database:
 * <ol>
 * <li>create sequences;</li>
 * <li>create unique constraints on identifiers, natural ids and unique columns</li>
 * </ol>
 * <p>
 * Note that unique constraints involving multiple columns won't be applied because Neo4j does not support it.
 * <p>
 * The creation of unique constraints can be skipped setting the property
 * {@link Environment#UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY} to the value
 * {@link UniqueConstraintSchemaUpdateStrategy#SKIP}. Because in Neo4j unique constraints don't have a name, setting the
 * value to {@link UniqueConstraintSchemaUpdateStrategy#RECREATE_QUIETLY} or
 * {@link UniqueConstraintSchemaUpdateStrategy#DROP_RECREATE_QUIETLY} will have the same effect: keep the existing
 * constraints and create the missing one.
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 */
public class RemoteNeo4jSchemaDefiner extends Neo4jSchemaDefiner<Statements> {

	private static final Log log = LoggerFactory.getLogger();

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		SessionFactoryImplementor sessionFactoryImplementor = context.getSessionFactory();
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();

		RemoteNeo4jDatastoreProvider provider = (RemoteNeo4jDatastoreProvider) registry.getService( DatastoreProvider.class );
		createSequences( context.getDatabase(), context.getAllIdSourceKeyMetadata(), provider );
		createEntityConstraints( provider.getDatabase(), context.getDatabase(), sessionFactoryImplementor.getProperties() );
	}

	private void createSequences(Database database, Iterable<IdSourceKeyMetadata> idSourceKeyMetadata, RemoteNeo4jDatastoreProvider provider) {
		List<Sequence> sequences = sequences( database );

		Statements constraintStatements = new Statements();
		provider.getSequenceGenerator().createSequencesConstraints( constraintStatements, sequences );
		provider.getSequenceGenerator().createUniqueConstraintsForTableSequences( constraintStatements, idSourceKeyMetadata );

		StatementsResponse response = provider.getDatabase().executeQueriesInNewTransaction( constraintStatements );
		validateSequencesCreation( response );

		// We create the sequences in a separate transaction because
		// Neo4j does not allow the creation of constraints and graph elements in the same transaction
		Statements sequenceStatements = new Statements();
		provider.getSequenceGenerator().createSequences( sequenceStatements, sequences );
		response = provider.getDatabase().executeQueriesInNewTransaction( sequenceStatements );
		validateConstraintsCreation( response );
	}

	private void createEntityConstraints(RemoteNeo4jClient remoteNeo4j, Database database, Properties properties) {
		UniqueConstraintSchemaUpdateStrategy constraintMethod = UniqueConstraintSchemaUpdateStrategy.interpret( properties.get(
				Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY )
		);

		log.debugf( "%1$s property set to %2$s" , Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, constraintMethod );
		if ( constraintMethod == UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			log.tracef( "Skipping generation of unique constraints" );
		}
		else {
			Statements statements = new Statements();
			addUniqueConstraints( statements, database );
			log.debug( "Creating missing constraints" );
			StatementsResponse response = remoteNeo4j.executeQueriesInNewTransaction( statements );
			validateConstraintsCreation( response );
		}
	}

	private void validateSequencesCreation(StatementsResponse response) {
		if ( !response.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = response.getErrors().get( 0 );
			throw log.sequenceCreationException( errorResponse.getCode(), errorResponse.getMessage() );
		}
	}

	private void validateConstraintsCreation(StatementsResponse response) {
		if ( !response.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = response.getErrors().get( 0 );
			throw log.constraintCreationException( errorResponse.getCode(), errorResponse.getMessage() );
		}
	}

	@Override
	protected void createUniqueConstraintIfMissing(Statements statements, Label label, String property) {
		log.tracef( "Creating unique constraint for nodes labeled as %1$s on property %2$s", label, property );
		StringBuilder queryBuilder = new StringBuilder( "CREATE CONSTRAINT ON (n:" );
		escapeIdentifier( queryBuilder, label.name() );
		queryBuilder.append( ") ASSERT n." );
		escapeIdentifier( queryBuilder, property );
		queryBuilder.append( " IS UNIQUE" );
		statements.addStatement( queryBuilder.toString() );
	}
}

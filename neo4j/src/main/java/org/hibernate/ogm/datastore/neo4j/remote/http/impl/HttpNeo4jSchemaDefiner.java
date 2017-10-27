/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.cfg.Environment;
import org.hibernate.ogm.datastore.neo4j.impl.BaseNeo4jSchemaDefiner;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;

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
public class HttpNeo4jSchemaDefiner extends BaseNeo4jSchemaDefiner {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	@Override
	protected void createSequences(List<Sequence> sequences, Set<IdSourceKeyMetadata> allIdSourceKeyMetadata, DatastoreProvider provider) {
		HttpNeo4jSequenceGenerator sequenceGenerator = ( (HttpNeo4jDatastoreProvider) provider ).getSequenceGenerator();
		sequenceGenerator.createSequences( sequences, allIdSourceKeyMetadata );
	}

	@Override
	protected void createUniqueConstraintsIfMissing(DatastoreProvider provider, List<UniqueConstraintDetails> constraints) {
		Statements statements = new Statements();
		for ( UniqueConstraintDetails constraint : constraints ) {
			log.tracef( "Creating unique constraint for nodes labeled as %1$s on property %2$s", constraint.getLabel().name(), constraint.getProperty() );
			statements.addStatement( new Statement( constraint.asCypherQuery() ) );
		}

		log.debug( "Creating missing constraints" );
		HttpNeo4jClient remoteClient = ( (HttpNeo4jDatastoreProvider) provider ).getClient();
		StatementsResponse response = remoteClient.executeQueriesInNewTransaction( statements );
		validateConstraintsCreation( response );
	}

	private void validateConstraintsCreation(StatementsResponse response) {
		if ( !response.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = response.getErrors().get( 0 );
			throw log.constraintsCreationException( errorResponse.getCode(), errorResponse.getMessage() );
		}
	}
}

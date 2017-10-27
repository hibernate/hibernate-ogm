/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.cfg.Environment;
import org.hibernate.ogm.datastore.neo4j.impl.BaseNeo4jSchemaDefiner;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.driver.v1.util.Resource;

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
public class BoltNeo4jSchemaDefiner extends BaseNeo4jSchemaDefiner {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	@Override
	protected void createSequences(List<Sequence> sequences, Set<IdSourceKeyMetadata> allIdSourceKeyMetadata, DatastoreProvider provider) {
		BoltNeo4jDatastoreProvider boltProvider = (BoltNeo4jDatastoreProvider) provider;
		BoltNeo4jSequenceGenerator sequenceGenerator = boltProvider.getSequenceGenerator();
		sequenceGenerator.createSequences( sequences, allIdSourceKeyMetadata );
	}

	@Override
	protected void createUniqueConstraintsIfMissing(DatastoreProvider provider, List<UniqueConstraintDetails> constraints) {
		List<Statement> statements = new ArrayList<>();
		for ( UniqueConstraintDetails constraint : constraints ) {
			log.tracef( "Creating unique constraint for nodes labeled as %1$s on property %2$s", constraint.getLabel(), constraint.getProperty() );
			statements.add( new Statement( constraint.asCypherQuery() ) );
		}

		run( provider, statements );
	}

	private void run(DatastoreProvider provider, List<Statement> statements) {
		BoltNeo4jDatastoreProvider boltProvider = (BoltNeo4jDatastoreProvider) provider;
		Driver driver = boltProvider.getClient().getDriver();
		Session session = null;
		try {
			session = driver.session();
			Transaction tx = null;
			try {
				tx = session.beginTransaction();
				runAll( tx, statements );
				tx.success();
			}
			catch (ClientException e) {
				throw log.constraintsCreationException( e.code(), e.getMessage() );
			}
			finally {
				close( tx );
			}
		}
		finally {
			close( session );
		}
	}

	private void runAll(Transaction tx, List<Statement> statements) {
		for ( Statement statement : statements ) {
			StatementResult result = tx.run( statement );
			validate( result );
		}
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

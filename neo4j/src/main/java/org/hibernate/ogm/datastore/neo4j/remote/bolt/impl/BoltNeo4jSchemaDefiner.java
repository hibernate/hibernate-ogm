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
import org.hibernate.ogm.datastore.neo4j.index.impl.Neo4jIndexSpec;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Query;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.ClientException;

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
	protected void createIndexesIfMissing(DatastoreProvider provider, List<Neo4jIndexSpec> indexes ) {
		List<Query> queries = new ArrayList<>();
		for ( Neo4jIndexSpec index : indexes ) {
			log.tracef( "Creating composite index for nodes labeled as %1$s on properties %2$s", index.getLabel(), index.getProperties() );
			queries.add( new Query( index.asCypherCreateQuery() ) );
		}
		run( provider, queries );
	}

	@Override
	protected void createUniqueConstraintsIfMissing(DatastoreProvider provider, List<UniqueConstraintDetails> constraints) {
		List<Query> queries = new ArrayList<>();
		for ( UniqueConstraintDetails constraint : constraints ) {
			log.tracef( "Creating unique constraint for nodes labeled as %1$s on property %2$s", constraint.getLabel(), constraint.getProperty() );
			queries.add( new Query( constraint.asCypherQuery() ) );
		}

		run( provider, queries );
	}

	private void run(DatastoreProvider provider, List<Query> queries) {
		BoltNeo4jDatastoreProvider boltProvider = (BoltNeo4jDatastoreProvider) provider;
		Driver driver = boltProvider.getClient().getDriver();

		try ( Session session = driver.session();
			Transaction tx = session.beginTransaction() ) {
			runAll( tx, queries );
			tx.commit();
		}
		catch (ClientException e) {
			throw log.constraintsCreationException( e.code(), e.getMessage() );
		}
	}

	private void runAll(Transaction tx, List<Query> queries) {
		List<Result> results = new ArrayList<>();
		for ( Query query : queries ) {
			Result result = tx.run( query );
			validate( result );
			results.add( result );
		}
	}

	private void validate(Result result) {
		result.hasNext();
	}
}

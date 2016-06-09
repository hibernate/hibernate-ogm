/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.impl;

import java.util.List;
import java.util.Properties;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.neo4j.impl.BaseNeo4jSchemaDefiner;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;

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
public class EmbeddedNeo4jSchemaDefiner extends BaseNeo4jSchemaDefiner<GraphDatabaseService> {

	private static final Log log = LoggerFactory.getLogger();

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		SessionFactoryImplementor sessionFactoryImplementor = context.getSessionFactory();
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
		EmbeddedNeo4jDatastoreProvider provider = (EmbeddedNeo4jDatastoreProvider) registry.getService( DatastoreProvider.class );

		createSequences( context.getDatabase(), context.getAllIdSourceKeyMetadata(), provider );
		createEntityConstraints( provider.getDatabase(), context.getDatabase(), sessionFactoryImplementor.getProperties() );
	}

	private void createSequences(Database database, Iterable<IdSourceKeyMetadata> idSourceKeyMetadata, EmbeddedNeo4jDatastoreProvider provider) {
		List<Sequence> sequences = sequences( database );
		provider.getSequenceGenerator().createSequences( sequences );
		provider.getSequenceGenerator().createUniqueConstraintsForTableSequences( idSourceKeyMetadata );
	}

	private void createEntityConstraints(GraphDatabaseService neo4jDb, Database database, Properties properties) {
		UniqueConstraintSchemaUpdateStrategy constraintMethod = UniqueConstraintSchemaUpdateStrategy.interpret( properties.get(
				Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY )
		);

		log.debugf( "%1$s property set to %2$s" , Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, constraintMethod );
		if ( constraintMethod == UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			log.tracef( "Skipping generation of unique constraints" );
		}
		else {
			log.debug( "Creating missing constraints" );
			Transaction tx = null;
			try {
				tx = neo4jDb.beginTx();
				addUniqueConstraints( neo4jDb, database );
				tx.success();
			}
			finally {
				tx.close();
			}
		}
	}

	@Override
	protected void createUniqueConstraintIfMissing(GraphDatabaseService neo4jDb, Label label, String property) {
		if ( isMissingUniqueConstraint( neo4jDb, label, property ) ) {
			log.tracef( "Creating unique constraint for nodes labeled as %1$s on property %2$s", label, property );
			neo4jDb.schema().constraintFor( label ).assertPropertyIsUnique( property ).create();
		}
		else {
			log.tracef( "Unique constraint already exists for nodes labeled as %1$s on property %2$s", label, property );
		}
	}

	private boolean isMissingUniqueConstraint(GraphDatabaseService neo4jDb, Label label, String propertyName) {
		Iterable<ConstraintDefinition> constraints = neo4jDb.schema().getConstraints( label );
		for ( ConstraintDefinition constraint : constraints ) {
			if ( constraint.isConstraintType( ConstraintType.UNIQUENESS ) ) {
				for ( String propertyKey : constraint.getPropertyKeys() ) {
					if ( propertyKey.equals( propertyName ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}
}

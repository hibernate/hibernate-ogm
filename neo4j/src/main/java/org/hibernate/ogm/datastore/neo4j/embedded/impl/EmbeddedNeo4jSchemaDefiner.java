/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.impl;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.cfg.Environment;
import org.hibernate.ogm.datastore.neo4j.impl.BaseNeo4jSchemaDefiner;
import org.hibernate.ogm.datastore.neo4j.index.impl.Neo4jIndexSpec;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.graphdb.schema.IndexCreator;
import org.neo4j.graphdb.schema.IndexDefinition;

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
public class EmbeddedNeo4jSchemaDefiner extends BaseNeo4jSchemaDefiner {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	@Override
	protected void createSequences(List<Sequence> sequences, Set<IdSourceKeyMetadata> allIdSourceKeyMetadata, DatastoreProvider provider) {
		EmbeddedNeo4jDatastoreProvider neo4jProvider = (EmbeddedNeo4jDatastoreProvider) provider;
		neo4jProvider.getSequenceGenerator().createSequences( sequences );
		neo4jProvider.getSequenceGenerator().createUniqueConstraintsForTableSequences( allIdSourceKeyMetadata );
	}

	@Override
	protected void createIndexesIfMissing(DatastoreProvider provider, List<Neo4jIndexSpec> indexes ) {
		EmbeddedNeo4jDatastoreProvider neo4jProvider = (EmbeddedNeo4jDatastoreProvider) provider;
		GraphDatabaseService neo4jDb = neo4jProvider.getDatabase();
		try ( Transaction tx = neo4jDb.beginTx() ) {
			for ( Neo4jIndexSpec index : indexes ) {
				createIndex( neo4jDb, index );
			}
			tx.success();
		}
	}

	@Override
	protected void createUniqueConstraintsIfMissing(DatastoreProvider provider, List<UniqueConstraintDetails> constraints) {
		EmbeddedNeo4jDatastoreProvider neo4jProvider = (EmbeddedNeo4jDatastoreProvider) provider;
		GraphDatabaseService neo4jDb = neo4jProvider.getDatabase();
		Transaction tx = neo4jDb.beginTx();
		try {
			for ( UniqueConstraintDetails constraint : constraints ) {
				createUniqueConstraint( neo4jDb, constraint );
			}
			tx.success();
		}
		finally {
			if ( tx != null ) {
				tx.close();
			}
		}
	}

	private void createIndex(GraphDatabaseService neo4jDb, Neo4jIndexSpec index) {
		Label label = index.getLabel();
		if ( isIndexMissing( neo4jDb, index ) ) {
			log.tracef( "Creating index for nodes labeled as %1$s on properties %2$s", label, index.getProperties() );
			IndexCreator indexCreator = neo4jDb.schema().indexFor( label );
			for ( String property : index.getProperties() ) {
				indexCreator = indexCreator.on( property );
			}
			indexCreator.create();
		}
		else {
			log.tracef( "Index already exists for nodes labeled as %1$s on properties %2$s", label, index.getProperties() );
		}
	}

	private boolean isIndexMissing(GraphDatabaseService neo4jDb, Neo4jIndexSpec indexDetails) {
		Iterable<IndexDefinition> indexes = neo4jDb.schema().getIndexes( indexDetails.getLabel() );
		for ( IndexDefinition index : indexes ) {
			List<String> indexProperties = CollectionHelper.toList( index.getPropertyKeys() );
			if ( Objects.equals( indexProperties, indexDetails.getProperties() ) ) {
				return false;
			}
		}
		return true;
	}

	private void createUniqueConstraint(GraphDatabaseService neo4jDb, UniqueConstraintDetails constraint) {
		Label label = constraint.getLabel();
		String property = constraint.getProperty();
		if ( isMissingUniqueConstraint( neo4jDb, constraint ) ) {
			log.tracef( "Creating unique constraint for nodes labeled as %1$s on property %2$s", label, property );
			neo4jDb.schema().constraintFor( constraint.getLabel() ).assertPropertyIsUnique( constraint.getProperty() ).create();
		}
		else {
			log.tracef( "Unique constraint already exists for nodes labeled as %1$s on property %2$s", label, property );
		}
	}

	private boolean isMissingUniqueConstraint(GraphDatabaseService neo4jDb, UniqueConstraintDetails constraintDetails) {
		Iterable<ConstraintDefinition> constraints = neo4jDb.schema().getConstraints( constraintDetails.getLabel() );
		for ( ConstraintDefinition constraint : constraints ) {
			if ( constraint.isConstraintType( ConstraintType.UNIQUENESS ) ) {
				for ( String propertyKey : constraint.getPropertyKeys() ) {
					if ( propertyKey.equals( constraintDetails.getProperty() ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}
}

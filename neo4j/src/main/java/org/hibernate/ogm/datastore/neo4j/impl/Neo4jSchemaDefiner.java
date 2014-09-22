/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.impl;

import static org.neo4j.graphdb.DynamicLabel.label;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.jboss.logging.Logger.Level;
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
public class Neo4jSchemaDefiner extends BaseSchemaDefiner {

	private static final Log log = LoggerFactory.getLogger();

	@Override
	public void initializeSchema(Configuration configuration, SessionFactoryImplementor factory) {
		SessionFactoryImplementor sessionFactoryImplementor = factory;
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
		Neo4jDatastoreProvider provider = (Neo4jDatastoreProvider) registry.getService( DatastoreProvider.class );

		createSequences( sessionFactoryImplementor, provider );
		createEntityConstraints( provider.getDataBase(), configuration );
	}

	private void createSequences(SessionFactoryImplementor sessionFactoryImplementor, Neo4jDatastoreProvider provider) {
		Set<PersistentNoSqlIdentifierGenerator> sequences = getPersistentGenerators( sessionFactoryImplementor );
		provider.getSequenceGenerator().createSequences( sequences );
	}

	private void createEntityConstraints(GraphDatabaseService neo4jDb, Configuration configuration) {
		UniqueConstraintSchemaUpdateStrategy constraintMethod = UniqueConstraintSchemaUpdateStrategy.interpret( configuration.getProperties().get(
				Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY ) );
		log.debugf( "%1$s property set to %2$s" , Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY );
		if ( constraintMethod == UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			log.tracef( "%1$s property set to %2$s: Skipping generation of unique constraints", Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, UniqueConstraintSchemaUpdateStrategy.SKIP );
		}
		else {
			log.debug( "Creating missing constraints" );
			Transaction tx = null;
			try {
				tx = neo4jDb.beginTx();
				addUniqueConstraints( neo4jDb, configuration );
				tx.success();
			}
			finally {
				tx.close();
			}
		}
	}

	private void addUniqueConstraints(GraphDatabaseService neo4jDb, Configuration configuration) {
		Iterator<Table> tableMappings = configuration.getTableMappings();
		while ( tableMappings.hasNext() ) {
			Table table = tableMappings.next();
			if ( table.isPhysicalTable() ) {
				Label label = label( table.getName() );
				PrimaryKey primaryKey = table.getPrimaryKey();
				createConstraint( neo4jDb, table, label, primaryKey );
				@SuppressWarnings("unchecked")
				Iterator<Column> columnIterator = table.getColumnIterator();
				while ( columnIterator.hasNext() ) {
					Column column = columnIterator.next();
					if ( column.isUnique() ) {
						createUniqueConstraintIfMissing( neo4jDb, label, column.getName() );
					}
				}
				Iterator<UniqueKey> uniqueKeyIterator = table.getUniqueKeyIterator();
				while ( uniqueKeyIterator.hasNext() ) {
					createConstraint( neo4jDb, table, label, uniqueKeyIterator.next() );
				}
			}
		}
	}

	private void createConstraint(GraphDatabaseService neo4jDb, Table table, Label label, Constraint constraint) {
		if ( constraint != null ) {
			// Neo4j does not store properties representing foreign key columns, so we don't need to create unique
			// constraints for them
			if ( !isAppliedToForeignColumns( table, constraint ) ) {
				if ( constraint.getColumnSpan() == 1 ) {
					String propertyName = constraint.getColumn( 0 ).getName();
					createUniqueConstraintIfMissing( neo4jDb, label, propertyName );
				}
				else if ( log.isEnabled( Level.WARN ) ) {
					logMultipleColumnsWarning( table, constraint );
				}
			}
		}
	}

	private boolean isAppliedToForeignColumns(Table table, Constraint constraint) {
		List<?> constraintColumns = constraint.getColumns();
		for ( Iterator<?> iterator = table.getForeignKeyIterator(); iterator.hasNext(); ) {
			ForeignKey foreignKey = (ForeignKey) iterator.next();
			List<?> foreignKeyColumns = foreignKey.getColumns();
			for ( Object object : foreignKeyColumns ) {
				if ( constraintColumns.contains( object ) ) {
					// This constraint requires a foreign column
					return true;
				}
			}
		}
		return false;
	}

	private void logMultipleColumnsWarning(Table table, Constraint constraint) {
		StringBuilder builder = new StringBuilder();
		for ( Iterator<Column> columnIterator = constraint.getColumnIterator(); columnIterator.hasNext(); ) {
			Column column = columnIterator.next();
			builder.append( ", " );
			builder.append( column.getName() );
		}
		String columns = "[" + builder.substring( 2 ) + "]";
		log.constraintSpanningMultipleColumns( constraint.getName(), table.getName(), columns );
	}

	private void createUniqueConstraintIfMissing(GraphDatabaseService neo4jDb, Label label, String property) {
		if ( isMissingUniqueConstraint( neo4jDb, label, property ) ) {
			log.tracef( "Creating unique constraint for nodes labeled as %1$s on property %2$s", label, property);
			neo4jDb.schema().constraintFor( label ).assertPropertyIsUnique( property ).create();
		}
		else {
			log.tracef( "Unique constraint already exists for nodes labeled as %1$s on property %2$s", label, property);
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

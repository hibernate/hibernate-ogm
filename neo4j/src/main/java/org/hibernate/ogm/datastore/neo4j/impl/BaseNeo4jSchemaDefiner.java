/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;
import static org.neo4j.graphdb.Label.label;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
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
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.jboss.logging.Logger.Level;
import org.neo4j.graphdb.Label;

/**
 * Allows the reuse of the logic for the creation of unique constraints when the schema is created.
 *
 * @author Davide D'Alto
 */
public abstract class BaseNeo4jSchemaDefiner extends BaseSchemaDefiner {

	public static class UniqueConstraintDetails {
		private final Label label;
		private final String property;

		public UniqueConstraintDetails(Label label, String property) {
			this.label = label;
			this.property = property;
		}

		public Label getLabel() {
			return label;
		}

		public String getProperty() {
			return property;
		}

		/**
		 * @return the cypher query for the creation of the constraint
		 */
		public String asCypherQuery() {
			StringBuilder queryBuilder = new StringBuilder( "CREATE CONSTRAINT ON (n:" );
			escapeIdentifier( queryBuilder, label.name() );
			queryBuilder.append( ") ASSERT n." );
			escapeIdentifier( queryBuilder, property );
			queryBuilder.append( " IS UNIQUE" );
			String query = queryBuilder.toString();
			return query;
		}

		@Override
		public String toString() {
			return "UniqueConstraintDetails [label=" + label + ", property=" + property + "]";
		}
	}

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	protected void createUniqueConstraints(DatastoreProvider provider, Database database) {
		List<UniqueConstraintDetails> constraints = new ArrayList<>();
		for ( Namespace namespace : database.getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( table.isPhysicalTable() ) {
					Label label = label( table.getName() );
					PrimaryKey primaryKey = table.getPrimaryKey();
					addConstraint( constraints, table, label, primaryKey );
					@SuppressWarnings("unchecked")
					Iterator<Column> columnIterator = table.getColumnIterator();
					while ( columnIterator.hasNext() ) {
						Column column = columnIterator.next();
						if ( column.isUnique() ) {
							constraints.add( new UniqueConstraintDetails( label, column.getName() ) );
						}
					}
					Iterator<UniqueKey> uniqueKeyIterator = table.getUniqueKeyIterator();
					while ( uniqueKeyIterator.hasNext() ) {
						addConstraint( constraints, table, label, uniqueKeyIterator.next() );
					}
				}
			}
		}
		createUniqueConstraintsIfMissing( provider, constraints );
	}

	private void addConstraint(List<UniqueConstraintDetails> constraints, Table table, Label label, Constraint constraint) {
		if ( constraint != null ) {
			// Neo4j does not store properties representing foreign key columns, so we don't need to create unique
			// constraints for them
			if ( !isAppliedToForeignColumns( table, constraint ) ) {
				if ( constraint.getColumnSpan() == 1 ) {
					String propertyName = constraint.getColumn( 0 ).getName();
					constraints.add( new UniqueConstraintDetails( label, propertyName ) );
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

	protected List<Sequence> sequences(Database database) {
		List<Sequence> sequences = new ArrayList<>();
		for ( Namespace namespace : database.getNamespaces() ) {
			for ( Sequence sequence : namespace.getSequences() ) {
				sequences.add( sequence );
			}
		}
		return sequences;
	}

	protected void createEntityConstraints(DatastoreProvider provider, Database database, Map<String, Object> properties) {
		UniqueConstraintSchemaUpdateStrategy constraintMethod = UniqueConstraintSchemaUpdateStrategy
				.interpret( properties.get( Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY ) );

		log.debugf( "%1$s property set to %2$s", Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, constraintMethod );
		if ( constraintMethod == UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			log.tracef( "Skipping generation of unique constraints" );
		}
		else {
			createUniqueConstraints( provider, database );
		}
	}

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		SessionFactoryImplementor sessionFactoryImplementor = context.getSessionFactory();
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
		DatastoreProvider provider = registry.getService( DatastoreProvider.class );
		List<Sequence> sequences = sequences( context.getDatabase() );

		createSequences( sequences, context.getAllIdSourceKeyMetadata(), provider );
		createEntityConstraints( provider, context.getDatabase(), sessionFactoryImplementor.getProperties() );
	}

	protected abstract void createSequences(List<Sequence> sequences, Set<IdSourceKeyMetadata> allIdSourceKeyMetadata, DatastoreProvider provider);

	protected abstract void createUniqueConstraintsIfMissing( DatastoreProvider provider, List<UniqueConstraintDetails> constraints );
}

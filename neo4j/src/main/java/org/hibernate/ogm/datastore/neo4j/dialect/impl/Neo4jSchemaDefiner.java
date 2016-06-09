/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.neo4j.graphdb.DynamicLabel.label;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.jboss.logging.Logger.Level;
import org.neo4j.graphdb.Label;
/**
 * @author Davide D'Alto
 */
public abstract class Neo4jSchemaDefiner<T> extends BaseSchemaDefiner {

	private static final Log log = LoggerFactory.getLogger();

	protected void addUniqueConstraints(T neo4jDb, Database database) {
		for ( Namespace namespace : database.getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
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
	}

	private void createConstraint(T neo4jDb, Table table, Label label, Constraint constraint) {
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

	protected List<Sequence> sequences(Database database) {
		List<Sequence> sequences = new ArrayList<>();
		for ( Namespace namespace : database.getNamespaces() ) {
			for ( Sequence sequence : namespace.getSequences() ) {
				sequences.add( sequence );
			}
		}
		return sequences;
	}

	protected abstract void createUniqueConstraintIfMissing(T neo4jDb, Label label, String name);
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.type.Type;

/**
 * Table and index creation methods.
 *
 * @author Jonathan Halliday
 */
public class CassandraSchemaDefiner extends BaseSchemaDefiner {

	@Override
	public void initializeSchema(Database database, SessionFactoryImplementor sessionFactoryImplementor) {
		CassandraDatastoreProvider datastoreProvider = (CassandraDatastoreProvider) sessionFactoryImplementor.getServiceRegistry()
				.getService( DatastoreProvider.class );
		Set<PersistentNoSqlIdentifierGenerator> identifierGenerators = getPersistentGenerators(
				sessionFactoryImplementor
		);
		for ( PersistentNoSqlIdentifierGenerator identifierGenerator : identifierGenerators ) {

			CassandraSequenceHandler sequenceHandler = datastoreProvider.getSequenceHandler();
			sequenceHandler.createSequence( identifierGenerator.getGeneratorKeyMetadata(), datastoreProvider );
		}

		for ( Namespace namespace : database.getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( table.isPhysicalTable() ) {
					processTable( sessionFactoryImplementor, datastoreProvider, table );
				}
			}
		}
	}

	private void processTable(
			SessionFactoryImplementor sessionFactoryImplementor,
			CassandraDatastoreProvider datastoreProvider,
			Table table) {
		TypeTranslator typeTranslator = sessionFactoryImplementor.getServiceRegistry()
				.getService( TypeTranslator.class );
		datastoreProvider.setTableMetadata( table.getName(), table );

		List<String> primaryKeys = new ArrayList<String>();
		if ( table.hasPrimaryKey() ) {
			for ( Object pkColumn : table.getPrimaryKey().getColumns() ) {
				primaryKeys.add( ((Column) pkColumn).getName() );
			}
		}
		List<String> columnNames = new ArrayList<String>();
		List<String> columnTypes = new ArrayList<String>();

		Iterator<Column> columnIterator = table.getColumnIterator();
		while ( columnIterator.hasNext() ) {
			Column column = columnIterator.next();
			columnNames.add( column.getName() );
			Value value = column.getValue();
			Type type = value.getType();

			if ( type.isAssociationType() ) {
				type = type.getSemiResolvedType( sessionFactoryImplementor );
				if ( type.isComponentType() ) {
					int index = column.getTypeIndex();
					type = ((org.hibernate.type.ComponentType) type).getSubtypes()[index];
				}
			}
			else if ( type.isComponentType() ) {
				int index = column.getTypeIndex();
				type = ((org.hibernate.type.ComponentType) column.getValue().getType()).getSubtypes()[index];
			}

			GridType gridType = typeTranslator.getType( type );
			String cqlType = CassandraTypeMapper.INSTANCE.hibernateToCQL( gridType );
			columnTypes.add( cqlType );
		}

		datastoreProvider.createColumnFamilyIfNeeded( table.getName(), primaryKeys, columnNames, columnTypes );
		processIndexes( datastoreProvider, table, primaryKeys );
	}

	private void processIndexes(CassandraDatastoreProvider datastoreProvider, Table table, List<String> primaryKeys) {

		// cassandra won't allow table scanning, so we need to explicitly index for the fk relations:
		Iterator<ForeignKey> fkMappings = table.getForeignKeyIterator();
		while ( fkMappings.hasNext() ) {
			ForeignKey foreignKey = fkMappings.next();

			List<String> fkColumnNames = new ArrayList<String>();

			Iterator<Column> fkColumnIterator = foreignKey.getColumnIterator();
			while ( fkColumnIterator.hasNext() ) {
				Column column = fkColumnIterator.next();
				fkColumnNames.add( column.getName() );
			}

			// cassandra won't allow single index on multiple cols, so index first col only.
			if ( !primaryKeys.contains( fkColumnNames.get( 0 ) ) ) {
				datastoreProvider.createSecondaryIndexIfNeeded( table.getName(), fkColumnNames.get( 0 ) );
			}
		}
	}
}

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

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.ogm.datastore.cassandra.logging.impl.Log;
import org.hibernate.ogm.datastore.cassandra.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.type.Type;

/**
 * Table and index creation methods.
 *
 * @author Jonathan Halliday
 */
public class CassandraSchemaDefiner extends BaseSchemaDefiner {

	private static final Log LOG = LoggerFactory.getLogger();

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		CassandraDatastoreProvider datastoreProvider = (CassandraDatastoreProvider) context.getSessionFactory().getServiceRegistry()
				.getService( DatastoreProvider.class );

		for ( IdSourceKeyMetadata iddSourceKeyMetadata : context.getAllIdSourceKeyMetadata() ) {
			CassandraSequenceHandler sequenceHandler = datastoreProvider.getSequenceHandler();
			sequenceHandler.createSequence( iddSourceKeyMetadata, datastoreProvider );
		}

		for ( Namespace namespace : context.getDatabase().getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( table.isPhysicalTable() ) {
					processTable( context.getSessionFactory(), datastoreProvider, table );
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
			createSecondaryIndex( datastoreProvider, table, foreignKey.getName(), foreignKey.getColumnIterator() );
		}

		Iterator<Index> indexIterator = table.getIndexIterator();
		while ( indexIterator.hasNext() ) {
			Index index = indexIterator.next();
			createSecondaryIndex( datastoreProvider, table, index.getName(), index.getColumnIterator() );
		}
	}

	private void createSecondaryIndex(CassandraDatastoreProvider datastoreProvider, Table table, String sourceName, Iterator<Column> columns) {
		if ( !columns.hasNext() ) {
			throw LOG.indexWithNoColumns( table.getName(), sourceName );
		}

		datastoreProvider.createSecondaryIndexIfNeeded( table.getName(), columns.next().getName() );

		// cassandra won't allow single index on multiple cols, so index first col only.
		if ( columns.hasNext() ) {
			LOG.multiColumnIndexNotSupported( table.getName(), sourceName );
		}
	}
}

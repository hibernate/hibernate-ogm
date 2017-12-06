/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.SchemaDefinitions;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.TableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.options.cache.CacheTemplate;
import org.hibernate.ogm.datastore.infinispanremote.options.cache.impl.CacheTemplateOption;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * Create and/or validate the protobuf schema definitions on the Infinispan grid.
 */
public class ProtobufSchemaInitializer extends BaseSchemaDefiner {

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {
		ServiceRegistryImplementor serviceRegistry = context.getSessionFactory().getServiceRegistry();
		TypeTranslator typeTranslator = serviceRegistry.getService( TypeTranslator.class );
		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		Map tableEntityTypeMapping = context.getTableEntityTypeMapping();
		InfinispanRemoteDatastoreProvider datastoreProvider = (InfinispanRemoteDatastoreProvider) serviceRegistry.getService( DatastoreProvider.class );
		String protobufPackageName = datastoreProvider.getProtobufPackageName();
		SchemaDefinitions sd = new SchemaDefinitions( protobufPackageName );
		for ( Namespace namespace : context.getDatabase().getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( table.isPhysicalTable() ) {
					createTableDefinition( context.getSessionFactory(), sd, table, typeTranslator, protobufPackageName,
											getCacheTemplate( tableEntityTypeMapping, optionsService, table.getName() )
					);
				}
			}
		}
		for ( IdSourceKeyMetadata iddSourceKeyMetadata : context.getAllIdSourceKeyMetadata() ) {
			sd.createSequenceSchemaDefinition( iddSourceKeyMetadata, datastoreProvider.getProtobufPackageName() );
		}
		datastoreProvider.registerSchemaDefinitions( sd );
	}

	private String getCacheTemplate(Map tableEntityTypeMapping, OptionsService optionsService, String tableName) {
		Class entity = (Class) tableEntityTypeMapping.get( tableName );
		if ( entity == null ) {
			return null;
		}
		CacheTemplate cacheTemplate = optionsService.context().getEntityOptions( entity )
				.getUnique(	CacheTemplateOption.class );
		if ( cacheTemplate == null ) {
			return null;
		}
		return cacheTemplate.value();
	}

	private void createTableDefinition(SessionFactoryImplementor sessionFactory, SchemaDefinitions sd,
			Table table, TypeTranslator typeTranslator, String protobufPackageName, String cacheTemplate ) {
		TableDefinition td = new TableDefinition( table.getName(), protobufPackageName, cacheTemplate );
		if ( table.hasPrimaryKey() ) {
			for ( Column pkColumn : table.getPrimaryKey().getColumns() ) {
				String name = pkColumn.getName();
				//We only collect the column names, as the Type assigned to PrimaryKey columns
				//is not the one of the kind we need.
				//We need the ones defined by getColumnIterator, which we collect in the next
				//step when collecting all column definitions.
				td.markAsPrimaryKey( name );
			}
		}
		Iterator<Column> columnIterator = table.getColumnIterator();
		while ( columnIterator.hasNext() ) {
			Column column = columnIterator.next();
			Value value = column.getValue();
			Type type = value.getType();
			if ( type.isAssociationType() ) {
				type = type.getSemiResolvedType( sessionFactory );
				if ( type.isComponentType() ) {
					int index = column.getTypeIndex();
					type = ( (org.hibernate.type.ComponentType) type ).getSubtypes()[index];
				}
			}
			else if ( type.isComponentType() ) {
				int index = column.getTypeIndex();
				type = ( (org.hibernate.type.ComponentType) column.getValue().getType() ).getSubtypes()[index];
			}
			GridType gridType = typeTranslator.getType( type );
			td.addColumnnDefinition( column, gridType, type );
		}
		sd.registerTableDefinition( td );
	}

	@Override
	public void validateMapping(SchemaDefinitionContext context) {
		//TODO something interesting to do here?
	}
}

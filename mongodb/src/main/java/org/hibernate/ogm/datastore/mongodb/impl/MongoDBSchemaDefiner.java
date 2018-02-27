/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.index.impl.MongoDBIndexSpec;
import org.hibernate.ogm.datastore.mongodb.index.impl.MongoDBIndexType;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.options.shared.impl.IndexOptionsOption;
import org.hibernate.ogm.options.shared.spi.IndexOption;
import org.hibernate.ogm.options.shared.spi.IndexOptions;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.ogm.util.impl.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Performs sanity checks of the mapped objects.
 *
 * @author Gunnar Morling
 * @author Sanne Grinovero
 * @author Francois Le Droff
 * @author Guillaume Smet
 */
public class MongoDBSchemaDefiner extends BaseSchemaDefiner {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final int INDEX_CREATION_ERROR_CODE = 85;

	private List<MongoDBIndexSpec> indexSpecs = new ArrayList<>();

	@Override
	public void validateMapping(SchemaDefinitionContext context) {
		validateGenerators( context.getAllIdSourceKeyMetadata() );
		validateEntityCollectionNames( context.getAllEntityKeyMetadata() );
		validateAssociationNames( context.getAllAssociationKeyMetadata() );
		validateAllPersisters( context.getSessionFactory().getMetamodel().entityPersisters().values() );
		validateIndexSpecs( context );
	}

	@Override
	public void initializeSchema( SchemaDefinitionContext context) {
		SessionFactoryImplementor sessionFactoryImplementor = context.getSessionFactory();
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) registry.getService( DatastoreProvider.class );

		for ( MongoDBIndexSpec indexSpec : indexSpecs ) {
			createIndex( provider.getDatabase(), indexSpec );
		}
	}

	private void validateAllPersisters(Iterable<EntityPersister> persisters) {
		for ( EntityPersister persister : persisters ) {
			if ( persister instanceof OgmEntityPersister ) {
				OgmEntityPersister ogmPersister = (OgmEntityPersister) persister;
				int propertySpan = ogmPersister.getEntityMetamodel().getPropertySpan();
				for ( int i = 0; i < propertySpan; i++ ) {
					String[] columnNames = ogmPersister.getPropertyColumnNames( i );
					for ( String columnName : columnNames ) {
						validateAsMongoDBFieldName( columnName );
					}
				}
			}
		}
	}

	private void validateAssociationNames(Iterable<AssociationKeyMetadata> allAssociationKeyMetadata) {
		for ( AssociationKeyMetadata associationKeyMetadata : allAssociationKeyMetadata ) {
			validateAsMongoDBCollectionName( associationKeyMetadata.getTable() );
			for ( String column : associationKeyMetadata.getRowKeyColumnNames() ) {
				validateAsMongoDBFieldName( column );
			}
		}
	}

	private void validateEntityCollectionNames(Iterable<EntityKeyMetadata> allEntityKeyMetadata) {
		for ( EntityKeyMetadata entityKeyMetadata : allEntityKeyMetadata ) {
			validateAsMongoDBCollectionName( entityKeyMetadata.getTable() );
			for ( String column : entityKeyMetadata.getColumnNames() ) {
				validateAsMongoDBFieldName( column );
			}
		}
	}

	private void validateGenerators(Iterable<IdSourceKeyMetadata> allIdSourceKeyMetadata) {
		for ( IdSourceKeyMetadata idSourceKeyMetadata : allIdSourceKeyMetadata ) {
			String keyColumn = idSourceKeyMetadata.getKeyColumnName();

			if ( !keyColumn.equals( MongoDBDialect.ID_FIELDNAME ) ) {
				log.cannotUseGivenPrimaryKeyColumnName( keyColumn, MongoDBDialect.ID_FIELDNAME );
			}
		}
	}

	private void validateIndexSpecs(SchemaDefinitionContext context) {
		OptionsService optionsService = context.getSessionFactory().getServiceRegistry().getService( OptionsService.class );
		Map<String, Class<?>> tableEntityTypeMapping = context.getTableEntityTypeMapping();

		Database database = context.getDatabase();
		UniqueConstraintSchemaUpdateStrategy constraintMethod = UniqueConstraintSchemaUpdateStrategy.interpret(
				context.getSessionFactory().getProperties().get( Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY ) );
		if ( constraintMethod == UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			log.tracef( "Skipping generation of unique constraints" );
		}

		for ( Namespace namespace : database.getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( table.isPhysicalTable() ) {
					Class<?> entityType = tableEntityTypeMapping.get( table.getName() );
					if ( entityType == null ) {
						continue;
					}

					IndexOptions indexOptions = getIndexOptions( optionsService, entityType );
					Set<String> forIndexNotReferenced = new HashSet<>( indexOptions.getReferencedIndexes() );

					validateIndexSpecsForUniqueColumns( table, indexOptions, forIndexNotReferenced, constraintMethod );

					validateIndexSpecsForUniqueKeys( table, indexOptions, forIndexNotReferenced, constraintMethod );

					validateIndexSpecsForIndexes( table, indexOptions, forIndexNotReferenced );

					for ( String forIndex : forIndexNotReferenced ) {
						log.indexOptionReferencingNonExistingIndex( table.getName(), forIndex );
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void validateIndexSpecsForUniqueColumns(Table table, IndexOptions indexOptions, Set<String> forIndexNotReferenced,
			UniqueConstraintSchemaUpdateStrategy constraintMethod) {
		Iterator<Column> columnIterator = table.getColumnIterator();
		while ( columnIterator.hasNext() ) {
			Column column = columnIterator.next();
			if ( column.isUnique() ) {
				String indexName = NamingHelper.INSTANCE.generateHashedConstraintName(
						"UK_",
						table.getNameIdentifier(),
						Identifier.toIdentifier( column.getName() ) );
				forIndexNotReferenced.remove( indexName );

				if ( constraintMethod != UniqueConstraintSchemaUpdateStrategy.SKIP ) {
					MongoDBIndexSpec indexSpec = new MongoDBIndexSpec( table.getName(), column.getName(), indexName,
							getIndexOptionDocument( table, indexOptions.getOptionForIndex( indexName ) ) );
					if ( validateIndexSpec( indexSpec ) ) {
						indexSpecs.add( indexSpec );
					}
				}
			}
		}
	}

	private void validateIndexSpecsForUniqueKeys(Table table, IndexOptions indexOptions, Set<String> forIndexNotReferenced,
			UniqueConstraintSchemaUpdateStrategy constraintMethod) {
		Iterator<UniqueKey> keys = table.getUniqueKeyIterator();
		while ( keys.hasNext() ) {
			UniqueKey uniqueKey = keys.next();
			forIndexNotReferenced.remove( uniqueKey.getName() );

			if ( constraintMethod != UniqueConstraintSchemaUpdateStrategy.SKIP ) {
				MongoDBIndexSpec indexSpec = new MongoDBIndexSpec( uniqueKey,
						getIndexOptionDocument( table, indexOptions.getOptionForIndex( uniqueKey.getName() ) ) );
				if ( validateIndexSpec( indexSpec ) ) {
					indexSpecs.add( indexSpec );
				}
			}
		}
	}

	private void validateIndexSpecsForIndexes(Table table, IndexOptions indexOptions, Set<String> forIndexNotReferenced) {
		Iterator<Index> indexes = table.getIndexIterator();
		while ( indexes.hasNext() ) {
			Index index = indexes.next();
			forIndexNotReferenced.remove( index.getName() );
			MongoDBIndexSpec indexSpec = new MongoDBIndexSpec( index,
					getIndexOptionDocument( table, indexOptions.getOptionForIndex( index.getName() ) ) );
			if ( validateIndexSpec( indexSpec ) ) {
				indexSpecs.add( indexSpec );
			}
		}
	}

	private Document getIndexOptionDocument(Table table, IndexOption indexOption) {
		try {
			Document options;
			if ( StringHelper.isNullOrEmptyString( indexOption.getOptions() ) ) {
				options = new Document();
			}
			else {
				options = Document.parse( indexOption.getOptions() );
			}
			options.put( "name", indexOption.getTargetIndexName() );
			return options;
		}
		catch (Exception e) {
			throw log.invalidOptionsFormatForIndex( table.getName(), indexOption.getTargetIndexName(), e );
		}
	}

	private IndexOptions getIndexOptions(OptionsService optionsService, Class<?> entityType) {
		IndexOptions options = optionsService.context().getEntityOptions( entityType ).getUnique( IndexOptionsOption.class );
		if ( options == null ) {
			options = new IndexOptions();
		}
		return options;
	}

	private boolean validateIndexSpec(MongoDBIndexSpec indexSpec) {
		boolean valid = true;
		if ( StringHelper.isNullOrEmptyString( indexSpec.getIndexName() ) ) {
			log.indexNameIsEmpty( indexSpec.getCollection() );
			valid = false;
		}
		if ( indexSpec.getIndexKeysDocument().keySet().isEmpty() ) {
			log.noValidKeysForIndex( indexSpec.getCollection(), indexSpec.getIndexName() );
			valid = false;
		}
		return valid;
	}

	public void createIndex(MongoDatabase database, MongoDBIndexSpec indexSpec) {
		MongoCollection<Document> collection = database.getCollection( indexSpec.getCollection() );
		Map<String, Document> preexistingIndexes = getIndexes( collection );
		String preexistingTextIndex = getPreexistingTextIndex( preexistingIndexes );

		// if a text index already exists in the collection, MongoDB silently ignores the creation of the new text index
		// so we might as well log a warning about it
		if ( MongoDBIndexType.TEXT.equals( indexSpec.getIndexType() )
				&& preexistingTextIndex != null && !preexistingTextIndex.equalsIgnoreCase( indexSpec.getIndexName() ) ) {
			throw log.unableToCreateTextIndex( collection.getNamespace().getCollectionName(), indexSpec.getIndexName(), preexistingTextIndex );
		}

		try {
			// if the index is already present and with the same definition, MongoDB simply ignores the call
			// if the definition is not the same, MongoDB throws an error, except in the case of a text index
			// where it silently ignores the creation
			collection.createIndex( indexSpec.getIndexKeysDocument(), indexSpec.getOptions() );
		}
		catch (MongoException e) {
			String indexName = indexSpec.getIndexName();
			if ( e.getCode() == INDEX_CREATION_ERROR_CODE
					&& !StringHelper.isNullOrEmptyString( indexName )
					&& preexistingIndexes.containsKey( indexName ) ) {
				// The index already exists with a different definition and has a name: we drop it and we recreate it
				collection.dropIndex( indexName );
				collection.createIndex( indexSpec.getIndexKeysDocument(), indexSpec.getOptions() );
			}
			else {
				throw log.unableToCreateIndex( collection.getNamespace().getCollectionName(), indexName, e );
			}
		}
	}

	private Map<String, Document> getIndexes(MongoCollection<Document> collection) {
		Map<String, Document> indexMap = new HashMap<>();
		MongoCursor<Document> it = collection.listIndexes().iterator();
		while ( it.hasNext() ) {
			Document index = it.next();
			indexMap.put( index.get( "name" ).toString(), index );
		}
		return indexMap;
	}

	private String getPreexistingTextIndex(Map<String, Document> preexistingIndexes) {
		for ( Entry<String, Document> indexEntry : preexistingIndexes.entrySet() ) {
			Document keys = (Document) indexEntry.getValue().get( "key" );
			if ( keys != null && keys.containsKey( "_fts" ) ) {
				return indexEntry.getKey();
			}
		}
		return null;
	}

	/**
	 * Validates a String to be a valid name to be used in MongoDB for a collection name.
	 *
	 * @param collectionName
	 */
	private static void validateAsMongoDBCollectionName(String collectionName) {
		Contracts.assertStringParameterNotEmpty( collectionName, "requestedName" );
		//Yes it has some strange requirements.
		if ( collectionName.startsWith( "system." ) ) {
			throw log.collectionNameHasInvalidSystemPrefix( collectionName );
		}
		else if ( collectionName.contains( "\u0000" ) ) {
			throw log.collectionNameContainsNULCharacter( collectionName );
		}
		else if ( collectionName.contains( "$" ) ) {
			throw log.collectionNameContainsDollarCharacter( collectionName );
		}
	}

	/**
	 * Validates a String to be a valid name to be used in MongoDB for a field name.
	 *
	 * @param fieldName
	 */
	private void validateAsMongoDBFieldName(String fieldName) {
		if ( fieldName.startsWith( "$" ) ) {
			throw log.fieldNameHasInvalidDollarPrefix( fieldName );
		}
		else if ( fieldName.contains( "\u0000" ) ) {
			throw log.fieldNameContainsNULCharacter( fieldName );
		}
	}

}

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

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.index.MongoDBIndexSpec;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.impl.MongoDBCollection;
import org.hibernate.ogm.datastore.mongodb.options.impl.MongoDBCollectionOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.MongoDBIndexOptions;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.ogm.util.impl.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

/**
 * Performs sanity checks of the mapped objects and creates objects.
 *
 * @author Gunnar Morling
 * @author Sanne Grinovero
 * @author Francois Le Droff
 * @author Guillaume Smet
 */
public class MongoDBSchemaDefiner extends BaseSchemaDefiner {

	private static final Log log = LoggerFactory.getLogger();

	private static final int INDEX_CREATION_ERROR_CODE = 85;

	private List<MongoDBIndexSpec> indexSpecs = new ArrayList<>();

	@Override
	public void validateMapping(SchemaDefinitionContext context) {
		validateGenerators( context.getAllIdSourceKeyMetadata() );
		validateEntityCollectionNames( context.getAllEntityKeyMetadata() );
		validateAssociationNames( context.getAllAssociationKeyMetadata() );
		validateAllPersisters( context.getSessionFactory().getEntityPersisters().values() );
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

	private void validateIndexSpecs(SchemaDefinitionContext context) {
		OptionsService optionsService = context.getSessionFactory().getServiceRegistry().getService( OptionsService.class );
		Map<String, Class<?>> tableEntityTypeMapping = context.getTableEntityTypeMapping();

		Database database = context.getDatabase();
		for ( Namespace namespace : database.getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				Class<?> entityType = tableEntityTypeMapping.get( table.getName() );
				if ( entityType == null ) {
					continue;
				}

				MongoDBCollection mongoDBOptions = getMongoDBOptions( optionsService, entityType );
				Set<String> forIndexNotReferenced = new HashSet<>( mongoDBOptions.getReferencedIndexes() );

				Iterator<UniqueKey> keys = table.getUniqueKeyIterator();
				while ( keys.hasNext() ) {
					UniqueKey uniqueKey = keys.next();
					forIndexNotReferenced.remove( uniqueKey.getName() );
					MongoDBIndexSpec indexSpec = new MongoDBIndexSpec( uniqueKey, getMongoDBIndexOption( mongoDBOptions, uniqueKey.getName() ) );
					if ( validateIndexSpec( indexSpec ) ) {
						indexSpecs.add( indexSpec );
					}
				}

				Iterator<Index> indexes = table.getIndexIterator();
				while ( indexes.hasNext() ) {
					Index index = indexes.next();
					forIndexNotReferenced.remove( index.getName() );
					MongoDBIndexSpec indexSpec = new MongoDBIndexSpec( index, getMongoDBIndexOption( mongoDBOptions, index.getName() ) );
					if ( validateIndexSpec( indexSpec ) ) {
						indexSpecs.add( indexSpec );
					}
				}

				for (String forIndex : forIndexNotReferenced) {
					log.indexOptionsReferencingNonExistingIndex( table.getName(), forIndex );
				}
			}
		}
	}

	private MongoDBCollection getMongoDBOptions(OptionsService optionsService, Class<?> entityType) {
		MongoDBCollection mongoDBOptions = optionsService.context().getEntityOptions( entityType ).getUnique( MongoDBCollectionOption.class );
		if ( mongoDBOptions == null ) {
			mongoDBOptions = new MongoDBCollection();
		}
		return mongoDBOptions;
	}

	private MongoDBIndexOptions getMongoDBIndexOption(MongoDBCollection mongoDBOptions, String indexName) {
		return mongoDBOptions.getOptionsForIndex( indexName );
	}

	private boolean validateIndexSpec(MongoDBIndexSpec indexSpec) {
		boolean valid = true;
		if ( StringHelper.isNullOrEmptyString( indexSpec.getIndexName() ) ) {
			log.indexNameIsEmpty( indexSpec.getCollection() );
			valid = false;
		}
		if ( indexSpec.getIndexKeysDBObject().keySet().isEmpty() ) {
			log.noValidKeysForIndex( indexSpec.getCollection(), indexSpec.getIndexName() );
			valid = false;
		}
		MongoDBIndexOptions options = indexSpec.getIndexOption();
		valid = valid && parseDBObject( indexSpec.getCollection(), indexSpec.getIndexName(), "partialFilterExpression", options.getPartialFilterExpression() );
		valid = valid && parseDBObject( indexSpec.getCollection(), indexSpec.getIndexName(), "storageEngine", options.getStorageEngine() );
		if ( indexSpec.isTextIndex() ) {
			valid = valid && parseDBObject( indexSpec.getCollection(), indexSpec.getIndexName(), "text.weights", options.getText().getWeights() );
		}
		return valid;
	}

	public void createIndex(DB database, MongoDBIndexSpec indexSpec) {
		DBCollection collection = database.getCollection( indexSpec.getCollection() );
		Map<String, DBObject> preexistingIndexes = getIndexes( collection );
		String preexistingTextIndex = getPreexistingTextIndex( preexistingIndexes );

		// if a text index already exists in the collection, MongoDB silently ignores the creation of the new text index
		// so we might as well log a warning about it
		if ( indexSpec.isTextIndex() && preexistingTextIndex != null && !preexistingTextIndex.equalsIgnoreCase( indexSpec.getIndexName() ) ) {
			log.unableToCreateTextIndex( collection.getName(), indexSpec.getIndexName(), preexistingTextIndex );
			return;
		}

		try {
			// if the index is already present and with the same definition, MongoDB simply ignores the call
			// if the definition is not the same, MongoDB throws an error, except in the case of a text index
			// where it silently ignores the creation
			collection.createIndex( indexSpec.getIndexKeysDBObject(), indexSpec.getIndexOptionsDBObject() );
		}
		catch (MongoException e) {
			String indexName = indexSpec.getIndexName();
			if ( e.getCode() == INDEX_CREATION_ERROR_CODE
					&& !StringHelper.isNullOrEmptyString( indexName )
					&& preexistingIndexes.containsKey( indexName ) ) {
				// The index already exists with a different definition and has a name: we drop it and we recreate it
				collection.dropIndex( indexName );
				collection.createIndex( indexSpec.getIndexKeysDBObject(), indexSpec.getIndexOptionsDBObject() );
			}
			else {
				throw log.unableToCreateIndex( collection.getName(), indexName, e );
			}
		}
	}

	private Map<String, DBObject> getIndexes(DBCollection collection) {
		List<DBObject> indexes = collection.getIndexInfo();
		Map<String, DBObject> indexMap = new HashMap<>();
		for (DBObject index : indexes) {
			indexMap.put( index.get( "name" ).toString(), index );
		}
		return indexMap;
	}

	private String getPreexistingTextIndex(Map<String, DBObject> preexistingIndexes) {
		for ( Entry<String, DBObject> indexEntry : preexistingIndexes.entrySet() ) {
			DBObject keys = (DBObject) indexEntry.getValue().get( "key" );
			if ( keys != null && keys.containsField( "_fts" ) ) {
				return indexEntry.getKey();
			}
		}
		return null;
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

	private boolean parseDBObject(String collection, String indexName, String optionKey, String jsonString) {
		if ( StringHelper.isNullOrEmptyString( jsonString ) ) {
			return true;
		}
		try {
			BasicDBObject.parse( jsonString );
			return true;
		}
		catch (Exception e) {
			log.invalidMongoDBDocumentForOptionKey( collection, indexName, optionKey );
			return false;
		}
	}

}

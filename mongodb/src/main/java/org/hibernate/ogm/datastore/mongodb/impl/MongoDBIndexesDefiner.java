/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.ogm.datastore.mongodb.index.impl.MongoDBIndexSpec;
import org.hibernate.ogm.datastore.mongodb.index.impl.MongoDBIndexType;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.options.shared.impl.IndexOptionsOption;
import org.hibernate.ogm.options.shared.spi.IndexOption;
import org.hibernate.ogm.options.shared.spi.IndexOptions;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.util.impl.StringHelper;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Performs sanity checks of defined indexes in the mapped objects.
 */
public class MongoDBIndexesDefiner {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
	private static final int INDEX_CREATION_ERROR_CODE = 85;

	private List<MongoDBIndexSpec> indexSpecs = new ArrayList<>();

	public List<MongoDBIndexSpec> getIndexSpecs() {
		return indexSpecs;
	}

	void validateIndexSpecs(SchemaDefiner.SchemaDefinitionContext context) {
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

	void createIndexes(MongoDatabase database) {
		for ( MongoDBIndexSpec indexSpec : indexSpecs ) {
			createIndex( database, indexSpec );
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
						addHandleDuplications( indexSpec );
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
					addHandleDuplications( indexSpec );
				}
			}
		}
	}

	void validateIndexSpecsForIndexes(Table table, IndexOptions indexOptions, Set<String> forIndexNotReferenced) {
		Iterator<Index> indexes = table.getIndexIterator();
		while ( indexes.hasNext() ) {
			Index index = indexes.next();
			forIndexNotReferenced.remove( index.getName() );
			MongoDBIndexSpec indexSpec = new MongoDBIndexSpec( index,
					getIndexOptionDocument( table, indexOptions.getOptionForIndex( index.getName() ) ) );
			if ( validateIndexSpec( indexSpec ) ) {
				addHandleDuplications( indexSpec );
			}
		}
	}

	private void addHandleDuplications(MongoDBIndexSpec indexSpec) {
		// we can add any index defined on different collection or different key set
		// we rely on hashcode and equals to verify conflicts
		if ( !indexSpecs.contains( indexSpec ) ) {
			indexSpecs.add( indexSpec );
			return;
		}

		int existingIndexPosition = indexSpecs.indexOf( indexSpec );
		MongoDBIndexSpec existingIndex = indexSpecs.get( existingIndexPosition );

		// in case on conflict
		// we rely on compareTo to choose the best candidate
		if ( indexSpec.compareTo( existingIndex ) < 0 ) {
			// replace the existing definition with the new one
			indexSpecs.remove( existingIndexPosition );
			indexSpecs.add( indexSpec );
			log.tryToDefineMultipleIndexesOnTheSameKeySet( indexSpec.getIndexKeysDocument().toJson(), existingIndex.getIndexName() );
		}
		else {
			// discard the current definition
			log.tryToDefineMultipleIndexesOnTheSameKeySet( indexSpec.getIndexKeysDocument().toJson(), indexSpec.getIndexName() );
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
		for ( Map.Entry<String, Document> indexEntry : preexistingIndexes.entrySet() ) {
			Document keys = (Document) indexEntry.getValue().get( "key" );
			if ( keys != null && keys.containsKey( "_fts" ) ) {
				return indexEntry.getKey();
			}
		}
		return null;
	}

	private IndexOptions getIndexOptions(OptionsService optionsService, Class<?> entityType) {
		IndexOptions options = optionsService.context().getEntityOptions( entityType ).getUnique( IndexOptionsOption.class );
		if ( options == null ) {
			options = new IndexOptions();
		}
		return options;
	}
}

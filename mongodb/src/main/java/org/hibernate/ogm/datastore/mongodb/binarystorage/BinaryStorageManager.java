/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.binarystorage;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.BinaryStorageType;
import org.hibernate.ogm.datastore.mongodb.options.impl.BinaryStorageOption;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.OptionsService;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class BinaryStorageManager {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
	private static final BinaryStorage NOOP_DELEGATOR = new NoopBinaryStore();

	private final Map<BinaryStorageType, BinaryStorage> BINARY_STORAGE_MAP;
	private final MongoDBDatastoreProvider provider;

	public BinaryStorageManager(MongoDatabase mongoDatabase, MongoDBDatastoreProvider provider) {
		EnumMap<BinaryStorageType, BinaryStorage> map = new EnumMap<>( BinaryStorageType.class );
		map.put( BinaryStorageType.GRID_FS, new GridFSBinaryStore( mongoDatabase ) );
		BINARY_STORAGE_MAP = Collections.unmodifiableMap( map );
		this.provider = provider;
	}

	public  void storeContentToBinaryStorage(Document currentDocument, EntityKey entityKey, OptionsService optionService, Tuple tuple) {
		Class<?> entityClass = provider.getTableEntityTypeMapping().get( entityKey.getTable() );
		if ( currentDocument == null ) {
			return;
		}
		for ( String fieldName : currentDocument.keySet() ) {
			if ( fieldName.equals( "$set" ) ) {
				// it is part of request. it is not document
				Document queryFields = (Document) currentDocument.get( fieldName );
				for ( String queryField : queryFields.keySet() ) {
					storeContentFromFieldToBinaryStorage( queryFields, entityClass, queryField, optionService, tuple );
				}
			}
			else {
				// it is not document
				storeContentFromFieldToBinaryStorage( currentDocument, entityClass, fieldName, optionService, tuple );
			}
		}
	}

	private void storeContentFromFieldToBinaryStorage(Document currentDocument, Class<?> entityClass, String fieldName, OptionsService optionService, Tuple tuple) {
		OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, fieldName );
		BinaryStorage binaryStorage = binaryStorage( optionsContext );

		binaryStorage.storeContentToBinaryStorage( optionsContext, currentDocument, fieldName, tuple );
	}

	private BinaryStorage binaryStorage(OptionsContext optionsContext) {
		BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
		BinaryStorage binaryStorage = BINARY_STORAGE_MAP.getOrDefault( binaryStorageType, NOOP_DELEGATOR );
		return binaryStorage;
	}

	public void removeFromBinaryStorageByEntity(OptionsService optionService, Document deletedDocument, EntityKey entityKey) {
		Class<?> entityClass =  provider.getTableEntityTypeMapping().get( entityKey.getTable() );
		for ( Field currentField : entityClass.getDeclaredFields() ) {
			OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, currentField.getName() );
			BinaryStorage binaryStorage = binaryStorage( optionsContext );

			binaryStorage.removeContentFromBinaryStore( optionsContext, deletedDocument, currentField.getName() );
		}
	}

	public void loadContentFromBinaryStorage(Document currentDocument, EntityKey entityKey, OptionsService optionService) {
		Class<?> entityClass =  provider.getTableEntityTypeMapping().get( entityKey.getTable() );
		if ( currentDocument == null ) {
			return;
		}
		Set<String> sourceKeySet = new HashSet<>( currentDocument.keySet() );
		for ( String fieldName : sourceKeySet ) {
			OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, fieldName );
			BinaryStorage binaryStorage = binaryStorage( optionsContext );
			if ( !( binaryStorage instanceof NoopBinaryStore ) ) {
				Class<?> fieldType = null;
				try {
					fieldType = getFieldType( entityClass, fieldName );
				}
				catch (NoSuchFieldException e) {
					throw log.unknownField( entityClass, fieldName );
				}
				binaryStorage.loadContentFromBinaryStorageToField(
						optionsContext, currentDocument, fieldName, fieldType );
			}
		}
	}

	private  OptionsContext getPropertyOptions( OptionsService optionService, Class entityClass, String fieldName) {
		return optionService.context().getPropertyOptions( entityClass, fieldName );
	}

	private  Class<?> getFieldType( Class entityClass, String fieldName) throws NoSuchFieldException {
		Field field = entityClass.getDeclaredField( fieldName );
		return field.getType();
	}

}

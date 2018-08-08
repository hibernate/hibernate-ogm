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
import java.util.Map;

import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.BinaryStorageType;
import org.hibernate.ogm.datastore.mongodb.options.impl.BinaryStorageOption;
import org.hibernate.ogm.datastore.mongodb.utils.TableEntityTypeMappingInfo;
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

	private static final Map<BinaryStorageType,BinaryStorageDelegator> BINARY_STORAGE_DELEGATOR_MAP;
	private static final BinaryStorageDelegator NULL_DELEGATOR = new NullDelegator();

	static {
		EnumMap<BinaryStorageType, BinaryStorageDelegator> map = new EnumMap<>( BinaryStorageType.class );
		map.put( BinaryStorageType.GRID_FS, new GridFSDelegator() );
		BINARY_STORAGE_DELEGATOR_MAP = Collections.unmodifiableMap( map );
	}

	public static void storeContentToBinaryStorage(MongoDatabase mongoDatabase, Document currentDocument,
											EntityKey entityKey, OptionsService optionService, Tuple tuple) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		if ( currentDocument == null ) {
			return;
		}
		for ( String fieldName : currentDocument.keySet() ) {
			if ( fieldName.equals( "$set" ) ) {
				// it is part of request. it is not document
				Document queryFields = (Document) currentDocument.get( fieldName );
				for ( String queryField : queryFields.keySet() ) {
					storeContentFromFieldToBinaryStorage( mongoDatabase, queryFields, entityClass, queryField, optionService, tuple );
				}
			}
			else {
				// it is not document
				storeContentFromFieldToBinaryStorage( mongoDatabase, currentDocument, entityClass, fieldName, optionService, tuple );
			}
		}
	}

	private static void storeContentFromFieldToBinaryStorage(MongoDatabase mongoDatabase,  Document currentDocument, Class entityClass, String fieldName,
									OptionsService optionService,Tuple tuple) {
		OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, fieldName );
		BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
		BinaryStorageDelegator binaryStorageDelegator = BINARY_STORAGE_DELEGATOR_MAP.getOrDefault( binaryStorageType,NULL_DELEGATOR  );
		binaryStorageDelegator.storeContentToBinaryStorage( mongoDatabase, optionsContext, currentDocument, fieldName,  tuple );
	}

	public static void removeFromGridFsByEntity(MongoDatabase mongoDatabase, OptionsService optionService, Document deletedDocument,
			EntityKey entityKey) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		for ( Field currentField : entityClass.getDeclaredFields() ) {
			OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, currentField.getName() );
			BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
			BinaryStorageDelegator binaryStorageDelegator = BINARY_STORAGE_DELEGATOR_MAP.getOrDefault( binaryStorageType,NULL_DELEGATOR  );
			binaryStorageDelegator.removeContentFromBinaryStore( mongoDatabase,optionsContext,deletedDocument,currentField.getName() );
		}
	}

	public static void loadContentFromGridFs( MongoDatabase mongoDatabase, Document currentDocument, EntityKey entityKey,
			OptionsService optionService) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		if ( currentDocument == null ) {
			return;
		}
		for ( String fieldName : currentDocument.keySet() ) {
			//has the field GridFS info?
			OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, fieldName );
			BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
			BinaryStorageDelegator binaryStorageDelegator = BINARY_STORAGE_DELEGATOR_MAP.getOrDefault( binaryStorageType,NULL_DELEGATOR  );
			binaryStorageDelegator.loadContentFromBinaryStorageToField( mongoDatabase,optionsContext,currentDocument,fieldName );
		}
	}

	private static OptionsContext getPropertyOptions( OptionsService optionService, Class entityClass, String fieldName) {
		return optionService.context().getPropertyOptions( entityClass, fieldName );
	}

}

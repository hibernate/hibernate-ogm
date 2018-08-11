/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.binarystorage;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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
	private static final BinaryStorage NOOP_DELEGATOR = new NoopBinaryStore();

	private final Map<BinaryStorageType, BinaryStorage> BINARY_STORAGE_MAP;

	public BinaryStorageManager(MongoDatabase mongoDatabase) {
		EnumMap<BinaryStorageType, BinaryStorage> map = new EnumMap<>( BinaryStorageType.class );
		map.put( BinaryStorageType.GRID_FS, new GridFSBinaryStore( mongoDatabase ) );
		BINARY_STORAGE_MAP = Collections.unmodifiableMap( map );
	}

	public  void storeContentToBinaryStorage(Document currentDocument, EntityKey entityKey, OptionsService optionService, Tuple tuple) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
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

	private void storeContentFromFieldToBinaryStorage(Document currentDocument, Class entityClass, String fieldName,OptionsService optionService, Tuple tuple) {
		OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, fieldName );
		BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
		BinaryStorage binaryStorage = BINARY_STORAGE_MAP.getOrDefault( binaryStorageType, NOOP_DELEGATOR );
		binaryStorage.storeContentToBinaryStorage( optionsContext, currentDocument, fieldName, tuple );
	}

	public void removeFromBinaryStorageByEntity(OptionsService optionService, Document deletedDocument, EntityKey entityKey) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		for ( Field currentField : entityClass.getDeclaredFields() ) {
			OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, currentField.getName() );
			BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
			BinaryStorage binaryStorage = BINARY_STORAGE_MAP.getOrDefault( binaryStorageType, NOOP_DELEGATOR );
			binaryStorage.removeContentFromBinaryStore( optionsContext, deletedDocument, currentField.getName() );
		}
	}

	public void loadContentFromBinaryStorage(Document currentDocument, EntityKey entityKey, OptionsService optionService) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		if ( currentDocument == null ) {
			return;
		}
		for ( String fieldName : currentDocument.keySet() ) {
			OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, fieldName );
			BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
			BinaryStorage binaryStorage = BINARY_STORAGE_MAP.getOrDefault( binaryStorageType, NOOP_DELEGATOR );
			binaryStorage.loadContentFromBinaryStorageToField( optionsContext, currentDocument, fieldName );
		}
	}

	private  OptionsContext getPropertyOptions( OptionsService optionService, Class entityClass, String fieldName) {
		return optionService.context().getPropertyOptions( entityClass, fieldName );
	}

}

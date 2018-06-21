/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.utils;

import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.BinaryStorageType;
import org.hibernate.ogm.datastore.mongodb.options.impl.BinaryStorageOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.GridFSBucketOption;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.OptionsService;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class GridFsUtil {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	public static void storeContentToGridFs(MongoDatabase mongoDatabase, Document currentDocument,
											EntityKey entityKey, OptionsService optionService, Tuple tuple, SnapshotType snapshotType) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		if ( currentDocument == null ) {
			return;
		}
		for ( String fieldName : currentDocument.keySet() ) {
			if ( fieldName.equals( "$set" ) ) {
				// it is part of request. it is not document
				Document queryFields = (Document) currentDocument.get( fieldName );
				for ( String queryField : queryFields.keySet() ) {
					loadContent( mongoDatabase,queryFields,entityClass,queryField,optionService,tuple,snapshotType );
				}
			}
			else {
				// it is not document
				loadContent( mongoDatabase,currentDocument,entityClass,fieldName,optionService,tuple,snapshotType );
			}
		}
	}

	private static void loadContent(MongoDatabase mongoDatabase,  Document currentDocument, Class entityClass, String fieldName,
									OptionsService optionService,Tuple tuple, SnapshotType snapshotType) {
		OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, fieldName );
		BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
		if ( BinaryStorageType.GRID_FS == binaryStorageType  ) {
			//the field has GridFS configuration. Process it!
			String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );

			GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
			String fileName = "";
			BinaryStream binaryStream = currentDocument.get( fieldName,BinaryStream.class );
			ObjectId uploadId = gridFSFilesBucket.uploadFromStream( fileName, binaryStream.getInputStream() );
			//change value of the field (BinaryStream -> ObjectId)
			currentDocument.put( fieldName, uploadId );
			if ( SnapshotType.UPDATE == snapshotType ) {
				ObjectId oldContentObjectId = (ObjectId) tuple.get( fieldName + "_uploadId" );
				gridFSFilesBucket.delete( oldContentObjectId );
			}
		}
	}

	public static void removeFromGridFsByEntity(MongoDatabase mongoDatabase, OptionsService optionService, MongoCollection<Document> collection,
												EntityKey entityKey, Document searchFilter) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		for ( Field currentField : entityClass.getDeclaredFields() ) {
			OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, currentField.getName() );
			BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
			if ( BinaryStorageType.GRID_FS == binaryStorageType ) {
				//the field has GridFS configuration. Process it!
				String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );
				GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
				MongoCursor<Document> cursor = collection.find( searchFilter )
						.projection( new Document( currentField.getName(), 1 ) ).iterator();
				if ( cursor.hasNext() ) {
					ObjectId gridFsLink = cursor.next().get( currentField.getName(), ObjectId.class );
					gridFSFilesBucket.delete( gridFsLink );
				}
			}
		}
	}

	public static void removeFromGridFsByEntity(MongoDatabase mongoDatabase, OptionsService optionService, Document deletedDocument,
			EntityKey entityKey) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		for ( Field currentField : entityClass.getDeclaredFields() ) {
			OptionsContext optionsContext = getPropertyOptions( optionService, entityClass, currentField.getName() );
			BinaryStorageType binaryStorageType = optionsContext.getUnique( BinaryStorageOption.class );
			if ( BinaryStorageType.GRID_FS == binaryStorageType ) {
				//the field has GridFS configuration. Process it!
				String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );
				GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
				ObjectId gridFsLink = deletedDocument.get( currentField.getName(), ObjectId.class );
				gridFSFilesBucket.delete( gridFsLink );

			}
		}
	}

	private static GridFSBucket getGridFSFilesBucket(MongoDatabase mongoDatabase, String gridfsBucketName) {
		return gridfsBucketName != null ?
				GridFSBuckets.create( mongoDatabase, gridfsBucketName ) : GridFSBuckets.create( mongoDatabase );
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
			if ( BinaryStorageType.GRID_FS == binaryStorageType ) {
				//the field has GridFS configuration. Process it!
				String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );

				GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );

				ObjectId uploadId = currentDocument.get( fieldName, ObjectId.class );
				ByteArrayOutputStream fullContent = new ByteArrayOutputStream( 100_000 );
				//@todo Ask Davide about Blob Proxy
				//GridFSDownloadStream downloadStream = gridFSFilesBucket.openDownloadStream( uploadId );
				//read full blob
				gridFSFilesBucket.downloadToStream( uploadId, fullContent );

				//change value of the field (ObjectId -> BinaryStream)
				currentDocument.put( fieldName, fullContent );
				currentDocument.put( fieldName + "_uploadId", uploadId );
			}
		}
	}

	private static OptionsContext getPropertyOptions( OptionsService optionService, Class entityClass, String fieldName) {
		return optionService.context().getPropertyOptions( entityClass, fieldName );
	}

}

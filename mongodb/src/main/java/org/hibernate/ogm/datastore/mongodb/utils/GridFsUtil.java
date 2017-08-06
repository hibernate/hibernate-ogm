/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.utils;

import java.io.ByteArrayOutputStream;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.BinaryStorageType;
import org.hibernate.ogm.datastore.mongodb.options.impl.BinaryStorageOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.GridFSBucketOption;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.options.spi.OptionsService;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class GridFsUtil {
	private static final Log log = LoggerFactory.getLogger();

	public static void storeContentToGridFs(MongoDatabase mongoDatabase, Document currentDocument,
												EntityKey entityKey, OptionsService optionService) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		if ( currentDocument == null ) {
			return;
		}
		for ( String fieldName : currentDocument.keySet() ) {
			//has the field GridFS info?
			BinaryStorageType binaryStorageType = optionService.context()
					.getPropertyOptions( entityClass, fieldName ).getUnique( BinaryStorageOption.class );
			if ( BinaryStorageType.GRID_FS == binaryStorageType  ) {
				//the field has GridFS configuration. Process it!
				String gridfsBucketName = optionService.context()
						.getPropertyOptions( entityClass, fieldName ).getUnique( GridFSBucketOption.class );

				GridFSBucket gridFSFilesBucket = gridfsBucketName != null ?
						GridFSBuckets.create( mongoDatabase, gridfsBucketName ) : GridFSBuckets.create( mongoDatabase );
				String fileName = "";
				BinaryStream binaryStream = currentDocument.get( fieldName,BinaryStream.class );
				ObjectId uploadId = gridFSFilesBucket.uploadFromStream( fileName, binaryStream.getInputStream() );
				//change value of the field (BinaryStream -> ObjectId)
				currentDocument.put( fieldName, uploadId );
			}
		}
	}

	public static void loadContentFromGridFs(
			MongoDatabase mongoDatabase, Document currentDocument,
			EntityKey entityKey, OptionsService optionService) {
		Class entityClass = TableEntityTypeMappingInfo.getEntityClass( entityKey.getTable() );
		if ( currentDocument == null ) {
			return;
		}
		for ( String fieldName : currentDocument.keySet() ) {
			//has the field GridFS info?
			BinaryStorageType binaryStorageType = optionService.context()
					.getPropertyOptions( entityClass, fieldName ).getUnique( BinaryStorageOption.class );
			if ( BinaryStorageType.GRID_FS == binaryStorageType ) {
				//the field has GridFS configuration. Process it!
				String gridfsBucketName = optionService.context()
						.getPropertyOptions( entityClass, fieldName ).getUnique( GridFSBucketOption.class );

				GridFSBucket gridFSFilesBucket = gridfsBucketName != null ?
						GridFSBuckets.create( mongoDatabase, gridfsBucketName ) : GridFSBuckets.create( mongoDatabase );

				ObjectId uploadId = currentDocument.get( fieldName, ObjectId.class );
				ByteArrayOutputStream fullContent = new ByteArrayOutputStream( 100_000 );
				//@todo Ask Davide about Blob Proxy
				//GridFSDownloadStream downloadStream = gridFSFilesBucket.openDownloadStream( uploadId );
				//read full blob
				gridFSFilesBucket.downloadToStream( uploadId, fullContent );

				//change value of the field (ObjectId -> BinaryStream)
				currentDocument.put( fieldName, fullContent );
			}
		}

	}


}

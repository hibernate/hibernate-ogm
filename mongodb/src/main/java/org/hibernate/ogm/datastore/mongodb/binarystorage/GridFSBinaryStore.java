/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.binarystorage;

import java.io.ByteArrayOutputStream;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.ogm.datastore.mongodb.options.impl.GridFSBucketOption;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * The implementation provides features for GridFS usage
 *
 * @see <a href ="https://docs.mongodb.com/manual/core/gridfs">GridFS documentation</a>
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class GridFSBinaryStore implements BinaryStorage {

	private static final int BUFFER_SIZE = 100_000;
	private static final String UPLOAD_ID = "_uploadId";
	private final MongoDatabase mongoDatabase;

	/**
	 * constructor
	 * @param mongoDatabase current database
	 */
	public GridFSBinaryStore(MongoDatabase mongoDatabase) {
		this.mongoDatabase = mongoDatabase;
	}

	/**
	 * store binary content of field to GridFS
	 *
	 * @param optionsContext options context
	 * @param currentDocument current BSON document
	 * @param fieldName fieldName with binary content
	 * @param tuple tuple for processing
	 */

	@Override
	public void storeContentToBinaryStorage(OptionsContext optionsContext, Document currentDocument, String fieldName, Tuple tuple ) {
		String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );

		GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
		String fileName = "";
		BinaryStream binaryStream = currentDocument.get( fieldName,BinaryStream.class );
		ObjectId uploadId = gridFSFilesBucket.uploadFromStream( fileName, binaryStream.getInputStream() );
		//change value of the field (BinaryStream -> ObjectId)
		currentDocument.put( fieldName, uploadId );
		if ( Tuple.SnapshotType.UPDATE == tuple.getSnapshotType() ) {
			ObjectId oldContentObjectId = (ObjectId) tuple.get( fieldName + UPLOAD_ID );
			gridFSFilesBucket.delete( oldContentObjectId );
		}
	}

	/**
	 * remove binary content from store
	 * @param optionsContext options context
	 * @param deletedDocument deleted BSON document
	 * @param fieldName field name with binary content
	 */

	@Override
	public void removeContentFromBinaryStore( OptionsContext optionsContext, Document deletedDocument, String fieldName) {
		String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );
		GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
		ObjectId gridFsLink = deletedDocument.get( fieldName, ObjectId.class );
		gridFSFilesBucket.delete( gridFsLink );
	}

	/**
	 * load content from binary store to field
	 * @param optionsContext options context
	 * @param currentDocument current BSON document
	 * @param fieldName field name with binary content
	 */
	@Override
	public void loadContentFromBinaryStorageToField( OptionsContext optionsContext, Document currentDocument, String fieldName) {
		String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );

		GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );

		ObjectId uploadId = currentDocument.get( fieldName, ObjectId.class );
		ByteArrayOutputStream fullContent = new ByteArrayOutputStream( BUFFER_SIZE );
		//read full blob
		gridFSFilesBucket.downloadToStream( uploadId, fullContent );

		//change value of the field (ObjectId -> BinaryStream)
		currentDocument.put( fieldName, fullContent );
		currentDocument.put( fieldName + UPLOAD_ID, uploadId );
	}

	private GridFSBucket getGridFSFilesBucket(MongoDatabase mongoDatabase, String gridfsBucketName) {
		return gridfsBucketName != null ?
				GridFSBuckets.create( mongoDatabase, gridfsBucketName ) : GridFSBuckets.create( mongoDatabase );
	}
}

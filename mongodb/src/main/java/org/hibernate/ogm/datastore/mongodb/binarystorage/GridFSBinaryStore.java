/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.binarystorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.sql.Blob;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.impl.GridFSBucketOption;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.BsonBinary;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

/**
 * The implementation provides features for GridFS usage
 *
 * @see <a href ="https://docs.mongodb.com/manual/core/gridfs">GridFS documentation</a>
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class GridFSBinaryStore implements BinaryStorage {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
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

	@Override
	public void storeContentToBinaryStorage(OptionsContext optionsContext, Document currentDocument, String fieldName, Tuple tuple) {
		String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );
		Document metadata = new Document(  );
		GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
		String fileName = "";
		Object binaryContentObject = currentDocument.get( fieldName );
		InputStream  binaryInputStream = null;
		if ( binaryContentObject instanceof BinaryStream ) {
			BinaryStream binaryStream = (BinaryStream) binaryContentObject;
			binaryInputStream = binaryStream.getInputStream();
			metadata.put( "binaryContent","BinaryStream" );
		}
		else if ( binaryContentObject instanceof BsonBinary ) {
			BsonBinary bsonBinary = (BsonBinary) binaryContentObject;
			binaryInputStream = new ByteArrayInputStream( bsonBinary.getData() );
			metadata.put( "binaryContent","BsonBinary" );
		}
		else if ( binaryContentObject instanceof String ) {
			String string = (String) binaryContentObject;
			//use default encoding
			binaryInputStream = new ByteArrayInputStream( string.getBytes() );
			metadata.put( "binaryContent","String" );
		}
		else {
			throw log.unsupportedBinaryType( binaryContentObject.getClass() );
		}
		GridFSUploadOptions gridFSUploadOptions = new GridFSUploadOptions();
		gridFSUploadOptions.metadata( metadata );
		ObjectId uploadId = gridFSFilesBucket.uploadFromStream( fileName, binaryInputStream,gridFSUploadOptions );
		// change value of the field (BinaryStream -> ObjectId)
		currentDocument.put( fieldName, uploadId );
		if ( Tuple.SnapshotType.UPDATE == tuple.getSnapshotType() ) {
			ObjectId oldContentObjectId = (ObjectId) tuple.get( fieldName + UPLOAD_ID );
			gridFSFilesBucket.delete( oldContentObjectId );
		}
	}

	@Override
	public void removeContentFromBinaryStore(OptionsContext optionsContext, Document deletedDocument, String fieldName) {
		String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );
		GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
		ObjectId gridFsLink = deletedDocument.get( fieldName, ObjectId.class );
		gridFSFilesBucket.delete( gridFsLink );
	}

	@Override
	public void loadContentFromBinaryStorageToField( OptionsContext optionsContext, Document currentDocument, String fieldName, Class<?> fieldType) {
		String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );

		GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
		ObjectId uploadId = currentDocument.get( fieldName, ObjectId.class );
		Document metadata = gridFSFilesBucket.openDownloadStream( uploadId ).getGridFSFile().getMetadata();

		if ( fieldType.equals( Blob.class ) ) {
			//lazy reading blob
			GridFSDownloadStream gridFSDownloadStream = gridFSFilesBucket.openDownloadStream( uploadId ).batchSize(
					BUFFER_SIZE );
			//change value of the field (ObjectId -> BinaryStream)
			currentDocument.put( fieldName, gridFSDownloadStream );
		}
		else if ( fieldType.equals( byte[].class ) ) {
			//change value of the field (ObjectId -> org.bson.types.Binary)
			ByteArrayOutputStream byteArrayContentStream = new ByteArrayOutputStream(  );
			gridFSFilesBucket.downloadToStream( uploadId, byteArrayContentStream  );
			currentDocument.put( fieldName, new Binary( byteArrayContentStream.toByteArray() ) );
		}
		else if ( fieldType.equals( String.class ) ) {
			//change value of the field (ObjectId -> org.bson.types.Binary)
			ByteArrayOutputStream byteArrayContentStream = new ByteArrayOutputStream(  );
			gridFSFilesBucket.downloadToStream( uploadId, byteArrayContentStream  );
			currentDocument.put( fieldName, new String( byteArrayContentStream.toByteArray() ) );
		}

		currentDocument.put( fieldName + UPLOAD_ID, uploadId );
	}

	private GridFSBucket getGridFSFilesBucket(MongoDatabase mongoDatabase, String gridfsBucketName) {
		return gridfsBucketName != null
				? GridFSBuckets.create( mongoDatabase, gridfsBucketName )
				: GridFSBuckets.create( mongoDatabase );
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.binarystorage;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.options.impl.GridFSBucketOption;
import org.hibernate.ogm.datastore.mongodb.type.GridFS;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.OptionsService;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;

/**
 * Tracks which fields have to be stored using GridFS.
 * <p>
 * GridFS is a specification for storing and retrieving files that exceed the BSON-document size limit of 16 MB.
 * <p>
 * The default bucket name is the name of the class followed by the suffix "_bucket". The file name is created using the
 * field name and the id of the document.
 *
 * @see <a href="https://docs.mongodb.com/manual/core/gridfs/">MongoDB GridFS documentation</a>
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class GridFSStorageManager {

	private static final String BUCKET_SUFFIX = "_bucket";

	private final Map<String, GridFSFields> tableEntityTypeMapping;

	private final OptionsService optionsService;

	private final MongoDatabase mongoDatabase;

	public GridFSStorageManager(MongoDBDatastoreProvider provider,
			OptionsService optionsService,
			Map<String, GridFSFields> tableEntityTypeMapping) {
		this.optionsService = optionsService;
		this.tableEntityTypeMapping = tableEntityTypeMapping == null ? (Map<String, GridFSFields>) Collections.emptyList() : tableEntityTypeMapping;
		this.mongoDatabase = provider.getDatabase();
	}

	public void storeContentToBinaryStorage(Document currentDocument, EntityKeyMetadata metadata, Object documentId) {
		if ( currentDocument != null && metadata != null ) {
			GridFSFields gridFSFields = tableEntityTypeMapping.get( metadata.getTable() );
			if ( gridFSFields != null ) {
				for ( Field field : gridFSFields.getFields() ) {
					String bucketName = bucketName( metadata, field.getName() );
					storeContentFromFieldToBinaryStorage( bucketName, currentDocument, field.getName(), documentId );
				}
			}
		}
	}

	private void storeContentFromFieldToBinaryStorage(String bucketName, Document documentToInsert, String fieldName, Object documentId) {
		if ( documentToInsert.containsKey( fieldName ) ) {
			GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, bucketName );
			// We delete the previous entry, first
			deleteExistingContent( fieldName, documentId, gridFSFilesBucket );
			GridFS gridfsObject = documentToInsert.get( fieldName, GridFS.class );
			if ( gridfsObject != null ) {
				ObjectId uploadId = gridFSFilesBucket.uploadFromStream( fileName( fieldName, documentId ), gridfsObject.getInputStream() );
				documentToInsert.put( fieldName, uploadId );
			}
		}
	}

	private void deleteExistingContent(String fieldName, Object documentId, GridFSBucket gridFSFilesBucket) {
		GridFSFindIterable results = gridFSFilesBucket.find( Filters.and( Filters.eq( "filename", fileName( fieldName, documentId ) ) ) );
		try ( MongoCursor<GridFSFile> iterator = results.iterator() ) {
			while ( iterator.hasNext() ) {
				GridFSFile next = iterator.next();
				gridFSFilesBucket.delete( next.getId() );
			}
		}
	}

	/*
	 * The id is necessary, otherwise two instances of the same entity will have the same file name
	 * and override each other.
	 */
	private String fileName(String fieldName, Object documentId) {
		return fieldName + "_" + String.valueOf( documentId );
	}

	public void removeEntityFromBinaryStorage(Document deletedDocument, EntityKeyMetadata entityKeyMetadata) {
		removeFieldsFromBinaryStorage( deletedDocument, entityKeyMetadata, deletedDocument.get( "_id" ) );
	}

	public void removeFieldsFromBinaryStorage(Document fieldsToDelete, EntityKeyMetadata entityKeyMetadata, Object documentId) {
		if ( fieldsToDelete != null && entityKeyMetadata != null ) {
			GridFSFields storageFields = tableEntityTypeMapping.get( entityKeyMetadata.getTable() );
			if ( storageFields != null ) {
				Set<Field> fields = storageFields.getFields();
				for ( Field gridfsField : fields ) {
					if ( fieldsToDelete.containsKey( gridfsField.getName() ) ) {
						String gridfsBucketName = bucketName( entityKeyMetadata, gridfsField.getName() );
						GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, gridfsBucketName );
						deleteExistingContent( gridfsField.getName(), documentId, gridFSFilesBucket );
					}
				}
			}
		}
	}

	private String bucketName(EntityKeyMetadata entityKeyMetadata, String fieldName) {
		GridFSFields storageFields = tableEntityTypeMapping.get( entityKeyMetadata.getTable() );
		OptionsContext optionsContext = propertyOptions( storageFields.getEntityClass(), fieldName );
		String gridfsBucketName = optionsContext.getUnique( GridFSBucketOption.class );
		if ( gridfsBucketName != null ) {
			return gridfsBucketName;
		}

		// Default
		return entityKeyMetadata.getTable() + BUCKET_SUFFIX;
	}

	public void loadContentFromBinaryStorage(Document currentDocument, EntityKeyMetadata metadata) {
		if ( metadata != null ) {
			GridFSFields fields = tableEntityTypeMapping.get( metadata.getTable() );
			if ( currentDocument != null && fields != null ) {
				for ( Field field : fields.getFields() ) {
					String bucketName = bucketName( metadata, field.getName() );
					loadContentFromBinaryStorageToField( bucketName, currentDocument, field.getName() );
				}
			}
		}
	}

	private void loadContentFromBinaryStorageToField(String bucketName, Document currentDocument, String fieldName) {
		GridFSBucket gridFSFilesBucket = getGridFSFilesBucket( mongoDatabase, bucketName );
		Object uploadId = currentDocument.get( fieldName );
		if ( uploadId != null ) {
			GridFSDownloadStream gridFSDownloadStream = gridFSFilesBucket.openDownloadStream( (ObjectId) uploadId );
			GridFS value = new GridFS( gridFSDownloadStream );
			currentDocument.put( fieldName, value );
		}
	}

	private OptionsContext propertyOptions(Class<?> entityType, String fieldName) {
		OptionsContext propertyOptions = optionsService.context().getPropertyOptions( entityType, fieldName );
		return propertyOptions;
	}

	private GridFSBucket getGridFSFilesBucket(MongoDatabase mongoDatabase, String bucketName) {
		return bucketName != null
				? GridFSBuckets.create( mongoDatabase, bucketName )
				: GridFSBuckets.create( mongoDatabase );
	}
}

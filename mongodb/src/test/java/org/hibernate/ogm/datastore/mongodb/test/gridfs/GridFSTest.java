/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.gridfs;

import static org.fest.assertions.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.ogm.datastore.mongodb.binarystorage.BinaryContentType;
import org.hibernate.ogm.datastore.mongodb.binarystorage.MetadataField;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.Test;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;

/**
 * @author Davide D'Alto
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 * @see <a href="http://mongodb.github.io/mongo-java-driver/3.4/driver/tutorials/gridfs/">GridFSBucket</a>
 */
@TestForIssue(jiraKey = "OGM-786")
public class GridFSTest extends OgmJpaTestCase {

	static final String BUCKET_NAME = "photos";

	// GridFS is usually for file of size bigger than 16 MB
	private static final int BLOB_SIZE = 30 * 1024 * 1024; // 30 MB

	private static final byte[] BLOB_CONTENT_1 = createByteArray( 'a', BLOB_SIZE );
	private static final byte[] BLOB_CONTENT_2 = createByteArray( 'x', BLOB_SIZE );
	private static final String BLOB_STRING_CONTENT_1 = new String( BLOB_CONTENT_1, StandardCharsets.UTF_8  );
	private static final String BLOB_STRING_CONTENT_2 = new String( BLOB_CONTENT_2, StandardCharsets.UTF_8  );

	private static final String ENTITY_ID_1 = "photo1";
	private static final String ENTITY_ID_2 = "photo2";

	private static byte[] createByteArray(char c, int size) {
		char[] chars = new char[size];
		Arrays.fill( chars, c );
		byte[] bytes = new String( chars ).getBytes( StandardCharsets.UTF_8 );
		return bytes;
	}

	@Test
	public void canCreateEntityWithBlob() {
		inTransaction( em -> {
			Photo photo = new Photo();
			photo.setId( ENTITY_ID_1 );
			photo.setDescription( "photo1" );
			Blob blob = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_1 );
			photo.setContentAsBlob( blob );
			photo.setContentAsByteArray( BLOB_CONTENT_1 );
			photo.setContentAsString( BLOB_STRING_CONTENT_1 );
			em.persist( photo );
		} );

		inTransaction( em -> {
			MongoDatabase mongoDatabase = getCurrentDB( em );
			GridFSBucket gridFSFilesBucket = GridFSBuckets.create( mongoDatabase, BUCKET_NAME );
			MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
			int itCount = 0;
			Set<BinaryContentType> readTypes = EnumSet.noneOf( BinaryContentType.class );
			for ( ; cursor.hasNext(); itCount++ ) {
				GridFSFile savedFile = cursor.next();
				assertThat( savedFile.getLength() ).isEqualTo( BLOB_CONTENT_1.length );
				Document metadata = savedFile.getMetadata();
				BinaryContentType binaryContentType = BinaryContentType.valueOf( metadata.getString( MetadataField.binaryContentType.name() ) );
				readTypes.add( binaryContentType );
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				gridFSFilesBucket.downloadToStream( savedFile.getObjectId(), outputStream );
				switch ( binaryContentType ) {
					case BinaryStream:
					case BsonBinary:
						assertThat( outputStream.size() ).isEqualTo( BLOB_CONTENT_1.length );
						assertThat( outputStream.toByteArray() ).isEqualTo( BLOB_CONTENT_1 );
						break;
					case String:
						String actual = new String( outputStream.toByteArray(), StandardCharsets.UTF_8  );
						assertThat( actual ).isEqualTo( BLOB_STRING_CONTENT_1 );
				}
			}
			assertThat( itCount ).isEqualTo( 3 );
			assertThat( readTypes ).hasSize( 3 );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, ENTITY_ID_1 );
			assertThat( photo ).isNotNull();
			assertBlobAreEqual( photo.getContentAsBlob(), BLOB_CONTENT_1 );
			assertThat( photo.getContentAsByteArray() ).isEqualTo( BLOB_CONTENT_1 );
			assertThat( photo.getContentAsString() ).isEqualTo( BLOB_STRING_CONTENT_1 );
		} );
	}

	@Test
	public void canReplaceBlobInEntity() {
		inTransaction( em -> {
			Photo photo = em.find( Photo.class, ENTITY_ID_1 );
			assertThat( photo ).isNotNull();
			assertThat( photo.getContentAsBlob() ).isNotNull();
			assertBlobAreEqual( photo.getContentAsBlob(), BLOB_CONTENT_1 );

			Blob blob2 = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_2 );
			photo.setContentAsBlob( blob2 );
			photo.setContentAsByteArray( BLOB_CONTENT_2 );
			photo.setContentAsString( BLOB_STRING_CONTENT_2 );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, ENTITY_ID_1 );
			assertThat( photo ).isNotNull();
			assertBlobAreEqual( photo.getContentAsBlob(), BLOB_CONTENT_2 );
			assertThat( photo.getContentAsByteArray() ).isEqualTo( BLOB_CONTENT_2 );
			assertThat( photo.getContentAsString() ).isEqualTo( BLOB_STRING_CONTENT_2 );
		} );

		inTransaction( em -> {
			GridFSBucket gridFSFilesBucket = GridFSBuckets.create( getCurrentDB( em ), BUCKET_NAME );
			MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
			List<GridFSFile> files = new LinkedList<>();
			for ( ; cursor.hasNext(); ) {
				files.add( cursor.next() );
			}
			assertThat( files.size() ).isEqualTo( 3 );
		} );
	}

	@Test
	public void canRemoveEntityWithBlob() {

		inTransaction( em -> {
			Photo photo = new Photo();
			photo.setId( ENTITY_ID_2 );
			photo.setDescription( "photo2" );

			Blob blob = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_1 );
			photo.setContentAsBlob( blob );
			photo.setContentAsByteArray( BLOB_CONTENT_1 );
			photo.setContentAsString( BLOB_STRING_CONTENT_1 );
			em.persist( photo );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, ENTITY_ID_2 );
			assertThat( photo ).isNotNull();
			assertBlobAreEqual( photo.getContentAsBlob(), BLOB_CONTENT_1 );
			assertThat( photo.getContentAsByteArray() ).isEqualTo( BLOB_CONTENT_1 );
			assertThat( photo.getContentAsString() ).isEqualTo( BLOB_STRING_CONTENT_1 );
			em.remove( photo );
		} );

		inTransaction( em -> {
			GridFSBucket gridFSFilesBucket = GridFSBuckets.create( getCurrentDB( em ), BUCKET_NAME );
			MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
			List<GridFSFile> files = new LinkedList<>();
			while ( cursor.hasNext() ) {
				files.add( cursor.next() );
			}
			assertThat( files.size() ).isEqualTo( 3 ); // files for ENTITY_ID_1
		} );
	}

	private MongoDatabase getCurrentDB(EntityManager em) {
		Session session = em.unwrap( Session.class );
		OgmSessionFactoryImpl sessionFactory = (OgmSessionFactoryImpl) session.getSessionFactory();
		MongoDBDatastoreProvider mongoDBDatastoreProvider = (MongoDBDatastoreProvider) sessionFactory.getServiceRegistry()
				.getService( DatastoreProvider.class );
		return mongoDBDatastoreProvider.getDatabase();
	}

	private void assertBlobAreEqual(Blob actual, byte[] expected) {
		try ( InputStream binaryStream = actual.getBinaryStream() ) {
			byte[] actualAsByte = new byte[expected.length];
			DataInputStream stream = new DataInputStream( binaryStream );
			stream.readFully( actualAsByte );
			stream.close();
			assertThat( actualAsByte ).isEqualTo( expected );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Photo.class };
	}

}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.gridfs;

import static org.fest.assertions.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.Session;
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

/**
 * @author Davide D'Alto
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 * @see <a href="http://mongodb.github.io/mongo-java-driver/3.4/driver/tutorials/gridfs/">GridFSBucket</a>
 */
@TestForIssue(jiraKey = "OGM-786")
public class GridFSTest extends OgmJpaTestCase {

	// GridFS is usually for file of size bigger than 16 MB
	private final static int BLOB_SIZE = 30 * 1024 * 1024; // 30 MB

	static final String BUCKET_NAME = "photos";

	private static final byte[] BLOB_CONTENT_1 = createByteArray( 'a', BLOB_SIZE );
	private static final byte[] BLOB_CONTENT_2 = createByteArray( 'x', BLOB_SIZE );

	private static final String ENTITY_ID_1 = "photo1";
	private static final String ENTITY_ID_2 = "photo2";

	private static byte[] createByteArray(char c, int size) {
		char[] chars = new char[30 * 1024 * 1024];
		Arrays.fill( chars, c );
		byte[] bytes = new String( chars ).getBytes( StandardCharsets.UTF_8 );
		return bytes;
	}

	@Test
	public void canCreateEntityWithBlob() throws Exception {
		inTransaction( em -> {
			Photo photo = new Photo();
			photo.setId( ENTITY_ID_1 );
			photo.setDescription( "photo1" );
			Blob blob = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_1 );
			photo.setContent( blob );
			em.persist( photo );
		} );

		inTransaction( em -> {
			MongoDatabase mongoDatabase = getCurrentDB( em );
			GridFSBucket gridFSFilesBucket = GridFSBuckets.create( mongoDatabase, BUCKET_NAME );
			MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
			assertThat( cursor.hasNext() ).isEqualTo( true );

			GridFSFile savedFile = cursor.next();
			assertThat( savedFile ).isNotNull();
			assertThat( savedFile.getLength() ).isEqualTo( BLOB_CONTENT_1.length );

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			gridFSFilesBucket.downloadToStream( savedFile.getObjectId(), outputStream );
			assertThat( outputStream.size() ).isEqualTo( BLOB_CONTENT_1.length );
			assertThat( outputStream.toByteArray() ).isEqualTo( BLOB_CONTENT_1 );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, ENTITY_ID_1 );
			assertThat( photo ).isNotNull();
			assertThat( photo.getContent() ).isNotNull();
			assertBlobAreEqual( photo.getContent(), BLOB_CONTENT_1 );
		} );
	}

	@Test
	public void canReplaceBlobInEntity() throws Exception {
		inTransaction( em -> {
			Photo photo = em.find( Photo.class, ENTITY_ID_1 );
			assertThat( photo ).isNotNull();
			assertThat( photo.getContent() ).isNotNull();
			assertBlobAreEqual( photo.getContent(), BLOB_CONTENT_1 );

			Blob blob2 = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_2 );
			photo.setContent( blob2 );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, ENTITY_ID_1 );
			assertThat( photo ).isNotNull();
			assertThat( photo.getContent() ).isNotNull();
			assertBlobAreEqual( photo.getContent(), BLOB_CONTENT_2 );
		} );

		inTransaction( em -> {
			GridFSBucket gridFSFilesBucket = GridFSBuckets.create( getCurrentDB( em ), BUCKET_NAME );
			MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
			List<GridFSFile> files = new LinkedList<>();
			for ( ; cursor.hasNext(); ) {
				files.add( cursor.next() );
			}
			assertThat( files.size() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void canRemoveEntityWithBlob() throws Exception {

		inTransaction( em -> {
			Photo photo = new Photo();
			photo.setId( ENTITY_ID_2 );
			photo.setDescription( "photo2" );

			Blob blob = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_1 );
			photo.setContent( blob );
			em.persist( photo );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, ENTITY_ID_2 );
			assertThat( photo ).isNotNull();
			assertThat( photo.getContent() ).isNotNull();
			assertBlobAreEqual( photo.getContent(), BLOB_CONTENT_1 );

			em.remove( photo );
		} );

		inTransaction( em -> {
			GridFSBucket gridFSFilesBucket = GridFSBuckets.create( getCurrentDB( em ), BUCKET_NAME );
			MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
			List<GridFSFile> files = new LinkedList<>();
			while ( cursor.hasNext() ) {
				files.add( cursor.next() );
			}
			assertThat( files.size() ).isEqualTo( 1 );
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
		try {
			assertThat( actual.length() ).isEqualTo( expected.length );
			assertThat( actual.getBytes( 1, expected.length ) ).isEqualTo( expected );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Photo.class };
	}

}

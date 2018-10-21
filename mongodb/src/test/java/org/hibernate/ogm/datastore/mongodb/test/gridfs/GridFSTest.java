/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.gridfs;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.test.gridfs.Photo.BUCKET_NAME;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.type.GridFS;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
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

	private static final String DEFAULT_BUCKET_NAME = Photo.class.getSimpleName() + "_bucket";

	/*
	 * WARNING: GridFS makes sense for big files (over 16 MB). In case of error
	 * the test might try to print the result and it will take some time.
	 *
	 * I will keep this to a high value to make sure that there are no problems but,
	 * for development, it makes sense to use something smaller.
	 */
	private static final int CONTENT_SIZE = 30_000_000; // ~30 MB

	private static final String STRING_CONTENT_1 = createString( 'a', CONTENT_SIZE );
	private static final String STRING_CONTENT_2 = createString( 'x', CONTENT_SIZE );

	private static final byte[] BYTE_ARRAY_CONTENT_1 = STRING_CONTENT_1.getBytes();
	private static final byte[] BYTE_ARRAY_CONTENT_2 = STRING_CONTENT_2.getBytes();

	private static String createString(char c, int size) {
		char[] chars = new char[size];
		Arrays.fill( chars, c );
		return new String( chars );
	}

	@After
	public void deleteAll() throws Exception {
		removeEntities();
	}

	@Test
	public void testGridFSFieldWithBucketAnnotation() throws Exception {
		final String photoId  = "testGridFSFieldWithBucketAnnotation";
		inTransaction( em -> {
			Photo photo = new Photo( photoId );
			photo.setGridFS( new GridFS( BYTE_ARRAY_CONTENT_1 ) );
			em.persist( photo );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, photoId );
			assertThat( photo ).isNotNull();
			assertThatGridFSAreEqual( photo.getGridFS(), BYTE_ARRAY_CONTENT_1 );
		} );
	}

	@Test
	public void testGridFSFieldWithDefaultBucket() throws Exception {
		final String photoId  = "testGridFSFieldWithDefaultBucket";
		inTransaction( em -> {
			Photo photo = new Photo( photoId );
			photo.setGridfsWithDefaultBucket( new GridFS( BYTE_ARRAY_CONTENT_1 ) );
			em.persist( photo );
		} );

		// Check GridFS has been updated
		List<GridFS> bucketContent = bucketContent( DEFAULT_BUCKET_NAME );

		assertThat( bucketContent ).hasSize( 1 );
		assertThatGridFSAreEqual( bucketContent.get( 0 ), BYTE_ARRAY_CONTENT_1 );
	}

	@Test
	public void testSettingDifferentBuckets() throws Exception {
		final String photoId  = "testGridFSFieldWithDefaultBucket";
		inTransaction( em -> {
			Photo photo = new Photo( photoId );
			photo.setGridfsWithDefaultBucket( new GridFS( BYTE_ARRAY_CONTENT_1 ) );
			photo.setGridFS( new GridFS( BYTE_ARRAY_CONTENT_2 ) );
			em.persist( photo );
		} );

		List<GridFS> defaultBucketName = bucketContent( DEFAULT_BUCKET_NAME );
		assertThat( defaultBucketName ).hasSize( 1 );
		assertThatGridFSAreEqual( defaultBucketName.get( 0 ), BYTE_ARRAY_CONTENT_1 );

		List<GridFS> customBucketName = bucketContent( BUCKET_NAME );
		assertThat( customBucketName ).hasSize( 1 );
		assertThatGridFSAreEqual( customBucketName.get( 0 ), BYTE_ARRAY_CONTENT_2 );
	}

	@Test
	public void testUpdateFieldToNull() throws Exception {
		final String photoId  = "testUpdateFieldToNull";
		inTransaction( em -> {
			Photo photo = new Photo( photoId );
			photo.setGridFS( new GridFS( BYTE_ARRAY_CONTENT_2 ) );
			em.persist( photo );
		} );

		inTransaction( em -> {
			Photo photo1 = em.find( Photo.class, photoId );
			photo1.setGridFS( null );
		} );

		List<GridFS> bucketName = bucketContent( BUCKET_NAME );

		// Normally I would use containsExactly, but it fails for some reason
		assertThat( bucketName ).hasSize( 0 );
	}

	@Test
	public void testMultipleUpdates() throws Exception {
		final String photoId  = "testMultipleUpdates";
		inTransaction( em -> {
			Photo photo = new Photo( photoId );
			photo.setGridfsWithDefaultBucket( new GridFS( BYTE_ARRAY_CONTENT_1 ) );
			photo.setGridFS( new GridFS( BYTE_ARRAY_CONTENT_2 ) );
			em.persist( photo );
		} );

		inTransaction( em -> {
			Photo photo1 = em.find( Photo.class, photoId );
			photo1.setGridfsWithDefaultBucket( new GridFS( BYTE_ARRAY_CONTENT_2 ) );
			photo1.setGridFS( null );
		} );

		List<GridFS> bucketName = bucketContent( BUCKET_NAME );
		List<GridFS> defaultBucketName = bucketContent( DEFAULT_BUCKET_NAME );

		assertThat( bucketName ).hasSize( 0 );

		assertThat( defaultBucketName ).hasSize( 1 );
		assertThatGridFSAreEqual( defaultBucketName.get( 0 ), BYTE_ARRAY_CONTENT_2 );
	}

	@Test
	public void testSaveMultipleEntities() {
		final String photoId1  = "testSaveMultipleEntities1";
		final String photoId2  = "testSaveMultipleEntities2";
		inTransaction( em -> {
			Photo photo1 = new Photo( photoId1 );
			photo1.setGridFS( new GridFS( BYTE_ARRAY_CONTENT_1 ) );
			em.persist( photo1 );

			Photo photo2 = new Photo( photoId2 );
			photo2.setGridFS( new GridFS( BYTE_ARRAY_CONTENT_2 ) );
			em.persist( photo2 );
		} );

		inTransaction( em -> {
			Photo photo1 = em.find( Photo.class, photoId1 );
			assertThat( photo1 ).isNotNull();
			assertThatGridFSAreEqual( photo1.getGridFS(), BYTE_ARRAY_CONTENT_1 );

			Photo photo2 = em.find( Photo.class, photoId2 );
			assertThat( photo2 ).isNotNull();
			assertThatGridFSAreEqual( photo2.getGridFS(), BYTE_ARRAY_CONTENT_2 );
		} );
	}

	@Test
	public void canUpdateEntityAndBucket() {
		final String photoId  = "canUpdateEntityAndBucket";
		inTransaction( em -> {
			Photo photo = new Photo( photoId );
			em.persist( photo );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, photoId );
			photo.setGridFS( new GridFS( BYTE_ARRAY_CONTENT_1 ) );
		} );

		// Check change has been saved
		inTransaction( em -> {
			Photo photo = em.find( Photo.class, photoId );

			assertThat( photo ).isNotNull();
			assertThatGridFSAreEqual( photo.getGridFS(), BYTE_ARRAY_CONTENT_1 );
		} );

		// Check GridFS has been updated
		List<GridFS> bucketContent = bucketContent( BUCKET_NAME );

		assertThat( bucketContent ).hasSize( 1 );
		assertThatGridFSAreEqual( bucketContent.get( 0 ), BYTE_ARRAY_CONTENT_1 );
	}

	@Test
	public void testBucketDeletion() {
		final String photoId  = "testBucketDeletion";
		inTransaction( em -> {
			Photo photo = new Photo();
			photo.setId( photoId );

			em.persist( photo );
		} );

		inTransaction( em -> {
			Photo photo = em.find( Photo.class, photoId );
			em.remove( photo );
		} );

		List<GridFS> bucketContent = bucketContent( BUCKET_NAME );

		assertThat( bucketContent.isEmpty() );
	}

	private List<GridFS> bucketContent(String bucketName) {
		List<GridFS> bucketContent = new ArrayList<>();
		inTransaction( em -> {
			MongoDatabase mongoDatabase = getCurrentDB( em );
			GridFSBucket gridFSFilesBucket = GridFSBuckets.create( mongoDatabase, bucketName );
			MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
			while ( cursor.hasNext() ) {
				GridFSFile savedFile = cursor.next();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				gridFSFilesBucket.downloadToStream( savedFile.getObjectId(), outputStream );
				bucketContent.add( new GridFS( outputStream.toByteArray() ) );
			}
		} );
		return bucketContent;
	}

	private void assertThatGridFSAreEqual(GridFS actual, byte[] expected) {
		byte[] bytes = convertToBytes( actual.getInputStream() );
		assertThat( bytes ).isEqualTo( expected );
	}

	private static byte[] convertToBytes(InputStream is) {
		byte[] bytes = new byte[CONTENT_SIZE];
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try ( DataInputStream data = new DataInputStream( is ) ) {
			int nRead = 0;
			while ( ( nRead =  is.read( bytes, 0, CONTENT_SIZE ) ) != -1 ) {
				os.write( bytes, 0, nRead );
			}
			os.flush();
			return os.toByteArray();
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private MongoDatabase getCurrentDB(EntityManager em) {
		Session session = em.unwrap( Session.class );
		OgmSessionFactoryImpl sessionFactory = (OgmSessionFactoryImpl) session.getSessionFactory();
		MongoDBDatastoreProvider mongoDBDatastoreProvider = (MongoDBDatastoreProvider) sessionFactory.getServiceRegistry()
				.getService( DatastoreProvider.class );
		return mongoDBDatastoreProvider.getDatabase();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Photo.class };
	}

}

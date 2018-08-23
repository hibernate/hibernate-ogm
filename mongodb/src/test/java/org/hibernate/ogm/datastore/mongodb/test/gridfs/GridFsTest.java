/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.gridfs;

import static org.fest.assertions.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.impl.GridDialectLogger;
import org.hibernate.ogm.dialect.impl.OgmDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 * @see <a href="http://mongodb.github.io/mongo-java-driver/3.4/driver/tutorials/gridfs/">GridFSBucket</a>
 */
@TestForIssue(jiraKey = "OGM-786")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GridFsTest extends OgmJpaTestCase {
	static final String BUCKET_NAME = "photos";

	private static final byte[] BLOB_CONTENT_1 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	private static final byte[] BLOB_CONTENT_2 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
	private static final String ENTITY_ID_1 = "photo1";
	private static final String ENTITY_ID_2 = "photo2";

	private EntityManager em;
	private MongoDatabase mongoDatabase;

	@Before
	public void setUpClass() {
		em = getFactory().createEntityManager();
		mongoDatabase = getCurrentDB( em );
	}

	@After
	public void tearDown() {
		if ( em.getTransaction().isActive() ) {
			em.getTransaction().rollback();
		}
		em.clear();
		em.close();
	}


	@Test
	public void canCreateEntityWithBlob() throws SQLException {
		em.getTransaction().begin();

		Photo photo = new Photo();
		photo.setId( ENTITY_ID_1 );
		photo.setDescription( "photo1" );
		Blob blob = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_1 );
		photo.setContent( blob );
		em.persist( photo );
		em.getTransaction().commit();

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

		em.getTransaction().begin();
		photo = em.find( Photo.class, ENTITY_ID_1 );
		assertThat( photo ).isNotNull();
		assertThat( photo.getContent() ).isNotNull();
		Blob content = photo.getContent();
		assertThat( content.length() ).isEqualTo( BLOB_CONTENT_1.length );
		assertThat( content.getBytes( 1, BLOB_CONTENT_1.length ) ).isEqualTo( BLOB_CONTENT_1 );
		em.getTransaction().commit();
	}

	@Test
	public void canReplaceBlobInEntity() throws SQLException {
		em.getTransaction().begin();
		Photo photo = em.find( Photo.class, ENTITY_ID_1 );
		assertThat( photo ).isNotNull();
		assertThat( photo.getContent() ).isNotNull();
		Blob content = photo.getContent();
		assertThat( content.length() ).isEqualTo( BLOB_CONTENT_1.length );
		assertThat( content.getBytes( 1, BLOB_CONTENT_1.length ) ).isEqualTo( BLOB_CONTENT_1 );

		Blob blob2 = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_2 );
		photo.setContent( blob2 );
		em.merge( photo );
		em.getTransaction().commit();

		em.getTransaction().begin();

		photo = em.find( Photo.class, ENTITY_ID_1 );
		assertThat( photo ).isNotNull();
		assertThat( photo.getContent() ).isNotNull();
		content = photo.getContent();
		assertThat( content.length() ).isEqualTo( BLOB_CONTENT_2.length );
		assertThat( content.getBytes( 1, BLOB_CONTENT_2.length ) ).isEqualTo( BLOB_CONTENT_2 );
		em.getTransaction().commit();

		GridFSBucket gridFSFilesBucket = GridFSBuckets.create( mongoDatabase, BUCKET_NAME );
		MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
		List<GridFSFile> files = new LinkedList<>(  );
		for ( ; cursor.hasNext(); ) {
			files.add( cursor.next() );
		}
		assertThat( files.size() ).isEqualTo( 1 );
	}

	@Test
	public void canRemoveEntityWithBlob() throws SQLException {

		em.getTransaction().begin();
		Photo photo = new Photo();
		photo.setId( ENTITY_ID_2 );
		photo.setDescription( "photo2" );
		Blob blob = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_1 );
		photo.setContent( blob );
		em.persist( photo );
		em.getTransaction().commit();

		em.getTransaction().begin();
		photo = em.find( Photo.class, ENTITY_ID_2 );
		assertThat( photo ).isNotNull();
		assertThat( photo.getContent() ).isNotNull();
		Blob content = photo.getContent();
		assertThat( content.length() ).isEqualTo( BLOB_CONTENT_1.length );
		assertThat( content.getBytes( 1, BLOB_CONTENT_1.length ) ).isEqualTo( BLOB_CONTENT_1 );
		em.remove( photo );
		em.getTransaction().commit();

		GridFSBucket gridFSFilesBucket = GridFSBuckets.create( mongoDatabase, BUCKET_NAME );
		MongoCursor<GridFSFile> cursor = gridFSFilesBucket.find().iterator();
		List<GridFSFile> files = new LinkedList<>(  );
		for ( ; cursor.hasNext(); ) {
			files.add( cursor.next() );
		}
		assertThat( files.size() ).isEqualTo( 1 );
	}


	private MongoDatabase getCurrentDB(EntityManager em) {
		Session session = em.unwrap( Session.class );
		OgmSessionFactoryImpl sessionFactory = (OgmSessionFactoryImpl) session.getSessionFactory();
		OgmDialect dialect = (OgmDialect) sessionFactory.getDialect();
		GridDialect gridDialect = dialect.getGridDialect();
		MongoDBDialect mongoDBDialect = null;
		if ( gridDialect instanceof MongoDBDialect ) {
			mongoDBDialect = (MongoDBDialect) gridDialect;
		}
		else if ( gridDialect instanceof GridDialectLogger ) {
			GridDialectLogger dialectLogger = (GridDialectLogger) gridDialect;
			BatchOperationsDelegator d = (BatchOperationsDelegator) dialectLogger.getGridDialect();
			mongoDBDialect = (MongoDBDialect) d.getGridDialect();
		}

		return mongoDBDialect.getCurrentDB();
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Photo.class };
	}

}

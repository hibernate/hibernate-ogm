/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.gridfs;

import java.io.ByteArrayOutputStream;
import java.sql.Blob;
import java.sql.SQLException;
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

import static org.fest.assertions.Assertions.assertThat;

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
	private static final String ENTITY_ID = "photo1";

	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.clear();
		em.close();
	}


	@Test
	public void test1SaveBlob() {
			em.getTransaction().begin();

			Photo photo = new Photo();
			photo.setId( ENTITY_ID );
			photo.setDescription( "photo1" );
			Blob blob = Hibernate.getLobCreator( em.unwrap( Session.class ) ).createBlob( BLOB_CONTENT_1 );
			photo.setContent( blob );
			em.persist( photo );
			em.getTransaction().commit();
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
	}

	@Test
	public void test2ReadBlob() throws SQLException {
		em.getTransaction().begin();
		try {
			Photo photo = em.find( Photo.class, ENTITY_ID );
			assertThat( photo ).isNotNull();
			assertThat( photo.getContent() ).isNotNull();
			Blob content = photo.getContent();
			assertThat( content.length() ).isEqualTo( BLOB_CONTENT_1.length );
			assertThat( content.getBytes( 0, BLOB_CONTENT_1.length  ) ).isEqualTo( BLOB_CONTENT_1 );
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		em.getTransaction().commit();
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

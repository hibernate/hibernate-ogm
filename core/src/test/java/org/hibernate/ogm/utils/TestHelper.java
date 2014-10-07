/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

import com.arjuna.ats.arjuna.coordinator.TxControl;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
public class TestHelper {

	private static final Log log = LoggerFactory.make();
	private static final TestableGridDialect helper = createStoreSpecificHelper();

	static {
		// set 5 hours timeout on transactions: enough for debug, but not too high in case of CI problems.
		TxControl.setDefaultTimeout( 60 * 60 * 2 );
	}

	private TestHelper() {
	}

	public static long getNumberOfEntities(EntityManager em) {
		return getNumberOfEntities( em.unwrap( Session.class ) );
	}

	private static TestableGridDialect createStoreSpecificHelper() {
		for ( GridDialectType gridType : GridDialectType.values() ) {
			Class<TestableGridDialect> classForName = gridType.loadTestableGridDialectClass();
			if ( classForName != null ) {
				try {
					TestableGridDialect attempt = classForName.newInstance();
					log.debugf( "Using TestGridDialect %s", classForName );
					return attempt;
				}
				catch ( Exception e ) {
					// but other errors are not expected:
					log.errorf( e, "Could not load TestGridDialect by name from %s", gridType );
				}
			}
		}
		return new org.hibernate.ogm.utils.HashMapTestHelper();
	}

	public static GridDialectType getCurrentDialectType() {
		return GridDialectType.valueFromHelperClass( helper.getClass() );
	}

	public static GridDialect getCurrentGridDialect(DatastoreProvider datastoreProvider) {
		return helper.getGridDialect( datastoreProvider );
	}

	public static long getNumberOfEntities( Session session) {
		return getNumberOfEntities( session.getSessionFactory() );
	}

	public static long getNumberOfEntities(SessionFactory sessionFactory) {
		return helper.getNumberOfEntities( sessionFactory );
	}

	public static Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return helper.extractEntityTuple( sessionFactory, key );
	}

	public static long getNumberOfAssociations(SessionFactory sessionFactory) {
		return helper.getNumberOfAssociations( sessionFactory );
	}

	/**
	 * Returns the number of associations of the given type.
	 * <p>
	 * Optional operation which only is supported for document datastores.
	 */
	public static long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		return helper.getNumberOfAssociations( sessionFactory, type );
	}

	public static boolean backendSupportsTransactions() {
		return helper.backendSupportsTransactions();
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Session session, Class<T> clazz, Serializable id) {
		return (T) session.get( clazz, id );
	}

	public static void dropSchemaAndDatabase(Session session) {
		if ( session != null ) {
			dropSchemaAndDatabase( session.getSessionFactory() );
		}
	}

	public static void dropSchemaAndDatabase(EntityManagerFactory emf) {
		dropSchemaAndDatabase( ( (HibernateEntityManagerFactory) emf ).getSessionFactory() );
	}

	public static void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		// if the factory is closed, we don't have access to the service registry
		if ( sessionFactory != null && !sessionFactory.isClosed() ) {
			try {
				helper.dropSchemaAndDatabase( sessionFactory );
			}
			catch ( Exception e ) {
				log.warn( "Exception while dropping schema and database in test", e );
			}
		}
	}

	public static Map<String, String> getEnvironmentProperties() {
		//TODO hibernate.properties is ignored due to HHH-8635, thus explicitly load its properties
		Map<String, String> properties = getHibernateProperties();
		Map<String, String> environmentProperties = helper.getEnvironmentProperties();

		if (environmentProperties != null ) {
			properties.putAll( environmentProperties );
		}

		return properties;
	}

	private static Map<String, String> getHibernateProperties() {
		InputStream hibernatePropertiesStream = null;
		Map<String, String> properties = new HashMap<String, String>();

		try {
			hibernatePropertiesStream = TestHelper.class.getResourceAsStream( "/hibernate.properties" );
			Properties hibernateProperties = new Properties();
			hibernateProperties.load( hibernatePropertiesStream );

			for ( Entry<Object, Object> property : hibernateProperties.entrySet() ) {
				properties.put( property.getKey().toString(), property.getValue().toString() );
			}

			return properties;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
		finally {
			closeQuietly( hibernatePropertiesStream );
		}
	}

	private static void closeQuietly(InputStream stream) {
		if ( stream != null ) {
			try {
				stream.close();
			}
			catch (IOException e) {
				//ignore
			}
		}
	}

	public static void checkCleanCache(SessionFactory sessionFactory) {
		assertThat( getNumberOfEntities( sessionFactory ) ).as( "Entity cache should be empty" ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).as( "Association cache should be empty" ).isEqualTo( 0 );
	}

	/**
	 * Provides a default {@link OgmConfiguration} for tests, using the given set of annotated entity types.
	 *
	 * @param entityTypes the entity types for which to build a configuration
	 * @return a default configuration based on the given types
	 */
	public static OgmConfiguration getDefaultTestConfiguration(Class<?>... entityTypes) {
		OgmConfiguration configuration = new OgmConfiguration();

		for ( Map.Entry<String, String> entry : TestHelper.getEnvironmentProperties().entrySet() ) {
			configuration.setProperty( entry.getKey(), entry.getValue() );
		}

		configuration.setProperty( Environment.HBM2DDL_AUTO, "none" );

		// volatile indexes for Hibernate Search (if used)
		configuration.setProperty( "hibernate.search.default.directory_provider", "ram" );
		// disable warnings about unspecified Lucene version
		configuration.setProperty( "hibernate.search.lucene_version", "LUCENE_35" );

		for ( Class<?> aClass : entityTypes ) {
			configuration.addAnnotatedClass( aClass );
		}

		return configuration;
	}

	/**
	 * Returns a {@link GlobalContext} for configuring the current datastore.
	 *
	 * @param configuration the target the configuration will be applied to
	 * @return a context object for configuring the current datastore.
	 */
	public static GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return helper.configureDatastore( configuration );
	}
}

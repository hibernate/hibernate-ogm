/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.boot.OgmSessionFactoryBuilder;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.ConfigurableImpl;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.GridDialects;
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
	private static final String TX_CONTROL_CLASS_NAME = "com.arjuna.ats.arjuna.coordinator.TxControl";
	private static final GridModule GRID_MODULE = determineGridModule();
	private static final TestableGridDialect helper = instantiate( GRID_MODULE.loadTestableGridDialectClass() );
	private static final GridDialectType GRID_DIALECT_TYPE = determineGridDialectType();

	static {
		Class<?> txControlClass = loadClass( TX_CONTROL_CLASS_NAME );
		if ( txControlClass != null ) {
			// set 2 hours timeout on transactions: enough for debug, but not too high in case of CI problems.
			try {
				Method timeoutMethod = txControlClass.getMethod( "setDefaultTimeout", int.class );
				timeoutMethod.invoke( null, 60 * 60 * 2 );
			}
			catch ( NoSuchMethodException e ) {
				log.error( "Found TxControl class, but unable to set timeout" );
			}
			catch ( IllegalAccessException e ) {
				log.error( "Found TxControl class, but unable to set timeout" );
			}
			catch ( InvocationTargetException e ) {
				log.error( "Found TxControl class, but unable to set timeout" );
			}
			TxControl.setDefaultTimeout( 60 * 60 * 2 );
		}
	}

	private TestHelper() {
	}

	private static GridModule determineGridModule() {
		for ( GridModule gridType : GridModule.values() ) {
			Class<TestableGridDialect> testDialectClass = gridType.loadTestableGridDialectClass();
			if ( testDialectClass != null ) {
				return gridType;
			}
		}

		return GridModule.HASHMAP;
	}

	private static GridDialectType determineGridDialectType() {
		Class<? extends GridDialect> gridDialectClass = getCurrentGridDialect();
		for ( GridDialectType gridDialectType : GridDialectType.values() ) {
			Class<TestableGridDialect> testDialectClass = gridDialectType.loadGridDialectClass();
			if ( testDialectClass != null && testDialectClass.isAssignableFrom( gridDialectClass ) ) {
				return gridDialectType;
			}
		}
		return GridDialectType.HASHMAP;
	}

	private static TestableGridDialect instantiate(Class<TestableGridDialect> testableGridDialectClass) {
		if ( testableGridDialectClass == null ) {
			return new HashMapTestHelper();
		}

		try {
			TestableGridDialect testableGridDialect = testableGridDialectClass.newInstance();
			log.debugf( "Using TestGridDialect %s", testableGridDialectClass );
			return testableGridDialect;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	public static long getNumberOfEntities(EntityManager em) {
		return getNumberOfEntities( em.unwrap( Session.class ) );
	}

	public static GridDialectType getCurrentDialectType() {
		return GRID_DIALECT_TYPE;
	}

	public static AvailableDatastoreProvider getCurrentDatastoreProvider() {
		return DatastoreProviderHolder.INSTANCE;
	}
	public static Class<? extends GridDialect> getCurrentGridDialect() {
		return GridDialectHolder.INSTANCE;
	}

	public static GridDialect getCurrentGridDialect(DatastoreProvider datastoreProvider) {
		return helper.getGridDialect( datastoreProvider );
	}

	public static <D extends DatastoreConfiguration<?>> Class<D> getCurrentDatastoreConfiguration() {
		@SuppressWarnings("unchecked") // relies on the fact that the caller assigns correctly; that's ok for this purpose
		Class<D> configurationType = (Class<D>) helper.getDatastoreConfigurationType();
		return configurationType;
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

	public static <T> T get(Session session, Class<T> clazz, Serializable id) {
		return session.get( clazz, id );
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

	public static void checkCleanCache(SessionFactory sessionFactory) {
		assertThat( getNumberOfEntities( sessionFactory ) ).as( "Entity cache should be empty" ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).as( "Association cache should be empty" ).isEqualTo( 0 );
	}

	public static Map<String, String> getDefaultTestSettings() {
		Map<String, String> settings = new HashMap<>();

		settings.put( OgmProperties.ENABLED, "true" );
		settings.put( Environment.HBM2DDL_AUTO, "none" );
		settings.put( "hibernate.search.default.directory_provider", "ram" );

		Map<String, String> environmentProperties = helper.getEnvironmentProperties();

		if ( environmentProperties != null ) {
			settings.putAll( environmentProperties );
		}

		return settings;
	}

	public static StandardServiceRegistry getDefaultTestStandardServiceRegistry(Map<String, Object> settings) {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();

		for ( Entry<String, String> setting : getDefaultTestSettings().entrySet() ) {
			registryBuilder.applySetting( setting.getKey(), setting.getValue() );
		}

		for ( Entry<String, Object> setting : settings.entrySet() ) {
			registryBuilder.applySetting( setting.getKey(), setting.getValue() );
		}

		return registryBuilder.build();
	}

	private static MetadataSources getMetadataSources(Class<?>... entityTypes) {
		MetadataSources sources = new MetadataSources();

		for (Class<?> entityType : entityTypes) {
			sources.addAnnotatedClass( entityType );
		}

		return sources;
	}
	private static Metadata getDefaultTestMetadata(Map<String, Object> settings, Class<?>... entityTypes) {
		StandardServiceRegistry serviceRegistry = getDefaultTestStandardServiceRegistry( settings );
		MetadataSources sources = getMetadataSources( entityTypes );

		return sources.getMetadataBuilder( serviceRegistry ).build();
	}

	public static OgmSessionFactory getDefaultTestSessionFactory(Class<?>... entityTypes) {
		return getDefaultTestSessionFactory( Collections.<String, Object>emptyMap(), entityTypes );
	}

	public static OgmSessionFactory getDefaultTestSessionFactory(Map<String, Object> settings, Class<?>... entityTypes) {
		return getDefaultTestMetadata(
				settings,
				entityTypes
			)
			.getSessionFactoryBuilder()
			.unwrap( OgmSessionFactoryBuilder.class )
			.build();
	}

	public static <D extends DatastoreConfiguration<G>, G extends GlobalContext<?, ?>> G configureOptionsFor(Map<String, Object> settings, Class<D> datastoreType) {
		ConfigurableImpl configurable = new ConfigurableImpl();
		settings.put( InternalProperties.OGM_OPTION_CONTEXT, configurable.getContext() );
		return configurable.configureOptionsFor( datastoreType );
	}

	@SuppressWarnings("unchecked")
	static <T> Class<T> loadClass(String className) {
		try {
			return (Class<T>) Class.forName( className, true, TestHelper.class.getClassLoader() );
		}
		catch ( ClassNotFoundException e ) {
			//ignore -- try using the class loader of context first
		}
		catch ( RuntimeException e ) {
			// ignore
		}
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if ( contextClassLoader != null ) {
				return (Class<T>) Class.forName( className, false, contextClassLoader );
			}
			else {
				return null;
			}
		}
		catch ( ClassNotFoundException e ) {
			return null;
		}
	}

	private static class GridDialectHolder {

		private static final Class<? extends GridDialect> INSTANCE = getGridDialect();

		private static Class<? extends GridDialect> getGridDialect() {
			Object gridDialect = getDefaultTestStandardServiceRegistry( Collections.<String, Object>emptyMap() )
					.getService( GridDialect.class );

			return GridDialects.getWrappedDialect( (GridDialect) gridDialect );
		}
	}

	private static class DatastoreProviderHolder {

		private static final AvailableDatastoreProvider INSTANCE = getDatastoreProvider();

		private static AvailableDatastoreProvider getDatastoreProvider() {
			Object datastoreProviderProperty = getDefaultTestStandardServiceRegistry( Collections.<String, Object>emptyMap() )
					.getService( ConfigurationService.class )
					.getSettings()
					.get( OgmProperties.DATASTORE_PROVIDER );

			if ( datastoreProviderProperty == null ) {
				return null;
			}

			String value = datastoreProviderProperty.toString();

			// try to resolve by short name
			AvailableDatastoreProvider byShortName = findByShortName( value );
			if ( byShortName != null ) {
				return byShortName;
			}

			// try to resolve by exact class name
			AvailableDatastoreProvider byClassName = findByExactClassName( value );
			if ( byClassName != null ) {
				return byClassName;
			}

			// try to resolve by assignable type (allows matching on subtypes of a data store provider)
			AvailableDatastoreProvider byAssignableType = findByAssignableType( value );
			if ( byAssignableType != null ) {
				return byAssignableType;
			}

			throw new IllegalStateException( "Could not determine datastore provider from value: " + datastoreProviderProperty );
		}

		private static AvailableDatastoreProvider findByShortName(String value) {
			if ( AvailableDatastoreProvider.isShortName( value ) ) {
				return AvailableDatastoreProvider.byShortName( value );
			}
			return null;
		}

		private static AvailableDatastoreProvider findByExactClassName(String value) {
			for ( AvailableDatastoreProvider provider : AvailableDatastoreProvider.values() ) {
				if ( provider.getDatastoreProviderClassName().equals( value ) ) {
					return provider;
				}
			}
			return null;
		}

		private static AvailableDatastoreProvider findByAssignableType(String value) {
			Class<?> configuredProviderClass = loadClass( value );
			if ( configuredProviderClass == null ) {
				return null;
			}

			for ( AvailableDatastoreProvider provider : AvailableDatastoreProvider.values() ) {
				Class<?> availableProviderClass = loadClass( provider.getDatastoreProviderClassName() );

				if ( availableProviderClass != null &&
						availableProviderClass.isAssignableFrom( configuredProviderClass ) ) {
					return provider;
				}
			}
			return null;
		}
	}
}

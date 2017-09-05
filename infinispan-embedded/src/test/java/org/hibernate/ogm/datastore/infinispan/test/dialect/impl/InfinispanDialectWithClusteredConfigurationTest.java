/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyTupleContext;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyTupleTypeContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanEmbeddedDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.navigation.impl.OptionsServiceImpl;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.service.impl.DefaultSchemaInitializationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test which makes sure that {@link InfinispanDialect} and {@link InfinispanEmbeddedDatastoreProvider} can operate
 * in clustered mode, in particular that objects can be serialized and de-serialized when being written into and read
 * from the data grid.
 * <p>
 *
 * @author Gunnar Morling
 */
public class InfinispanDialectWithClusteredConfigurationTest {

	private static InfinispanEmbeddedDatastoreProvider provider1;
	private static InfinispanEmbeddedDatastoreProvider provider2;
	private static InfinispanDialect dialect1;
	private static InfinispanDialect dialect2;

	@BeforeClass
	public static void setupProvidersAndDialects() throws Exception {
		SessionFactoryImplementor sessionFactory1 = getSessionFactory();
		SessionFactoryImplementor sessionFactory2 = getSessionFactory();
		provider1 = (InfinispanEmbeddedDatastoreProvider) sessionFactory1.getServiceRegistry().getService( DatastoreProvider.class );
		provider2 = (InfinispanEmbeddedDatastoreProvider) sessionFactory2.getServiceRegistry().getService( DatastoreProvider.class );
		dialect1 = new InfinispanDialect( provider1 );
		dialect2 = new InfinispanDialect( provider2 );
		provider1.getSchemaDefinerType().newInstance().initializeSchema( new DefaultSchemaInitializationContext( database(), sessionFactory1 ) );
		provider2.getSchemaDefinerType().newInstance().initializeSchema( new DefaultSchemaInitializationContext( database(), sessionFactory2 ) );
	}

	private static Database database() {
		Database database = mock( Database.class );
		Iterable<Namespace> namespaces = Collections.emptyList();
		when( database.getNamespaces() ).thenReturn( namespaces );
		return database;
	}

	@AfterClass
	public static void stopProvider() {
		if ( provider1 != null ) {
			provider1.stop();
		}
		if ( provider2 != null ) {
			provider2.stop();
		}
	}

	@Test
	public void shouldWriteAndReadTupleInClusteredMode() throws Exception {
		// given
		String[] columnNames = { "foo", "bar", "baz" };
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( "Foobar", columnNames );
		Object[] values = { 123, "Hello", 456L };

		EntityKey key = new EntityKey( keyMetadata, values );

		// when
		Tuple tuple = dialect1.createTuple( key, emptyTupleContext() );
		tuple.put( "foo", "bar" );
		dialect1.insertOrUpdateTuple( key, new TuplePointer( tuple ), emptyTupleContext() );

		// then
		Tuple readTuple = dialect2.getTuple( key, null );
		assertThat( readTuple.get( "foo" ) ).isEqualTo( "bar" );
	}

	@Test
	public void shoulReadAndWriteSequenceInClusteredMode() throws Exception {
		// given
		IdSourceKeyMetadata keyMetadata = DefaultIdSourceKeyMetadata.forTable( "Hibernate_Sequences", "sequence_name", "next_val" );
		IdSourceKey key = IdSourceKey.forTable( keyMetadata, "Foo_Sequence" );

		// when
		Number value = dialect1.nextValue( new NextValueRequest( key, 1, 1 ) );
		assertThat( value ).isEqualTo( 1L );

		// then
		value = dialect2.nextValue( new NextValueRequest( key, 1, 1 ) );
		assertThat( value ).isEqualTo( 2L );
	}

	@Test
	public void shouldWriteAndReadAssociationInClusteredMode() throws Exception {
		// given
		String[] columnNames = { "foo", "bar", "baz" };
		AssociationKeyMetadata keyMetadata = new DefaultAssociationKeyMetadata.Builder().table( "Foobar" ).columnNames( columnNames )
				.associatedEntityKeyMetadata( new DefaultAssociatedEntityKeyMetadata( null, null ) ).build();
		Object[] values = { 123, "Hello", 456L };

		AssociationKey key = new AssociationKey( keyMetadata, values, null );

		RowKey rowKey = new RowKey( columnNames, values );
		Tuple tuple = new Tuple();
		tuple.put( "zip", "zap" );

		// when
		Association association = dialect1.createAssociation( key, null );
		association.put( rowKey, tuple );
		dialect1.insertOrUpdateAssociation( key, association, null );

		// then
		Association readAssociation = dialect2.getAssociation( key, null );
		Tuple readKey = readAssociation.get( rowKey );
		assertThat( readKey ).isNotNull();
		assertThat( readKey.get( "zip" ) ).isEqualTo( "zap" );
	}

	@Test
	public void shouldApplyForEachTupleInClusteredMode() throws Exception {
		// given
		String[] columnNames = { "foo", "bar", "baz" };
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( "Foobar", columnNames );
		Object[] values = { 123, "Hello", 456L };

		EntityKey key = new EntityKey( keyMetadata, values );

		// when
		Tuple tuple = dialect1.createTuple( key, emptyTupleContext() );
		tuple.put( "foo", "bar" );
		dialect1.insertOrUpdateTuple( key, new TuplePointer( tuple ), emptyTupleContext() );

		// then
		MyConsumer consumer = new MyConsumer();
		dialect2.forEachTuple( consumer, emptyTupleTypeContext(), keyMetadata );
		assertThat( consumer.consumedTuple.get( "foo" ) ).isEqualTo( "bar" );
	}

	private final class MyConsumer implements ModelConsumer {

		private Tuple consumedTuple;

		@Override
		public void consume(TuplesSupplier supplier) {
			consumedTuple = supplier.get( null ).next();
		}
	}

	private static InfinispanEmbeddedDatastoreProvider createAndStartNewProvider(ServiceRegistryImplementor serviceRegistry) {
		Map<String, Object> configurationValues = new HashMap<String, Object>();
		configurationValues.put( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, "infinispan-dist.xml" );
		InfinispanEmbeddedDatastoreProvider provider = new InfinispanEmbeddedDatastoreProvider();

		provider.configure( configurationValues );
		provider.injectServices( serviceRegistry );
		provider.start();

		return provider;
	}

	private static ServiceRegistryImplementor getServiceRegistry() {
		ServiceRegistryImplementor serviceRegistry = mock( ServiceRegistryImplementor.class );

		JBossStandAloneJtaPlatform jtaPlatform = new JBossStandAloneJtaPlatform();
		jtaPlatform.injectServices( serviceRegistry );
		when( serviceRegistry.getService( JtaPlatform.class ) ).thenReturn( jtaPlatform );

		InfinispanEmbeddedDatastoreProvider provider = createAndStartNewProvider( serviceRegistry );
		when( serviceRegistry.getService( DatastoreProvider.class ) ).thenReturn( provider );

		when( serviceRegistry.getService( ClassLoaderService.class ) ).thenReturn( new ClassLoaderServiceImpl() );

		OptionsServiceImpl optionsService = new OptionsServiceImpl();
		optionsService.injectServices( serviceRegistry );
		optionsService.configure( Collections.emptyMap() );
		when( serviceRegistry.getService( OptionsService.class ) ).thenReturn( optionsService );

		return serviceRegistry;
	}

	private static SessionFactoryImplementor getSessionFactory() {
		SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );

		// metamodel
		MetamodelImplementor metamodel = mock( MetamodelImplementor.class );
		when( sessionFactory.getMetamodel() ).thenReturn( metamodel );

		// entity persister
		OgmEntityPersister foobarPersister = mock( OgmEntityPersister.class );
		when( foobarPersister.getEntityKeyMetadata() ).thenReturn( new DefaultEntityKeyMetadata( "Foobar", new String[] {} ) );
		when( foobarPersister.getPropertyNames() ).thenReturn( new String[] {} );

		// id generator
		PersistentNoSqlIdentifierGenerator generator = mock( PersistentNoSqlIdentifierGenerator.class );
		when( generator.getGeneratorKeyMetadata() ).thenReturn( DefaultIdSourceKeyMetadata.forTable( "Hibernate_Sequences", "sequence_name", "next_val" ) );
		when( foobarPersister.getIdentifierGenerator() ).thenReturn( generator );

		when( metamodel.entityPersisters() ).thenReturn( Collections.<String, EntityPersister>singletonMap( "Foobar", foobarPersister ) );

		// collection persister
		OgmCollectionPersister foobarCollectionPersister = mock( OgmCollectionPersister.class );
		when( foobarCollectionPersister.getAssociationKeyMetadata() ).thenReturn( new DefaultAssociationKeyMetadata.Builder().table( "Foobar" ).build() );
		when( metamodel.collectionPersisters() ).thenReturn(
				Collections.<String, CollectionPersister>singletonMap( "Foobar", foobarCollectionPersister ) );

		// service registry
		ServiceRegistryImplementor serviceRegistry = getServiceRegistry();
		when( sessionFactory.getServiceRegistry() ).thenReturn( serviceRegistry );

		return sessionFactory;
	}
}

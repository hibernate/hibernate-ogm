/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.IdGeneratorKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.utils.EmptyOptionsContext;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test which makes sure that {@link InfinispanDialect} and {@link InfinispanDatastoreProvider} can operate
 * in clustered mode, in particular that objects can be serialized and de-serialized when being written into and read
 * from the data grid.
 * <p>
 *
 * @author Gunnar Morling
 */
public class InfinispanDialectWithClusteredConfigurationTest {

	private static InfinispanDatastoreProvider provider1;
	private static InfinispanDatastoreProvider provider2;
	private InfinispanDialect dialect1;
	private InfinispanDialect dialect2;

	@BeforeClass
	public static void setupProviders() {
		provider1 = createAndStartNewProvider();
		provider2 = createAndStartNewProvider();
	}

	@Before
	public void setupDialects() {
		dialect1 = new InfinispanDialect( provider1 );
		dialect2 = new InfinispanDialect( provider2 );
	}

	@AfterClass
	public static void stopProvider() {
		provider1.stop();
		provider2.stop();
	}

	@Test
	public void shouldWriteAndReadTupleInClusteredMode() throws Exception {
		// given
		String[] columnNames = { "foo", "bar", "baz" };
		EntityKeyMetadata keyMetadata = new EntityKeyMetadata( "Foobar", columnNames );
		Object[] values = { 123, "Hello", 456L };

		EntityKey key = new EntityKey( keyMetadata, values );

		// when
		Tuple tuple = dialect1.createTuple( key, getEmptyTupleContext() );
		tuple.put( "foo", "bar" );
		dialect1.updateTuple( tuple, key, getEmptyTupleContext() );

		// then
		Tuple readTuple = dialect2.getTuple( key, null );
		assertThat( readTuple.get( "foo" ) ).isEqualTo( "bar" );
	}

	@Test
	public void shoulReadAndWriteSequenceInClusteredMode() throws Exception {
		// given
		IdGeneratorKeyMetadata keyMetadata = IdGeneratorKeyMetadata.forTable( "Hibernate_Sequences", "sequence_name", "next_val" );
		IdGeneratorKey key = IdGeneratorKey.forTable( keyMetadata, "Foo_Sequence" );

		// when
		BasicHolder value = new BasicHolder( Long.class );
		dialect1.nextValue( key, value, 1, 1 );
		assertThat( value.getActualLongValue() ).isEqualTo( 1L );

		// then
		value = new BasicHolder( Long.class );
		dialect2.nextValue( key, value, 1, 1 );
		assertThat( value.getActualLongValue() ).isEqualTo( 2L );
	}

	@Test
	public void shouldWriteAndReadAssociationInClusteredMode() throws Exception {
		// given
		String[] columnNames = { "foo", "bar", "baz" };
		AssociationKeyMetadata keyMetadata = new AssociationKeyMetadata( "Foobar", columnNames, null );
		Object[] values = { 123, "Hello", 456L };

		AssociationKey key = new AssociationKey( keyMetadata, values, null, null, null );

		RowKey rowKey = new RowKey( "QaxZup", columnNames, values );
		Tuple tuple = new Tuple();
		tuple.put( "zip", "zap" );

		// when
		Association association = dialect1.createAssociation( key, null );
		association.put( rowKey, tuple );
		dialect1.updateAssociation( association, key, null );

		// then
		Association readAssociation = dialect2.getAssociation( key, null );
		Tuple readKey = readAssociation.get( rowKey );
		assertThat( readKey ).isNotNull();
		assertThat( readKey.get( "zip" ) ).isEqualTo( "zap" );
	}

	private static InfinispanDatastoreProvider createAndStartNewProvider() {
		Map<String, Object> configurationValues = new HashMap<String, Object>();
		configurationValues.put( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, "infinispan-dist-duplicate-domains-allowed.xml" );
		ServiceRegistryImplementor serviceRegistry = getServiceRegistry();

		InfinispanDatastoreProvider provider = new InfinispanDatastoreProvider();
		provider.configure( configurationValues );
		provider.injectServices( serviceRegistry );
		provider.start();

		return provider;
	}

	private static ServiceRegistryImplementor getServiceRegistry() {
		ServiceRegistryImplementor serviceRegistry = mock( ServiceRegistryImplementor.class );
		JBossStandAloneJtaPlatform jtaPlatform = new JBossStandAloneJtaPlatform();
		jtaPlatform.injectServices( serviceRegistry );

		when( serviceRegistry.getService( ClassLoaderService.class ) ).thenReturn( new ClassLoaderServiceImpl() );
		when( serviceRegistry.getService( JtaPlatform.class ) ).thenReturn( jtaPlatform );

		return serviceRegistry;
	}

	private TupleContext getEmptyTupleContext() {
		return new TupleContext(
				Collections.<String>emptyList(),
				EmptyOptionsContext.INSTANCE
		);
	}
}

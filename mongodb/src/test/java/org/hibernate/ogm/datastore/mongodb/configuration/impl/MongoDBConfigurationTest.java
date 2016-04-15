/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.configuration.impl;

import com.mongodb.DBDecoder;
import com.mongodb.DBDecoderFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import javax.net.SocketFactory;

import com.mongodb.MongoClientOptions;
import com.mongodb.WriteConcern;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Hardy Ferentschik
 */
public class MongoDBConfigurationTest {
	private Map<String, String> configProperties;
	private ConfigurationPropertyReader propertyReader;
	private OptionsContext globalOptions;

	@Before
	public void setUp() {
		configProperties = new HashMap<>();
		configProperties.put( OgmProperties.DATABASE, "foo" );

		propertyReader = new ConfigurationPropertyReader( configProperties, new ClassLoaderServiceImpl() );
		globalOptions = OptionsContextImpl.forGlobal( OptionValueSources.getDefaultSources( propertyReader ) );
	}

	@Test
	public void testDefaultWriteConcernGetsApplied() {
		MongoClientOptions clientOptions = createMongoClientOptions();

		assertEquals( "Unexpected write concern", WriteConcern.ACKNOWLEDGED, clientOptions.getWriteConcern() );
	}

	@Test
	public void testDefaultServerSelectionTimeoutIsApplied() {
		MongoClientOptions clientOptions = createMongoClientOptions();

		assertEquals(
				"Unexpected server selection timeout",
				MongoClientOptions.builder().build().getServerSelectionTimeout(),
				clientOptions.getServerSelectionTimeout()
		);
	}

	@Test
	public void testCustomServerSelectionTimeoutIsApplied() {
		int expectedTimeout = 1000;
		configProperties.put( "hibernate.ogm.mongodb.driver.serverSelectionTimeout", String.valueOf( expectedTimeout ) );
		MongoClientOptions clientOptions = createMongoClientOptions();

		assertEquals(
				"Unexpected server selection timeout",
				expectedTimeout,
				clientOptions.getServerSelectionTimeout()
		);
	}

	@Test
	public void testSocketFactoryGetsApplied() {
		configProperties.put( "hibernate.ogm.mongodb.driver.socketFactory", TestPropRetriever.class.getName() );
		MongoClientOptions clientOptions = createMongoClientOptions();

		assertEquals(
				"Unexpected socket factory",
				TestPropRetriever.socketFactory(),
				clientOptions.getSocketFactory()
		);
	}

	@Test
	public void testDBDecoderFactoryGetsApplied() {
		configProperties.put( "hibernate.ogm.mongodb.driver.dbDecoderFactory", TestPropRetriever.class.getName() );
		MongoClientOptions clientOptions = createMongoClientOptions();

		assertEquals(
				"Unexpected DB decoder factory",
				TestPropRetriever.dbDecoderFactory(),
				clientOptions.getDbDecoderFactory()
		);
	}

	private MongoClientOptions createMongoClientOptions() {
		MongoDBConfiguration mongoConfig = new MongoDBConfiguration(
				propertyReader,
				globalOptions
		);
		assertNotNull( mongoConfig );

		MongoClientOptions clientOptions = mongoConfig.buildOptions();
		assertNotNull( clientOptions );
		return clientOptions;
	}

}

class TestPropRetriever {
	private static final DBDecoderFactory dbDecoderFactory = new FakeDBDecoderFactory();

	public static SocketFactory socketFactory() {
		return MySocketFactory.getDefault();
	}

	public static DBDecoderFactory dbDecoderFactory() {
		return dbDecoderFactory;
	}
}

class MySocketFactory extends SocketFactory {
	private static final SocketFactory factory = SocketFactory.getDefault();

	private static final MySocketFactory defaultFactory = new MySocketFactory();

	public static MySocketFactory getDefault() {
		return defaultFactory;
	}

	@Override
	public Socket createSocket() throws IOException {
		return factory.createSocket();
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return factory.createSocket( host, port );
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
		return factory.createSocket( host, port, localHost, localPort );
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return factory.createSocket( host, port );
	}

	@Override
	public Socket createSocket(InetAddress host, int port, InetAddress localHost, int localPort) throws IOException {
		return factory.createSocket( host, port, localHost, localPort );
	}
}

class FakeDBDecoderFactory implements DBDecoderFactory {
	@Override
	public DBDecoder create() {
		// This is not functional, but it doesn't matter in this case because we
		// don't need it to be. The existence of an object is enough.
		return null;
	}
}

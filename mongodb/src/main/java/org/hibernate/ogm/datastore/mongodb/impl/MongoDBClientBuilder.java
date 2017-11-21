/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * Builds the {@link MongoClient} given the {@link MongoDBConfiguration}.
 *
 * @author Davide D'Alto
 *
 */
public class MongoDBClientBuilder {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final MongoDBConfiguration config;

	public MongoDBClientBuilder(MongoDBConfiguration config) {
		this.config = config;
	}

	public MongoClient build() {
		MongoClientOptions clientOptions = config.buildOptions();
		List<MongoCredential> credentials = config.buildCredentials();
		log.connectingToMongo( config.getHosts().toString(), clientOptions.getConnectTimeout() );
		try {
			List<ServerAddress> serverAddresses = new ArrayList<>( config.getHosts().size() );
			for ( Hosts.HostAndPort hostAndPort : config.getHosts() ) {
				serverAddresses.add( new ServerAddress( hostAndPort.getHost(), hostAndPort.getPort() ) );
			}
			return credentials == null
					? new MongoClient( serverAddresses, clientOptions )
					: new MongoClient( serverAddresses, credentials, clientOptions );
		}
		catch (RuntimeException e) {
			throw log.unableToInitializeMongoDB( e );
		}
	}
}

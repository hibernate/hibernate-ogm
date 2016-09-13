/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.hibernate.ogm.datastore.infinispanremote.configuration.impl.InfinispanRemoteConfiguration;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

public class HotRodClientBuilder {

	private static final Log log = LoggerFactory.getLogger();

	private InfinispanRemoteConfiguration config;

	private OgmProtoStreamMarshaller marshaller;

	private HotRodClientBuilder() {
		//not to be created directly
	}

	public static HotRodClientBuilder builder() {
		return new HotRodClientBuilder();
	}

	public HotRodClientBuilder withConfiguration(InfinispanRemoteConfiguration config, OgmProtoStreamMarshaller marshaller) {
		this.config = config;
		this.marshaller = marshaller;
		return this;
	}

	public RemoteCacheManager build() {
		return new RemoteCacheManager(
				new ConfigurationBuilder()
					.classLoader( HotRodClientBuilder.class.getClassLoader() )
					.withProperties( getHotRodConfigurationProperties() )
					.marshaller( marshaller )
					.build() );
	}

	private Properties getHotRodConfigurationProperties() {
		if ( config != null ) {
			URL configurationResourceUrl = config.getConfigurationResourceUrl();
			if ( configurationResourceUrl == null ) {
				throw log.hotrodClientConfigurationMissing();
			}
			Properties p = new Properties();
			try ( InputStream openStream = configurationResourceUrl.openStream() ) {
				p.load( openStream );
			}
			catch (IOException e) {
				throw log.failedLoadingHotRodConfigurationProperties( e );
			}
			return p;
		}
		else {
			throw log.hotrodClientConfigurationMissing();
		}
	}

}

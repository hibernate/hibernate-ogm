/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.ogm.datastore.infinispanremote.configuration.impl.InfinispanRemoteConfiguration;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.Marshaller;

public class HotRodClientBuilder {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private InfinispanRemoteConfiguration config;

	private Marshaller marshaller;

	private HotRodClientBuilder() {
		//not to be created directly
	}

	public static HotRodClientBuilder builder() {
		return new HotRodClientBuilder();
	}

	public HotRodClientBuilder withConfiguration(InfinispanRemoteConfiguration config, Marshaller marshaller) {
		this.config = config;
		this.marshaller = marshaller;
		return this;
	}

	public RemoteCacheManager build() {
		return new RemoteCacheManager(
				new ConfigurationBuilder()
					.classLoader( HotRodClientBuilder.class.getClassLoader() )
					.withProperties( config.getClientProperties() )
					.marshaller( marshaller )
					.build() );
	}
}

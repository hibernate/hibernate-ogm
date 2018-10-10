/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import javax.transaction.TransactionManager;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispanremote.configuration.impl.InfinispanRemoteConfiguration;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.tx.lookup.TransactionManagerLookup;

public class HotRodClientBuilder {

	private InfinispanRemoteConfiguration config;

	private Marshaller marshaller;

	private JtaPlatform platform;

	private TransactionMode transactionMode;

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

	public HotRodClientBuilder withTransactionMode(TransactionMode transactionMode, JtaPlatform platform) {
		this.platform = platform;
		this.transactionMode = transactionMode;
		return this;
	}

	public RemoteCacheManager build() {
		ConfigurationBuilder builder = new ConfigurationBuilder()
				.classLoader( HotRodClientBuilder.class.getClassLoader() )
				.withProperties( config.getClientProperties() )
				.marshaller( this.marshaller );

		if ( platform != null && !TransactionMode.NONE.equals( transactionMode ) ) {
			builder.transaction().transactionMode( transactionMode );
			builder.transaction().transactionManagerLookup( new TransactionManagerLookupDelegator( platform ) );
		}

		return new RemoteCacheManager( builder.build() );
	}

	public static class TransactionManagerLookupDelegator implements TransactionManagerLookup {

		private final JtaPlatform platform;

		public TransactionManagerLookupDelegator(JtaPlatform platform) {
			this.platform = platform;
		}

		@Override
		public TransactionManager getTransactionManager() throws Exception {
			return ( platform != null ) ?
					platform.retrieveTransactionManager() : null;
		}
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.util.configurationreader.spi.ShortNameResolver;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Loads the appropriate {@link DatastoreProvider}. Driven by the {@link OgmProperties#DATASTORE_PROVIDER} property.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public final class DatastoreProviderInitiator implements StandardServiceInitiator<DatastoreProvider> {

	public static final DatastoreProviderInitiator INSTANCE = new DatastoreProviderInitiator();

	private static final Log log = LoggerFactory.make();
	private static final String DEFAULT_DATASTORE_PROVIDER = "infinispan";

	@Override
	public Class<DatastoreProvider> getServiceInitiated() {
		return DatastoreProvider.class;
	}

	@Override
	public DatastoreProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, registry.getService( ClassLoaderService.class ) );

		DatastoreProvider datastoreProvider = propertyReader.property( OgmProperties.DATASTORE_PROVIDER, DatastoreProvider.class )
				.instantiate()
				.withDefaultImplementation( DEFAULT_DATASTORE_PROVIDER )
				.withShortNameResolver( DatastoreProviderShortNameResolver.INSTANCE )
				.getValue();

		log.useDatastoreProvider( datastoreProvider.getClass().getName() );
		return datastoreProvider;
	}

	// TODO This only public to support the hack in OgmJtaPlatformInitiator#isNeo4j(); Can be made private once that
	// has been removed
	public static class DatastoreProviderShortNameResolver implements ShortNameResolver {

		private static final DatastoreProviderShortNameResolver INSTANCE = new DatastoreProviderShortNameResolver();

		@Override
		public boolean isShortName(String name) {
			return AvailableDatastoreProvider.isShortName( name );
		}

		@Override
		public String resolve(String shortName) {
			return AvailableDatastoreProvider.byShortName( shortName ).getDatastoreProviderClassName();
		}
	}
}

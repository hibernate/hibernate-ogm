/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.impl;

import java.util.Arrays;
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

		log.useDatastoreProvider( datastoreProvider.getClass() );
		return datastoreProvider;
	}

	private static class DatastoreProviderShortNameResolver implements ShortNameResolver {

		private static final DatastoreProviderShortNameResolver INSTANCE = new DatastoreProviderShortNameResolver();

		@Override
		public boolean isShortName(String name) {
			boolean isShortName = AvailableDatastoreProvider.isShortName( name );

			if ( !isShortName ) {
				// There is the legitimate case of the provider FQN name given; As we encourage the usage of the short
				// names though, chances are much higher that a misspelled short name has been given; Let's thus raise a
				// warning, as long as the provided name does not look like a FQN (i.e. have two dots)
				if ( name != null && name.indexOf( '.' ) == name.lastIndexOf( '.' ) ) {
					// we know that there is at least two dots so the name looks like com.acme.SomeClass
					// we check for null because we are good people
					String validProviderNames = Arrays.toString( AvailableDatastoreProvider.values() );
					log.noValidDatastoreProviderShortName( name, validProviderNames.substring( 1, validProviderNames.length() - 1 ) );
				}
			}

			return isShortName;
		}

		@Override
		public String resolve(String shortName) {
			return AvailableDatastoreProvider.byShortName( shortName ).getDatastoreProviderClassName();
		}
	}
}

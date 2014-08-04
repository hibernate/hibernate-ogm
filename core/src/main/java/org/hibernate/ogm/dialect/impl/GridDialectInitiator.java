/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.BatchableGridDialect;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.GridDialectLogger;
import org.hibernate.ogm.dialect.spi.QueryableGridDialect;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
import org.hibernate.ogm.util.configurationreader.impl.Instantiator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link GridDialect} service, based on the configuration via {@link OgmProperties#GRID_DIALECT}, using
 * the implementation returned by {@link DatastoreProvider#getDefaultDialect()} as fallback.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class GridDialectInitiator implements StandardServiceInitiator<GridDialect> {

	public static final GridDialectInitiator INSTANCE = new GridDialectInitiator();

	private static final Log log = LoggerFactory.make();

	private GridDialectInitiator() {
	}

	@Override
	public Class<GridDialect> getServiceInitiated() {
		return GridDialect.class;
	}

	@Override
	public GridDialect initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		DatastoreProvider datastore = registry.getService( DatastoreProvider.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, registry.getService( ClassLoaderService.class ) );

		return propertyReader.property( OgmProperties.GRID_DIALECT, GridDialect.class )
				.instantiate()
				.withDefaultImplementation( registry.getService( DatastoreProvider.class ).getDefaultDialect() )
				.withInstantiator( new GridDialectInstantiator( datastore, registry ) )
				.getValue();
	}

	private static class GridDialectInstantiator implements Instantiator<GridDialect> {

		private final DatastoreProvider datastore;
		private final ServiceRegistryImplementor registry;

		public GridDialectInstantiator(DatastoreProvider datastore, ServiceRegistryImplementor registry) {
			this.datastore = datastore;
			this.registry = registry;
		}

		@Override
		public GridDialect newInstance(Class<? extends GridDialect> clazz) {
			try {
				// FIXME not sure I like this constructor business. Argue with Sanne
				// to me that's blocking the doors for future enhancements (ie injecting more things)
				// an alternative is to pass the ServiceRegistry verbatim but I'm not sure that's enough either
				Constructor<?> injector = null;
				for ( Constructor<?> constructor : clazz.getConstructors() ) {
					Class<?>[] parameterTypes = constructor.getParameterTypes();
					if ( parameterTypes.length == 1 && DatastoreProvider.class.isAssignableFrom( parameterTypes[0] ) ) {
						injector = constructor;
						break;
					}
				}
				if ( injector == null ) {
					log.gridDialectHasNoProperConstructor( clazz );
				}
				GridDialect gridDialect = (GridDialect) injector.newInstance( datastore );
				boolean supportsQueries = gridDialect instanceof QueryableGridDialect;

				if ( gridDialect instanceof ServiceRegistryAwareService ) {
					( (ServiceRegistryAwareService) gridDialect ).injectServices( registry );
				}

				if ( gridDialect instanceof BatchableGridDialect ) {
					BatchableGridDialect batchable = (BatchableGridDialect) gridDialect;
					gridDialect = supportsQueries ? new QueryableBatchOperationsDelegator( batchable ) : new BatchOperationsDelegator( batchable );
				}

				log.useGridDialect( gridDialect.getClass().getName() );
				if ( GridDialectLogger.activationNeeded() ) {
					gridDialect = supportsQueries ? new QueryableGridDialectLogger( (QueryableGridDialect) gridDialect ) : new GridDialectLogger( gridDialect );
					log.info( "Grid dialect logs are active" );
				}
				else {
					log.info( "Grid dialect logs are disabled" );
				}
				return gridDialect;
			}
			catch ( Exception e ) {
				throw log.cannotInstantiateGridDialect( clazz, e );
			}
		}
	}
}

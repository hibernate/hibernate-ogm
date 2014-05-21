/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.lang.reflect.Constructor;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.BatchableGridDialect;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.GridDialectLogger;
import org.hibernate.ogm.service.impl.AutoFlushBatchManagerEventListener;
import org.hibernate.ogm.service.impl.FlushBatchManagerEventListener;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
import org.hibernate.ogm.util.configurationreader.impl.Instantiator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Contributes the {@link GridDialect} service, based on the configuration via {@link OgmProperties#GRID_DIALECT}, using
 * the implementation returned by {@link DatastoreProvider#getDefaultDialect()} as fallback.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Gunnar Morling
 */
public class GridDialectInitiator implements SessionFactoryServiceInitiator<GridDialect> {

	public static final SessionFactoryServiceInitiator<GridDialect> INSTANCE = new GridDialectInitiator();

	private static final Log log = LoggerFactory.make();

	@Override
	public Class<GridDialect> getServiceInitiated() {
		return GridDialect.class;
	}

	@Override
	public GridDialect initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		DatastoreProvider datastore = registry.getService( DatastoreProvider.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configuration, registry.getService( ClassLoaderService.class ) );

		return propertyReader.property( OgmProperties.GRID_DIALECT, GridDialect.class )
				.instantiate()
				.withDefaultImplementation( registry.getService( DatastoreProvider.class ).getDefaultDialect() )
				.withInstantiator( new GridDialectInstantiator( datastore, registry ) )
				.getValue();
	}

	@Override
	public GridDialect initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		throw new UnsupportedOperationException( "Cannot create " + GridDialect.class.getName() + " service using metadata" );
	}

	private static class GridDialectInstantiator implements Instantiator<GridDialect> {

		private final DatastoreProvider datastore;
		private final ServiceRegistryImplementor registry;
		private final EventListenerRegistry eventListenerRegistry;

		public GridDialectInstantiator(DatastoreProvider datastore, ServiceRegistryImplementor registry) {
			this.datastore = datastore;
			this.registry = registry;
			this.eventListenerRegistry = registry.getService( EventListenerRegistry.class );
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

				if ( gridDialect instanceof ServiceRegistryAwareService ) {
					( (ServiceRegistryAwareService) gridDialect ).injectServices( registry );
				}

				if ( gridDialect instanceof BatchableGridDialect ) {
					BatchOperationsDelegator delegator = new BatchOperationsDelegator( (BatchableGridDialect) gridDialect );
					addListeners( delegator );
					gridDialect = delegator;
				}

				log.useGridDialect( gridDialect.getClass().getName() );
				if ( GridDialectLogger.activationNeeded() ) {
					gridDialect = new GridDialectLogger( gridDialect );
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

		private void addListeners(BatchOperationsDelegator gridDialect) {
			eventListenerRegistry.addDuplicationStrategy( new FlushBatchManagerEventListener.FlushDuplicationStrategy() );
			eventListenerRegistry.addDuplicationStrategy( new AutoFlushBatchManagerEventListener.AutoFlushDuplicationStrategy() );
			eventListenerRegistry.getEventListenerGroup( EventType.FLUSH ).appendListener( new FlushBatchManagerEventListener( gridDialect ) );
			eventListenerRegistry.getEventListenerGroup( EventType.AUTO_FLUSH ).appendListener( new AutoFlushBatchManagerEventListener( gridDialect ) );
		}

	}
}

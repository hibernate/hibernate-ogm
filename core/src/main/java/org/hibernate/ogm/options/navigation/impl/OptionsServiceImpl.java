/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Provides read access to option contexts maintained at the session factory and session level.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OptionsServiceImpl implements OptionsService, Configurable, ServiceRegistryAwareService {

	private OptionsServiceContext sessionFactoryOptions;
	private ServiceRegistryImplementor registry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.registry = serviceRegistry;
	}

	@Override
	public void configure(Map configurationValues) {
		ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, classLoaderService );

		sessionFactoryOptions = new OptionsServiceContextImpl( OptionValueSources.getDefaultSources( propertyReader ) );
	}

	@Override
	public OptionsServiceContext context() {
		return sessionFactoryOptions;
	}

	@Override
	public OptionsServiceContext context(SessionImplementor session) {
		throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
	}

	private static class OptionsServiceContextImpl implements OptionsServiceContext {

		private final List<OptionValueSource> sources;

		private final OptionsContext globalOptions;
		private final ConcurrentMap<Class<?>, OptionsContext> entityContexts;
		private final ConcurrentMap<PropertyKey, OptionsContext> propertyContexts;

		public OptionsServiceContextImpl(List<OptionValueSource> sources) {
			this.sources = sources;

			globalOptions = OptionsContextImpl.forGlobal( sources );
			entityContexts = new ConcurrentHashMap<Class<?>, OptionsContext>();
			propertyContexts = new ConcurrentHashMap<PropertyKey, OptionsContext>();
		}

		@Override
		public OptionsContext getGlobalOptions() {
			return globalOptions;
		}

		@Override
		public OptionsContext getEntityOptions(Class<?> entityType) {
			OptionsContext entityOptions = entityContexts.get( entityType );

			if ( entityOptions == null ) {
				entityOptions = getAndCacheEntityOptions( entityType );
			}

			return entityOptions;
		}

		@Override
		public OptionsContext getPropertyOptions(Class<?> entityType, String propertyName) {
			PropertyKey key = new PropertyKey( entityType, propertyName );
			OptionsContext propertyOptions = propertyContexts.get( key );

			if ( propertyOptions == null ) {
				propertyOptions = getAndCachePropertyOptions( key );
			}

			return propertyOptions;
		}

		private OptionsContext getAndCacheEntityOptions(Class<?> entityType) {
			OptionsContext entityOptions = OptionsContextImpl.forEntity( sources, entityType );

			OptionsContext cachedOptions = entityContexts.putIfAbsent( entityType, entityOptions );
			if ( cachedOptions != null ) {
				entityOptions = cachedOptions;
			}

			return entityOptions;
		}

		private OptionsContext getAndCachePropertyOptions(PropertyKey key) {
			OptionsContext propertyOptions = OptionsContextImpl.forProperty( sources, key.getEntity(), key.getProperty() );

			OptionsContext cachedOptions = propertyContexts.putIfAbsent( key, propertyOptions );
			if ( cachedOptions != null ) {
				propertyOptions = cachedOptions;
			}

			return propertyOptions;
		}
	}
}

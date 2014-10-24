/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import org.hibernate.ogm.options.navigation.EntityContext;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.PropertyContext;
import org.hibernate.ogm.options.navigation.spi.BaseContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.ReflectionHelper;

/**
 * Keeps track of the entities and properties configured using the fluent configuration API. There is one instance of
 * this context per invocation of this API (beginning with the creation of a {@link GlobalContext}). This instance is
 * passed between the individual context types created in the course of using the fluent API. The book-keeping of
 * configured options is delegated to {@link AppendableConfigurationContext}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class ConfigurationContextImpl implements ConfigurationContext {

	private static final Log log = LoggerFactory.make();

	/**
	 * Contains all options configured via this and other configuration contexts.
	 */
	private final AppendableConfigurationContext allOptions;

	private Class<?> currentEntityType;
	private String currentPropertyName;

	public ConfigurationContextImpl(AppendableConfigurationContext appendableContext) {
		this.allOptions = appendableContext;
	}

	@Override
	public <V> void addGlobalOption(Option<?, V> option, V value) {
		allOptions.addGlobalOption( option, value );
	}

	@Override
	public <V> void addEntityOption(Option<?, V> option, V value) {
		allOptions.addEntityOption( currentEntityType, option, value );
	}

	@Override
	public <V> void addPropertyOption(Option<?, V> option, V value) {
		allOptions.addPropertyOption( currentEntityType, currentPropertyName, option, value );
	}

	public void configureEntity(Class<?> entityType) {
		this.currentEntityType = entityType;
	}

	public void configureProperty(String propertyName, ElementType elementType) {
		if ( elementType != ElementType.FIELD && elementType != ElementType.METHOD ) {
			throw log.getUnsupportedElementTypeException( elementType );
		}

		if ( !ReflectionHelper.propertyExists( currentEntityType, propertyName, elementType ) ) {
			throw log.getPropertyDoesNotExistException( currentEntityType.getName(), propertyName, elementType );
		}

		this.currentPropertyName = propertyName;
	}

	/**
	 * Creates a new {@link GlobalContext} object based on the given context implementation types. All implementation
	 * types must declare a public or protected constructor with a single parameter, accepting {@link ConfigurationContext}.
	 * <p>
	 * Each context implementation type must provide an implementation of the method(s) declared on the particular
	 * provider-specific context interface. All methods declared on context super interfaces - {@code entity()} and
	 * {@code property()} - are implemented following the dynamic proxy pattern, the implementation types therefore can
	 * be declared abstract, avoiding the need to implement these methods themselves.
	 * <p>
	 * By convention, the implementation types should directly or indirectly extend {@link BaseContext}.
	 *
	 * @param globalContextImplType the provider-specific global context implementation type
	 * @param entityContextImplType the provider-specific entity context implementation type
	 * @param propertyContextImplType the provider-specific property context implementation type
	 * @return a new {@link GlobalContext} object based on the given context implementation types
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <G extends GlobalContext<?, ?>> G createGlobalContext(Class<? extends G> globalContextImplType,
			final Class<? extends EntityContext<?, ?>> entityContextImplType, Class<? extends PropertyContext<?, ?>> propertyContextImplType) {

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setSuperclass( globalContextImplType );
		proxyFactory.setFilter( new EntityMethodFilter() );

		try {
			return (G) proxyFactory.create(
					new Class<?>[] { ConfigurationContext.class },
					new Object[] { this },
					new EntityOrPropertyMethodHandler( entityContextImplType, propertyContextImplType ) );
		}
		catch (Exception e) {
			throw log.cannotCreateGlobalContextProxy( globalContextImplType, e);
		}
	}

	@SuppressWarnings("unchecked")
	private <E extends EntityContext<?, ?>> E createEntityMappingContext(Class<? extends E> entityContextImplType,
			Class<? extends PropertyContext<?, ?>> propertyContextImplType) {

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setSuperclass( entityContextImplType );
		proxyFactory.setFilter( new EntityOrPropertyMethodFilter() );

		try {
			return (E) proxyFactory.create(
					new Class<?>[] { ConfigurationContext.class },
					new Object[] { this },
					new EntityOrPropertyMethodHandler( entityContextImplType, propertyContextImplType ) );
		}
		catch (Exception e) {
			throw log.cannotCreateEntityContextProxy( entityContextImplType, e);
		}
	}

	@SuppressWarnings("unchecked")
	private <P extends PropertyContext<?, ?>> P createPropertyMappingContext(Class<? extends EntityContext<?, ?>> entityContextImplType,
			Class<? extends P> propertyContextImplType) {

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setSuperclass( propertyContextImplType );
		proxyFactory.setFilter( new EntityOrPropertyMethodFilter() );

		try {
			return (P) proxyFactory.create(
					new Class<?>[] { ConfigurationContext.class },
					new Object[] { this },
					new EntityOrPropertyMethodHandler( entityContextImplType, propertyContextImplType ) );
		}
		catch (Exception e) {
			throw log.cannotCreateEntityContextProxy( propertyContextImplType, e);
		}
	}

	private final class EntityOrPropertyMethodHandler implements MethodHandler {

		private final Class<? extends EntityContext<?, ?>> entityContextImplType;
		private final Class<? extends PropertyContext<?, ?>> propertyContextImplType;

		private EntityOrPropertyMethodHandler(Class<? extends EntityContext<?, ?>> entityContextImplType,
				Class<? extends PropertyContext<?, ?>> propertyContextImplType) {
			this.entityContextImplType = entityContextImplType;
			this.propertyContextImplType = propertyContextImplType;
		}

		@Override
		public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
			if ( thisMethod.getName().equals( "entity" ) ) {
				configureEntity( (Class<?>) args[0] );
				return createEntityMappingContext( entityContextImplType, propertyContextImplType );
			}
			else {
				configureProperty( (String) args[0], (ElementType) args[1] );
				return createPropertyMappingContext( entityContextImplType, propertyContextImplType );
			}
		}
	}

	private final class EntityMethodFilter implements MethodFilter {

		@Override
		public boolean isHandled(Method m) {
			return m.getName().equals( "entity" ) && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == Class.class;
		}
	}

	private final class EntityOrPropertyMethodFilter implements MethodFilter {

		@Override
		public boolean isHandled(Method m) {
			return ( m.getName().equals( "entity" ) && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == Class.class )
					|| ( m.getName().equals( "property" ) && m.getParameterTypes().length == 2 && m.getParameterTypes()[0] == String.class && m
							.getParameterTypes()[1] == ElementType.class );
		}
	}
}

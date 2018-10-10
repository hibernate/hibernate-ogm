/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState.resolveClassLoadingStrategy;

import java.lang.annotation.ElementType;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.ogm.options.navigation.EntityContext;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.PropertyContext;
import org.hibernate.ogm.options.navigation.spi.BaseContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.ReflectionHelper;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Keeps track of the entities and properties configured using the fluent configuration API. There is one instance of
 * this context per invocation of this API (beginning with the creation of a {@link GlobalContext}). This instance is
 * passed between the individual context types created in the course of using the fluent API. The book-keeping of
 * configured options is delegated to {@link AppendableConfigurationContext}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 * @author Fabio Massimo Ercoli
 */
public class ConfigurationContextImpl implements ConfigurationContext {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final String ENTITY_METHOD_NAME = "entity";
	private static final String PROPERTY_METHOD_NAME = "property";

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
	 *
	 * @return a new {@link GlobalContext} object based on the given context implementation types
	 */
	@Override
	public <G extends GlobalContext<?, ?>> G createGlobalContext(Class<? extends G> globalContextImplType,
			final Class<? extends EntityContext<?, ?>> entityContextImplType, Class<? extends PropertyContext<?, ?>> propertyContextImplType) {

		// ByteBuddyState#resolveClassLoadingStrategy static method is an Hibernate ORM internal,
		// please remove its use here as soon the issue HHH-13014 has been closed.
		// url https://hibernate.atlassian.net/browse/HHH-13014.
		Class<? extends G> globalContextType = new ByteBuddy()
				.subclass( globalContextImplType )
				.method( filterEntityMethod() )
				.intercept( to( new EntityOrPropertyMethodInterceptor( entityContextImplType, propertyContextImplType ) ) )
				.make().load( globalContextImplType.getClassLoader(), resolveClassLoadingStrategy( globalContextImplType ) ).getLoaded();

		try {
			return globalContextType.getConstructor( ConfigurationContext.class ).newInstance( this );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw log.cannotCreateGlobalContextProxy( globalContextImplType, e );
		}
	}

	private <E extends EntityContext<?, ?>> E createEntityMappingContext(Class<? extends E> entityContextImplType,
			Class<? extends PropertyContext<?, ?>> propertyContextImplType) {

		// ByteBuddyState#resolveClassLoadingStrategy static method is an Hibernate ORM internal,
		// please remove its use here as soon the issue HHH-13014 has been closed.
		// url https://hibernate.atlassian.net/browse/HHH-13014.
		Class<? extends E> entityContextType = new ByteBuddy()
				.subclass( entityContextImplType )
				.method( filterEntityMethod().or( filterPropertyMethod() ) )
				.intercept( to( new EntityOrPropertyMethodInterceptor( entityContextImplType, propertyContextImplType ) ) )
				.make().load( entityContextImplType.getClassLoader(), resolveClassLoadingStrategy( entityContextImplType ) ).getLoaded();

		try {
			return entityContextType.getConstructor( ConfigurationContext.class ).newInstance( this );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw log.cannotCreateEntityContextProxy( entityContextType, e );
		}
	}

	private <P extends PropertyContext<?, ?>> P createPropertyMappingContext(Class<? extends EntityContext<?, ?>> entityContextImplType,
			Class<? extends P> propertyContextImplType) {

		// ByteBuddyState#resolveClassLoadingStrategy static method is an Hibernate ORM internal,
		// please remove its use here as soon the issue HHH-13014 has been closed.
		// url https://hibernate.atlassian.net/browse/HHH-13014.
		Class<? extends P> propertyContextType = new ByteBuddy()
				.subclass( propertyContextImplType )
				.method( filterEntityMethod().or( filterPropertyMethod() ) )
				.intercept( to( new EntityOrPropertyMethodInterceptor( entityContextImplType, propertyContextImplType ) ) )
				.make().load( propertyContextImplType.getClassLoader(), resolveClassLoadingStrategy( entityContextImplType ) ).getLoaded();

		try {
			return propertyContextType.getConstructor( ConfigurationContext.class ).newInstance( this );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw log.cannotCreatePropertyContextProxy( propertyContextType, e );
		}
	}

	private ElementMatcher.Junction<MethodDescription> filterEntityMethod() {
		return named( ENTITY_METHOD_NAME ).and( takesArguments( Class.class ) );
	}

	private ElementMatcher.Junction<MethodDescription> filterPropertyMethod() {
		return named( PROPERTY_METHOD_NAME ).and( takesArguments( String.class, ElementType.class ) );
	}

	public final class EntityOrPropertyMethodInterceptor {

		private final Class<? extends EntityContext<?, ?>> entityContextImplType;
		private final Class<? extends PropertyContext<?, ?>> propertyContextImplType;

		public EntityOrPropertyMethodInterceptor(Class<? extends EntityContext<?, ?>> entityContextImplType,
				Class<? extends PropertyContext<?, ?>> propertyContextImplType) {
			this.entityContextImplType = entityContextImplType;
			this.propertyContextImplType = propertyContextImplType;
		}

		@RuntimeType
		public EntityContext entity(Class<?> type) {
			configureEntity( type );
			return createEntityMappingContext( entityContextImplType, propertyContextImplType );
		}

		@RuntimeType
		public PropertyContext property(String propertyName, ElementType target) {
			configureProperty( propertyName, target );
			return createPropertyMappingContext( entityContextImplType, propertyContextImplType );
		}
	}
}

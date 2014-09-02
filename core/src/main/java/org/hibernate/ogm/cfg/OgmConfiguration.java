/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.impl.ConfigurableImpl;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.cfg.impl.OgmNamingStrategy;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.jpa.impl.OgmMutableIdentifierGeneratorFactory;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.query.impl.OgmQueryTranslatorFactory;
import org.hibernate.type.Type;

/**
 * An instance of {@link OgmConfiguration} allows the application
 * to specify properties and mapping documents to be used when
 * creating an {@link OgmSessionFactory}.
 *
 * @author Davide D'Alto
 */
public class OgmConfiguration extends Configuration implements Configurable {

	private final MutableIdentifierGeneratorFactory identifierGeneratorFactory = new OgmMutableIdentifierGeneratorFactory();

	public OgmConfiguration() {
		super();
		resetOgm();
	}

	private void resetOgm() {
		//NOTE: When performing changes here, be sure to do the same in setProperties() below

		super.setNamingStrategy( OgmNamingStrategy.INSTANCE );
		setProperty( InternalProperties.OGM_ON, "true" );
		// This property binds the OgmMassIndexer with Hibernate Search. An application could use OGM without Hibernate
		// Search therefore we set property value and key using a String in case the dependency is not on the classpath.
		setProperty( "hibernate.search.massindexer.factoryclass", "org.hibernate.ogm.massindex.impl.OgmMassIndexerFactory" );

		// by default use the new id generator scheme...
		setProperty( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );

		setProperty( AvailableSettings.QUERY_TRANSLATOR, OgmQueryTranslatorFactory.class.getName() );
	}

	@Override
	public Mapping buildMapping() {
		final Mapping delegate = super.buildMapping();
		return new Mapping() {

			@Override
			public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
				return identifierGeneratorFactory;
			}

			@Override
			public Type getIdentifierType(String entityName) throws MappingException {
				return delegate.getIdentifierType( entityName );
			}

			@Override
			public String getIdentifierPropertyName(String entityName) throws MappingException {
				return delegate.getIdentifierPropertyName( entityName );
			}

			@Override
			public Type getReferencedPropertyType(String entityName, String propertyName) throws MappingException {
				return delegate.getReferencedPropertyType( entityName, propertyName );
			}
		};
	}

	@Override
	public MutableIdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return identifierGeneratorFactory;
	}

	@Override
	@Deprecated
	public OgmSessionFactory buildSessionFactory() throws HibernateException {
		return new OgmSessionFactoryImpl( (SessionFactoryImplementor) super.buildSessionFactory() );
	}

	@Override
	public Configuration setProperties(Properties properties) {
		super.setProperties( properties );
		//Unless the new configuration properties explicitly disable OGM's default properties
		//assume there was no intention to disable them:
		if ( ! properties.containsKey( InternalProperties.OGM_ON ) ) {
			setProperty( InternalProperties.OGM_ON, "true" );
		}
		if ( ! properties.containsKey(  "hibernate.search.massindexer.factoryclass" ) ) {
			setProperty( "hibernate.search.massindexer.factoryclass", "org.hibernate.ogm.massindex.OgmMassIndexerFactory" );
		}
		if ( ! properties.containsKey( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS ) ) {
			setProperty( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		}
		if ( !properties.containsKey( AvailableSettings.QUERY_TRANSLATOR ) ) {
			setProperty( AvailableSettings.QUERY_TRANSLATOR, OgmQueryTranslatorFactory.class.getName() );
		}
		return this;
	}

	/**
	 * Applies configuration options to the bootstrapped session factory. Use either this method or pass a
	 * {@link OptionConfigurator} via {@link OgmProperties#OPTION_CONFIGURATOR} but don't use both at the same time.
	 *
	 * @param datastoreType represents the datastore to be configured; it is the responsibility of the caller to make
	 * sure that this matches the underlying datastore provider.
	 * @return a context object representing the entry point into the fluent configuration API.
	 */
	@Override
	public <D extends DatastoreConfiguration<G>, G extends GlobalContext<?, ?>> G configureOptionsFor(Class<D> datastoreType) {
		ConfigurableImpl configurable = new ConfigurableImpl();
		getProperties().put( InternalProperties.OGM_OPTION_CONTEXT, configurable.getContext() );

		return configurable.configureOptionsFor( datastoreType );
	}
}

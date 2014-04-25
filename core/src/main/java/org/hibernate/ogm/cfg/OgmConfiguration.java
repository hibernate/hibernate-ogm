/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
		super.setNamingStrategy( OgmNamingStrategy.INSTANCE );
		setProperty( InternalProperties.OGM_ON, "true" );
		// Hibernate will check the syntax of the queries when using NativeNamedQueries if this property is not set to
		// false
		setProperty( AvailableSettings.QUERY_STARTUP_CHECKING, "false" );
		// This property binds the OgmMassIndexer with Hibernate Search. An application could use OGM without Hibernate
		// Search therefore we set property value and key using a String in case the dependency is not on the classpath.
		setProperty( "hibernate.search.massindexer.factoryclass", "org.hibernate.ogm.massindex.OgmMassIndexerFactory" );
	}

	@Override
	public Mapping buildMapping() {
		final Mapping delegate = super.buildMapping();
		return new Mapping() {

			public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
				return identifierGeneratorFactory;
			}

			public Type getIdentifierType(String entityName) throws MappingException {
				return delegate.getIdentifierType( entityName );
			}

			public String getIdentifierPropertyName(String entityName) throws MappingException {
				return delegate.getIdentifierPropertyName( entityName );
			}

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
		if ( ! properties.containsKey(  AvailableSettings.QUERY_STARTUP_CHECKING ) ) {
			setProperty( AvailableSettings.QUERY_STARTUP_CHECKING, "false" );
		}
		if ( ! properties.containsKey(  "hibernate.search.massindexer.factoryclass" ) ) {
			setProperty( "hibernate.search.massindexer.factoryclass", "org.hibernate.ogm.massindex.OgmMassIndexerFactory" );
		}
		return this;
	}

	/**
	 * Applies configuration options to the bootstrapped session factory. Use either this method or pass a
	 * {@link org.hibernate.ogm.cfg.spi.OptionConfigurator} via {@link OgmProperties#OPTION_CONFIGURATOR} but don't use
	 * both at the same time.
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

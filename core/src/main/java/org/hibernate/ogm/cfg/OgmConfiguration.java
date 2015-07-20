/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.impl.ConfigurableImpl;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.GlobalContext;

/**
 * An instance of {@link OgmConfiguration} allows the application
 * to specify properties and mapping documents to be used when
 * creating an {@link OgmSessionFactory}.
 *
 * @author Davide D'Alto
 */
public class OgmConfiguration extends Configuration implements Configurable {

	public OgmConfiguration() {
		super();
		super.setProperty( OgmProperties.ENABLED, "true" );
	}

	@Override
	public OgmSessionFactory buildSessionFactory() throws HibernateException {
		return (OgmSessionFactory) super.buildSessionFactory();
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

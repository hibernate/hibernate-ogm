/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.impl;

import org.hibernate.ogm.cfg.Configurable;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContextImpl;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;


/**
 * @author Gunnar Morling
 *
 */
public class ConfigurableImpl implements Configurable {

	private static final Log log = LoggerFactory.make();
	private final AppendableConfigurationContext context;

	public ConfigurableImpl() {
		context = new AppendableConfigurationContext();
	}

	@Override
	public <D extends DatastoreConfiguration<G>, G extends GlobalContext<?, ?>> G configureOptionsFor(Class<D> datastoreType) {
		D configuration = newInstance( datastoreType );
		return configuration.getConfigurationBuilder( new ConfigurationContextImpl( context ) );
	}

	public AppendableConfigurationContext getContext() {
		return context;
	}

	private <D extends DatastoreConfiguration<?>> D newInstance(Class<D> datastoreType) {
		try {
			return datastoreType.newInstance();

		}
		catch (Exception e) {
			throw log.unableToInstantiateType( datastoreType.getName(), e );
		}
	}
}

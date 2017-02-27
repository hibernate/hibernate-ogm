/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.OgmSessionFactory;

public class OgmSessionFactoryRule extends org.junit.rules.ExternalResource {

	private final Class<?>[] entityTypes;
	private final Map<String, Object> configurationSettings = new HashMap<>();
	private OgmSessionFactory factory;

	public OgmSessionFactoryRule(Class<?>... entityTypes) {
		this.entityTypes = entityTypes;
	}

	/**
	 * Set a Configuration property before the SessionFactory is started
	 * @param key the name of the configuration property to set
	 * @param value the Value to assign
	 * @return {@code this} for method chaining
	 */
	public synchronized OgmSessionFactoryRule setConfigurationProperty( String key, Object value) {
		if ( factory != null ) {
			throw new IllegalStateException( "Can not set configuration after the SessionFactory was started" );
		}
		configurationSettings.put( key, value );
		return this;
	}

	public synchronized OgmSessionFactory getOgmSessionFactory() {
		if ( factory == null ) {
			throw new IllegalStateException( "OgmSessionFactory was not successfully started" );
		}
		return factory;
	}

	@Override
	public synchronized void before() throws Exception {
		if ( factory != null ) {
			throw new IllegalStateException( "Duplicate start?" );
		}
		factory = TestHelper.getDefaultTestSessionFactory( configurationSettings, entityTypes );
	}

	@Override
	public synchronized void after() {
		if ( factory != null ) {
			factory.close();
			factory = null;
		}
	}

}

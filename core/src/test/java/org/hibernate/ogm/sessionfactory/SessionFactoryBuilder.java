/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.sessionfactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.utils.TestHelper;

/**
 * Builder class to create a test configuration.
 *
 * @author Fabio Massimo Ercoli
 */
public class SessionFactoryBuilder {

	// input
	private final Class<?>[] entities;
	private final Map<String, Object> customProperties = new HashMap<>();

	// output
	private Configuration configuration;
	private StandardServiceRegistry serviceRegistry;

	private SessionFactoryBuilder(Class<?>[] entities) {
		this.entities = entities;
	}

	public static SessionFactoryBuilder entities(Class... entities) {
		return new SessionFactoryBuilder( entities );
	}

	public SessionFactoryBuilder property(String key, Object value) {
		customProperties.put( key, value );
		return this;
	}

	public SessionFactory build() {
		Properties properties = new Properties();
		properties.putAll( TestHelper.getDefaultTestSettings() );
		properties.putAll( customProperties );

		configuration = new OgmConfiguration();
		for ( Class entity : entities ) {
			configuration.addAnnotatedClass( entity );
		}
		configuration.addProperties( properties );
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() )
				.build();
		return configuration.buildSessionFactory( serviceRegistry );
	}
}

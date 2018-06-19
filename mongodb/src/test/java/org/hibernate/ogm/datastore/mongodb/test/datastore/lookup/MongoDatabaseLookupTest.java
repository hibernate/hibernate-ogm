/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.datastore.lookup;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.backendtck.id.GuitarPlayer;
import org.hibernate.ogm.backendtck.id.PianoPlayer;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.service.spi.ServiceException;

import org.junit.Test;

/**
 * Test usage of {@link OgmProperties#NATIVE_CLIENT_RESOURCE}
 *
 * @author Fabio Massimo Ercoli
 */
public class MongoDatabaseLookupTest {

	@Test
	public void testWrongJndiResource() {
		Properties properties = new Properties();
		properties.putAll( TestHelper.getDefaultTestSettings() );
		properties.put( OgmProperties.NATIVE_CLIENT_RESOURCE, "java:ciao/data" );

		Configuration configuration = new OgmConfiguration()
				.addAnnotatedClass( PianoPlayer.class )
				.addAnnotatedClass( GuitarPlayer.class )
				.addProperties( properties );

		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() )
				.build();

		SessionFactory sessionFactory = null;
		try {
			sessionFactory = configuration.buildSessionFactory( serviceRegistry );
			fail( "expected exception at Hibernate factory creation time was not raised" );
		}
		catch (Exception rootException) {
			assertThat( rootException ).isExactlyInstanceOf( ServiceException.class );
			Throwable hibException = rootException.getCause();

			assertThat( hibException ).isExactlyInstanceOf( HibernateException.class );
			assertThat( hibException ).hasMessage( "OGM000098: Error on fetch property hibernate.connection.resource: 'java:ciao/data'." );
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
		}
	}
}

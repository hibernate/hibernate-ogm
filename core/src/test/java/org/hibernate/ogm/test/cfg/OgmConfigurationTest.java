/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.cfg;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.junit.Test;

/**
 * Test for bootstrapping via {@link OgmConfiguration}.
 *
 * @author Gunnar Morling
 */
public class OgmConfigurationTest {

	@Test
	public void canBootstrapViaOgmConfiguration() {
		OgmConfiguration cfg = new OgmConfiguration();
		cfg.addAnnotatedClass( LawnMower.class );

		OgmSessionFactory sf = cfg.buildSessionFactory();
		OgmSession session = sf.openSession();
		session.beginTransaction();

		LawnMower mower = new LawnMower();
		mower.setBrand( "Findboard 9000" );
		session.persist( mower );
		session.getTransaction().commit();
		session.clear();

		session.beginTransaction();
		LawnMower loadedMower = session.get( LawnMower.class, mower.getId() );
		assertThat( loadedMower.getBrand() ).isEqualTo( "Findboard 9000" );
		session.getTransaction().commit();

		sf.close();
	}
}

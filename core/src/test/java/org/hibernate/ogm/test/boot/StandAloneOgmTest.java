/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.boot;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.boot.OgmSessionFactoryBuilder;
import org.hibernate.ogm.cfg.OgmProperties;
import org.junit.Test;

/**
 * Simple test for bootstrapping Hibernate OGM.
 *
 * @author Gunnar Morling
 */
public class StandAloneOgmTest {

	@Test
	public void sessionFactoryIsTheOneFromOgm() {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
			.applySetting( OgmProperties.ENABLED, true )
			.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" )
			.build();

		OgmSessionFactory osf = new MetadataSources( registry )
			.buildMetadata()
			.getSessionFactoryBuilder()
			.unwrap( OgmSessionFactoryBuilder.class )
			.build();

		assertThat( osf ).isNotNull();
	}
}

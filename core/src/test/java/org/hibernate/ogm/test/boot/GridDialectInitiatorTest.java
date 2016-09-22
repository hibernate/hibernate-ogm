/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.boot;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.boot.OgmSessionFactoryBuilder;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class GridDialectInitiatorTest {

	@Test
	@TestForIssue(jiraKey = "OGM-1104")
	public void testInvalidGridDialect() {
		try {
			StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
					.applySetting( OgmProperties.ENABLED, true )
					.applySetting( OgmProperties.GRID_DIALECT, InvalidGridDialect.class )
					.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" )
					.build();

			new MetadataSources( registry )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.unwrap( OgmSessionFactoryBuilder.class )
					.build();

			fail( "Expected exception was not raised" );
		}
		catch (Exception e) {
			assertThat( e.getCause().getCause().getMessage() ).startsWith( "OGM000014" );
		}

	}

}

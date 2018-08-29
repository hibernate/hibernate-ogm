/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.initialize;

import java.util.Map;

import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteServerRunner;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(InfinispanRemoteServerRunner.class)
@TestForIssue( jiraKey = "OGM-1518" )
public class MinimalHotRodConfigTest extends OgmTestCase {

	@Test
	public void insertEntity() {
		inTransaction( session -> session.persist( new DisneyCharacter( 1, "Mickey Mouse" ) ) );
	}

	@After
	public void cleanUp() {
		deleteAll( DisneyCharacter.class, 1 );
	}

	@Override
	protected void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		cfg.put( InfinispanRemoteProperties.CONFIGURATION_RESOURCE_NAME, "hotrodclient-minimal.properties" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { DisneyCharacter.class };
	}
}

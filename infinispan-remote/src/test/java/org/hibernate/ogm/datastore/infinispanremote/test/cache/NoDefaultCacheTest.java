/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.cache;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;

import org.junit.ClassRule;
import org.junit.Test;

public class NoDefaultCacheTest {

	@ClassRule
	public static final RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule(
			"wildfly-trimmed-config-test-no-cache-default.xml" );

	@Test
	public void noDefaultCacheTest() throws HibernateException {
	}

}

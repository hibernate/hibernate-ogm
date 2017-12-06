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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NoTemplateTest {

	@ClassRule
	public static final RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void noTemplateTest() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001709:" );
		CacheTestHelper.getDatastoreProvider( CacheEntity.class );
	}
}

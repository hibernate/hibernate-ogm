/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import static org.hibernate.ogm.utils.GridDialectType.COUCHDB;
import static org.hibernate.ogm.utils.GridDialectType.EHCACHE;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.REDIS_JSON;
import static org.junit.Assert.fail;

import org.hibernate.ogm.backendtck.simpleentity.Hypothesis;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.Test;

/**
 * Test {@link SkipByGridDialect} is working with {@link OgmJpaTestCase}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class SkipByGridDialectSelfJpaTest extends OgmJpaTestCase {

	@Test
	@SkipByGridDialect({ HASHMAP, INFINISPAN, MONGODB, EHCACHE, NEO4J_EMBEDDED, NEO4J_REMOTE, COUCHDB, REDIS_JSON })
	public void testWhichAlwaysFails() {
		fail( "This should never be executed" );
	}

	@Test
	public void testCorrect() {
		// all fine
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class };
	}

}

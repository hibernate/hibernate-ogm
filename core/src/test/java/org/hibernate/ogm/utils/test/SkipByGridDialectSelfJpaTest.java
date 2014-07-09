/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import junit.framework.Assert;

import org.hibernate.ogm.backendtck.simpleentity.Hypothesis;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.Test;

/**
 * Test {@link SkipByGridDialect} is working with {@link JpaTestCase}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class SkipByGridDialectSelfJpaTest extends JpaTestCase {

	@Test
	@SkipByGridDialect({
		GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.MONGODB, GridDialectType.EHCACHE, GridDialectType.NEO4J, GridDialectType.COUCHDB
	})
	public void testWhichAlwaysFails() {
		Assert.fail( "This should never be executed" );
	}

	@Test
	public void testCorrect() {
		// all fine
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Hypothesis.class };
	}

}

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
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.junit.Test;

/**
 * Verifies that SkipByGridDialect is applied by the
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 */
public class SkipByGridDialectSelfTest extends OgmTestCase {

	@Test
	@SkipByGridDialect({ GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.MONGODB, GridDialectType.EHCACHE, GridDialectType.NEO4J,
			GridDialectType.COUCHDB })
	public void testWhichAlwaysFails() {
		Assert.fail( "This should never be executed" );
	}

	@Test
	public void testCorrect() {
		// all fine
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class };
	}

}

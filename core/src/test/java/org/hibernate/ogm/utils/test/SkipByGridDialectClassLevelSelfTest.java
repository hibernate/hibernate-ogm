/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import static org.junit.Assert.fail;

import org.hibernate.ogm.backendtck.simpleentity.Hypothesis;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that {@link @SkipByGridDialect} given on the class-level is applied.
 *
 * @author Gunnar Morling
 */
@SkipByGridDialect( GridDialectType.HASHMAP )
public class SkipByGridDialectClassLevelSelfTest extends OgmTestCase {

	@Test
	public void testWhichAlwaysFails() {
		fail( "This should never be executed" );
	}

	@BeforeClass
	public static void beforeClass() {
		fail( "This should never be executed" );
	}

	@AfterClass
	public static void afterClass() {
		fail( "This should never be executed" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class };
	}
}

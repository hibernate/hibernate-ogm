/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.ogm.utils.PackagingRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
public class JPAStandaloneNoOGMTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone-noogm.xml", Poem.class );

	@Rule
	public ExpectedException error = ExpectedException.none();

	@Test
	public void testJTAStandaloneNoOgm() throws Exception {
		// Failure is expected as we didn't configure a JDBC connection nor a Dialect
		// (and this would fail only if effectively loading Hibernate ORM without OGM superpowers)
		error.expect( javax.persistence.PersistenceException.class );
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone-noogm" );
		emf.close(); // should not be reached, but cleanup in case the test fails.
	}

}

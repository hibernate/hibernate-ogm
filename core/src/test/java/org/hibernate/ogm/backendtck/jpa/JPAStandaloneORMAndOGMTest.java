/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.Rule;
import org.junit.Test;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
public class JPAStandaloneORMAndOGMTest {

	@Rule
	public PackagingRule ogmPackaging = new PackagingRule( "persistencexml/jpajtastandalone-ormogm.xml", Poem.class );

	@Test
	//Test for OGM-416 (avoid StackOverFlow when both an OGM and ORM PU are used
	public void testJTAStandaloneNoOgm() throws Exception {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone", TestHelper.getEnvironmentProperties() );
		emf.close();
		emf = Persistence.createEntityManagerFactory( "jpajtastandalone-noogm" );
		emf.close();
	}

}

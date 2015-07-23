/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.type;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;

import java.util.Date;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;


import org.hibernate.ogm.utils.PackagingRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class TypeOverridingInDialectTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/customdialect.xml",
			Poem.class,
			OverridingTypeDialect.class,
			ExplodingType.class,
			TypeOverridingInDialectTest.class
		);

	@Test
	public void testOverriddenTypeInDialect() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "customdialect" );
		final EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		try {
			Poem poem = new Poem();
			poem.setName( "L'albatros" );
			poem.setPoemSocietyId( UUID.randomUUID() );
			poem.setCreation( new Date() );
			em.persist( poem );
			em.getTransaction().commit();
			assertThat( true ).as( "Custom type not used" ).isFalse();
		}
		catch ( RollbackException e ) {
			//make this chaining more robust
			assertThat( e.getCause().getMessage() ).isEqualTo( "Exploding type" );
		}
		finally {
			try {
				em.getTransaction().rollback();
			}
			catch ( Exception e ) {
				//we try
			}
			em.close();
			dropSchemaAndDatabase( emf );
			emf.close();
		}
	}
}

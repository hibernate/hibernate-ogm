/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.hint;

import static org.hibernate.ogm.test.hint.CustomHintSupportedProvider.Dialect.DIALECT_SPECIFIED_HINT;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.hibernate.ogm.test.datastore.Noise;
import org.hibernate.ogm.utils.PackagingRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class SupportCustomHintTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/hint.xml", Noise.class );
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void shouldPassDialectSpecificHint() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "hint" );
		EntityManager em = emf.createEntityManager();

		Query query = em.createQuery( "select n from Noise n" );
		query.setHint( DIALECT_SPECIFIED_HINT, true );
		query.getResultList();

	}

}

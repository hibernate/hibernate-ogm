/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that entities obtained from queries can be updated.
 *
 * @author Gunnar Morling
 */
public class QueryUpdateTest extends OgmTestCase {

	@Before
	public void insertTestEntities() throws Exception {
		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		Helicopter helicopter = new Helicopter();
		helicopter.setMake( "Lama" );
		helicopter.setName( "Sergio" );
		session.persist( helicopter );

		transaction.commit();
		session.close();
	}

	@Test
	public void canUpdateEntityReturnedByQuery() {
		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		Query query = session.createQuery( "from Helicopter h where name = 'Sergio'" );
		Helicopter helicopter = (Helicopter) query.uniqueResult();
		assertThat( helicopter ).isNotNull();
		helicopter.setName( "Leonie" );

		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		query = session.createQuery( "from Helicopter h where name = 'Leonie'" );
		helicopter = (Helicopter) query.uniqueResult();
		assertThat( helicopter ).isNotNull();

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Helicopter.class };
	}
}

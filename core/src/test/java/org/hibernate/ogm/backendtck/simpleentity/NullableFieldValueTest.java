/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.simpleentity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Test that a column value can be reset to null.
 * <p>
 * For example, Neo4j does not allow to set the property of a node to null. In that case you could decide to remove the
 * property, throw an exception or skip the operation.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class NullableFieldValueTest extends OgmTestCase {

	@Test
	public void testValueShouldBeNullWhenSetToNull() throws Exception {
		Session session = openSession();
		String uuid = null;
		{
			Helicopter entity = helicopter( "Honey Bee CP3" );
			Transaction tx = session.beginTransaction();
			session.persist( entity );
			uuid = entity.getUUID();
			tx.commit();
			session.clear();
		}
		{
			Transaction tx = session.beginTransaction();
			Helicopter entity = (Helicopter) session.get( Helicopter.class, uuid );
			entity.setName( null );
			tx.commit();
			session.clear();
		}
		{
			Transaction tx = session.beginTransaction();
			Helicopter entity = (Helicopter) session.get( Helicopter.class, uuid );
			session.delete( entity );
			tx.commit();
			assertThat( entity.getName(), is( nullValue() ) );
		}
		session.close();
	}

	private Helicopter helicopter(String name) {
		Helicopter entity = new Helicopter();
		entity.setName( name );
		return entity;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Helicopter.class };
	}
}

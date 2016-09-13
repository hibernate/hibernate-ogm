/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.manytoone;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard
 */
@SkipByGridDialect(
		value = { GridDialectType.CASSANDRA, GridDialectType.INFINISPAN_REMOTE },
		comment = "Basket.products list - bag semantics unsupported (no primary key)"
)
public class ManyToOneExtraTest extends OgmTestCase {

	@Test
	public void testUnidirectionalOneToMany() throws Exception {
		final Session session = openSession();
		Transaction tx = session.beginTransaction();
		Product beer = new Product( "Beer", "Tactical nuclear penguin" );
		session.persist( beer );

		Product pretzel = new Product( "Pretzel", "Glutino Pretzel Sticks" );
		session.persist( pretzel );

		Basket basket = new Basket();
		basket.setId( "davide_basket" );
		basket.setOwner( "Davide" );
		basket.setProducts( Arrays.asList( beer, pretzel ) );
		session.persist( basket );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();
		basket = (Basket) session.get( Basket.class, basket.getId() );
		assertThat( basket ).isNotNull();
		assertThat( basket.getId() ).isEqualTo( basket.getId() );
		assertThat( basket.getProducts() )
			.onProperty( "name" ).containsOnly( beer.getName(), pretzel.getName() );
		tx.commit();

		session.clear();

		tx = session.beginTransaction();
		session.delete( basket );
		session.delete( pretzel );
		session.delete( beer );
		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Basket.class,
				Product.class,
		};
	}
}

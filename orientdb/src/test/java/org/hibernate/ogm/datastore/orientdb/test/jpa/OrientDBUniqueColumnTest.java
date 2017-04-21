/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.jpa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.RollbackException;

import org.apache.log4j.Logger;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.BuyingOrder;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.Customer;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.OrderItem;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.Pizza;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.Product;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.ProductType;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.Status;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Test checks unique columns
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OrientDBUniqueColumnTest extends OgmJpaTestCase {

	private static final Logger log = Logger.getLogger( OrientDBUniqueColumnTest.class.getName() );
	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		if ( em.getTransaction().isActive() ) {
			em.getTransaction().rollback();
		}
		em.clear();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test1InsertNewUniqueCustomer() {
		log.debug( "start" );
		try {
			em.getTransaction().begin();
			Customer newCustomer = new Customer();
			newCustomer.setCustomerNumber( "11-22" );
			newCustomer.setName( "test" );
			newCustomer.setStatus( Status.VIP );
			log.debug( "New Customer ready for  persit" );
			em.persist( newCustomer );
			em.flush();
			em.getTransaction().commit();

			em.getTransaction().begin();
			Query query = em.createNativeQuery( "select from Customer where name=:name", Customer.class );
			query.setParameter( "name", "test" );
			List<Customer> customers = query.getResultList();
			log.debug( String.format( "customers.size(): %s", customers.size() ) );
			assertFalse( "Customers must be", customers.isEmpty() );
			Customer testCustomer = customers.get( 0 );
			assertNotNull( "Customer with 'test' must be saved!", testCustomer );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			em.getTransaction().rollback();
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test2InsertNewNonUniqueCustomer() {
		log.debug( "start" );
		try {
			em.getTransaction().begin();
			Customer newCustomer = new Customer();
			newCustomer.setCustomerNumber( "11-22" );
			newCustomer.setName( "test1" );
			newCustomer.setStatus( Status.VIP );
			log.debug( "New Customer ready for  persit" );
			em.persist( newCustomer );
			em.flush();
			em.getTransaction().commit();
		}
		catch (RollbackException e) {
			log.error( "Error", e );
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
		try {

			em.getTransaction().begin();
			Query query = em.createNativeQuery( "select from Customer where name=:name", Customer.class );
			query.setParameter( "name", "test1" );
			List<Customer> customers = query.getResultList();
			log.debug( String.format( "customers.size(): %s", customers.size() ) );
			assertTrue( "Customer with name 'test1' must not exists!", customers.isEmpty() );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			em.getTransaction().rollback();
		}

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Customer.class, Pizza.class, Product.class, BuyingOrder.class,
				ProductType.class, OrderItem.class };
	}

}

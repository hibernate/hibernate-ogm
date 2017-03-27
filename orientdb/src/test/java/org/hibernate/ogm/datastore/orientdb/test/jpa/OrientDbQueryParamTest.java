/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.jpa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.hibernate.ogm.datastore.orientdb.constant.OrientDBConstant;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.SimpleTypesEntity;
import org.hibernate.ogm.type.impl.EnumType;
import org.hibernate.ogm.type.impl.NumericBooleanType;
import org.hibernate.ogm.type.impl.TimestampType;
import org.hibernate.ogm.type.impl.TrueFalseType;
import org.hibernate.ogm.type.impl.DateType;
import org.hibernate.ogm.type.impl.YesNoType;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;

/**
 * Test checks CRUD for entities with associations (with links with other entities)
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

public class OrientDbQueryParamTest extends OgmJpaTestCase {

	private static final Logger log = Logger.getLogger( OrientDbQueryParamTest.class.getName() );
	private static EntityManager em;

	private static Iterable<Object[]> prepareTestData() throws ParseException {
		TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
		List<Object[]> list = new LinkedList<>();

		SimpleTypesEntity entity1 = new SimpleTypesEntity( 1L );
		list.add( new Object[]{ Long.class, 1L, "id", 1, entity1 } );

		SimpleTypesEntity entity2 = new SimpleTypesEntity( 2L );
		entity2.setIntValue( 1 );
		list.add( new Object[]{ Integer.class, 1, "intValue", 1, entity2 } );

		SimpleTypesEntity entity3 = new SimpleTypesEntity( 3L );
		entity3.setShortValue( (short) 1 );
		list.add( new Object[]{ Short.class, (short) 1, "shortValue", 1, entity3 } );

		SimpleTypesEntity entity4 = new SimpleTypesEntity( 4L );
		entity4.setByteValue( (byte) 1 );
		list.add( new Object[]{ Byte.class, (byte) 1, "byteValue", 1, entity4 } );

		SimpleTypesEntity entity5 = new SimpleTypesEntity( 5L );
		entity5.setNumericBooleanValue( Boolean.TRUE );
		list.add( new Object[]{ NumericBooleanType.class, (short) 1, "numericBooleanValue", 1, entity5 } );

		SimpleTypesEntity entity6 = new SimpleTypesEntity( 6L );
		entity6.setYesNoBooleanValue( Boolean.TRUE );
		list.add( new Object[]{ YesNoType.class, 'Y', "yesNoBooleanValue", 1, entity6 } );

		SimpleTypesEntity entity7 = new SimpleTypesEntity( 7L );
		entity7.setTfBooleanValue( Boolean.TRUE );
		list.add( new Object[]{ TrueFalseType.class, 'T', "tfBooleanValue", 1, entity7 } );

		SimpleTypesEntity entity8 = new SimpleTypesEntity( 8L );
		entity8.setE1( SimpleTypesEntity.EnumType.E1 );
		list.add( new Object[]{ EnumType.class, 0, "e1", 1, entity8 } );

		SimpleTypesEntity entity9 = new SimpleTypesEntity( 9L );
		entity9.setE2( SimpleTypesEntity.EnumType.E2 );
		list.add( new Object[]{ EnumType.class, "E2", "e2", 1, entity9 } );

		Calendar calendar = Calendar.getInstance();
		Date now = calendar.getTime();

		SimpleDateFormat df1 = new SimpleDateFormat( OrientDBConstant.DEFAULT_DATETIME_FORMAT );
		Date correctedNow = df1.parse( df1.format( now ) );
		log.info( "CreatedTimestamp :" + df1.format( now ) + "; " + df1.format( correctedNow ) );
		SimpleTypesEntity entity10 = new SimpleTypesEntity( 10L );
		entity10.setCreatedTimestamp( correctedNow );
		list.add( new Object[]{ TimestampType.class, correctedNow, "createdTimestamp", 1, entity10 } );

		SimpleDateFormat df2 = new SimpleDateFormat( OrientDBConstant.DEFAULT_DATE_FORMAT );
		Date today = df2.parse( df2.format( new Date() ) );
		SimpleTypesEntity entity11 = new SimpleTypesEntity( 11L );
		entity11.setCreatedDate( today );
		list.add( new Object[]{ DateType.class, today, "createdDate", 1, entity11 } );

		return list;
	}

	@Test
	public void testSearchBy() throws ParseException {
		for ( Object[] paramArray : prepareTestData() ) {

			@SuppressWarnings("rawtypes")
			Class searchByClass = (Class) paramArray[0];
			Object searchByValue = paramArray[1];
			String paramName = (String) paramArray[2];
			Integer requiredCount = (Integer) paramArray[3];
			SimpleTypesEntity preparedEntity = (SimpleTypesEntity) paramArray[4];

			testSearchBy( searchByClass, searchByValue, paramName, requiredCount, preparedEntity );

		}
	}

	@SuppressWarnings("rawtypes")
	private void testSearchBy(Class searchByClass, Object searchByValue, String paramName, Integer requiredCount, SimpleTypesEntity preparedEntity) {

		try {
			em.getTransaction().begin();
			em.persist( preparedEntity );
			em.getTransaction().commit();
			em.clear();

			em.getTransaction().begin();
			Query query = null;
			if ( searchByClass.equals( TimestampType.class ) ) {
				query = em.createNativeQuery( "select from SimpleTypesEntity where " + paramName + ".asDatetime()=:" + paramName, SimpleTypesEntity.class );
				SimpleDateFormat df = new SimpleDateFormat( OrientDBConstant.DEFAULT_DATETIME_FORMAT );
				query.setParameter( paramName, df.format( (Date) searchByValue ) );
			}
			else if ( searchByClass.equals( DateType.class ) ) {
				query = em.createNativeQuery( "select from SimpleTypesEntity where " + paramName + ".asDate()=:" + paramName, SimpleTypesEntity.class );
				SimpleDateFormat df = new SimpleDateFormat( OrientDBConstant.DEFAULT_DATE_FORMAT );
				query.setParameter( paramName, df.format( (Date) searchByValue ) );
			}
			else {
				query = em.createNativeQuery( "select from SimpleTypesEntity where " + paramName + "=:" + paramName, SimpleTypesEntity.class );
				query.setParameter( paramName, searchByValue );
			}

			assertEquals( String.format( "Incorrect search by class %s", searchByClass ), requiredCount, Integer.valueOf( query.getResultList().size() ) );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			em.getTransaction().rollback();
			throw e;
		}

	}

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

	@BeforeClass
	public static void setUpClass() {
		TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ SimpleTypesEntity.class };
	}

}

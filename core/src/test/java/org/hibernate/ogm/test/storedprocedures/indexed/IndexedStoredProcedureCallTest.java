/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.storedprocedures.indexed;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TemporalType;

import org.hibernate.ogm.backendtck.storedprocedures.indexed.IndexedStoredProcDialect;
import org.hibernate.ogm.backendtck.storedprocedures.indexed.IndexedStoredProcProvider;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.jpa.impl.OgmStoredProcedureQuery;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.junit.Test;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@TestForIssue( jiraKey = {"OGM-359"})
public class IndexedStoredProcedureCallTest extends org.hibernate.ogm.backendtck.storedprocedures.indexed.IndexedStoredProcedureCallTest {
	public static final String TEST_SIMPLE_DATE_VALUE_STORED_PROC = "testSimpleDateValue";
	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		Properties properties = info.getProperties();
		properties.setProperty( OgmProperties.DATASTORE_PROVIDER, IndexedStoredProcProvider.class.getName() );

		// function with one parameter and result as list of entities
		IndexedStoredProcDialect.FUNCTIONS.put( TEST_RESULT_SET_STORED_PROC, ( Object[] params ) -> {
			List<Tuple> result = new ArrayList<>( 1 );
			Tuple resultTuple = new Tuple();
			resultTuple.put( "id", params[0] );
			resultTuple.put( "title", params[1] );
			result.add( resultTuple );
			return CollectionHelper.newClosableIterator( result );
		} );
		// function with one parameter and returned simple value
		IndexedStoredProcDialect.FUNCTIONS.put( TEST_SIMPLE_VALUE_STORED_PROC, ( Object[] params ) -> {
			List<Tuple> result = new ArrayList<>( 1 );
			Tuple resultTuple = new Tuple();
			resultTuple.put( "result", params[0] );
			result.add( resultTuple );
			return CollectionHelper.newClosableIterator( result );
		} );
		// function with one parameter and returned simple value
		IndexedStoredProcDialect.FUNCTIONS.put( TEST_SIMPLE_DATE_VALUE_STORED_PROC, ( Object[] params ) -> {
			List<Tuple> result = new ArrayList<>( 1 );
			Tuple resultTuple = new Tuple();
			Date date = null;
			if ( params[0] instanceof Date ) {
				date = (Date) params[0];
			}
			else if ( params[0] instanceof Calendar ) {
				date = ( (Calendar) params[0] ).getTime();
			}
			resultTuple.put( "result", Long.valueOf( date.getTime() ) );
			result.add( resultTuple );
			return CollectionHelper.newClosableIterator( result );
		} );
	}

	@Test
	public void testSingleResultDynamicCallWithTemporalTypeData() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set( 2017, Calendar.NOVEMBER, 1, 0, 0, 0 );
		calendar.set( Calendar.MILLISECOND, 0 );
		long dateMillis = calendar.getTimeInMillis();
		calendar.set( 2017, Calendar.NOVEMBER, 1, 10, 10, 10 );
		long dateTimeMillis = calendar.getTimeInMillis();

		StoredProcedureQuery storedProcedureQuery = getEntityManager().createStoredProcedureQuery( TEST_SIMPLE_DATE_VALUE_STORED_PROC );
		assertThat( storedProcedureQuery ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		storedProcedureQuery.registerStoredProcedureParameter( 0, Date.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( 0, new Date( dateTimeMillis ), TemporalType.DATE );
		Long singleResult = (Long) storedProcedureQuery.getSingleResult();
		assertThat( singleResult ).isNotNull();
		assertThat( singleResult ).isEqualTo( dateMillis );

	}

	@Test
	public void testSingleResultDynamicCallWithTemporalTypeTimestamp() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set( 2017, Calendar.NOVEMBER, 1, 10, 10, 10 );
		calendar.set( Calendar.MILLISECOND, 0 );
		long dateTimeMillis = calendar.getTimeInMillis();
		StoredProcedureQuery storedProcedureQuery = getEntityManager().createStoredProcedureQuery( TEST_SIMPLE_DATE_VALUE_STORED_PROC );
		assertThat( storedProcedureQuery ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		storedProcedureQuery.registerStoredProcedureParameter( 0, Date.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( 0, calendar, TemporalType.TIMESTAMP );
		Long singleResult = (Long) storedProcedureQuery.getSingleResult();
		assertThat( singleResult ).isNotNull();
		assertThat( singleResult ).isEqualTo( dateTimeMillis );
	}

	@Test
	public void testSingleResultDynamicCallWithTemporalTypeTime() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime( new Date( 0L ) );
		calendar.set( Calendar.HOUR_OF_DAY, 1 );
		calendar.set( Calendar.MINUTE, 10 );
		calendar.set( Calendar.SECOND, 55 );
		calendar.set( Calendar.MILLISECOND, 555 );

		long dateTimeMillis = calendar.getTimeInMillis();
		StoredProcedureQuery storedProcedureQuery = getEntityManager().createStoredProcedureQuery( TEST_SIMPLE_DATE_VALUE_STORED_PROC );
		assertThat( storedProcedureQuery ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		storedProcedureQuery.registerStoredProcedureParameter( 0, Date.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( 0, calendar, TemporalType.TIMESTAMP );
		Long singleResult = (Long) storedProcedureQuery.getSingleResult();
		assertThat( singleResult ).isNotNull();
		assertThat( singleResult ).isEqualTo( dateTimeMillis );
	}
}

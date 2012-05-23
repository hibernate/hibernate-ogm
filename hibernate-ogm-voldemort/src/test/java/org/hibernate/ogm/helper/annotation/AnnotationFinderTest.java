/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.ogm.helper.annotation;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.helper.annotation.AnnotationFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class AnnotationFinderTest {

	private AnnotationFinder finder;
	private Job job;
	private Person person;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		job = new Job();
		person = new Person();
		finder = new AnnotationFinder();
	}

	@Test
	public void testIsEmbeddableAnnotated() {
		assertTrue( finder.isEmbeddableAnnotated( job.getClass() ) );
		assertFalse( finder.isEmbeddableAnnotated( person.getClass() ) );
		assertFalse( finder.isEmbeddableAnnotated( getClass() ) );
		assertFalse( finder.isEmbeddableAnnotated( null ) );
	}

	@Test
	public void testFindFieldColumns() {
		Map<String, Class> columnMap = new HashMap<String, Class>();
		finder.findFieldColumns( job.getClass(), null, columnMap, false );
		String columnType = columnMap.get( "summary" ).getCanonicalName();
		assertEquals( "expecting 'java.lang.String' but found " + columnType, columnType, "java.lang.String" );
		columnMap.clear();
		finder.findFieldColumns( job.getClass(), null, columnMap, true );
		assertNotNull( "expecting not null but found " + columnMap.get( "summary" ), columnMap.get( "summary" ) );
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1 );
		columnMap.clear();
		finder.findFieldColumns( person.getClass(), null, columnMap, false );
		columnType = columnMap.get( "summary" ).getCanonicalName();
		assertTrue( "expecting 'java.lang.String' but found " + columnType, columnType.equals( "java.lang.String" ) );
		columnMap.clear();
		finder.findFieldColumns( person.getClass(), null, columnMap, true );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
		columnMap.clear();
		finder.findFieldColumns( job.getClass(), "zipCode", columnMap, false );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
		columnMap.clear();
		finder.findFieldColumns( job.getClass(), "zipCode", columnMap, true );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
		columnMap.clear();
		finder.findFieldColumns( job.getClass(), "name", columnMap, true );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
		columnMap.clear();
		finder.findFieldColumns( job.getClass(), "dummyFieldName", columnMap, false );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
		columnMap.clear();
		finder.findFieldColumns( job.getClass(), "description", columnMap, true );
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1 );
		assertNotNull( "expecting not null but found " + columnMap.get( "summary" ), columnMap.get( "summary" ) );
	}

	@Test
	public void testFindMethodColumns() {
		Map<String, Class> columnMap = new HashMap<String, Class>();
		finder.findMethodColumns( Job.class, null, columnMap, false );
		String columnType = columnMap.get( "job_name" ).getCanonicalName();
		assertTrue( "expecting size == 2 but found " + columnMap.size(), columnMap.size() == 2 );
		assertNotNull( "expecting not null but found " + columnMap.get( "job_name" ), columnMap.get( "job_name" ) );
		assertNotNull( "expecting not null but found " + columnMap.get( "postal_code" ), columnMap.get( "postal_code" ) );
		columnMap.clear();
		finder.findMethodColumns( Job.class, null, columnMap, true );
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1 );
		assertNotNull( "expecting not null but found " + columnMap.get( "job_name" ), columnMap.get( "job_name" ) );
		columnMap.clear();
		finder.findMethodColumns( Address.class, "zipcode", columnMap, false );
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1 );
		assertNotNull( "expecting not null but found " + columnMap.get( "postal_code" ), columnMap.get( "postal_code" ) );
		assertTrue( "expecting 'java.lang.String' but found " + columnMap.get( "postal_code" ).getCanonicalName(),
				columnMap.get( "postal_code" ).getCanonicalName().equals( "java.lang.String" ) );
		columnMap.clear();
		finder.findMethodColumns( Address.class, "zipcode", columnMap, true );
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1 );
		assertNotNull( "expecting not null but found " + columnMap.get( "postal_code" ), columnMap.get( "postal_code" ) );
		columnMap.clear();
		finder.findMethodColumns( Person.class, "", columnMap, false );
		assertTrue( "expecting size == 2 but found " + columnMap.size(), columnMap.size() == 2 );
		assertNotNull( "expecting not null but found " + columnMap.get( "job_name" ), columnMap.get( "job_name" ) );
		assertNotNull( "expecting not null but found " + columnMap.get( "postal_code" ), columnMap.get( "postal_code" ) );
		columnMap.clear();
		finder.findMethodColumns( Person.class, "", columnMap, true );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
		columnMap.clear();
		finder.findMethodColumns( Person.class, "name", columnMap, false );
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1 );
		assertNotNull( "expecting not null but found " + columnMap.get( "job_name" ), columnMap.get( "job_name" ) );
		columnMap.clear();
		finder.findMethodColumns( Person.class, "name", columnMap, true );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
		columnMap.clear();
		finder.findMethodColumns( job.getClass(), "dummyMethodName", columnMap, false );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
	}

	@Test
	public void testFindAllColumnNamesFrom() {
		checkColumnNameWith( finder.findAllColumnNamesFrom( job.getClass(), null, false ) );
		checkColumnNameWith( finder.findAllColumnNamesFrom( person.getClass(), "", false ) );

		Map<String, Class> columnMap = finder.findAllColumnNamesFrom( person.getClass(), "name", false );
		assertEquals( columnMap.get( "job_name" ).getCanonicalName(), "java.lang.String" );
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1 );

		columnMap = finder.findAllColumnNamesFrom( person.getClass(), "dummy", false );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );

		columnMap = finder.findAllColumnNamesFrom( job.getClass(), null, true );
		assertTrue( "expecting size == 2 but found " + columnMap.size(), columnMap.size() == 2 );
		assertNotNull( "expecting not null but found " + columnMap.get( "job_name" ), columnMap.get( "job_name" ) );
		assertNotNull( "expecting not null but found " + columnMap.get( "summary" ), columnMap.get( "summary" ) );

		columnMap = finder.findAllColumnNamesFrom( person.getClass(), "", true );
		assertTrue( "expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
		columnMap = finder.findAllColumnNamesFrom( person.getClass(), "name", true );
		assertTrue( "expecing size == 0 but found " + columnMap.size(), columnMap.isEmpty() );
	}

	private void checkColumnNameWith(Map<String, Class> columnMap) {
		assertEquals( "expecting 'java.lang.String' but found " + columnMap.get( "job_name" ).getCanonicalName(),
				columnMap.get( "job_name" ).getCanonicalName(), "java.lang.String" );
		assertTrue( "expecting 'java.lang.String' but found " + columnMap.get( "summary" ).getCanonicalName(),
				columnMap.get( "summary" ).getCanonicalName().equals( "java.lang.String" ) );
		assertTrue( "expecting 'java.lang.String' but found " + columnMap.get( "postal_code" ).getCanonicalName(),
				columnMap.get( "postal_code" ).getCanonicalName().equals( "java.lang.String" ) );
		assertTrue( "expecting size == 3 but found " + columnMap.size() ,columnMap.size() == 3 );
	}
	
	@Test
	public void testFindAllJoinColumns(){
		Map<String,Class> columnMap = finder.findAllJoinColumnNamesFrom(Person.class,null,true );
		assertTrue("expecting size == 0 but found " + columnMap.size(),columnMap.isEmpty());
		columnMap = finder.findAllJoinColumnNamesFrom( Person.class, "", false );
		assertTrue("expecting size == 0 but found " + columnMap.size(), columnMap.isEmpty());
		columnMap = finder.findAllJoinColumnNamesFrom( Beer.class, null, true );
		assertTrue("expecting size == 1 but found " + columnMap.size(),columnMap.size() == 1);
		assertNotNull("expecting not null but found " + columnMap.get( "brewery_id" ),columnMap.get( "brewery_id" ));
		columnMap = finder.findAllJoinColumnNamesFrom( Brewery.class, "", false );
		assertTrue("expecting size == 2 but found " + columnMap.size(),columnMap.size() == 1);
		assertNotNull("expecting not null but found " + columnMap.get( "brewery_id" ),columnMap.get( "brewery_id" ));
	}
	
	@Test
	public void testFindAllIds() {
		Map<String, Class> ids = finder.findAllIdsFrom( Person.class, null, true );
		assertTrue( "expecting size == 1 but found " + ids.size(), ids.size() == 1 );
		assertNotNull( "expecting not null but found " + ids.get( "id" ), ids.get( "id" ) );
		ids = finder.findAllIdsFrom( Person.class, "id", true );
		assertTrue( "expecting size == 1 but found " + ids.size(), ids.size() == 1 );
		assertNotNull( "expecting not null but found " + ids.get( "id" ), ids.get( "id" ) );
		ids = finder.findAllIdsFrom( Phone.class, "", true );
		assertTrue( "expecting size == 1 but found " + ids.size(), ids.size() == 1 );
		assertNotNull( "expecting not null but found " + ids.get( "p_id" ), ids.get( "p_id" ) );
		ids = finder.findAllIdsFrom( Beer.class, "", false );
		assertTrue( "edxpecting size == 2 but found " + ids.size(), ids.size() == 2 );
		assertNotNull( "expecting not null but found " + ids.get( "beer_pk" ), ids.get( "beer_pk" ) );
		assertNotNull( "expecting not null but found " + ids.get( "brewery_pk" ), ids.get( "brewery_pk" ) );
	}
}

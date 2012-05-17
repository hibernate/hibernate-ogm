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
	public void testFindColumnNameFromFieldOn() {
		Map<String, Class> columnMap = new HashMap<String, Class>();
		finder.findColumnNameFromFieldOnRecursively( job.getClass(), columnMap );
		String columnType = columnMap.get( "summary" ).getCanonicalName();
		assertEquals( "expecting 'java.lang.String' but found " + columnType, columnType, "java.lang.String" );
		columnMap.clear();
		finder.findColumnNameFromFieldOnRecursively( person.getClass(), columnMap );
		columnType = columnMap.get( "summary" ).getCanonicalName();
		assertTrue( "expecting 'java.lang.String' but found " + columnType, columnType.equals( "java.lang.String" ) );
	}

	@Test
	public void testFindColumnNameFromMethodOn() {
		Map<String, Class> columnMap = finder.findColumnNameFromMethodOn( job.getClass() );
		assertEquals( "expecting 'java.lang.String' but found " + columnMap.get( "job_name" ),
				columnMap.get( "job_name" ).getCanonicalName(), "java.lang.String" );
	}

	@Test
	public void testFindAllColumnNamesFrom() {
		checkColumnNameWith( finder.findAllColumnNamesFrom( job.getClass(), null ) );
		checkColumnNameWith( finder.findAllColumnNamesFrom( person.getClass(), "" ) );
		Map<String,Class> columnMap = finder.findAllColumnNamesFrom( person.getClass(), "name" );
		assertEquals( columnMap.get( "job_name" ).getCanonicalName(), "java.lang.String");
		System.out.println(columnMap);
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1);
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
	public void testFindMethodColumnAnnotationsFrom() {
		Map<String, Class> columnMap = new HashMap<String, Class>();
		finder.findMethodColumnAnnotationsFrom( person.getClass(), null, columnMap );
		assertEquals( columnMap.get( "postal_code" ).getCanonicalName(), "java.lang.String" );
		assertEquals( columnMap.get( "job_name" ).getCanonicalName(), "java.lang.String" );
		columnMap.clear();
		finder.findMethodColumnAnnotationsFrom( job.getClass(), "", columnMap );
		assertEquals( columnMap.get( "postal_code" ).getCanonicalName(), "java.lang.String" );
		assertEquals( columnMap.get( "job_name" ).getCanonicalName(), "java.lang.String" );
		columnMap.clear();
		finder.findMethodColumnAnnotationsFrom( job.getClass(), "name" , columnMap );
		assertEquals( columnMap.get( "job_name" ).getCanonicalName(), "java.lang.String");
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1);
		columnMap.clear();
		finder.findMethodColumnAnnotationsFrom( job.getClass(), "zipCode", columnMap );
		assertEquals( columnMap.get( "postal_code" ).getCanonicalName(), "java.lang.String" );
		assertTrue( "expecting size == 1 but found " + columnMap.size(), columnMap.size() == 1);
	}
}

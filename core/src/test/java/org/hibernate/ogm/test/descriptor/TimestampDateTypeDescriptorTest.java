/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.descriptor;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.ogm.type.descriptor.TimestampDateTypeDescriptor;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class TimestampDateTypeDescriptorTest {

	@Test
	public void testSameDatesAreEquals() throws Exception {
		assertThat( TimestampDateTypeDescriptor.INSTANCE.areEqual( createDate( 2 ), createDate( 2 ) ) ).isTrue();
	}

	@Test
	public void testDifferentDatesAreNotEquals() throws Exception {
		assertThat( TimestampDateTypeDescriptor.INSTANCE.areEqual( createDate( 3 ), createDate( 2 ) ) ).isFalse();
	}

	@Test
	public void testNullDatesAreEquals() throws Exception {
		assertThat( TimestampDateTypeDescriptor.INSTANCE.areEqual( null, null ) ).isTrue();
	}

	@Test
	public void testConversionToString() throws Exception {
		assertThat( TimestampDateTypeDescriptor.INSTANCE.toString( createDate( 5 ) ) )
			.matches( "2113/08/05 21:58:39:777.*" );
	}

	private Date createDate(int dayOfMonth) {
		Calendar instance = Calendar.getInstance();
		instance.set( Calendar.DAY_OF_MONTH, dayOfMonth );
		instance.set( Calendar.MONTH, Calendar.AUGUST );
		instance.set( Calendar.YEAR, 2113 );
		instance.set( Calendar.HOUR_OF_DAY, 21 );
		instance.set( Calendar.MINUTE, 58 );
		instance.set( Calendar.SECOND, 39 );
		instance.set( Calendar.MILLISECOND, 777 );
		Date one = instance.getTime();
		return one;
	}
}

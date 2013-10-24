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
package org.hibernate.ogm.type.descriptor;

import java.util.Calendar;
import java.util.Date;

import org.hibernate.type.descriptor.java.DateTypeDescriptor;

/**
 * Converts a {@link Date} into a {@link String} representing a timestamp.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class TimestampDateTypeDescriptor extends DateTypeDescriptor {

	public static final TimestampDateTypeDescriptor INSTANCE = new TimestampDateTypeDescriptor();

	@Override
	public Date fromString(String string) {
		return CalendarTimeZoneDateTimeTypeDescriptor.INSTANCE.fromString( string ).getTime();
	}

	@Override
	public String toString(Date value) {
		Calendar cal = Calendar.getInstance();
		cal.setTime( value );
		return CalendarTimeZoneDateTimeTypeDescriptor.INSTANCE.toString( cal );
	}

	@Override
	public boolean areEqual(Date one, Date another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}

		return one.equals( another );
	}

}

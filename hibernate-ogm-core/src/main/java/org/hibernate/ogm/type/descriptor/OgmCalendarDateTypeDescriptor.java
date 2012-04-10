/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
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
package org.hibernate.ogm.type.descriptor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.hibernate.HibernateException;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.descriptor.java.CalendarDateTypeDescriptor;

/**
 * @author Oliver Carr ocarr@redhat.com
 * 
 * NOTE: This class should probably be placed in hibernate-core
 */
public class OgmCalendarDateTypeDescriptor extends CalendarDateTypeDescriptor {

	private static final Log log = LoggerFactory.make();

	public static final OgmCalendarDateTypeDescriptor INSTANCE = new OgmCalendarDateTypeDescriptor();

	private SimpleDateFormat DATE_TIME_FORMAT;

	public OgmCalendarDateTypeDescriptor() {
		DATE_TIME_FORMAT = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss:SSS Z" );
		DATE_TIME_FORMAT.setLenient( false );
	}

	@Override
	public Calendar fromString(String string) {
		log.info( "OgmCalendar reading: " + string );
		Calendar result = new GregorianCalendar();
		try {
			result.setTime( DATE_TIME_FORMAT.parse( string ) );
		}
		catch ( ParseException pe ) {
			log.error( "OgmCalendar reading failed " + result );
			throw new HibernateException( "could not parse date string" + string, pe );
		}
		log.info( "OgmCalendar reading created " + result );
		return result;
	}

	@Override
	public String toString(Calendar value) {
		log.info( "OgmCalendar formatting: " + value );
		log.info( "OgmCalendar formatted: " + DATE_TIME_FORMAT.format( value.getTime() ) );
		return DATE_TIME_FORMAT.format( value.getTime() );
	}

}

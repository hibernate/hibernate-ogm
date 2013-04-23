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
package org.hibernate.ogm.dialect.couchdb.type.descriptor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.java.DateTypeDescriptor;

/**
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBDateTypeDescriptor extends DateTypeDescriptor {

	public static final CouchDBDateTypeDescriptor INSTANCE = new CouchDBDateTypeDescriptor();

	private static final String DATE_TIME_TIMEZONE_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS Z";

	public String toString(Date value) {
		return new SimpleDateFormat( DATE_TIME_TIMEZONE_FORMAT ).format( value );
	}

	public Date fromString(String string) {
		try {
			return new SimpleDateFormat( DATE_TIME_TIMEZONE_FORMAT ).parse( string );
		}
		catch ( ParseException pe ) {
			throw new HibernateException( "could not parse date string" + string, pe );
		}
	}
}

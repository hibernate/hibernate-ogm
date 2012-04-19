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
package org.hibernate.ogm.test.type;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.type.AbstractGenericBasicType;
import org.hibernate.ogm.type.descriptor.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.GridValueBinder;
import org.hibernate.ogm.type.descriptor.GridValueExtractor;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcDateTypeDescriptor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Convert date into yyMMdd
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CustomDateType extends AbstractGenericBasicType<Date> {

	private static final Log log = LoggerFactory.make();

	public static CustomDateType INSTANCE = new CustomDateType();

	public CustomDateType() {
		super( CustomDateTypeDescriptor.INSTANCE, JdbcDateTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "date";
	}

	static class CustomDateTypeDescriptor implements GridTypeDescriptor {
		public static CustomDateTypeDescriptor INSTANCE = new CustomDateTypeDescriptor();

		@Override
		public <Date> GridValueBinder<Date> getBinder(final JavaTypeDescriptor<Date> javaTypeDescriptor) {
			return new BasicGridBinder<Date>(javaTypeDescriptor, this) {

				@Override
				protected void doBind(Tuple resultset, Date value, String[] names, WrapperOptions options) {
					String stringDate = new SimpleDateFormat( "yyyyMMdd" ).format( value );
					resultset.put( names[0], stringDate );
				}
			};
		}

		@Override
		public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new GridValueExtractor<X>() {
				@Override
				public X extract(Tuple resultset, String name) {
					final String result = (String) resultset.get( name );
					if ( result == null ) {
						log.tracef( "found [null] as column [$s]", name );
						return null;
					}
					else {
						Date date = null;
						try {
							date = new SimpleDateFormat(  "yyyyMMdd" ).parse( result );
						} catch ( ParseException e ) {
							throw new HibernateException("Unable to read date from datastore " + result, e);
						}
						if ( log.isTraceEnabled() ) {
							log.tracef( "found [$s] as column [$s]", result, name );
						}
						return (X) date;
					}
				}
			};
		}
	}
}

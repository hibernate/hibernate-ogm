/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.type;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
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
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class CustomDateType extends AbstractGenericBasicType<Date> {

	public static final CustomDateType INSTANCE = new CustomDateType();

	private static final Log log = LoggerFactory.make();

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
							date = new SimpleDateFormat( "yyyyMMdd" ).parse( result );
						}
						catch ( ParseException e ) {
							throw new HibernateException( "Unable to read date from datastore " + result, e );
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

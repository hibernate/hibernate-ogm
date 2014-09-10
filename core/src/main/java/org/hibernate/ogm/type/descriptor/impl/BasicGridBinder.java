/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import java.util.Arrays;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public abstract class BasicGridBinder<X> implements GridValueBinder<X> {
	private static final Log log = LoggerFactory.make();
	private static final WrapperOptions DEFAULT_OPTIONS = new WrapperOptions() {

		@Override
		public boolean useStreamForLobBinding() {
			return false;
		}

		@Override
		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}

		@Override
		public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
			//OGM dialect don't remap types yet
			return sqlTypeDescriptor;
		}
	};

	private final JavaTypeDescriptor<X> javaDescriptor;
	private final GridTypeDescriptor gridDescriptor;

	public BasicGridBinder(JavaTypeDescriptor<X> javaDescriptor, GridTypeDescriptor gridDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.gridDescriptor = gridDescriptor;
	}

	@Override
	public void bind(Tuple resultset, X value, String[] names) {
		if ( value == null ) {
			for ( String name : names ) {
				log.tracef( "binding [null] to parameter [%1$s]", name );
				resultset.put( name, null );
			}
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.tracef( "binding [%1$s] to parameter(s) %2$s", javaDescriptor.extractLoggableRepresentation( value ), Arrays.toString( names ) );
			}
			doBind( resultset, value, names, DEFAULT_OPTIONS );
		}
	}

	/**
	 * Perform the binding.  Safe to assume that value is not null.
	 *
	 * @param st The prepared statement
	 * @param value The value to bind (not null).
	 * @param index The index at which to bind
	 * @param options The binding options
	 *
	 * @throws java.sql.SQLException Indicates a problem binding to the prepared statement.
	 */
	protected abstract void doBind(Tuple resultset, X value, String[] names, WrapperOptions options);
}

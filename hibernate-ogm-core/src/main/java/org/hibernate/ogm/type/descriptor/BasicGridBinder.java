/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.type.descriptor;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public abstract class BasicGridBinder<X> implements GridValueBinder<X>{
	private static final Logger log = LoggerFactory.getLogger( BasicGridBinder.class );
	private final JavaTypeDescriptor<X> javaDescriptor;
	private final GridTypeDescriptor gridDescriptor;
	private static final WrapperOptions DEFAULT_OPTIONS = new WrapperOptions() {

		@Override
		public boolean useStreamForLobBinding() {
			return false;
		}

		@Override
		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}
	};

	public BasicGridBinder(JavaTypeDescriptor<X> javaDescriptor, GridTypeDescriptor gridDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.gridDescriptor = gridDescriptor;
	}

	@Override
	public void bind(Map<String, Object> resultset, X value, String[] names) {
		if ( value == null ) {
			for ( String name : names ) {
				log.trace( "binding [null] to parameter [{}]", name );
				resultset.put( name, null );
			}
		}
		else {

			log.trace( "binding [{}] to parameter(s) {}", javaDescriptor.extractLoggableRepresentation( value ), Arrays.toString( names ) );
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
	 * @throws SQLException Indicates a problem binding to the prepared statement.
	 */
	protected abstract void doBind(Map<String, Object> resultset, X value, String[] names, WrapperOptions options);
}

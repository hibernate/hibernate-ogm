/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
 * @author Nicolas Helleringer
 */
public abstract class StringMappedGridBinder<X> implements GridValueBinder<X>{
	private static final Logger log = LoggerFactory.getLogger( StringMappedGridBinder.class );
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

	public StringMappedGridBinder(JavaTypeDescriptor<X> javaDescriptor, GridTypeDescriptor gridDescriptor) {
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

	protected abstract void doBind(Map<String, Object> resultset, X value, String[] names, WrapperOptions options);
}

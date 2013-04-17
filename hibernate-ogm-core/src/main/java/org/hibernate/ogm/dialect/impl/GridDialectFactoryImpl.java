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
package org.hibernate.ogm.dialect.impl;

import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.GridDialectLogger;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class GridDialectFactoryImpl implements GridDialectFactory {

	private static final Log log = LoggerFactory.make();

	public GridDialect buildGridDialect(Map configurationValues, ServiceRegistry registry) {
		Object value = configurationValues.get( GRID_DIALECT );
		Class<? extends GridDialect> dialectClass = null;
		if ( value == null ) {
			dialectClass = registry.getService( DatastoreProvider.class ).getDefaultDialect();
		}
		else if ( value instanceof String ) {
			Class<?> maybeDialectClass;
			try {
				maybeDialectClass = registry.getService( ClassLoaderService.class ).classForName( value.toString() );
			}
			catch ( RuntimeException e ) {
				throw log.dialectClassCannotBeFound( value.toString() );
			}
			if ( GridDialect.class.isAssignableFrom( maybeDialectClass ) ) {
				dialectClass = (Class<? extends GridDialect>) maybeDialectClass;
			}
			else {
				throw log.doesNotImplementGridDialect( value.toString() );
			}
		}
		else {
			throw log.gridDialectPropertyOfUnknownType( value.getClass() );
		}
		try {
			// FIXME not sure I like this constructor business. Argue with Sanne
			// to me that's blocking the doors for future enhancements (ie injecting more things)
			// an alternative is to pass the ServiceRegistry verbatim but I'm not sure that's enough either
			Constructor injector = null;
			for ( Constructor constructor : dialectClass.getConstructors() ) {
				Class[] parameterTypes = constructor.getParameterTypes();
				if ( parameterTypes.length == 1 && DatastoreProvider.class.isAssignableFrom( parameterTypes[0] ) ) {
					injector = constructor;
					break;
				}
			}
			if ( injector == null ) {
				log.gridDialectHasNoProperConstrutor( dialectClass );
			}
			GridDialect gridDialect = (GridDialect) injector.newInstance( registry.getService( DatastoreProvider.class ) );
			log.useGridDialect( gridDialect.getClass().getName() );
			if ( GridDialectLogger.activationNeeded() ) {
				gridDialect = new GridDialectLogger( gridDialect );
				log.info( "Grid dialect logs are active" );
			}
			else {
				log.info( "Grid dialect logs are disabled" );
			}
			return gridDialect;
		}
		catch ( Exception e ) {
			throw log.cannotInstantiateGridDialect( dialectClass, e );
		}
	}
}

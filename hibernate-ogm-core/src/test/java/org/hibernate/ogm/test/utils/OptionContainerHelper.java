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
package org.hibernate.ogm.test.utils;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.ogm.options.navigation.impl.MappingContext;
import org.hibernate.ogm.options.navigation.impl.PropertyKey;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContainer;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OptionContainerHelper {

	public static Set<Option<?, ?>> retrieveOptionsFor(MappingContext context) {
		return retrieveOptionsFor( context.getGlobalOptions() );
	}

	public static Set<Option<?, ?>> retrieveOptionsFor(MappingContext context, Class<?> type) {
		return retrieveOptionsFor( context.getOptionsPerEntity().get( type ) );
	}

	public static  Set<Option<?, ?>> retrieveOptionsFor(MappingContext context, Class<?> type, String property) {
		return retrieveOptionsFor( context.getOptionsPerProperty().get( new PropertyKey( type, property ) ) );
	}

	public static Set<Option<?, ?>> retrieveOptionsFor(OptionsContainer globalOptions) {
		Set<Option<?, ?>> options = new HashSet<Option<?,?>>();
		for ( Option<?, ?> option : globalOptions ) {
			options.add( option );
		}
		return options;
	}
}

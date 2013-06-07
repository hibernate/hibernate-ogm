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

import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.mapping.impl.MappingContext;
import org.hibernate.ogm.mapping.impl.OptionsContainer;
import org.hibernate.ogm.options.Option;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OptionContainerHelper {

	public static List<Option> options(MappingContext context) {
		return context.getGlobalOptions().asList();
	}

	public static List<Option> options(MappingContext context, Class<?> clazz) {
		OptionsContainer container = context.getOptionsPerEntity().get( clazz );
		return container.asList();
	}

	public static List<Option> options(MappingContext context, Class<?> clazz, String property) {
		OptionsContainer container = context.getOptionsPerProperty().get( new MappingContext.PropertyKey( clazz, property ) );
		if ( container == null ) {
			return Collections.emptyList();
		}
		else {
			return container.asList();
		}
	}

}

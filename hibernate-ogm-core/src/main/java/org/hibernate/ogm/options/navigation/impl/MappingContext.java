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
package org.hibernate.ogm.options.navigation.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.options.Option;
import org.hibernate.ogm.options.spi.OptionsContainer;

/**
 * Contain all the options set using the mapping API, all the options are separated in different context: global, per entity and per property.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class MappingContext {

	private final OptionsContainer globaloptions = new OptionsContainer();

	private final Map<Class<?>, OptionsContainer> optionsPerEntity = new HashMap<Class<?>, OptionsContainer>();

	private final Map<PropertyKey, OptionsContainer> optionsPerProperty = new HashMap<PropertyKey, OptionsContainer>();

	public void addGlobalOption(Option<?, ?> option) {
		globaloptions.add( option );
	}

	public void addEntityOption(Class<?> klass, Option<?, ?> option) {
		if ( !optionsPerEntity.containsKey( klass ) ) {
			optionsPerEntity.put( klass, new OptionsContainer() );
		}
		optionsPerEntity.get( klass ).add( option );
	}

	public void addPropertyOption(Class<?> klass, String property, Option<?, ?> option) {
		PropertyKey key = new PropertyKey( klass, property );
		if ( !optionsPerProperty.containsKey( key ) ) {
			optionsPerProperty.put( key, new OptionsContainer() );
		}
		optionsPerProperty.get( key ).add( option );
	}

	public OptionsContainer getGlobalOptions() {
		return globaloptions;
	}

	public Map<Class<?>, OptionsContainer> getOptionsPerEntity() {
		return optionsPerEntity;
	}

	public Map<PropertyKey, OptionsContainer> getOptionsPerProperty() {
		return optionsPerProperty;
	}

}

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
package org.hibernate.ogm.options.spi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Container for a group of options. Carries set semantics, i.e. the same option can not be contained more than once in
 * one given container instance.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OptionsContainer implements Iterable<Option<?>> {

	/**
	 * An immutable empty options container. A {@link java.lang.UnsupportedOperationException} will be raised when
	 * adding or removing elements to/from this container.
	 */
	public static final OptionsContainer EMPTY = new OptionsContainer( Collections.<Option<?>>emptySet() );

	private final Set<Option<?>> options;

	public OptionsContainer() {
		this( Collections.synchronizedSet( new HashSet<Option<?>>() ) );
	}

	private OptionsContainer(Set<Option<?>> options) {
		this.options = options;
	}

	@Override
	public Iterator<Option<?>> iterator() {
		return options.iterator();
	}

	/**
	 * Add an {@link Option} to the container.
	 *
	 * @param option to add to the container.
	 */
	public void add(Option<?> option) {
		options.add( option );
	}

	/**
	 * Remove an existing option form the container.
	 *
	 * @param option to remove from the container.
	 */
	public void remove(Option<?> option) {
		options.remove( option );
	}

	/**
	 * Checks if the OptionContainer contains an option.
	 *
	 * @param option to find in the container
	 * @return true if the option is in the container, false otherwise
	 */
	public boolean contains(Option<?> option) {
		return options.contains( option );
	}

}

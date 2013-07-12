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
package org.hibernate.ogm.mapping.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.ogm.options.Option;

/**
 * A container that is always empty.
 * <p>
 * Adding or removing options to this container will have no effect.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
@SuppressWarnings("rawtypes")
public class EmptyOptionsContainer implements OptionsContainer {

	public static final EmptyOptionsContainer INSTANCE = new EmptyOptionsContainer();

	private EmptyOptionsContainer() {
	}

	@Override
	public Iterator<Option> iterator() {
		return EmptyIterator.INSTANCE;
	}

	@Override
	public void add(Option option) {
	}

	@Override
	public void remove(Option option) {
	}

	@Override
	public boolean contains(Option option) {
		return false;
	}

	@Override
	public List<Option> asList() {
		return Collections.emptyList();
	}

	private static class EmptyIterator implements Iterator<Option> {

		private static final EmptyIterator INSTANCE = new EmptyIterator();

		private EmptyIterator() {
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Option next() {
			return null;
		}

		@Override
		public void remove() {
		}

	}
}

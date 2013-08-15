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
package org.hibernate.ogm.options;

/**
 * A configuration value.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public abstract class Option<I, T extends Option<I, T>> {

	private final Class<T> type;

	@SuppressWarnings("unchecked")
	public Option() {
		type = (Class<T>) getClass();
	}

	public abstract I getOptionIdentifier();

	public Class<T> getOptionType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		@SuppressWarnings("unchecked")
		Option<I, T> option = (Option<I, T>) o;

		if ( !getOptionIdentifier().equals( option.getOptionIdentifier() ) ) {
			return false;
		}
		if ( !type.equals( option.type ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + getOptionIdentifier().hashCode();
		return result;
	}

}

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

/**
 * A configuration option describing a generic or datastore-specific setting for which a (set of) value is attached. A
 * setting can apply globally, for a given entity type or for a given entity property.
 * <p/>
 * Options are maintained in {@link OptionsContainer}s and can be unique or non-unique. Unique options are the most
 * common type. An example is "show_query". Non-unique options really represent a family of options differentiated by a
 * key. An example is named query where the identifying key is the query name.
 * <p/>
 * When adding a unique option to a given container several times this option will only be contained exactly once.
 * When in contrast adding a non-unique option several times, all the values set are stored and retrievable from the
 * container.
 * <p/>
 * Unique option types should be derived from {@link UniqueOption}.
 * <p/>
 * The Option implementor defines what it means for a given setting to be unique. This identity is captured by
 * getUniqueIdentifier() which should return the same value if two Option instances represent the same setting.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 * @param <I> The type of this option's identifier
 * @param <V> The type of value associated to the option
 * @see UniqueOption
 * @see OptionsContainer
 */
public abstract class Option<I, V> {

	/**
	 * Returns this option's identifier.
	 *
	 * @return this option's identifier
	 */
	public abstract I getOptionIdentifier();

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Option<?,?> option = (Option<?,?>) o;

		if ( !getOptionIdentifier().equals( option.getOptionIdentifier() ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getClass().hashCode();
		result = 31 * result + getOptionIdentifier().hashCode();
		return result;
	}

}

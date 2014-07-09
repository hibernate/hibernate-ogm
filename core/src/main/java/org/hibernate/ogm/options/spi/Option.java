/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.spi;

import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;

/**
 * A configuration option describing a generic or datastore-specific setting for which a (set of) value is attached. A
 * setting can apply globally, for a given entity type or for a given entity property.
 * <p/>
 * Options are maintained in {@link OptionsContext}s and can be unique or non-unique. Unique options are the most common
 * type. An example is "show_query". Non-unique options really represent a family of options differentiated by a key. An
 * example is named query where the identifying key is the query name.
 * <p/>
 * When adding a unique option to a given container several times this option will only be contained exactly once. When
 * in contrast adding a non-unique option several times, all the values set are stored and retrievable from the
 * container.
 * <p/>
 * Unique option types should be derived from {@link UniqueOption}.
 * <p/>
 * The Option implementor defines what it means for a given setting to be unique. This identity is captured by
 * getUniqueIdentifier() which should return the same value if two Option instances represent the same setting.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 * @param <I> The type of this option's identifier
 * @param <V> The type of value associated to the option
 * @see UniqueOption
 * @see OptionsContext
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

		Option<?, ?> option = (Option<?, ?>) o;

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

	public V getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return null;
	}

}

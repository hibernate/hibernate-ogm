/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.spi;

/**
 * Represents an {@link Option} and its associated value.
 *
 * @author Gunnar Morling
 */
public class OptionValuePair<V> {

	private final Option<?, V> option;
	private final V value;

	private OptionValuePair(Option<?, V> option, V value) {
		this.option = option;
		this.value = value;
	}

	public static <V> OptionValuePair<V> getInstance(Option<?, V> option, V value) {
		return new OptionValuePair<V>( option, value );
	}

	public Option<?, V> getOption() {
		return option;
	}

	public V getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "OptionValue [option=" + option + ", value=" + value + "]";
	}
}

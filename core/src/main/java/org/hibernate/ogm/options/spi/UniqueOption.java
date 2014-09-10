/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.spi;


/**
 * Specialized class to be extended by options that may be defined only once in the context of a given
 * {@link OptionsContext}. Most options should subclass this class.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public abstract class UniqueOption<V> extends Option<Object, V> {

	private static final Object IDENTITY = new Object();

	/**
	 * Return the identifier of this option.
	 * <p>
	 * Since two instances of the same {@link UniqueOption} must be unique this method cannot be overridden.
	 */
	@Override
	public final Object getOptionIdentifier() {
		return IDENTITY;
	}

}

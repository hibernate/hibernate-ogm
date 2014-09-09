/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.configurationreader.impl;

/**
 * Implementations instantiate given classes. By default an implementation invoking the no-args constructor of the
 * given type is used.
 *
 * @author Gunnar Morling
 */
public interface Instantiator<T> {

	T newInstance(Class<? extends T> clazz);
}

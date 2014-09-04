/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.configurationreader.spi;

import org.hibernate.HibernateException;

/**
 * Implementations validate given property values.
 *
 * @author Gunnar Morling
 */
public interface PropertyValidator<T> {

	/**
	 * Validates the given property value.
	 *
	 * @param value the value to validate
	 * @throws HibernateException in case the given property value is not valid
	 */
	void validate(T value) throws HibernateException;
}

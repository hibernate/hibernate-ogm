/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.configurationreader.impl;

import org.hibernate.HibernateException;
import org.hibernate.ogm.util.configurationreader.spi.PropertyValidator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Provides common property implementations.
 *
 * @author Gunnar Morling
 */
public class Validators {

	/**
	 * A {@link PropertyValidator} which asserts that a given number is a valid port number.
	 */
	public static final PropertyValidator<Integer> PORT = new PropertyValidator<Integer>() {

		@Override
		public void validate(Integer value) throws HibernateException {
			if ( value < 1 || value > 65535 ) {
				throw log.illegalPortValue( value );
			}
		}
	};

	private static final Log log = LoggerFactory.make();

	private Validators() {
	};
}

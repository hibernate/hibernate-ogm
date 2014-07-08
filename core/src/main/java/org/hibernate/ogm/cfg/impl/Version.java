/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.impl;

import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Display the version number on touch
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class Version {

	/**
	 * Returns the current version of Hibernate OGM.
	 */
	public static String getVersionString() {
		// The actual value will be injected into the class file during the build
		return "[WORKING]";
	}

	static {
		LoggerFactory.make().version( getVersionString() );
	}

	public static void touch() {
	}
}

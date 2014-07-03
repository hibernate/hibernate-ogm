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

	public static String getVersionString() {
		return "4.1.0-SNAPSHOT";
	}

	static {
		LoggerFactory.make().version( getVersionString() );
	}

	public static void touch() {
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.transaction.impl;

public enum ApplicationServer {
	WEBSPHERE, JBOSS, DEFAULT;

	private static final String IBM_CLASSLOADER_PACKAGE_PART = "ibm";
	private static final String JBOSS_CLASSLOADER_PACKAGE_PART = "jboss";

	private static boolean isWebSphere() {
		return ApplicationServer.class.getClassLoader().getClass().getName().contains( IBM_CLASSLOADER_PACKAGE_PART );
	}

	private static boolean isJBoss() {
		return ApplicationServer.class.getClassLoader().getClass().getName().contains( JBOSS_CLASSLOADER_PACKAGE_PART );
	}

	public static ApplicationServer currentApplicationServer() {
		if ( isWebSphere() ) {
			return WEBSPHERE;
		}
		else if ( isJBoss() ) {
			return JBOSS;
		}
		return DEFAULT;
	}
}

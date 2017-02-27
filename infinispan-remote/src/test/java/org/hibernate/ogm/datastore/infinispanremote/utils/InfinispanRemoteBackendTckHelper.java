/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.utils;

import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Helper class allowing you to run all or any specified subset of test available on the classpath.
 *
 * This method is for example useful to run all or parts of the <i>backendtck</i>.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({ ".*CollectionUnidirectionalTest" })
public class InfinispanRemoteBackendTckHelper {

	@ClassRule
	public static RemoteHotRodServerRule hotrodServer = new RemoteHotRodServerRule();

	/**
	 * Useful to occasionally start the Hot Rod server explicitly
	 * and leave it running in background
	 */
	public static void main(String[] args) throws Throwable {
		RemoteHotRodServerRule manualServerStart = new RemoteHotRodServerRule();
		manualServerStart.before();
	}

}

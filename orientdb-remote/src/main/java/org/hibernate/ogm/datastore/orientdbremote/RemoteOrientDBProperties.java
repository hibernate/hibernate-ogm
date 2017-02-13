/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdbremote;

import org.hibernate.ogm.datastore.orientdb.OrientDBProperties;

/**
 * Own properties of Remote OrientDB Database Provider
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class RemoteOrientDBProperties extends OrientDBProperties {

	/**
	 * Property for setting the user name to connect with. Accepts {@code String}.
	 */
	public static final String ROOT_USERNAME = "hibernate.ogm.orientdb.remote.root.username";

	/**
	 * Property for setting the password to connect with. Accepts {@code String}.
	 */
	public static final String ROOT_PASSWORD = "hibernate.ogm.orientdb.remote.root.password";
}

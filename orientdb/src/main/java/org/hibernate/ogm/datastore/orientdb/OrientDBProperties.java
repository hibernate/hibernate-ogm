/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb;

import org.hibernate.ogm.cfg.OgmProperties;

/**
 * Own properties of OrientDB Database Provider
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBProperties implements OgmProperties {

	/**
	 * Format of datetime. Default value 'yyyy-MM-dd HH:mm:ss z'
	 */
	public static final String DATETIME_FORMAT = "hibernate.ogm.orientdb.format.datetime";

	/**
	 * Format of date. Default value 'yyyy-MM-dd'
	 */
	public static final String DATE_FORMAT = "hibernate.ogm.orientdb.format.date";

	/**
	 * Type of database.
	 *
	 * @see DatabaseTypeEnum
	 */
	public static final String DATEBASE_TYPE = "hibernate.ogm.orientdb.dbtype";

	/**
	 * Type of storage.
	 *
	 * @see StorageModeEnum
	 */
	public static final String STORAGE_MODE_TYPE = "hibernate.ogm.orientdb.storage";

	/**
	 * Database pool size
	 *
	 * @see StorageModeEnum
	 */
	public static final String POOL_SIZE = "hibernate.ogm.orientdb.pool.size";

	/**
	 * Property for setting the file path to database. Accepts {@code String}.
	 */
	public static final String PLOCAL_PATH = "hibernate.ogm.orientdb.plocal.path";

	/**
	 * Enumeration of database's types
	 *
	 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
	 */
	public static enum StorageModeEnum {
		MEMORY, PLOCAL, REMOTE
	}

	public static enum DatabaseTypeEnum {
		DOCUMENT, GRAPH;
	}
}

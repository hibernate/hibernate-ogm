/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.utils;

import org.apache.log4j.BasicConfigurator;
import static org.hibernate.datastore.ogm.orientdb.OrientDBSimpleTest.MEMORY_TEST;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Helper class allowing you to run all or any specified subset of test available on the classpath. This method is for
 * example useful to run all or parts of the <i>backendtck</i>.
 *
 * @author Hardy Ferentschik
 */
@RunWith(ClasspathSuite.class)
// @ClasspathSuite.ClassnameFilters({ "org.hibernate.ogm.backendtck.*" })
@ClasspathSuite.ClassnameFilters({ ".*BuiltInTypeTest" })
public class OrientDBBackendTckHelper {
        @BeforeClass
	public static void setUpClass() {
		MemoryDBUtil.createDbFactory(MEMORY_TEST);
		BasicConfigurator.configure();
	}
    

	@AfterClass
	public static void tearDownClass() {
		MemoryDBUtil.getOrientGraphFactory().drop();
                MemoryDBUtil.createDbFactory(MEMORY_TEST);
                MemoryDBUtil.getOrientGraphFactory().close();
	}
}

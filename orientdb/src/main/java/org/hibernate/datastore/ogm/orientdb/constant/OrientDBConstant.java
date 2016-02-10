/*
* Hibernate OGM, Domain model persistence for NoSQL datastores
* 
* License: GNU Lesser General Public License (LGPL), version 2.1 or later
* See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
*/

package org.hibernate.datastore.ogm.orientdb.constant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */

public class OrientDBConstant {

	public static final String SYSTEM_VERSION = "@version";
	public static final String SYSTEM_RID = "@rid";

	public static final Set<String> SYSTEM_FIELDS;

	static {
		Set<String> set = new HashSet<>();
		set.add( SYSTEM_RID );
		set.add( SYSTEM_VERSION );
		SYSTEM_FIELDS = Collections.unmodifiableSet( set );
	}
}

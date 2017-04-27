/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.orientdb.bridge;

import com.orientechnologies.orient.core.id.ORecordId;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class ORecordIdTwoWayStringBridge implements TwoWayStringBridge {

	@Override
	public Object stringToObject(String string) {
		return new ORecordId( string );
	}

	@Override
	public String objectToString(Object o) {
		ORecordId id = (ORecordId) o;
		return id.toString();
	}

}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils.test.associations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ignite.binary.BinaryObject;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class JUGBinaryStore extends AbstractHashMapBinaryStore {
	public static final Map<String, BinaryObject> store = Collections.synchronizedMap( new HashMap<>() );

	@Override
	protected Map<String, BinaryObject> getStore() {
		return store;
	}
}

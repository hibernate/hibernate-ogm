/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import org.hibernate.type.BinaryType;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class BytesAsBinaryType extends BinaryType {

	public static final String TYPE_NAME = "binary_bytes";
	@Override
	public String getName() {
		return TYPE_NAME;
	}
}

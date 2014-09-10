/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import org.hibernate.ogm.model.key.spi.RowKey;


/**
 * Operation applied to the association.
 * A RowKey is provided and when it makes sense a Tuple
 * (eg DELETE or PUT_NULL do not have tuples)
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class AssociationOperation {
	private final RowKey key;
	private final Tuple value;
	private final AssociationOperationType type;

	public AssociationOperation(RowKey key, Tuple value, AssociationOperationType type) {
		this.key = key;
		this.value = value;
		this.type = type;
	}

	public RowKey getKey() {
		return key;
	}

	public Tuple getValue() {
		return value;
	}

	public AssociationOperationType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "AssociationOperation [key=" + key + ", type=" + type + "]";
	}
}

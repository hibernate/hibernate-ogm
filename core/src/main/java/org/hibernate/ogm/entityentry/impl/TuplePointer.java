/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.entityentry.impl;

import org.hibernate.ogm.model.spi.Tuple;

/**
 * A pointer to a {@link Tuple}.
 *
 * @author Guillaume Smet
 */
public class TuplePointer {

	private Tuple tuple;

	public TuplePointer() {
	}

	public TuplePointer(Tuple tuple) {
		this.tuple = tuple;
	}

	public void setTuple(Tuple tuple) {
		this.tuple = tuple;
	}

	public Tuple getTuple() {
		return tuple;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "[" );
		sb.append( tuple );
		sb.append( "]" );
		return sb.toString();
	}

}

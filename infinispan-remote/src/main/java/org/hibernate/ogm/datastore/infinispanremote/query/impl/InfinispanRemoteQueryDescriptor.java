/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Describes a query to be executed against Infinispan Server.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteQueryDescriptor implements Serializable {

	private final String cache;
	private final String text;
	private final List<String> projections;

	public InfinispanRemoteQueryDescriptor(String cache, String text, List<String> projections) {
		this.cache = cache;
		this.text = text;
		this.projections = projections;
	}

	public String getCache() {
		return cache;
	}

	public String getText() {
		return text;
	}

	public List<String> getProjections() {
		return projections;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		InfinispanRemoteQueryDescriptor that = (InfinispanRemoteQueryDescriptor) o;
		return Objects.equals( cache, that.cache ) &&
				Objects.equals( text, that.text ) &&
				Objects.equals( projections, that.projections );
	}

	@Override
	public int hashCode() {
		return Objects.hash( cache, text, projections );
	}

	@Override
	public String toString() {
		return "InfinispanRemoteQueryDescriptor{" +
				"cache='" + cache + '\'' +
				", text='" + text + '\'' +
				", projections=" + projections +
				'}';
	}
}

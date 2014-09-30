/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import javax.persistence.Embeddable;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;

@Embeddable
public class AnotherEmbeddable {
	// Store.YES for filtering in query
	// Analyze.NO for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	String embeddedString;

	public AnotherEmbeddable() {
	}

	public AnotherEmbeddable(String embeddedString) {
		this.embeddedString = embeddedString;
	}

	public String getEmbeddedString() {
		return embeddedString;
	}

	public void setEmbeddedString(String embeddedString) {
		this.embeddedString = embeddedString;
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

@Embeddable
public class AnEmbeddable {
	// Store.YES for filtering in query
	// Analyze.NO for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	private String embeddedString;

	@Embedded
	@IndexedEmbedded
	private AnotherEmbeddable anotherEmbeddable;

	@ElementCollection
	@IndexedEmbedded
	private List<AnotherEmbeddable> anotherCollection;

	public AnEmbeddable() {
	}

	public AnEmbeddable(String embeddedString) {
		this.embeddedString = embeddedString;
	}

	public AnEmbeddable(String embeddedString, AnotherEmbeddable anotherEmbeddable) {
		this.embeddedString = embeddedString;
		this.anotherEmbeddable = anotherEmbeddable;
	}

	public String getEmbeddedString() {
		return embeddedString;
	}

	public void setEmbeddedString(String embeddedString) {
		this.embeddedString = embeddedString;
	}

	public AnotherEmbeddable getAnotherEmbeddable() {
		return anotherEmbeddable;
	}

	public void setAnotherEmbeddable(AnotherEmbeddable anotherEmbeddable) {
		this.anotherEmbeddable = anotherEmbeddable;
	}

	public List<AnotherEmbeddable> getAnotherCollection() {
		return anotherCollection;
	}

	public void setAnotherCollection(List<AnotherEmbeddable> anotherCollection) {
		this.anotherCollection = anotherCollection;
	}
}

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
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

@Embeddable
public class EmbeddedCollectionItem {

	// Store.YES for filtering in query
	// Analyze.NO for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	private String item;

	@Field(store = Store.YES, analyze = Analyze.NO)
	private String anotherItem;

	@IndexedEmbedded
	private AnotherEmbeddable embedded;

	public EmbeddedCollectionItem() {
	}

	public EmbeddedCollectionItem(String embeddedString, String anotherItem, AnotherEmbeddable anotherEmbeddable) {
		this.item = embeddedString;
		this.anotherItem = anotherItem;
		this.embedded = anotherEmbeddable;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String embeddedString) {
		this.item = embeddedString;
	}

	public AnotherEmbeddable getEmbedded() {
		return embedded;
	}

	public void setEmbedded(AnotherEmbeddable anotherEmbeddable) {
		this.embedded = anotherEmbeddable;
	}

	public String getAnotherItem() {
		return anotherItem;
	}

	public void setAnotherItem(String anotherItem) {
		this.anotherItem = anotherItem;
	}
}

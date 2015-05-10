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
public class OptionalStoryBranch {

	// Store.YES for filtering in query
	// Analyze.NO for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	private String evilText;

	@Field(store = Store.YES, analyze = Analyze.NO)
	private String goodText;

	@IndexedEmbedded
	private Ending evilEnding;

	@IndexedEmbedded
	private Ending goodEnding;

	public OptionalStoryBranch() {
	}

	public OptionalStoryBranch(String evilText, String goodText, Ending evilEnding) {
		this.evilText = evilText;
		this.goodText = goodText;
		this.evilEnding = evilEnding;
	}

	public String getEvilText() {
		return evilText;
	}

	public void setEvilText(String embeddedString) {
		this.evilText = embeddedString;
	}

	public Ending getEvilEnding() {
		return evilEnding;
	}

	public void setEvilEnding(Ending anotherEmbeddable) {
		this.evilEnding = anotherEmbeddable;
	}

	public String getGoodText() {
		return goodText;
	}

	public void setGoodText(String anotherItem) {
		this.goodText = anotherItem;
	}

	public Ending getGoodEnding() {
		return goodEnding;
	}

	public void setGoodEnding(Ending goodEnding) {
		this.goodEnding = goodEnding;
	}
}

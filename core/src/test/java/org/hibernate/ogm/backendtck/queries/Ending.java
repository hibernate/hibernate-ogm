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
public class Ending {

	// Store.YES for filtering in query
	// Analyze.NO for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	private String text;

	// Store.YES for filtering in query
	// Analyze.NO for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	private Integer score;

	public Ending() {
	}

	public Ending(String text, Integer score) {
		this.text = text;
		this.score = score;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}
}

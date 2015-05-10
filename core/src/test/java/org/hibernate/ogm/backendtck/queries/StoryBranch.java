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
public class StoryBranch {
	// Store.YES for filtering in query
	// Analyze.NO for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	private String storyText;

	@Embedded
	@IndexedEmbedded
	private Ending ending;

	@ElementCollection
	@IndexedEmbedded
	private List<Ending> additionalEndings;

	public StoryBranch() {
	}

	public StoryBranch(String embeddedString) {
		this.storyText = embeddedString;
	}

	public StoryBranch(String storyText, Ending endBranch) {
		this.storyText = storyText;
		this.ending = endBranch;
	}

	public String getStoryText() {
		return storyText;
	}

	public void setStoryText(String storyText) {
		this.storyText = storyText;
	}

	public Ending getEnding() {
		return ending;
	}

	public void setEnding(Ending anotherEmbeddable) {
		this.ending = anotherEmbeddable;
	}

	public List<Ending> getAdditionalEndings() {
		return additionalEndings;
	}

	public void setAdditionalEndings(List<Ending> anotherCollection) {
		this.additionalEndings = anotherCollection;
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.model;

import java.util.Date;

import javax.persistence.Embeddable;

/**
 * @author Gunnar Morling
 */
@Embeddable
public class ResearchPaper {

	private String title;
	private Date published;
	private int wordCount;

	ResearchPaper() {
	}

	public ResearchPaper(String title, Date published, int wordCount) {
		this.title = title;
		this.published = published;
		this.wordCount = wordCount;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getPublished() {
		return published;
	}

	public void setPublished(Date published) {
		this.published = published;
	}

	public int getWordCount() {
		return wordCount;
	}

	public void setWordCount(int wordCount) {
		this.wordCount = wordCount;
	}
}

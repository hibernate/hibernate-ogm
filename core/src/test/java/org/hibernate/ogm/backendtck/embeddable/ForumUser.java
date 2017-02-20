/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * An Hibernate forum user that has contributed to an issue on JIRA.
 *
 * @author Davide D'Alto
 */
@Entity
@Table(name = ForumUser.LABEL)
public class ForumUser {

	public static final String LABEL = "FORUM_USER";

	@Id
	private String id;

	@ElementCollection
	private Map<String, JiraIssue> issues = new HashMap<>();

	public ForumUser() {
	}

	public ForumUser(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, JiraIssue> getIssues() {
		return issues;
	}

	public void setIssues(Map<String, JiraIssue> issues) {
		this.issues = issues;
	}
}

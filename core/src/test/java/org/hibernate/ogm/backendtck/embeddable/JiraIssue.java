/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import javax.persistence.Embeddable;
import javax.persistence.Table;

/**
 * Identify an issue on JIRA.
 * <p>
 * Example: OGM-1243
 *
 * @author Davide D'Alto
 */
@Embeddable
@Table(name = JiraIssue.LABEL)
public class JiraIssue {

	public static final String LABEL = "JIRA_ISSUE";

	private Integer number;
	private String project;

	public JiraIssue() {
	}

	public JiraIssue(Integer number, String project) {
		this.number = number;
		this.project = project;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( number == null ) ? 0 : number.hashCode() );
		result = prime * result + ( ( project == null ) ? 0 : project.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		JiraIssue other = (JiraIssue) obj;
		if ( number == null ) {
			if ( other.number != null ) {
				return false;
			}
		}
		else if ( !number.equals( other.number ) ) {
			return false;
		}
		if ( project == null ) {
			if ( other.project != null ) {
				return false;
			}
		}
		else if ( !project.equals( other.project ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return project + "-" + number;
	}
}

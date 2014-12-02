/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;

/**
 * @author Gunnar Morling
 */
@Entity
public class ScientistWithSequence {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scientist_seq")
	@SequenceGenerator(name = "scientist_seq", allocationSize=100)
	private Long id;

	private String name;
	private Date dob;
	private String bio;

	@ElementCollection
	private List<ResearchPaper> publishedPapers = new ArrayList<ResearchPaper>();

	@ManyToMany
	private List<FieldOfScience> interestedIn = new ArrayList<FieldOfScience>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public List<ResearchPaper> getPublishedPapers() {
		return publishedPapers;
	}

	public void setPublishedPapers(List<ResearchPaper> publishedPapers) {
		this.publishedPapers = publishedPapers;
	}

	public List<FieldOfScience> getInterestedIn() {
		return interestedIn;
	}

	public void setInterestedIn(List<FieldOfScience> interestedIn) {
		this.interestedIn = interestedIn;
	}
}

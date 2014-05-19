/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.writeconcern;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorage;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcern;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;

/**
 * @author Gunnar Morling
 */
@Entity
@WriteConcern(WriteConcernType.MAJORITY)
public class GolfPlayer {

	private long id;
	private String name;
	private double handicap;
	private List<GolfCourse> playedCourses;

	public GolfPlayer(long id, String name, double handicap, GolfCourse... playedCourses) {
		this.id = id;
		this.name = name;
		this.handicap = handicap;
		this.playedCourses = playedCourses != null ? Arrays.asList( playedCourses ) : Collections.<GolfCourse>emptyList();
	}

	public GolfPlayer() {
	}

	@Id
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getHandicap() {
		return handicap;
	}

	public void setHandicap(double handicap) {
		this.handicap = handicap;
	}

	@OneToMany
	@AssociationDocumentStorage(AssociationDocumentType.GLOBAL_COLLECTION)
	public List<GolfCourse> getPlayedCourses() {
		return playedCourses;
	}

	public void setPlayedCourses(List<GolfCourse> playedCourses) {
		this.playedCourses = playedCourses;
	}

	@Override
	public String toString() {
		return "GolfPlayer [id=" + id + ", name=" + name + ", handicap=" + handicap + ", playedCourses=" + playedCourses + "]";
	}
}

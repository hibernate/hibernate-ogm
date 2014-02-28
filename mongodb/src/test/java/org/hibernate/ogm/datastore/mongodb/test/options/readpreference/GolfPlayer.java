/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readpreference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.ogm.datastore.mongodb.options.ReadPreference;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;

/**
 * @author Gunnar Morling
 */
@Entity
@ReadPreference(ReadPreferenceType.SECONDARY_PREFERRED)
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
	@ReadPreference(ReadPreferenceType.PRIMARY_PREFERRED)
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

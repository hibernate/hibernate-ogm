/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.perf;

import javax.persistence.*;
import java.util.Set;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class Blog {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="blog_seq")
	@SequenceGenerator(name="blog_seq")
	public Integer getId() { return id; }
	public void setId(Integer id) {  this.id = id; }
	private Integer id;

	public String getTitle() { return title; }
	public void setTitle(String title) {  this.title = title; }
	private String title;

	public String getDescription() { return description; }
	public void setDescription(String description) {  this.description = description; }
	private String description;

	@OneToMany(mappedBy = "blog")
	public Set<BlogEntry> getEntries() { return entries; }
	public void setEntries(Set<BlogEntry> entries) {  this.entries = entries; }
	private Set<BlogEntry> entries;
}

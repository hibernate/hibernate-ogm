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

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class BlogEntry {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator="blog_seq")
	@SequenceGenerator(name="blog_seq")
	public Long getId() { return id; }
	public void setId(Long id) {  this.id = id; }
	private Long id;

	public String getTitle() { return title; }
	public void setTitle(String title) {  this.title = title; }
	private String title;

	@ManyToOne
	public Author getAuthor() { return author; }
	public void setAuthor(Author author) {  this.author = author; }
	private Author author;

	public String getContent() { return content; }
	public void setContent(String content) {  this.content = content; }
	private String content;

	@ManyToOne
	public Blog getBlog() { return blog; }
	public void setBlog(Blog blog) {  this.blog = blog; }
	private Blog blog;
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;

@Entity
@Table(name = OscarWildePoem.TABLE_NAME)
@NamedNativeQuery(name = "AthanasiaQuery", query = "MATCH ( n:" + OscarWildePoem.TABLE_NAME + " { name:'Athanasia', author:'Oscar Wilde' } ) RETURN n", resultClass = OscarWildePoem.class )
class OscarWildePoem {

	public static final String TABLE_NAME = "WILDE_POEM";

	private Long id;

	private String name;

	private String author;

	private Date dateOfCreation;

	public OscarWildePoem() {
	}

	public OscarWildePoem(Long id, String name, String author, Date dateOfCreation) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.dateOfCreation = dateOfCreation;
	}

	@Id
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

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getDateOfCreation() {
		return dateOfCreation;
	}

	public void setDateOfCreation(Date dateOfCreation) {
		this.dateOfCreation = dateOfCreation;
	}

	@Override
	public String toString() {
		return "OscarWildePoem [id=" + id + ", name=" + name + ", author=" + author + "]";
	}

}

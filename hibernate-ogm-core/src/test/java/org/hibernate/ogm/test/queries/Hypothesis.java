/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.queries;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Hypothesis {
	private String id;
	private String description;
	private int position;
	private Date date;
	private Author author;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "pos")
	@Field(analyze = Analyze.NO)
	@NumericField
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Field(analyze = Analyze.NO)
	@DateBridge(resolution=Resolution.DAY)
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@OneToOne( cascade = { CascadeType.PERSIST, CascadeType.REMOVE } )
	@IndexedEmbedded
	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	@Override
	public String toString() {
		return "Hypothesis [id=" + id + ", description=" + description + ", position=" + position  + ", date=" + date + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( date == null ) ? 0 : date.hashCode() );
		result = prime * result + ( ( description == null ) ? 0 : description.hashCode() );
		result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
		result = prime * result + position;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		Hypothesis other = (Hypothesis) obj;
		if ( date == null ) {
			if ( other.date != null )
				return false;
		}
		else if ( !date.equals( other.date ) )
			return false;
		if ( description == null ) {
			if ( other.description != null )
				return false;
		}
		else if ( !description.equals( other.description ) )
			return false;
		if ( id == null ) {
			if ( other.id != null )
				return false;
		}
		else if ( !id.equals( other.id ) )
			return false;
		if ( position != other.position )
			return false;
		return true;
	}
}

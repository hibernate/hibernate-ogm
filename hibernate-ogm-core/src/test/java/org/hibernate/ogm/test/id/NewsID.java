/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.id;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
@Embeddable
public class NewsID implements Serializable {

	public NewsID() { super(); }

	public NewsID(String title, String author) {
		this.title = title;
		this.author = author;
	}

	private String title;
	private String author;

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public String getAuthor() {	return author; }
	public void setAuthor(String author) { this.author = author; }

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		NewsID newsID = (NewsID) o;

		if ( author != null ? !author.equals( newsID.author ) : newsID.author != null ) {
			return false;
		}
		if ( title != null ? !title.equals( newsID.title ) : newsID.title != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = title != null ? title.hashCode() : 0;
		result = 31 * result + ( author != null ? author.hashCode() : 0 );
		return result;
	}
}

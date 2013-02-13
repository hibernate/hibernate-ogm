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

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToMany;

/**
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
@Entity
public class News {

	@EmbeddedId
	private NewsID newsId;
	private String content;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumns({ @JoinColumn(name = "news_topic_fk", referencedColumnName = "newsid.title", nullable = false),
			@JoinColumn(name = "news_author_fk", referencedColumnName = "newsid.author", nullable = false) })
	private List<Label> labels;

	public News() {
	}

	public News(NewsID newsId, String content, List<Label> labels) {
		this.newsId = newsId;
		this.content = content;
		this.labels = labels;
	}

	public NewsID getNewsId() {
		return newsId;
	}

	public void setNewsId(NewsID newsId) {
		this.newsId = newsId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<Label> getLabels() {
		return labels;
	}

	public void setLabels(List<Label> labels) {
		this.labels = labels;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		News news = (News) o;

		if ( content != null ? !content.equals( news.content ) : news.content != null ) {
			return false;
		}
		if ( labels != null ? !labels.equals( news.labels ) : news.labels != null ) {
			return false;
		}
		if ( newsId != null ? !newsId.equals( news.newsId ) : news.newsId != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = newsId != null ? newsId.hashCode() : 0;
		result = 31 * result + ( content != null ? content.hashCode() : 0 );
		result = 31 * result + ( labels != null ? labels.hashCode() : 0 );
		return result;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2012 Red Hat Inc. and/or its affiliates and other contributors
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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
@Entity
public class News {

	public News() {
		super();
	}

	public News(NewsID newsId, String content) {
		this.newsId = newsId;
		this.content = content;
	}

	@EmbeddedId
	private NewsID newsId;

	public NewsID getNewsId() {
		return newsId;
	}

	public void setNewsId(NewsID newsId) {
		this.newsId = newsId;
	}

	private String content;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}

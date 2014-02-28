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
package org.hibernate.ogm.backendtck.massindex.model;

import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * This is a simple implementation used by some tests; it's not supposed to be an example on how to implement a
 * {@link TwoWayStringBridge}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class NewsIdFieldBridge implements TwoWayStringBridge {

	private static final String SEP = "::::";

	@Override
	public String objectToString(Object object) {
		NewsID newsId = (NewsID) object;
		return newsId.getTitle() + SEP + newsId.getAuthor();
	}

	@Override
	public Object stringToObject(String stringValue) {
		String[] split = stringValue.split( SEP );
		return new NewsID( split[0], split[1] );
	}

}

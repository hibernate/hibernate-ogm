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
package org.hibernate.ogm.dialect.couchdb.designdocument;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBDocument;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a CouchDB Design Documents
 *
 * It's serialized to Json in order to create a CouchDB Design Documents
 *
 * A Design Document contains a set of View.
 *
 * Each View has a name, a Javascript map function and an optional JavaScript reduce function
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBDesignDocument extends CouchDBDocument {

	private Map<String, View> views = new HashMap<String, View>();

	private final String language = "javascript";

	public Map<String, View> getViews() {
		return views;
	}

	public void setViews(Map<String, View> views) {
		this.views = views;
	}

	public String getLanguage() {
		return language;
	}

	/**
	 * Adds View with bot map and reduce functions to the Document.
	 *
	 * @param viewName
	 *            the name of the View
	 * @param map
	 *            a String representing the JavaScript map function code
	 * @param reduce
	 *            a String representing the JavaScript reduce function code
	 */
	public void addView(String viewName, String map, String reduce) {
		View view = new View();
		view.setMap( map );
		view.setReduce( reduce );
		addView( viewName, view );
	}

	/**
	 * Adds View with only the map function to the Document.
	 *
	 * @param viewName
	 *            the name of the View
	 * @param map
	 *            a String representing the JavaScript map function code
	 */
	public void addView(String viewName, String map) {
		View view = new View();
		view.setMap( map );
		addView( viewName, view );
	}

	private void addView(String viewName, View view) {
		views.put( viewName, view );
	}

	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	public class View {
		private String map;
		private String reduce;

		public String getMap() {
			return map;
		}

		public void setMap(String map) {
			this.map = map;
		}

		public String getReduce() {
			return reduce;
		}

		public void setReduce(String reduce) {
			this.reduce = reduce;
		}

	}
}

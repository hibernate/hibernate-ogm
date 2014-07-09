/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents a CouchDB design document. Design documents are special CouchDB documents containing application logic in
 * form of JavaScript, more specifically views (which apply map/reduce routines) and lists (which render other documents
 * or views).
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
public class DesignDocument extends Document {

	private Map<String, View> views = new HashMap<String, View>();
	private Map<String, String> lists = new HashMap<String, String>();
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

	public Map<String, String> getLists() {
		return lists;
	}

	public void setLists(Map<String, String> lists) {
		this.lists = lists;
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

	/**
	 * Adds the given list function to this design document.
	 *
	 * @param name the name of the function
	 * @param listFunction the JavaScript code of the function
	 */
	public void addList(String name, String listFunction) {
		lists.put( name, listFunction );
	}

	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	public static class View {
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

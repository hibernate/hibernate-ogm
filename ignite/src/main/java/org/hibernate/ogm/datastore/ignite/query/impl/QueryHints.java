/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class QueryHints {

	public static final String HINT_LOCAL_QUERY = "local";
	public static final String HINT_AFFINITY_QUERY = "affinityKey";
	public static final String HINT_COLLOCATED = "collocated";
	public static final String HINT_SEPARATOR = ",";

	private final boolean local;
	private final boolean affinityRun;
	private final Object affinityKey;
	private final boolean collocated;

	public QueryHints() {
		local = false;
		affinityRun = false;
		affinityKey = null;
		collocated = false;
	}

	public QueryHints(List<String> hints) {
		boolean lcl = false;
		boolean affRun = false;
		Object affKey = null;
		boolean clc = false;

		if ( hints != null ) {
			for ( String h : hints ) {
				int index = h.indexOf( '=' );
				String key = StringUtils.trim( ( index > 0 ) ? h.substring( 0, index ) : h );
				if ( key.equalsIgnoreCase( HINT_LOCAL_QUERY ) ) {
					lcl = true;
				}
				else if ( key.equalsIgnoreCase( HINT_AFFINITY_QUERY ) ) {
					affRun = true;
					affKey = StringUtils.trim( h.substring( index ) );
				}
				else if ( key.equalsIgnoreCase( HINT_COLLOCATED ) ) {
					clc = true;
				}
			}
		}

		this.local = lcl;
		this.affinityRun = affRun;
		this.affinityKey = affKey;
		this.collocated = clc;
	}

	public boolean isLocal() {
		return local;
	}

	public boolean isAffinityRun() {
		return affinityRun;
	}

	public Object getAffinityKey() {
		return affinityKey;
	}

	public boolean isCollocated() {
		return collocated;
	}
}

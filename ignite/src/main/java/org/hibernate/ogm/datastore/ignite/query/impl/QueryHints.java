/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;

/**
 * Hints for executing Ignite queries
 *
 * @author Victor Kadachigov
 */
public class QueryHints {

	public static final String HINT_LOCAL_QUERY = "local";
	public static final String HINT_AFFINITY_QUERY = "affinityKey";
	public static final String HINT_SEPARATOR = ",";

	private final boolean local;
	private final boolean affinityRun;
	private final Object affinityKey;

	private QueryHints(boolean local, boolean affinityRun, Object affinityKey) {
		this.local = local;
		this.affinityRun = affinityRun;
		this.affinityKey = affinityKey;
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

	public boolean isEmpty() {
		return !local && !affinityRun;
	}

	public String toComment() {
		if ( isEmpty() ) {
			return "";
		}

		StringBuilder sb = new StringBuilder( "/* Hints: " );
		if ( local ) {
			sb.append( "local " );
		}
		if ( affinityRun ) {
			sb.append( "affinityRun( key: " ).append( affinityKey ).append( " ) " );
		}
		sb.append( "*/ " );
		return sb.toString();
	}

	public static class Builder {

		private boolean local;
		private boolean affinityRun;
		private Object affinityKey;

		public Builder() {
		}

		public Builder(List<String> hints) {
			if ( hints != null ) {
				for ( String h : hints ) {
					int index = h.indexOf( '=' );
					String key = StringUtils.trim( ( index > 0 ) ? h.substring( 0, index ) : h );
					if ( key.equalsIgnoreCase( HINT_LOCAL_QUERY ) ) {
						this.local = true;
					}
					else if ( key.equalsIgnoreCase( HINT_AFFINITY_QUERY ) ) {
						this.affinityRun = true;
						this.affinityKey = StringUtils.trim( h.substring( index + 1 ) );
					}
				}
			}
		}

		public boolean isLocal() {
			return local;
		}

		/**
		 * Sets whether this query should be executed on local node only.
		 *
		 * @param local
		 * @return {@code this} for chaining.
		 */
		public Builder setLocal(boolean local) {
			this.local = local;
			return this;
		}

		public boolean isAffinityRun() {
			return affinityRun;
		}

		/**
		 * Sets whether this query should be executed on the node where data for provided {@code affinityKey} is located
		 *
		 * @param affinityRun
		 * @return {@code this} for chaining.
		 * @see org.hibernate.ogm.datastore.ignite.options.CollocatedAssociation
		 */
		public Builder setAffinityRun(boolean affinityRun) {
			this.affinityRun = affinityRun;
			return this;
		}

		public Object getAffinityKey() {
			return affinityKey;
		}

		/**
		 * @param affinityKey
		 * @return {@code this} for chaining.
		 */
		public Builder setAffinityKey(Object affinityKey) {
			this.affinityKey = affinityKey;
			return this;
		}

		public QueryHints build() {
			if ( affinityRun && affinityKey == null ) {
				throw new HibernateException( "AffinityKey can't be null" );
			}
			return new QueryHints( local, affinityRun, affinityKey );
		}
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Utility methods to simplify CRUD operations on groups of entities.
 *
 * @author Davide D'Alto
 */
public class SessionHelper {

	public static void persist(SessionFactory sessions, Object... entities) {
		final Session session = sessions.openSession();
		persist( session, entities );
	}

	public static void persist(Session session, Object... entities) {
		Transaction transaction = session.beginTransaction();

		for ( Object entity : entities ) {
			session.persist( entity );
		}

		transaction.commit();
		session.close();
	}

	public static void delete(SessionFactory sessions, Class<?> entityClass, Serializable... ids) {
		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		for ( Serializable id : ids ) {
			session.delete( session.load( entityClass, id ) );
		}

		transaction.commit();
		session.close();
	}

	public static List<ProjectionResult> asProjectionResults(Session session, String projectionQuery) {
		List<?> results = session.createQuery( projectionQuery ).list();
		List<ProjectionResult> projectionResults = new ArrayList<ProjectionResult>();

		for ( Object result : results ) {
			if ( !( result instanceof Object[] ) ) {
				throw new IllegalArgumentException( "No projection result: " + result );
			}
			projectionResults.add( ProjectionResult.forArray( (Object[]) result ) );
		}

		return projectionResults;
	}

	public static class ProjectionResult {

		private Object[] elements;

		public ProjectionResult(Object... elements) {
			this.elements = elements;
		}

		public static ProjectionResult forArray(Object[] element) {
			ProjectionResult result = new ProjectionResult();
			result.elements = element;
			return result;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode( elements );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			ProjectionResult other = (ProjectionResult) obj;
			if ( !Arrays.equals( elements, other.elements ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return Arrays.deepToString( elements );
		}
	}

}

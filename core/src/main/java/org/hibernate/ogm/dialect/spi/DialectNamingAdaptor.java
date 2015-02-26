/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

/**
 * The Dialect can validate or adjust the names which will be used to actually persist into the database "tables".
 *
 * @author Sanne Grinovero
 */
public interface DialectNamingAdaptor {

	/**
	 * If the proposed name is valid, the implementor should return it as-is.
	 * If the dialect can apply a sensible and non-ambiguous transformation - such as escaping - it should apply it and
	 * return the transformed string; optionally warning about it.
	 * If the proposed table name is not acceptable and there is no safe escaping the dialect should throw an
	 * org.hibernate.MappingException and provide a user friendly explanation, possibly suggesting to use the JPA Table
	 * annotation to provide a safe alternative explicitly.
	 *
	 * @param requestedName the generated default, or as requested by the user mapping
	 * @return a valid "table" name, potentially the same as requestedName
	 * @throws org.hibernate.MappingException
	 */
	String makeValidTableName(String requestedName);

}

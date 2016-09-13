/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

/**
 * Implementations should also implement equals() so that
 * we can validate against conflicting types being defined:
 * the name should be unique, but it is possible that multiple types
 * will attempt to register a same-named but different type definition,
 * and this should be reported as an error.
 * For example, for enums we generate types using the short name of the
 * class; ignoring package name that might be ambiguous.
 *
 * @author Sanne Grinovero
 */
public interface TypeDefinition {

	void exportProtobufTypeDefinition(StringBuilder sb);

	String getTypeName();

}

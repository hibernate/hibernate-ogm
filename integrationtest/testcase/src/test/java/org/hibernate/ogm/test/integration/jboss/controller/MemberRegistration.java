/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss.controller;

import org.hibernate.ogm.test.integration.jboss.model.Member;

/**
 * Represents a class used to register and find club's members.
 *
 * @author Davide D'Alto
 */
public interface MemberRegistration {

	Member getNewMember();

	void register();

	Member find(Long id);

	Member findWithQuery(Long id);

	Member findWithEmail(String string);

	Member findWithNativeQuery(String nativeQuery);

	void close();
}

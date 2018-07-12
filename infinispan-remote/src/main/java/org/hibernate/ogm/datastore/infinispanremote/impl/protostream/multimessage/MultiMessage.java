/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream.multimessage;

import java.io.Serializable;

/**
 * Could have more than one Protocol Buffer message types representations.
 *
 * The final message type couldn't be derived from class.
 * On the other hand it could discovered on instances,
 * using {@link MultiMessage#getMessageType()}.
 *
 * @author Fabio Massimo Ercoli
 */
public interface MultiMessage extends Serializable {

	String getMessageType();
}

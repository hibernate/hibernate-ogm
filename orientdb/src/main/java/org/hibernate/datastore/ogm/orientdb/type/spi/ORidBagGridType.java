 /*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 * 
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.datastore.ogm.orientdb.type.spi;

import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import org.hibernate.MappingException;
import org.hibernate.datastore.ogm.orientdb.type.descriptor.java.ORidBagTypeDescriptor;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

/**
 *
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */


public class ORidBagGridType extends AbstractGenericBasicType<ORidBag> {
    public static final ORidBagGridType INSTANCE = new ORidBagGridType();

    public ORidBagGridType() {
        super(PassThroughGridTypeDescriptor.INSTANCE, ORidBagTypeDescriptor.INSTANCE);
    }
    
    @Override
    public int getColumnSpan(Mapping mapping) throws MappingException {
        return 1;
    }

    @Override
    public String getName() {
        return "ORidBag";
    }
    
}

package org.hibernate.ogm.type;

import org.hibernate.service.Service;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface TypeTranslator extends Service {
	GridType getType(Type type);
}

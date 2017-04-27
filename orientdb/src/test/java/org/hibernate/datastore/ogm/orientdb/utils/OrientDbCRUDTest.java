/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.utils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hibernate.datastore.ogm.orientdb.OrientDBSimpleTest.MEMORY_TEST;
import org.hibernate.ogm.backendtck.simpleentity.CRUDTest;
import org.junit.BeforeClass;

/**
 * @author Davide D'Alto
 */
public class OrientDbCRUDTest extends CRUDTest {

    private static final Logger LOG = Logger.getLogger(OrientDbCRUDTest.class.getName());
    @BeforeClass
	public static void setUpClass() {
		LOG.log(Level.INFO, "call me!!!");
        }
    @Override
    protected void configure(Map<String, Object> cfg) {
        LOG.log(Level.INFO, "call me!!!");
        MemoryDBUtil.createDbFactory(MEMORY_TEST);
        super.configure(cfg); //To change body of generated methods, choose Tools | Templates.
    }

    
    
    
            
		//MemoryDBUtil.createDbFactory(MEMORY_TEST);
//		BasicConfigurator.configure();
                //super.setUp();
	
    


	/*public  void tearDownClass() {
            System.out.println("tearDownClass");
                MemoryDBUtil.recrateInMemoryDn(MEMORY_TEST);
                
	} */
    

}

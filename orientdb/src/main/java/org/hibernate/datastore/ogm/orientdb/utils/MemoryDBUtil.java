/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 * 
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.utils;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */
public class MemoryDBUtil {

	private static final Logger LOG = Logger.getLogger( MemoryDBUtil.class.getName() );
	private static OrientGraphFactory factory;
        
        
        
        public static OrientGraphNoTx recrateInMemoryDn(String url ) {
                if (getOrientGraphFactory().exists()) {
                        getOrientGraphFactory().drop();
                        getOrientGraphFactory().close();
                }                
                return createDbFactory(url);
        };

	@Deprecated
	private static void addCommonProperties(OrientGraphNoTx graph, OrientVertexType type) {
		type.createProperty( "bKey", OType.LONG );
		LOG.log( Level.INFO, "Create unique index like primary key for {0}", type.getName() );
		String indexCommand = "CREATE INDEX " + type.getName() + "PrimaryIndex ON " + type.getName() + "(bKey) UNIQUE";
		LOG.log( Level.INFO, "Command {0}", indexCommand );
		graph.command( new OCommandSQL( indexCommand ) ).execute();
		LOG.log( Level.INFO, "Create sequence for {0}", type.getName() );
		String seqCommand = "CREATE SEQUENCE seq_" + type.getName().toLowerCase() + "_bkey TYPE ORDERED START 2";
		LOG.log( Level.INFO, "Command {0}", seqCommand );
		graph.command( new OCommandSQL( seqCommand ) ).execute();

	}

	public static OrientGraphNoTx createDbFactory(String url) {
		factory = new OrientGraphFactory( url );
		// see https://github.com/orientechnologies/orientdb/issues/5688
		factory.setStandardElementConstraints( false );
		return factory.getNoTx();
	}

	public static OrientGraphFactory getOrientGraphFactory() {
		return factory;
	}

	@Deprecated
	public static Map<String, List<ORecordId>> prepareDb(String url) {

		Map<String, List<ORecordId>> idMap = new HashMap<>();
		try {
			// System.setProperty("ORIENTDB_HOME", "./target");
			OrientGraphNoTx graph = createDbFactory( url );

			// vertex classes
			OrientVertexType pizzaType = graph.createVertexType( "Pizza" );
			addCommonProperties( graph, pizzaType );
			pizzaType.createProperty( "name", OType.STRING );
			for ( OProperty p : pizzaType.declaredProperties() ) {
				LOG.log( Level.INFO, "Property: {0}", p );
			}

			// INSERT INTO Account SET id = sequence('mysequence').next()
			OrientVertexType buyingOrderType = graph.createVertexType( "BuyingOrder" );
			addCommonProperties( graph, buyingOrderType );
			buyingOrderType.createProperty( "orderKey", OType.STRING );
			OrientVertexType orderItemType = graph.createVertexType( "OrderItem" );
			orderItemType.createProperty( "cost", OType.DECIMAL );

			OrientVertexType customerType = graph.createVertexType( "Customer" );
			addCommonProperties( graph, customerType );
			customerType.createProperty( "name", OType.STRING );

			// create vertex
			Vertex pizza = graph.addVertex( "class:Pizza" );
			pizza.setProperty( "bKey", Long.valueOf( 1L ) );
			pizza.setProperty( "name", "Super Papa" );
			System.out.println( "pizza.getId():" + pizza.getId() );
			List<ORecordId> pizzaRids = new LinkedList<>();
			pizzaRids.add( (ORecordId) pizza.getId() );
			idMap.put( "Pizza", pizzaRids );

			Vertex customer = graph.addVertex( "class:Customer" );
			customer.setProperty( "bKey", Long.valueOf( 1L ) );
			customer.setProperty( "name", "Ivahoe" );
			System.out.println( "customer.getId():" + customer.getId() );
			List<ORecordId> customerRids = new LinkedList<>();
			customerRids.add( (ORecordId) customer.getId() );
			idMap.put( "Customer", customerRids );

			Vertex buyingOrder = graph.addVertex( "class:BuyingOrder" );
			buyingOrder.setProperty( "orderKey", "2233" );
			buyingOrder.setProperty( "bKey", Long.valueOf( 1L ) );
			System.out.println( "order.getId():" + buyingOrder.getId() );
			List<ORecordId> orderRids = new LinkedList<>();
			orderRids.add( (ORecordId) buyingOrder.getId() );
			idMap.put( "BuyingOrder", orderRids );

			Vertex orderItem = graph.addVertex( "class:OrderItem" );
			orderItem.setProperty( "bKey", Long.valueOf( 1L ) );
			orderItem.setProperty( "orderKey", "2233" );
			orderItem.setProperty( "cost", new BigDecimal( "10.2" ) );
			System.out.println( "orderItem.getId():" + orderItem.getId() );
			List<ORecordId> orderItemRids = new LinkedList<>();
			orderItemRids.add( (ORecordId) orderItem.getId() );
			idMap.put( "OrderItem", orderItemRids );

			// create edges
			graph.addEdge( null, pizza, orderItem, "Buying" );
			graph.addEdge( null, buyingOrder, orderItem, "Order" );
			graph.addEdge( null, customer, buyingOrder, "Owner" );

		}
		catch (Exception e) {
			LOG.log( Level.SEVERE, "Error", e );
		}
		return idMap;
	}

}

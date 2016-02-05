package org.hibernate.ogm.datastore.ignite.test.criteria;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.ignite.test.BaseIgniteTest;
import org.hibernate.ogm.datastore.ignite.test.Client;
import org.hibernate.ogm.datastore.ignite.test.Deposit;
import org.hibernate.ogm.datastore.ignite.test.ObjectId;
import org.hibernate.transform.ResultTransformer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IgniteProjectionTest extends BaseIgniteTest {

	private static final Logger log = LoggerFactory.getLogger(IgniteProjectionTest.class);
	
	@Test
	@Ignore
	public void test() throws Exception {
		log.info("==> test()");
		
		OgmSession session = openSession();
		ObjectId personId = new ObjectId(38, 2222, 3333);
		
		Client oldClient = (Client)session.get(Client.class, personId);
		if (oldClient != null)
			testRemove(session, oldClient);
		
		Client client = new Client(personId.toString(), "Criteria client", "Клиента для теста Criteria");
		testInsert(session, client);
		
		Criteria criteria = session.createCriteria(Client.class);
		criteria.setProjection(Projections.projectionList()
								.add(Projections.property("id")))
								.setResultTransformer(
					new ResultTransformer()
					{
						private static final long serialVersionUID = -7196534330429544778L;

						@Override
						@SuppressWarnings("rawtypes")
						public List transformList(List collection)
						{
							return collection;
						}
						@Override
						public Object transformTuple(Object[] tuple, String[] aliases)
						{
							return (String)tuple[0];
						}
					}
				);
		
		criteria.add(Restrictions.eq("id.megaId", 38))
				.add(Restrictions.like("name", "%client"));
		
		List list = criteria.list();
		
		Assert.assertTrue("Неверное количество найденных клиентов", list.size() > 0);
		
		log.info("<== test()");
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Client.class, Deposit.class};
	}

}

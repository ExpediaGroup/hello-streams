package com.homeaway.streamplatform.hellostreams.orderprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTest;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import com.homeaway.streamplatform.hellostreams.orderprocessor.dao.CustomerDao;
import com.homeaway.streamplatform.hellostreams.orderprocessor.dao.OrderDao;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@GraphQLTest
@RunWith(SpringRunner.class)
@Slf4j
public class OrderProcessorTests {

    private static final String NEW_CUSTOMER_ID = "35442a6a-ad7a-4e72-8af9-5611254306a6";
    @Resource
    private GraphQLTestTemplate graphQLTemplate;

    @Resource
    private OrderDao orderDao;

    @Resource
    private CustomerDao customerDao;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void resetDao() {
        orderDao.clearDB();
        customerDao.clearDB();
    }

    @Test
    public void placeOrderNewCustomer() throws Exception {
        // setup variables
        ObjectNode vars = getPlaceOrderVars(NEW_CUSTOMER_ID, "Latte");

        // place Order
        CustomerDao customerDao = new CustomerDao();
        assertThat(customerDao.getCustomer(NEW_CUSTOMER_ID), is(nullValue()));

        GraphQLResponse response = perform("placeOrder.mutation", vars);
        assertThat(response.get("data.placeOrder.customerId", String.class), is(NEW_CUSTOMER_ID));
    }

    @Test
    public void verifyOrderQuery() throws Exception {
        GraphQLResponse response = perform("getOrders.query", null);
        assertThat(response.get( "data.orders", List.class).size(), is(0));

        // create order
        ObjectNode vars = getPlaceOrderVars(NEW_CUSTOMER_ID, "Latte");
        response = perform("placeOrder.mutation", vars);
        assertThat(response.get("data.placeOrder.customerId", String.class), is(NEW_CUSTOMER_ID));

        // verify order is in the list
        response = perform("getOrders.query", null);
        assertThat(response.get( "data.orders", List.class).size(), is(1));
        assertThat(response.get( "data.orders[0].item", String.class), is("Latte"));
        assertThat(response.get( "data.orders[0].state", String.class), is("PLACED"));
    }

    private GraphQLResponse perform(String gqlResource, ObjectNode vars) throws IOException {
        GraphQLResponse response = graphQLTemplate.perform(gqlResource, vars);
        assertThat(response.getStatusCode().value(), is(200));
        if(didGraphQLFail(response)) {
            log.error("GraphQL failed. response={}", response.readTree().toString());
            throw new IllegalStateException("Did not expect graphQL to fail");
        }
        return response;
    }

    private ObjectNode getPlaceOrderVars(String customerId, String item) {
        ObjectNode vars = mapper.createObjectNode();
        vars.put("customerId", customerId);
        vars.put( "item", item);
        return vars;
    }

    private boolean didGraphQLFail(GraphQLResponse response) throws IOException {
        return response.readTree().get("errors") != null;
    }
}

package com.homeaway.streamplatform.hellostreams.orderprocessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTest;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import com.homeaway.streamplatform.hellostreams.orderprocessor.dao.CustomerDao;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@GraphQLTest
@RunWith(SpringRunner.class)
@Slf4j
public class OrderProcessorTests {

    private static final String INVALID_CUSTOMER_ID = "35442a6a-ad7a-4e72-8af9-5611254306a6";
    @Resource
    private GraphQLTestTemplate graphQLTemplate;

    @Test
    public void placeOrderValidCustomer() throws Exception {
        // setup variables
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode varsNode = mapper.createObjectNode();
        varsNode.put("customerId", CustomerDao.CUSTOMER_TEST_ID);
        varsNode.put( "item", "Latte");

        // place Order
        GraphQLResponse response = graphQLTemplate.perform("placeOrder.mutation", varsNode);
        log.info("statusCode={}", response.getStatusCode());
        JsonNode jsonNode = response.readTree();
        assertThat(response.getStatusCode().value(), is(200));
        assertThat(jsonNode.get("errors"), is(nullValue()));
        assertThat(jsonNode.get("data"), is(notNullValue()));
        assertThat(jsonNode.get("data").get("placeOrder"), is(notNullValue()));
        assertThat(response.get("data.placeOrder.customerId", String.class), is(CustomerDao.CUSTOMER_TEST_ID));
    }

    @Test
    public void placeOrderInvalidCustomer() throws Exception {
        // setup variables
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode varsNode = mapper.createObjectNode();
        varsNode.put("customerId", INVALID_CUSTOMER_ID);
        varsNode.put( "item", "Latte");

        // place Order
        GraphQLResponse response = graphQLTemplate.perform("placeOrder.mutation", varsNode);
        log.info("statusCode={}", response.getStatusCode());
        JsonNode jsonNode = response.readTree();
        assertThat(response.getStatusCode().value(), is(200));
        assertThat(jsonNode.get("errors"), is(notNullValue()));
        assertThat(jsonNode.get("data").isNull(), is(true));
        assertThat(response.get("errors[0].message", Object.class).toString(), containsString(INVALID_CUSTOMER_ID));
        assertThat(response.get("errors[0].message", Object.class).toString(), containsString("is non-existent"));
    }
}
